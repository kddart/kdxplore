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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Closure;

import com.diversityarrays.kdsmart.db.entities.PlotOrSpecimen;
import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdxplore.curate.SelectedValueStore;
import com.diversityarrays.kdxplore.curate.SuppressionHandler;

import net.pearcan.ui.desktop.DesktopObject;

public abstract class AbstractVisToolPanel extends JPanel implements VisToolPanel, DesktopObject, VisToolDataProvider {
	
	protected final String toolPanelId;

	private final String title;
	
	protected VisToolToolBar toolBar;
	
	protected final SelectedValueStore selectedValueStore;

	private ChangeEvent changeEvent = new ChangeEvent(this);
	
	private final VisualisationToolId<?> visualisationToolId;

	protected final SyncWhatOption syncedOption = new SyncWhatOption();

	protected boolean stillChanging = false;

	protected final List<TraitInstance> traitInstances;

	protected TraitNameStyle traitNameStyle;
	
	protected final SuppressionHandler suppressionHandler;
	
	protected final List<PlotOrSpecimen> usedPlotSpecimens = new ArrayList<>();
	
	public AbstractVisToolPanel(String title, 
			SelectedValueStore svs,
			VisualisationToolId<?> vtid, int unique, List<TraitInstance> traitInstances, SuppressionHandler suppressionHandler) 
	{
		super(new BorderLayout());
		this.title = title;
		this.selectedValueStore = svs;
		this.toolPanelId = vtid + "-" + unique; //$NON-NLS-1$
		this.visualisationToolId = vtid;
		this.traitInstances = traitInstances;
		this.suppressionHandler = suppressionHandler;
		
        syncedOption.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateSyncedOption();
			}
		});
	}
	
	abstract protected Closure<File> getSnapshotter();
	
	public VisualisationToolId<?> getVisualisationToolId() {
		return visualisationToolId;
	}

	@Override
	public String getToolPanelId() {
		return toolPanelId;
	}
	
	@Override
	final public void addSelectionChangeListener(ChangeListener l) {
		listenerList.add(ChangeListener.class, l);
	}

	@Override
	final public void removeSelectionChangeListener(ChangeListener l) {
		listenerList.remove(ChangeListener.class, l);
	}
	
	protected void fireSelectionStateChanged() {
		for (ChangeListener l : listenerList.getListeners(ChangeListener.class)) {
			l.stateChanged(changeEvent);
		}
	}

	@Override
	public JPanel getJPanel() {
		return this;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public JMenuBar getJMenuBar() {
		return null;
	}

	@Override
	final public JToolBar getJToolBar() {
		if (toolBar == null) {
			toolBar = VisToolbarFactory.create(
					getTitle(), 
					this, 
					getSnapshotter(), 
					this, 
					VisToolbarFactory.IMAGE_SUFFIXES);
		}		
		return toolBar;
	}

	@Override
	public void doPostOpenActions() {
	}

	@Override
	public boolean canClose() {
		return true;
	}

	@Override
	public boolean isClosable() {
		return true;
	}

	@Override
	public Object getWindowIdentifier() {
		return toolPanelId;
	}

	@Override
	public boolean getSelectionChanging() {
		return stillChanging;
	}

	@Override
	public SyncWhat getSyncWhat() {
		return syncedOption.getSyncWhat();
	}
	
	@Override
	public void setSyncWhat(SyncWhat syncWhat) {
		syncedOption.setSyncWhat(syncWhat);
		updateSyncedOption();
	}

	abstract protected void updateSyncedOption();
	
	/**
	 * @param plots
	 */
	public void setPlotSpecimensToWatch(List<PlotOrSpecimen> list) {
		usedPlotSpecimens.clear();
		if (list != null) {
			usedPlotSpecimens.addAll(list);
		}
	}
	
	protected void updateRefreshButton() {
		JButton refreshButton = toolBar==null ? null : toolBar.refreshButton;
		if  (refreshButton != null) {
			refreshButton.setEnabled(true);
		}
	}
	
	// VisToolDataProvider
	@Override
	public void addVisToolDataChangedListener(VisToolDataChangedListener l) {
		listenerList.add(VisToolDataChangedListener.class, l);
	}
	
	@Override
	public void removeVisToolDataChangedListener(VisToolDataChangedListener l) {
		listenerList.remove(VisToolDataChangedListener.class, l);
	}
	
	protected void fireVisToolDataChanged() {
		for (VisToolDataChangedListener l : listenerList.getListeners(VisToolDataChangedListener.class)) {
			l.visToolDataChanged(this);
		}
	}
	
}
