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
import java.awt.CardLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.apache.commons.collections15.Closure;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;

import com.diversityarrays.kdsmart.db.BatchHandler;
import com.diversityarrays.kdsmart.db.KDSmartDatabase;
import com.diversityarrays.kdsmart.db.KDSmartDbUtil;
import com.diversityarrays.kdsmart.db.SampleGroupChoice;
import com.diversityarrays.kdsmart.db.TrialItemVisitor;
import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.Specimen;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdsmart.db.util.BarcodeFactory;
import com.diversityarrays.kdxplore.barcode.BarcodeSheetDialog;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.kdxplore.data.OfflineData;
import com.diversityarrays.kdxplore.prefs.KdxplorePreferences;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.MsgBox;

import net.pearcan.ui.GuiUtil;
import net.pearcan.ui.desktop.WindowOpener;
import net.pearcan.util.BackgroundRunner;
import net.pearcan.util.BackgroundTask;
import net.pearcan.util.MessagePrinter;

public class TrialDetailsPanel extends JPanel {
	
	static private final String CARD_NO_TRIAL = "NoTrial"; //$NON-NLS-1$
	static private final String CARD_HAVE_TRIAL = "HaveTrial"; //$NON-NLS-1$

	class BarcodesMenuHandler {
	    
	    JPopupMenu menu;
	    public void handleMouseClick(Point pt) {
	        if (menu == null) {
                menu = new JPopupMenu("Barcodes");
                menu.add(generateBarcodesAction);
                menu.add(printTraitBarcodesAction);
                menu.add(printPlotBarcodesAction);
                menu.add(printSpecimenBarcodesAction);
                
                
                KDSmartDatabase database = offlineData.getKdxploreDatabase().getKDXploreKSmartDatabase();
                try {
                    int plotCount = database.getPlotCount(trial);
                    printPlotBarcodesAction.setEnabled(plotCount > 0);
                } catch (IOException e1) {
                    printPlotBarcodesAction.setEnabled(false);
                }
                
                try {
                    printSpecimenBarcodesAction.setEnabled(database.getSpecimenCount(trial) > 0);
                } catch (IOException ignore) {
                    printSpecimenBarcodesAction.setEnabled(false);
                }
            }
            
            menu.show(barcodesMenuButton, pt.x, pt.y);  
	    }
	}
	
	private final BarcodesMenuHandler barcodesMenuHandler = new BarcodesMenuHandler();
	
	private final JLabel barcodesMenuButton;

	private final Action generateBarcodesAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {

			BackgroundTask<Void,Void> task = new BackgroundTask<Void, Void>("Generating...", false) {
				
				@Override
				public void onTaskComplete(Void arg0) {
					// NO-OP
				}
				
				@Override
				public void onException(Throwable err) {
					reportError("Unexpected Error: " + err.getMessage(), err);
				}
				
				@Override
				public void onCancel(CancellationException arg0) {
					messagePrinter.println("Cancelled");
				}
				
				@Override
				public Void generateResult(Closure<Void> arg0) throws Exception {
					doGenerateBarcodes(trial);
					return null;
				}
			};
			backgroundRunner.runBackgroundTask(task);
		}
	};
	
	protected Closure<File> outputDirectoryChanged = new Closure<File>() {
		@Override
		public void execute(File dir) {
			if (dir != null) {
				KdxplorePreferences.getInstance().saveOutputDirectory(dir);
			}			
		}
	};
	
	private final Action printTraitBarcodesAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {			
			KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();
			try {
				Set<Trait> traits = kdxdb.getTrialTraits(trial.getTrialId());
				
				if (traits.isEmpty()) {
				    MsgBox.info(TrialDetailsPanel.this, 
				            Msg.MSG_NO_TRAITS_ALLOCATED(),
				            Msg.TITLE_PRINT_TRAIT_BARCODES(""));
				    return;
				}
				new BarcodeSheetDialog(GuiUtil.getOwnerWindow(TrialDetailsPanel.this),
				        Msg.TITLE_PRINT_TRAIT_BARCODES(""),
				        trial,
						new ArrayList<>(traits),
						outputDirectoryChanged, 
						getOutputDirectory(),
						kdxdb)
					.setVisible(true);
			} catch (IOException e1) {
				MsgBox.error(TrialDetailsPanel.this, 
						e1, 
						Msg.TITLE_PRINT_TRAIT_BARCODES(" - Database Error")); //$NON-NLS-1$
			}
		}
	};
	
	private final Action printPlotBarcodesAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			
			KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();
			final KDSmartDatabase database = kdxdb.getKDXploreKSmartDatabase();
			
			try {
				List<Plot> plots = KDSmartDbUtil.getDatabasePlots(database, 
						trial.getTrialId(), 
						SampleGroupChoice.NO_TAGS_SAMPLE_GROUP,
						KDSmartDatabase.WithPlotAttributesOption.WITH_PLOT_ATTRIBUTES);
				new BarcodeSheetDialog(
						GuiUtil.getOwnerWindow(TrialDetailsPanel.this),
						Msg.TITLE_PRINT_PLOT_BARCODES(""), //$NON-NLS-1$
						trial,
						plots,
						null, // No specimens
						outputDirectoryChanged, 
						getOutputDirectory(),
                        kdxdb)
					.setVisible(true);
			} catch (IOException e1) {
				MsgBox.error(TrialDetailsPanel.this, e1, 
				        Msg.TITLE_PRINT_PLOT_BARCODES(" - Database Error")); //$NON-NLS-1$
			}
		}
	};
	
	private final Action printSpecimenBarcodesAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			
			KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();
			KDSmartDatabase database = kdxdb.getKDXploreKSmartDatabase();
			
			try {
				List<Specimen> specimens = getSpecimens(database);
				List<Plot> plots = KDSmartDbUtil.getDatabasePlots(database, 
						trial.getTrialId(), 
						SampleGroupChoice.NO_TAGS_SAMPLE_GROUP,
						KDSmartDatabase.WithPlotAttributesOption.WITH_PLOT_ATTRIBUTES);
				new BarcodeSheetDialog(GuiUtil.getOwnerWindow(TrialDetailsPanel.this),
				        Msg.TITLE_PRINT_SUBPLOT_BARCODES(""), //$NON-NLS-1$
						trial,
						plots,
						specimens,
						outputDirectoryChanged, 
						getOutputDirectory(),
                        kdxdb)
				    .setVisible(true);
			}
			catch (IOException e1) {
			    MsgBox.error(TrialDetailsPanel.this, e1, 
			            Msg.TITLE_PRINT_SUBPLOT_BARCODES(" - Database Error")); //$NON-NLS-1$
			}
			

		}
	};
	
	private List<Specimen> getSpecimens(KDSmartDatabase db) throws IOException {
		final List<Specimen> specimens = new ArrayList<Specimen>();
		Predicate<Specimen> visitor = new Predicate<Specimen>() {
			@Override
			public boolean evaluate(Specimen s) {
				specimens.add(s);
				return true;
			}
		};
		db.visitSpecimensForTrial(trial.getEntityId(), visitor);
		return specimens;
	}
	
	private File getOutputDirectory() {
		File outdir = KdxplorePreferences.getInstance().getOutputDirectory();
		if (outdir==null || ! outdir.isDirectory()) {
			return null;
		}
		return outdir;
	}

	static class PrintTrialBarcodeDialog {
		
	}
	

	
	private final Action editTrialAction;
	private final Action uploadTrialAction;
	private final Action refreshTrialInfoAction;

	private Trial trial;

	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cardPanel = new JPanel(cardLayout);
	
	private final TrialViewPanel trialViewPanel;
	private final OfflineData offlineData;
	private final MessagePrinter messagePrinter;
	private final BackgroundRunner backgroundRunner;
	
    private final Consumer<Trial> onTraitInstancesRemoved;
	
	TrialDetailsPanel(
	        WindowOpener<JFrame> windowOpener,
			MessagePrinter mp,
			BackgroundRunner backgroundRunner, 
			OfflineData offlineData,
			Action editTrialAction, 
			Action seedPrepAction, 
			Action harvestAction, 
			Action uploadTrialAction, 
			Action refreshTrialInfoAction,
			ImageIcon barcodeIcon, 
			Transformer<Trial, Boolean> checkIfEditorActive,
			Consumer<Trial> onTraitInstancesRemoved) 
	{
		super(new BorderLayout());
		
		this.editTrialAction = editTrialAction;
		this.uploadTrialAction = uploadTrialAction;
		this.refreshTrialInfoAction = refreshTrialInfoAction;
		this.onTraitInstancesRemoved = onTraitInstancesRemoved;
	
		this.messagePrinter = mp;
		this.backgroundRunner = backgroundRunner;
		this.offlineData = offlineData;

		if (barcodeIcon == null) {
		    barcodesMenuButton = new JLabel("Barcodes"); //$NON-NLS-1$
		}
		else {
		    barcodesMenuButton = new JLabel(barcodeIcon);
		}
        barcodesMenuButton.setBorder(BorderFactory.createRaisedSoftBevelBorder());

		barcodesMenuButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (trial != null) {
                    barcodesMenuHandler.handleMouseClick(e.getPoint());
                }
            }
        });
		
		trialViewPanel = new TrialViewPanel(windowOpener,
		        offlineData, 
		        checkIfEditorActive, 
		        onTraitInstancesRemoved,
		        mp);

		Box buttons = Box.createHorizontalBox();
		buttons.add(new JButton(refreshTrialInfoAction));
		buttons.add(new JButton(seedPrepAction));
		buttons.add(new JButton(editTrialAction));
		buttons.add(new JButton(uploadTrialAction));
		buttons.add(new JButton(harvestAction));
		buttons.add(barcodesMenuButton);
		buttons.add(Box.createHorizontalGlue());
		
//		JPanel trialPanel = new JPanel(new BorderLayout());
//		trialPanel.add(buttons, BorderLayout.NORTH);
//		trialPanel.add(fieldViewPanel, BorderLayout.CENTER);

		cardPanel.add(new JLabel(Msg.LABEL_NO_TRIAL_SELECTED()), CARD_NO_TRIAL);
		cardPanel.add(trialViewPanel, CARD_HAVE_TRIAL);

		setSelectedTrial(null);
		
		add(buttons, BorderLayout.NORTH);
		add(cardPanel, BorderLayout.CENTER);
	}

	public Trial getCurrentTrial() {
		return trial;
	}

	public void setSelectedTrial(Trial t) {
		this.trial = t;

		cardLayout.show(cardPanel, trial==null ? CARD_NO_TRIAL : CARD_HAVE_TRIAL);

		editTrialAction.setEnabled(trial != null);
		refreshTrialInfoAction.setEnabled(trial != null);
		uploadTrialAction.setEnabled(trial != null);
		barcodesMenuButton.setEnabled(trial != null);
		

		trialViewPanel.setTrial(trial);
	}
	
	
	private boolean isNullOrEmpty(String s) {
		return s==null || s.trim().isEmpty();
	}


	private void reportError(String msg, Throwable err) {
		messagePrinter.println(err);
		MsgBox.error(TrialDetailsPanel.this,
				msg,
				"Add Barcodes");

	}

	private void doGenerateBarcodes(Trial trial) {

		KdxploreDatabase kdxdb = offlineData.getKdxploreDatabase();
		final KDSmartDatabase database = kdxdb.getKDXploreKSmartDatabase();

		
		
		String what = "";
		try {
			final List<Plot> plotsToUpdate = new ArrayList<>();
			final List<Specimen> specimensToUpdate = new ArrayList<>();
			final List<Trait> traitsToUpdate = new ArrayList<>();
			
			what = " for Traits";
			for (Trait trait : kdxdb.getTrialTraits(trial.getTrialId())) {
				if (isNullOrEmpty(trait.getBarcode())) {
					BarcodeFactory.setTraitBarcode(trait);
					traitsToUpdate.add(trait);
				}
			}
			
			// = = = = = = = =
			
			what = " for Plots and Specimens";

			TrialItemVisitor<Plot> trialPlotVisitor = new TrialItemVisitor<Plot>() {
				@Override
				public void setExpectedItemCount(int count) {
				}						
				@Override
				public boolean consumeItem(Plot plot) throws IOException {
					if (isNullOrEmpty(plot.getBarcode())) {
						BarcodeFactory.setPlotBarcode(plot);
						plotsToUpdate.add(plot);
					}
					
					for (Specimen specimen : plot.getSpecimens()) {
						if (isNullOrEmpty(specimen.getBarcode())) {
							BarcodeFactory.setSpecimenBarcode(specimen);
							specimensToUpdate.add(specimen);
						}
					}
					return true;
				}
			};
			
			Either<? extends Throwable, Boolean> either1 = database.visitPlotsForTrial(trial.getTrialId(), 
					SampleGroupChoice.ANY_SAMPLE_GROUP, 
					KDSmartDatabase.WithPlotAttributesOption.WITHOUT_PLOT_ATTRIBUTES,
					trialPlotVisitor);
	        if (! either1.isRight()) {
	        	Throwable error = either1.left();
	        	if (error instanceof IOException) {
	        		throw ((IOException) error);
	        	}
	            throw new IOException(error);
	        }
	        
			int nTraits = traitsToUpdate.size();
			int nPlots = plotsToUpdate.size();
			int nSpecimens = specimensToUpdate.size();
			
			if ( (nTraits + nPlots + nSpecimens) > 0 ) {
			
				BatchHandler<Void> callable = new BatchHandler<Void>() {

					@Override
					public Void call() throws Exception {
						for (Trait trait : traitsToUpdate) {
							database.saveTrait(trait, false);
						}
						for (Plot plot : plotsToUpdate) {
							database.savePlot(plot, false);
						}
						
						for (Specimen specimen : specimensToUpdate) {
							database.saveSpecimen(specimen, false);
						}
						return null;
					}

                    @Override
                    public boolean checkSuccess(Void arg0) {
                        return true;
                    }
				};
				Either<Exception,Void> either = database.doBatch(callable);
				if (either.isRight()) {
					StringBuilder sb = new StringBuilder("Added barcodes for");
					if (nTraits > 0) {
						sb.append("\nTraits: ").append(nTraits);
					}
					if (nPlots > 0) {
						sb.append("\nPlots: ").append(nPlots);
					}
					if (nSpecimens > 0) {
						sb.append("\nSpecimens: ").append(nSpecimens);
					}
					messagePrinter.println(sb.toString());
				}
				else {
					messagePrinter.println("Problem while adding barcodes:");
					messagePrinter.println(either.left());
				}
			}
			else {
				JOptionPane.showMessageDialog(TrialDetailsPanel.this,
						"All Traits,Plots and Specimens have barcodes", 
						"Add Barcodes", 
						JOptionPane.INFORMATION_MESSAGE);
			}

			
		} catch (IOException err) {
			reportError("Unable to get Trial data" + what + "\n" + err.getMessage(), err);
		}
	}

	public void updateTrial(Trial changedTrial, int[] sampleGroupIds, Plot[] changedPlots) {
		trialViewPanel.updateIfSameTrial(changedTrial.getTrialId());		
	}

}
