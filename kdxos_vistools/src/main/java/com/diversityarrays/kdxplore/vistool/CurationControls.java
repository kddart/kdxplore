/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017,2018  Diversity Arrays Technology, Pty Ltd.
    
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
package com.diversityarrays.kdxplore.vistool;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdxplore.Vocab;
import com.diversityarrays.kdxplore.curate.SampleName;
import com.diversityarrays.kdxplore.curate.SelectedValueStore;
import com.diversityarrays.kdxplore.curate.SuppressOption;
import com.diversityarrays.kdxplore.curate.SuppressionArgs;
import com.diversityarrays.kdxplore.curate.SuppressionHandler;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;

public class CurationControls extends JPanel {
	
	private final List<KdxSample> selectedSamples = new ArrayList<>();
	
	private final Action acceptSamplesAction = new AbstractAction(Vocab.ACTION_ACCEPT_TRAIT_VALUES()) {
        @Override
        public void actionPerformed(ActionEvent e) {            
            List<SampleName> samples = new ArrayList<SampleName>();
            for (TraitInstance ti : selectedTraitInstances) {
                Set<PlotOrSpecimen> plotSpecimens = selectedValueStore.getSelectedPlotsForToolId(getSyncedState()).getPlotSpecimens(ti);
                for (PlotOrSpecimen pos : plotSpecimens) {
                    SampleName sampleName = new SampleName(pos.getPlotId(), ti.getInstanceNumber(), ti.getTraitId());
                    samples.add(sampleName);        
                }
            }               
            if (samples.size() > 0) {
                SuppressionArgs sargs = CurationControls.this.suppressionHandler.createSuppressionArgs(
                        askAboutValueForUnscored, samples, selectedTraitInstances);
                CurationControls.this.suppressionHandler.setSamplesSuppressed(
                        SuppressOption.ACCEPT, 
                        sargs, 
                        CurationControls.this); 
            }
        }
    };
	
	private final Action rejectSamplesAction = new AbstractAction(Vocab.ACTION_SUPPRESS_TRAIT_VALUES()) {
        @Override
        public void actionPerformed(ActionEvent e) {
            List<SampleName> samples = new ArrayList<SampleName>();                         
            for (TraitInstance ti : selectedTraitInstances) {
                for (PlotOrSpecimen pos : selectedValueStore.getSelectedPlotsForToolId(getSyncedState()).getPlotSpecimens(ti)) {
                    SampleName sampleName = new SampleName(pos.getPlotId(), ti.getInstanceNumber(), ti.getTraitId());
                    samples.add(sampleName);    
                }
            }               
            if (samples.size() > 0) {
                SuppressionArgs sargs = CurationControls.this.suppressionHandler.createSuppressionArgs(
                        askAboutValueForUnscored, samples, selectedTraitInstances);
                CurationControls.this.suppressionHandler.setSamplesSuppressed(SuppressOption.REJECT, sargs, CurationControls.this);
            }
        }
    };
	
	private List<TraitInstance> selectedTraitInstances;
	
	private final TraitNameStyle traitNameStyle;

//	private final Predicate<KdxSample> checkIfCurated;

	private final SuppressionHandler suppressionHandler;	
	
	private final SelectedValueStore selectedValueStore;
	
	private final String toolName;
	
	private final List<TraitInstance> traitInstances;
	
	// FIXME work out how this is supposed to be used
	private boolean synced = true;

    private final boolean askAboutValueForUnscored;
	
	public CurationControls(
	        boolean askAboutValueForUnscored,
			SuppressionHandler suppressionHandler, 
			SelectedValueStore selectedValueStore,
//			Predicate<KdxSample> checkIfCurated,
			String toolName,
			String messageLine, 
			TraitNameStyle traitNameStyle, 
			List<TraitInstance> traitInstances) {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		if (messageLine != null) {
			add(new JLabel(messageLine));
		}

		this.askAboutValueForUnscored = askAboutValueForUnscored;
		this.traitInstances = traitInstances;
		this.toolName = toolName;
		this.selectedValueStore = selectedValueStore;
		this.suppressionHandler = suppressionHandler;
//		this.checkIfCurated = checkIfCurated;
		this.traitNameStyle = traitNameStyle; 

		if (traitInstances != null && traitInstances.size() > 0) {
			ButtonGroup bg = new ButtonGroup();
			Box axisChoicesBox = Box.createHorizontalBox();
			axisChoicesBox.add(new JLabel(Vocab.LABEL_APPLY_TO()));
			for (TraitInstance ti : traitInstances) {
				JRadioButton rb = addAxisChoice(bg, ti);
				if (bg.getButtonCount() == 1) {
					rb.doClick();
					traitInstanceButtonsByInstance.put(ti, rb);
				}
				axisChoicesBox.add(rb);
			}
			
			String label = null;
			switch (traitInstances.size()) {
			case 0:
				throw new RuntimeException("SNH"); //$NON-NLS-1$
			case 1:
				break;
			case 2:
				label = Vocab.LABEL_APPLY_TO_BOTH();
				break;
			default:
				label = Vocab.LABEL_APPLY_TO_ALL();
				break;
			}
			
			if (label != null) {
				Action action = new AbstractAction(label) {
					@Override
					public void actionPerformed(ActionEvent e) {
						selectedTraitInstances = traitInstances;
					}
				};
				JRadioButton rb = new JRadioButton(action);
				bg.add(rb);
				axisChoicesBox.add(rb);
			}
			axisChoicesBox.add(Box.createHorizontalGlue());
			
			add(axisChoicesBox);
		}
		
		Box buttons = Box.createHorizontalBox();
		buttons.add(new JButton(acceptSamplesAction));
		buttons.add(new JButton(rejectSamplesAction));
		buttons.add(Box.createHorizontalGlue());
		add(buttons);
		
		updateButtons();
	}
	
	public List<TraitInstance> getSelectedTraitInstances() {
		return selectedTraitInstances;
	}
	
	private JRadioButton addAxisChoice(ButtonGroup bg, TraitInstance ti) {
		String label = traitNameStyle.makeTraitInstanceName(ti);
		Action action = new AbstractAction(label) {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectedTraitInstances = Arrays.asList(new TraitInstance[] { ti });
			}
		};

		JRadioButton rb = new JRadioButton(action);
		bg.add(rb);
		return rb;
	}

	public void setSamples(List<KdxSample> samples) {
		this.selectedSamples.clear();
		if (samples != null) {
			this.selectedSamples.addAll(samples);
		}
		
		updateButtons();
	}

	public void updateButtons() {

		boolean enable = false;

		for (TraitInstance ti : selectedTraitInstances) {
			if (selectedTraitInstances.contains(ti)) {
				if (toolName != null) {
					if (selectedValueStore.getSelectedPlotsForToolId(getSyncedState()).getPlotSpecimens(ti).size() > 0) {
						enable = true;
					}
				}
			}
		}

		acceptSamplesAction.setEnabled(enable);
		rejectSamplesAction.setEnabled(enable);		
	}	
	
	public void updateButtons(boolean b) {
		acceptSamplesAction.setEnabled(b);
		rejectSamplesAction.setEnabled(b);
	}

	private String getSyncedState() {
//		if (synced) {
//			return "SYNC";
//		} 
		return toolName;
	}

	private Map<TraitInstance,JRadioButton> traitInstanceButtonsByInstance = new HashMap<TraitInstance,JRadioButton>();
	
	public void setSyncedState(boolean sync) {
		this.synced = sync;
	}


}
