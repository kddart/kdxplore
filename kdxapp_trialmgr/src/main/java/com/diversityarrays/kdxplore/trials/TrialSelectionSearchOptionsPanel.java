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
package com.diversityarrays.kdxplore.trials;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Transformer;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.renderer.CellContext;
import org.jdesktop.swingx.renderer.CheckBoxProvider;
import org.jdesktop.swingx.renderer.ComponentProvider;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.LabelProvider;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import com.diversityarrays.dalclient.DALClient;
import com.diversityarrays.kdxplore.data.tool.LoadState;
import com.diversityarrays.kdxplore.data.tool.LookupRecord;
import com.diversityarrays.kdxplore.data.tool.LookupTable;
import com.diversityarrays.kdxplore.data.tool.LookupTable.Chunk;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.Pair;

import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.widget.PromptScrollPane;
import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.BackgroundTask;

/**
 * Present a tree of the attributes used for searching for Trials in KDDart.
 * <p>
 * Need to:
 * <ol>
 *  <li>Provide support for Trial Factors</li>
 *  <li>Allow provide a configuration file to specify which Factor Names are used for filtering</li>
 *  <li>Retrieve "unique factor values" query <code>list/trial</code></li>
 *  <li>Support list/trial Filtering by Trial Factor/Value</li>
 * </ol>
 * @author brian
 *
 */
// TODO implement the changes in the comments above
public class TrialSelectionSearchOptionsPanel extends JPanel implements TrialSearchOptionsPanel {

	private static final int CHUNK_SIZE = 200;

	static private enum InitState {
		VIRGIN,
		INITIALISING,
		INCOMPLETE,
		DONE
	}

	private Map<LookupTable<?>, LookupTableNode> lookupTableRootNodeByTable = new HashMap<LookupTable<?>, LookupTableNode>();

	private DefaultTreeTableModel treeTableModel;
	private JXTreeTable treeTable;
	private PromptScrollPane promptScrollPane;

	// TODO make this visible as a button when we begin
	@SuppressWarnings("unused")
	private Action cancelLoadingAction = new AbstractAction("Cancel Loading") {
		@Override
		public void actionPerformed(ActionEvent e) {
			cancelRequested = true;
			setEnabled(false);
		}

	};
	private boolean cancelRequested = false;

	@SuppressWarnings("unused")
	private TrialSelectionSearchOptionsPanel.InitState lookupsInitialised = InitState.VIRGIN;

	private final List<LookupTable<?>> lookupTables = new ArrayList<>();
	private final BackgroundRunner backgroundRunner;

	private TrialSelectionSearchOptionsPanel(
	        BackgroundRunner backgroundRunner, 
	        String matchAttribute, 
	        List<Pair<String,LookupTable<?>>> lookupInfo) 
	{
		super(new BorderLayout());
		
		this.backgroundRunner = backgroundRunner;

		MutableTreeTableNode root = new AbstractMutableTreeTableNode() {
			@Override
			public Object getValueAt(int column) {
				return null;
			}

			@Override
			public int getColumnCount() {
				return 1;
			}
		};

		for (Pair<String, LookupTable<?>> pair : lookupInfo) {
		    String nodeName = pair.first;
		    LookupTable<?> lt = pair.second;
            lookupTables.add(lt);

			LookupTableNode node = new LookupTableNode(nodeName, lt);
			lookupTableRootNodeByTable.put(lt, node);
			root.insert(node, root.getChildCount());
		}

		List<String> columnNames = Arrays.asList(new String[] {
			matchAttribute, 
			"Use?"
		});
//		List<String> columnNames = Arrays.asList("Match Attribute,Use?,Note".split(","));
//		List<String> columnNames = Arrays.asList("Filter,Use?,Information".split(","));
		treeTableModel = new DefaultTreeTableModel(root, columnNames) {

			@Override
			public Class<?> getColumnClass(int column) {
				if (column==1) {
					return Boolean.class;
				}
				return String.class;
			}
			
			@Override
			public boolean isCellEditable(Object node, int col) {
				boolean result = false;
				if (node instanceof EntryNode) {
					result = ( col==1 );
				}
//				System.out.println("isCellEditable("+(node==null?"null":node.getClass().getName())+" , "+col+") == "+result);
				return result;
			}

			@Override
			public void setValueAt(Object value, Object node, int column) {
				if (node instanceof EntryNode) {
					if (column==1 && value instanceof Boolean) {
						EntryNode enode = (EntryNode) node;
						enode.chosen = ((Boolean) value).booleanValue();
						fireChoiceChanged();
					}
				}
			}
			
			
		};
		treeTable = new JXTreeTable(treeTableModel);
		treeTable.setLeafIcon(null);

		promptScrollPane = new PromptScrollPane(treeTable, "Not Loaded");

		add(promptScrollPane, BorderLayout.CENTER);
		

		treeTable.setDefaultRenderer(Boolean.class, 
				new DefaultTableRenderer(new MyComponentProvider()));
	}
	
	@Override
	public void addSearchOptionsChangeListener(SearchOptionsChangeListener l) {
		listenerList.add(SearchOptionsChangeListener.class, l);
	}

	@Override
	public void removeSearchOptionsChangeListener(SearchOptionsChangeListener l) {
		listenerList.remove(SearchOptionsChangeListener.class, l);
	}
	
	protected void fireChoiceChanged() {
		for (SearchOptionsChangeListener l : listenerList.getListeners(SearchOptionsChangeListener.class)) {
			l.choiceChanged();
		}
	}
	protected void fireLookupsLoaded() {
		for (SearchOptionsChangeListener l : listenerList.getListeners(SearchOptionsChangeListener.class)) {
			l.lookupsLoaded();
		}
	}

	public void setDALClient(DALClient client) {

		if (client==null) {
			lookupsInitialised = InitState.VIRGIN;
			for (LookupTableNode lookupRoot : lookupTableRootNodeByTable.values()) {
				lookupRoot.clear();
			}
		}
		else {
			initialiseLookups(client);
		}
	}

	private void initialiseLookups(final DALClient client) {

		lookupsInitialised = InitState.INITIALISING;
		
		// Returns the Set of LookupTables which were loaded
		// (if user cancelled then we don't get them all)
		// TODO make this fact visible !
		BackgroundTask<Set<LookupTable<?>>, Chunk<?>> task = new BackgroundTask<Set<LookupTable<?>>, Chunk<?>>("Loading Lookups...", false) {

			@Override
			public boolean onTaskStart() {
				// Do this otherwise when we twist it open after loading nothing happens!
				// TODO figure out why and fix it.
				treeTable.collapseAll();
				return true;
			}

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public Set<LookupTable<?>> generateResult(final Closure<Chunk<?>> publishPartial) throws Exception {
				Set<LookupTable<?>> processed = new HashSet<LookupTable<?>>();

				for (LookupTable lt : lookupTables) {
					LookupTableNode node = lookupTableRootNodeByTable.get(lt);
					node.clear();

					// This is the callback from getLookupRecords()
					Transformer<Chunk<?>,Boolean> consumer = new Transformer<Chunk<?>, Boolean>() {
						@Override
						public Boolean transform(Chunk<?> chunk) {
							publishPartial.execute(chunk);
							return ! cancelRequested;
						}
					};

					if (! lt.getLookupRecords(client, CHUNK_SIZE, consumer)) {
						break;
					}

					processed.add(lt);
				}
				lookupsInitialised = InitState.DONE;

				return processed;
			}

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void processPartial(List<Chunk<?>> chunks) {
				for (Chunk chunk : chunks) {
					LookupTable<?> lt = chunk.lookupTable;
					LookupTableNode node = lookupTableRootNodeByTable.get(lt);
					node.addRecords(chunk.payload);
				}
			}

			@SuppressWarnings("rawtypes")
			@Override
			public void onTaskComplete(Set<LookupTable<?>> processed) {
				for (LookupTable lt : lookupTableRootNodeByTable.keySet()) {
					LookupTableNode node = lookupTableRootNodeByTable.get(lt);
					node.establishLoadState(processed.contains(lt));
				}
//				treeTable.repaint();
				treeTable.expandAll();
				
				GuiUtil.initialiseTableColumnWidths(treeTable);
				
				fireLookupsLoaded();
//				TrialSelectionSearchOptionsPanel.this.firePropertyChange(PROP_LOOKUPS_LOADED, false, true);
			}

			@Override
			public void onException(Throwable cause) {
				cause.printStackTrace();
				MsgBox.error(TrialSelectionSearchOptionsPanel.this, cause, "Failed to load Lookup Tables");
			}

			@Override
			public void onCancel(CancellationException e) {
				throw new RuntimeException(e); // should never happen
			}

			@Override
			public void onInterrupt(InterruptedException e) {
				lookupsInitialised = InitState.INCOMPLETE;
				MsgBox.error(TrialSelectionSearchOptionsPanel.this, 
						"You chose to cancel the load operation",
						"Lookup Tables may be incomplete");
			}

		};
		

		backgroundRunner.runBackgroundTask(task);
	}

	
	class LookupTableNode extends AbstractMutableTreeTableNode {
		
		private LookupTable<?> lookupTable;
		private int columnCount;
		private LoadState loadState;
        private final String nodeName;

		public LookupTableNode(String nodeName, LookupTable<?> lookupTable) {
		    this.nodeName = nodeName;
			this.lookupTable = lookupTable;
			columnCount = 3; // lookupTable.hasNoteColumn() ? 3 : 2;
			loadState = LoadState.NOT_LOADED;
		}
		
		@Override
		public boolean isLeaf() {
			return false;
		}
		
		public String getFilteringClause() {
			String result = null;
			
			List<Integer> ids = new ArrayList<Integer>();
			int nChildren = getChildCount();
			for (int i = 0; i < nChildren; ++i) {
				EntryNode entryNode = (EntryNode) getChildAt(i);
				if (entryNode.chosen) {
					ids.add(entryNode.rec.id);
				}
			}
			
			if (! ids.isEmpty()) {
				if (ids.size()==1) {
					result = lookupTable.getIdColumnName() + "=" + ids.get(0); //$NON-NLS-1$
				}
				else {
					StringBuilder sb = new StringBuilder(lookupTable.getIdColumnName());
					String sep = " IN ("; //$NON-NLS-1$
					for (Integer id : ids) {
						sb.append(sep).append(id);
						sep = ","; //$NON-NLS-1$
					}
					sb.append(")"); //$NON-NLS-1$
					result = sb.toString();
				}
			}
			
			return result;
		}

		public void establishLoadState(boolean loadCompleted) {
			if (loadCompleted) {
				loadState = LoadState.COMPLETE;
			}
			else {
				loadState = getChildCount() > 0 ? LoadState.PARTIAL : LoadState.NOT_LOADED;
			}
		}

		public void clear() {
			for (int index = this.getChildCount(); --index >= 0; ) {
				this.remove(index);
			}
		}

		public void addRecords(List<LookupRecord<?>> payload) {
			if (Check.isEmpty(payload)) {
				return;
			}

			for (LookupRecord<?> lr : payload) {
				EntryNode lrnode = new EntryNode(columnCount, lr);
				this.insert(lrnode, getChildCount());
			}
		}

		@Override
		public int getColumnCount() {
			return columnCount;
		}

		@Override
		public Object getValueAt(int column) {
			switch (column) {
			case 0:
				return nodeName;
			case 1:
				return null; // the Boolean
			case 2:
				return LoadState.COMPLETE==loadState ? "" : loadState.toString(); // lookupTable.getNoteColumnName(); //$NON-NLS-1$
			}
			return null;
		}
		
	}
	
	class EntryNode extends AbstractMutableTreeTableNode {
		
		private int columnCount;
		private LookupRecord<?> rec;
		
		private boolean chosen = false;

		public EntryNode(int cc, LookupRecord<?> rec) {
			this.columnCount = cc;
			this.rec = rec;
		}

		@Override
		public int getColumnCount() {
			return columnCount;
		}

		@Override
		public Object getValueAt(int column) {
			switch (column) {
			case 0:
				return rec.nameValue;
			case 1:
				return chosen;
			case 2:
				return rec.noteValue;
			}
			return null;
		}
	}
	
	static class MyLabelProvider extends LabelProvider {
		public void format(CellContext context) {
			super.format(context);
		}
		public void configureState(CellContext context) {
			super.configureState(context);
		}
	}
	
	static class MyCheckBoxProvider extends CheckBoxProvider {
		public void format(CellContext context) {
			super.format(context);
		}
		public void configureState(CellContext context) {
			super.configureState(context);
		}
	}
	
	static class MyComponentProvider extends ComponentProvider<JComponent> {
		
		MyCheckBoxProvider checkboxProvider = new MyCheckBoxProvider();
		MyLabelProvider labelProvider = new MyLabelProvider();

		@Override
		protected void format(CellContext context) {
			Object value = context.getValue();
			if (value==null) {
				labelProvider.format(context);
			}
			else {
				checkboxProvider.format(context);
			}
		}

		@Override
		protected void configureState(CellContext context) {
			Object value = context.getValue();
			if (value==null) {
				labelProvider.configureState(context);
			}
			else {
				checkboxProvider.configureState(context);
			}
		}

		@Override
		protected JComponent createRendererComponent() {
			return null;
		}

		@Override
		public JComponent getRendererComponent(CellContext context) {
			Object value = context.getValue();
			return value==null ? labelProvider.getRendererComponent(context)
					: checkboxProvider.getRendererComponent(context);
		}
	}

	/**
	 * 
	 * @return String or null
	 */
	public String getFilteringClause() {
		StringBuilder sb = new StringBuilder();
		String sep = ""; //$NON-NLS-1$

		TreeTableNode root = treeTableModel.getRoot();
		int nChildren = root.getChildCount();
		for (int i = 0; i < nChildren; ++i) {
			LookupTableNode ltnode = (LookupTableNode) root.getChildAt(i);
			String clause = ltnode.getFilteringClause();
			if (clause!=null) {
				sb.append(sep).append(clause);
				sep = "&"; //$NON-NLS-1$
			}
		}
		
		return sb.length()<=0 ? null : sb.toString();
	}

	@Override
	public Component getViewComponent() {
		return this;
	}
	
	static public TrialSelectionSearchOptionsPanel create(BackgroundRunner br)
	{
		return new TrialSelectionSearchOptionsPanel(br,
		        MATCH_ATTRIBUTE, 
		        TrialSearchFilter.TRIAL_SEARCH_FILTERS);
	}

	public static final String MATCH_ATTRIBUTE = "Match Attribute";

	@Override
	public String getHtmlHelp(String getTrialsButtonLabel) {
		return "<HTML>" +
				"To search for <i>Trials</i>:" +
				"<ol>" +
				"<li>Check one or more selectors for the <b>"+MATCH_ATTRIBUTE.replace(" ", "&nbsp;")+"</b></li>" +
				"<li>Click on the <b>"+getTrialsButtonLabel.replace(" ", "&nbsp;")+"</b> button</li>" +
				"</ol>" +
				"<p>Within each <b>"+MATCH_ATTRIBUTE.replace(" ", "&nbsp;")+"</b> group, no selectors checked is the same as all checked.</p>"
				;
	}
}
