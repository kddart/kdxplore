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

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;

import net.pearcan.util.Util;

import org.apache.commons.collections15.Closure;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.kdxplore.curate.SelectedValueStore;
import com.diversityarrays.kdxplore.curate.SuppressionHandler;
import com.diversityarrays.kdxplore.vistool.VisToolbarFactory.ImageFormat;

public abstract class JFreeChartVisToolPanel extends AbstractVisToolPanel {

	protected final TraitNameStyle traitNameStyle;
	protected final Trial trial;

	public JFreeChartVisToolPanel(String title,
			VisualisationToolId<?> vtid,
			SelectedValueStore svs,
			int unique, 
			List<TraitInstance> traitInstances, 
			Trial trial, SuppressionHandler suppressionHandler) 
	{
		super(title, svs, vtid, unique, traitInstances, suppressionHandler);
		
		this.trial = trial;
		this.traitNameStyle = trial.getTraitNameStyle();		
	}


	abstract protected ChartPanel getChartPanel();
	abstract protected JFreeChart getJFreeChart();
	
	private Closure<File> snapshotter = new Closure<File>() {
		@Override
		public void execute(File xfile) {
			File file = xfile;
			ImageFormat imageFormat = VisToolbarFactory.getImageFormatName(file);
			if (imageFormat == null) {
				imageFormat = ImageFormat.PNG;
				file = new File(file.getParent(), file.getName() + imageFormat.suffix);
			}

			Dimension sz = getChartPanel().getSize();
			try {
				boolean saved = false;
				switch (imageFormat) {
				case JPEG:
				case JPG:
					ChartUtilities.saveChartAsJPEG(file, getJFreeChart(), sz.width, sz.height);
					saved = true;
					break;
				case PNG:
					ChartUtilities.saveChartAsPNG(file, getJFreeChart(), sz.width, sz.height);
					saved = true;
					break;
				default:
					break;				
				}
				
				if (saved) {
					if (JOptionPane.YES_OPTION ==
						JOptionPane.showConfirmDialog(
								JFreeChartVisToolPanel.this, 
								file.getPath(), 
								Msg.QUESTION_DO_YOU_WANT_TO_VIEW_THE_SNAPSHOT(), 
								JOptionPane.YES_NO_OPTION))
					{
						try {
							Util.openFile(file);
						} catch (IOException e) {
							JOptionPane.showMessageDialog(JFreeChartVisToolPanel.this, 
									e.getMessage(), 
									Msg.ERRTITLE_UNABLE_TO_OPEN_IMAGE_FILE(file.getPath()), 
									JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			}
			catch (IOException e) {
				JOptionPane.showMessageDialog(JFreeChartVisToolPanel.this, 
						e.getMessage(), Msg.ERRTITLE_UNABLE_TO_SNAPSHOT(), 
						JOptionPane.ERROR_MESSAGE);
			}
		}
	};

	@Override
	protected Closure<File> getSnapshotter() {
		return snapshotter;
	}
}
