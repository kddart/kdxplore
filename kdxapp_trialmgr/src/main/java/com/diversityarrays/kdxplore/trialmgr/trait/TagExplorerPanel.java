/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017  Diversity Arrays Technology, Pty Ltd.
    
    KDXplore may be redistributed and may be modified under the terms
    of the GNU General Public License as published by the Free Software
    Foundation, either version 3 of the License, or (at your option)
    any later version.
    
    KDXplore is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with KDXplore.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.diversityarrays.kdxplore.trialmgr.trait;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import com.diversityarrays.kdsmart.KDSmartApplication;
import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.TagChangeListener;
import com.diversityarrays.kdsmart.db.csvio.ImportError;
import com.diversityarrays.kdsmart.db.entities.Tag;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.util.ProgressReporter;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.OfflineData;
import com.diversityarrays.kdxplore.data.OfflineDataChangeListener;
import com.diversityarrays.kdxplore.model.TagTableModel;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.RunMode;

import android.content.Context;
import net.pearcan.dnd.ChainingTransferHandler;
import net.pearcan.dnd.DropLocationInfo;
import net.pearcan.dnd.FileDrop;
import net.pearcan.dnd.FileListTransferHandler;
import net.pearcan.dnd.TableTransferHandler;
import net.pearcan.ui.FileChooserFactory;
import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.PromptScrollPane;
import net.pearcan.util.StringUtil;

public class TagExplorerPanel extends JPanel {
			
	private static final String TAG_HELP = "Double-click on 'Description' to edit";

	boolean developer = RunMode.getRunMode().isDeveloper();

	private final FileDrop fileDrop = new FileDrop() {
		@Override
		public void dropFiles(Component arg0, List<File> files, DropLocationInfo arg2) {
			for (File f : files) {
				if  (FileChooserFactory.CSV_FILE_FILTER.accept(f)) {
					doImportTagsFile(f);
					break;
				}
			}
		}
	};
	
	private final FileListTransferHandler flth = new FileListTransferHandler(fileDrop);

	private final Action importTagsAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			File file = Shared.chooseFileToOpen(TagExplorerPanel.this, FileChooserFactory.CSV_FILE_FILTER);
			if (file != null) {
				doImportTagsFile(file);
			}
		}
	};
	
	private Action deleteTagsAction = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			List<Integer> modelRows = GuiUtil.getSelectedModelRows(tagsTable);
			List<Tag> tagsToCheck = new ArrayList<>();
			for (Integer row : modelRows) {
				tagsToCheck.add(tagTableModel.getEntityAt(row));
			}
			
			if (! tagsToCheck.isEmpty()) {
				KDSmartDatabase kdsDb = offlineData.getKdxploreDatabase().getKDXploreKSmartDatabase();
				List<Tag> okToDelete = new ArrayList<>();
				Map<Tag, List<Trial>> dontDelete = new HashMap<Tag, List<Trial>>();
				
				try {

					checkOnTagUsage(kdsDb, tagsToCheck, okToDelete, dontDelete);
					
					if (okToDelete.isEmpty()) {
						StringBuilder sb = new StringBuilder("<HTML>None of the selected Tags may be removed");
						appendTable(sb, dontDelete);
						JOptionPane.showMessageDialog(TagExplorerPanel.this, 
								sb.toString(), 
								"Remove Tags", JOptionPane.WARNING_MESSAGE);
					}
					else {
						Collections.sort(okToDelete);
						StringBuilder sb = new StringBuilder("<HTML><B>Tags to Remove</B><UL>");
						for (Tag tag: okToDelete) {
							sb.append("<LI>")
								.append(StringUtil.htmlEscape(tag.getLabel()))
								.append("</LI>");
						}
						sb.append("</UL>");
						
						if (! dontDelete.isEmpty()) {
							sb.append("<B>These will not be removed</B>");
							appendTable(sb, dontDelete);
						}
						
						if (JOptionPane.YES_OPTION ==
								JOptionPane.showConfirmDialog(TagExplorerPanel.this,
										sb.toString(), 
										"Confirm Tag Removal", 
										JOptionPane.YES_NO_OPTION))
						{
							int[] tagsIds = new int[okToDelete.size()];
							for (int index = okToDelete.size(); --index >= 0; ) {
								tagsIds[index] = okToDelete.get(index).getTagId();
							}
							kdsDb.removeTags(tagsIds);
						}
					}
					
				} catch (IOException e1) {
					GuiUtil.errorMessage(TagExplorerPanel.this, e1, "Error Removing Tags");
				}
			}
		}
		
		
		private void appendTable(StringBuilder sb, Map<Tag, List<Trial>> dontDelete) {
			sb.append("<TABLE border='1'>");
			sb.append("<TR><TH>Trait</TH><TH>Trial Count</TH></TR>");
			for (Tag t : dontDelete.keySet()) {
				sb.append("<TR>")
					.append("<TD>")
					.append(StringUtil.htmlEscape(t.getLabel()))
					.append("</TD>")
					.append("<TD>")
					.append(dontDelete.get(t).size()).append("</TD>")
					.append("</TR>");
			}
			sb.append("</TABLE>");
		}

		private void checkOnTagUsage(KDSmartDatabase db,
	            List<Tag> tagsToCheck,
	            List<Tag> okToDelete,
	            Map<Tag,List<Trial>> dontDelete) throws IOException
	    {            
	        
			int[] tagsIds = new int[tagsToCheck.size()];
			
	        for (int i=0;i<tagsToCheck.size();i++ ) {
	        	tagsIds[i] = tagsToCheck.get(i).getTagId();
	        }
	        	
	       
	        Map<Integer, Set<Trial>>  trialsByTagId = db.getTrialsUsedByTagId(tagsIds);

	        for (Tag tag : tagsToCheck) {
	            Set<Trial> trialSet = trialsByTagId.get(tag.getTagId());
	            if (trialSet.isEmpty()) {
	                okToDelete.add(tag);
	            }
	            else {
	                List<Trial> list = new ArrayList<>(trialSet);
	                Collections.sort(list, Trial.TRIAL_TITLE_COMPARATOR);
	                dontDelete.put(tag, list);
	            }
	        }
	    }
	};
	
	private final Action addTagsAction = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final TagAddDialog addTagsDialog = new TagAddDialog (GuiUtil.getOwnerWindow(TagExplorerPanel.this),
					tagTableModel.getAllTags(),
					offlineData.getKdxploreDatabase().getKDXploreKSmartDatabase());

			GuiUtil.centreOnOwner(addTagsDialog);
			addTagsDialog.setVisible(true);
		}
	};
	
	private TagTableModel tagTableModel = new TagTableModel();
	private JTable tagsTable = new JTable(tagTableModel) {

		@Override
		public String getToolTipText(MouseEvent event) {
			if (! developer) {
				return super.getToolTipText(event);
			}

			Point pt = event.getPoint();
			if (pt != null) {
				int vrow = rowAtPoint(pt);
				if (vrow >= 0) {
					int mrow = convertRowIndexToModel(vrow);
					if (mrow >= 0) {
						TableModel model = getModel();
						if (model instanceof TagTableModel) {
							TagTableModel ttm = (TagTableModel) model;
							Tag tag = ttm.getEntityAt(mrow);
							return "Tag ID=" + tag.getTagId();
						}
					}
				}
			}
			return super.getToolTipText(event);
		}
		
	};

	private final OfflineData offlineData;
	
	private final OfflineDataChangeListener offlineDataListener = new OfflineDataChangeListener() {
		@Override
		public void trialUnitsAdded(Object source, int trialId) {
		}
		
		// TODO NEED TO Add OfflineDataChangeListener.tagsAdded(Source, Trait[])
		//    tagChanged(Source, Tag)
		//    tagsRemoved(Integer[] tagIds);
		
		@Override
		public void offlineDataChanged(Object source, String reason, KdxploreDatabase oldDb, KdxploreDatabase newDb) {
			if (oldDb != null) {
				oldDb.removeEntityChangeListener(tagChangeListener);
				
			}
			if (newDb != null) {
				newDb.addEntityChangeListener(tagChangeListener);
			}			
			tagTableModel.setKDSmartDatabase(newDb==null ? null : newDb.getKDXploreKSmartDatabase());
			refreshTagsTable();
		}
	};

	
	private final JLabel detailsPane = new JLabel("You need to define some Tags");
	private JSplitPane splitPane;

	private TagChangeListener tagChangeListener = new TagChangeListener() {

		@Override
		public void entityAdded(KDSmartDatabase db, Tag tag) {
			if (tagTableModel.getRowCount()<=0) {
				detailsPane.setText(TAG_HELP);
			}
			tagTableModel.addTag(tag);
		}

		@Override
		public void entityChanged(KDSmartDatabase db, Tag tag) {
			tagTableModel.tagChanged(tag);
		}

		@Override
		public void entitiesRemoved(KDSmartDatabase db, Set<Integer> tagIds) {
			tagTableModel.removeTags(tagIds);
		}

		@Override
		public void listChanged(KDSmartDatabase db, int nChanges) {
			refreshTagsTable();
		}
	};

	public TagExplorerPanel(OfflineData offlineData) 
	{
		super(new BorderLayout());
		
		this.offlineData = offlineData;
		this.offlineData.addOfflineDataChangeListener(offlineDataListener);
		
		KDClientUtils.initAction(ImageId.ADD_TRIALS_24, importTagsAction, "Import Tags");		
		KDClientUtils.initAction(ImageId.PLUS_BLUE_24, addTagsAction,"Add Tags");
		KDClientUtils.initAction(ImageId.TRASH_24, deleteTagsAction, "Remove Tags");
		deleteTagsAction.setEnabled(false);
		
		add(new JLabel("Tags go here"), BorderLayout.CENTER);
		
		tagsTable.setAutoCreateRowSorter(true);
		tagsTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		tagsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (! e.getValueIsAdjusting()) {
					int rowCount = tagsTable.getSelectedRowCount();
					deleteTagsAction.setEnabled(rowCount > 0);
					
					if (1 == tagsTable.getSelectedRowCount()) {
						Tag tag = null;
						int vrow = tagsTable.getSelectedRow();
						if (vrow >= 0) {
							int mrow = tagsTable.convertRowIndexToModel(vrow);
							if (mrow >= 0) {
								tag = tagTableModel.getEntityAt(mrow);
							}
						}
						showTagDetails(tag);
					}
					else {
						detailsPane.setText(TAG_HELP);
					}
				}
			}
		});
		
		PromptScrollPane scrollPane = new PromptScrollPane(tagsTable, "Drag/Drop Tag CSV file or use 'Import Tags'");
		
		TableTransferHandler tth = TableTransferHandler.initialiseForCopySelectAll(tagsTable, true);
		tagsTable.setTransferHandler(new ChainingTransferHandler(flth, tth));
		
		scrollPane.setTransferHandler(flth);

		Box top = Box.createHorizontalBox();
		top.add(new JButton(importTagsAction));
		top.add(new JButton(addTagsAction));
		top.add(Box.createHorizontalGlue());
		top.add(new JButton(deleteTagsAction));
		

		JPanel left = new JPanel(new BorderLayout());
		left.add(top, BorderLayout.NORTH);
		left.add(scrollPane, BorderLayout.CENTER);
		
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, detailsPane);
		splitPane.setOneTouchExpandable(true);
		
		add(splitPane, BorderLayout.CENTER);
	}
	
	private void doImportTagsFile(File file) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				doImportTagsFileImpl(file);
			}
		});
	}

	private void doImportTagsFileImpl(File file) {
		Context context = KDSmartApplication.getInstance();
		final ProgressMonitor monitor = new ProgressMonitor(TagExplorerPanel.this, "Loading", "", 0, 100);
		ProgressReporter progressReporter = new ProgressReporter() {
			
			@Override
			public void setProgressNote(String note) {
				monitor.setNote(note);
			}
			
			@Override
			public void setProgressMaximum(int max) {
				monitor.setMaximum(max);
			}
			
			@Override
			public void setProgressCount(int count) {
				monitor.setProgress(count);
			}
			
			@Override
			public void dismissProgress() {
				monitor.close();
			}
		};

		Either<ImportError, Integer> either = offlineData.getKdxploreDatabase()
			.getKDXploreKSmartDatabase().importTagsFile(context, file, progressReporter);

		if (either.isLeft()) {
			ImportError ie = either.left();
			MsgBox.error(TagExplorerPanel.this, ie.getMessage("Import Tags"), "Import Failed");
		}
		else {
			refreshTagsTable();
			Integer count = either.right();
			MsgBox.info(TagExplorerPanel.this, count + " Tags imported", "Import Complete");
		}
	}

	public void doPostOpenActions() {
		splitPane.setDividerLocation(0.4);
	}
	
	private void refreshTagsTable() {
	    // In case we get called before the variable is assigned
	    // (from offlineDataChanged)
	    if (tagTableModel == null) {
	        return;
	    }
        KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();
        if (kdxdb != null) {
    		try {
    		    tagTableModel.setData(kdxdb.getKDXploreKSmartDatabase().getAllTags());
    		} catch (IOException e) {
    		    Shared.Log.e("TagExplorerPanel", "refreshTagsTable", e);
    		    MsgBox.error(TagExplorerPanel.this, e, "refreshTagsTable - Database Error");
    		}
        }
        if (tagTableModel.getRowCount() <= 0) {
            detailsPane.setText(TAG_HELP);
        }
	}

	private void showTagDetails(Tag tag) {
		// TODO Auto-generated method stub
		
	}
}

