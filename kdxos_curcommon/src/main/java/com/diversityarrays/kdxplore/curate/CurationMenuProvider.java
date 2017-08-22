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
package com.diversityarrays.kdxplore.curate;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.diversityarrays.kdsmart.db.entities.Plot;
import com.diversityarrays.kdsmart.db.entities.PlotIdentOption;
import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.Vocab;
import com.diversityarrays.kdxplore.data.kdx.CurationData;
import com.diversityarrays.kdxplore.vistool.VisToolUtil;
import com.diversityarrays.kdxplore.vistool.VisualisationTool;
import com.diversityarrays.util.MsgBox;
import com.diversityarrays.util.RunMode;

import net.pearcan.util.MessagePrinter;

public class CurationMenuProvider {
	
	public static final List<PlotOrSpecimen> NO_PLOTS = null;
	public static final TraitInstance NO_ACTIVE_INSTANCE = null;
	public static final List<TraitInstance> NO_INSTANCES = Collections.emptyList();
		
	private final VisualisationTool[] visualisationTools;
	private final CurationContext curationContext;
	
	private SuppressionInfoProvider suppressionInfoProvider;
	private CurationData curationData;
	private final SuppressionHandler suppressionHandler;
	
	private final MessagePrinter messagePrinter;
	public CurationMenuProvider(CurationContext cc,
			CurationData cd,
			MessagePrinter mp,
			VisualisationTool[] vtools,
			SuppressionHandler suppressionHandler)
	{
		this.curationContext = cc;
		this.curationData = cd;
		this.messagePrinter = mp;
		this.visualisationTools = vtools;
		this.suppressionHandler = suppressionHandler;
	}
	
	public void showTraitInstanceTableToolMenu(
			MouseEvent me,
			List<TraitInstance> checkedInstances,
			List<TraitInstance> selectedInstances,
			Action ... actions) 
	{
		JPopupMenu popupMenu = new JPopupMenu();
		
		addVisToolActions(me,
		        popupMenu,
				NO_PLOTS, 
				NO_ACTIVE_INSTANCE, 
				checkedInstances,
				selectedInstances);
		
		if (actions != null) {
			for (Action action : actions) {
				if (action != null) {
					popupMenu.add(new JMenuItem(action));
				}
			}
		}

		if (popupMenu.getSubElements().length > 0) {
			popupMenu.show(me.getComponent(), me.getX(), me.getY());
		}
		
	}
	
	public void showFieldViewToolMenu(
			MouseEvent me,
			List<PlotOrSpecimen> plotSpecimens,
			TraitInstance activeInstance,
			List<TraitInstance> checkedInstances)
	{
		final JPopupMenu popupMenu = new JPopupMenu(Vocab.MENU_TITLE_FIELD_VIEW());
		
		addPlotActions(popupMenu, plotSpecimens);
		
		List<TraitInstance> selectedInstances = NO_INSTANCES;
		addVisToolActions(me,
		        popupMenu, 
		        plotSpecimens, 
				activeInstance, 
				checkedInstances,
				selectedInstances);

		addAttachmentActions(popupMenu, plotSpecimens);

		popupMenu.show(me.getComponent(), me.getX(), me.getY());
	}

	private void addVisToolActions(
	        MouseEvent mouseEvent,
			final JPopupMenu popupMenu,
			List<PlotOrSpecimen> plotSpecimens,
			TraitInstance activeInstance, 
			List<TraitInstance> checkedInstances,
			List<TraitInstance> selectedInstances) 
	{
	    if (RunMode.getRunMode().isDeveloper()) {
	        VisToolUtil.allowSubplotTraits = true;
	    }
	    else {
	        VisToolUtil.allowSubplotTraits = 0 != (MouseEvent.SHIFT_DOWN_MASK & mouseEvent.getModifiersEx());
	    }

	    boolean needSeparator = popupMenu.getSubElements().length > 0;
		
		int toolCount = 0;
		for (VisualisationTool vtool : visualisationTools) {
			++toolCount;
			
			Icon icon = vtool.getToolIcon();
			
			List<Action> actions = vtool.createActionsFor(
					curationContext, 
					plotSpecimens,
					checkedInstances,
					activeInstance,
					selectedInstances);
			
			if (actions != null && ! actions.isEmpty()) {
				if (needSeparator && toolCount == 1) {
					popupMenu.addSeparator();
				}
				if (actions.size() == 1) {
					Action action = actions.get(0);
					String actionName = vtool.getToolButtonName() + ": " + action.getValue(Action.NAME).toString(); //$NON-NLS-1$
					action.putValue(Action.NAME, actionName);
					if (icon != null) {
						action.putValue(Action.SMALL_ICON, icon);
					}
					popupMenu.add(new JMenuItem(action));
				}
				else {
					JMenu menu = new JMenu(vtool.getToolButtonName());
					if (icon != null) {
						menu.setIcon(icon);
					}
					popupMenu.add(menu);
					for (Action action : actions) {
						menu.add(action);
					}
				}
			}
		}
	}
	

	public void showSampleTableToolMenu(
			MouseEvent me,
			List<PlotOrSpecimen> plotSpecimens,
			List<TraitInstance> checkedInstances,
			List<TraitInstance> selectedInstances)
	{
	    SuppressionArgs suppressionArgs = suppressionInfoProvider.createSuppressionArgs(
	            true /* askAboutValueForUnscored */);
		
		JPopupMenu popupMenu = new JPopupMenu();

		Action suppressMenuAction = new AbstractAction(Vocab.ACTION_SUPPRESS_TRAIT_VALUES()) {
			@Override
			public void actionPerformed(ActionEvent e) {
				suppressionHandler.setSamplesSuppressed(SuppressOption.REJECT, suppressionArgs);
			}		
		};
		
		Action acceptMenuAction = new AbstractAction(Vocab.ACTION_ACCEPT_TRAIT_VALUES()) {
			@Override
			public void actionPerformed(ActionEvent e) {
				suppressionHandler.setSamplesSuppressed(SuppressOption.ACCEPT, suppressionArgs);
			}		
		};
		
		popupMenu.add(acceptMenuAction);		
        popupMenu.add(suppressMenuAction);
		popupMenu.addSeparator();
		if (suppressionArgs == null) {
			suppressMenuAction.setEnabled(false);
			acceptMenuAction.setEnabled(false);
		}
		
		addPlotActions(popupMenu, plotSpecimens);
		
		addVisToolActions(me,
		        popupMenu, 
		        plotSpecimens, 
				NO_ACTIVE_INSTANCE,
				checkedInstances, 
				selectedInstances);

        addAttachmentActions(popupMenu, plotSpecimens);

		popupMenu.show(me.getComponent(), me.getX(), me.getY());
	}
	
    private void addAttachmentActions(JPopupMenu popupMenu, List<PlotOrSpecimen> plotSpecimens) {
        List<File> files = new ArrayList<>();
        for (PlotOrSpecimen pos : plotSpecimens) {
            Set<File> set = pos.getMediaFiles();
            files.addAll(set);
        }
        if (! files.isEmpty()) {
            popupMenu.addSeparator();
            JMenu attachments = new JMenu("View ...");
            popupMenu.add(attachments);
            for (File f : files) {
                Action action = new AbstractAction(f.getName()) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            Desktop.getDesktop().browse(f.toURI());
                        }
                        catch (IOException e1) {
                            MsgBox.error(null, e1, "Unable to open " + f.getPath());
                        }
                    }
                };
                attachments.add(action);
            }
            if (files.size() > 1) {
                Action action = new AbstractAction("All") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        List<File> failed = new ArrayList<>();
                        for (File f : files) {
                            try {
                                Desktop.getDesktop().browse(f.toURI());
                            }
                            catch (IOException e1) {
                                failed.add(f);
                            }
                        }
                        if (! failed.isEmpty()) {
                            String msg = failed.stream().map(File::getName)
                                .collect(Collectors.joining("\n"));
                            MsgBox.error(null, msg, "Unable to open images:" + failed.size());

                        }
                    }
                };
                attachments.add(action);
            }
        }
    }	
	
	private void addPlotActions(JPopupMenu popupMenu, List<PlotOrSpecimen> plotSpecimens) {
		
		Set<Plot> activatedPlots = new HashSet<>();
		Set<Plot> deactivatedPlots = new HashSet<>();
		for (PlotOrSpecimen pos : plotSpecimens) {
		    Plot p = curationData.getPlotByPlotId(pos.getPlotId());
		    if (p == null) {
		        continue;
		    }
			if (p.isActivated()) {
				activatedPlots.add(p);
			}
			else {
				deactivatedPlots.add(p);
			}
		}
				
		Action activatePlotAction = new AbstractAction(Vocab.ACTION_ACTIVATE_PLOTS()) {
			@Override
			public void actionPerformed(ActionEvent e) {
				String desc = getPlotsDescription(deactivatedPlots);
				curationData.activatePlots(deactivatedPlots);
				messagePrinter.println(Vocab.MSG_ACTIVATED_PLOT_COUNT(activatedPlots.size()));
				messagePrinter.println(desc);
			}		
		};

		Action deactivatePlotAction = new AbstractAction(Vocab.ACTION_DEACTIVATE_PLOTS()) {
			@Override
			public void actionPerformed(ActionEvent e) {				
				String desc = getPlotsDescription(activatedPlots);
				curationData.deactivatePlots(activatedPlots);
				messagePrinter.println(Vocab.MSG_DEACTIVATED_PLOT_COUNT(activatedPlots.size()));
				messagePrinter.println(desc);
			}
		};
		
		initialiseADaction(activatePlotAction, deactivatedPlots);
		initialiseADaction(deactivatePlotAction, activatedPlots);
		
		popupMenu.add(activatePlotAction);
		popupMenu.add(deactivatePlotAction);

	}
	
	private String getPlotsDescription(Collection<Plot> plots) {
		Trial trial = curationContext.getTrial();
		PlotIdentOption pio = trial.getPlotIdentOption();
		String desc;
		if (plots.size() < 10) { // TODO parameterize or use KDXplorePrefs
			StringBuilder sb = new StringBuilder(Vocab.TITLE_PLOT_LIST());
			for (Plot p : plots) {
				sb.append("\n") //$NON-NLS-1$
				    .append(pio.createPlotIdentString(p, "/")); //$NON-NLS-1$
			}
			desc = sb.toString();
		}
		else {
			desc = Vocab.MSG_COUNT_PLOTS(plots.size());
		}
		return desc;
	}
	
	private void initialiseADaction(Action action, Collection<Plot> plots) {
		int nPlotCount = plots.size();
		action.setEnabled(nPlotCount > 0);
	}
	
	
//	private void registerAcceptedSampleList(List<KdxSample> samples) {
//		curationData.registerAcceptedSamples(samples);
//		curationTable.repaint();
//	}
//	
//	private void registerRejectedSampleList(List<KdxSample> samples, String reason) {
//		curationData.registerRejectedSamples(samples, reason);
//		curationTable.repaint();
//	}

	public void setSuppressionInfoProvider(SuppressionInfoProvider sip) {
		suppressionInfoProvider = sip;
	}
}
