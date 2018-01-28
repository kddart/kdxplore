/**
 * 
 */
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
package com.diversityarrays.kdxplore.curate;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import net.pearcan.color.ColorGroups;
import net.pearcan.color.ColorPair;
import net.pearcan.color.ColorPairFactory;

import com.diversityarrays.kdsmart.db.entities.TraitInstance;
import com.diversityarrays.kdxplore.data.InstanceIdentifierUtil;

/**
 * @author alexs
 *
 */
public class TraitColorProvider {
	
	private static final ColorPair GRAY_COLOR_PAIR = new ColorPair(Color.GRAY, Color.DARK_GRAY);

	public static final int ICON_SIZE = 10;

	private final List<TraitInstance> traitInstances = new ArrayList<TraitInstance>();
	
	private final Map<String,ColorPair> colorByTraitInstanceId = new HashMap<>();
	private final Map<String,ImageIcon> iconByTraitInstanceId = new HashMap<>();
	
	private final ColorPairFactory cpf; // = ColorPairFactory.createBrightnessGroups();

	public TraitColorProvider() {
		this(true);
	}
	
	public TraitColorProvider(boolean reverse) {
		List<Color> list = new ArrayList<>();
		Collections.addAll(list, ColorGroups.COLOURS_GROUPED_BY_BRIGHTNESS);
		if (reverse) {
			Collections.reverse(list);
		}
		cpf = new ColorPairFactory(list.toArray(new Color[list.size()]));
	}
	
	private void generateColorMap() {
		for (TraitInstance ti : traitInstances) {
			String id = InstanceIdentifierUtil.getInstanceIdentifier(ti);
			if (colorByTraitInstanceId.get(id) == null) {
				ColorPair cp = generateNewColor();
				colorByTraitInstanceId.put(id, cp);
			}
		}
	}

	public ColorPair generateNewColor() {	
		return cpf.getNextColorPair();	
	}

	public ColorPair getTraitInstanceColor(TraitInstance instance) {
		ColorPair result = colorByTraitInstanceId.get(InstanceIdentifierUtil.getInstanceIdentifier(instance));
		
		if (result == null) {
			result = GRAY_COLOR_PAIR;
		}
		
		return result;
	}

	public ImageIcon getTraitLegendTile(TraitInstance instance) {
	
		String instanceIdentifier = InstanceIdentifierUtil.getInstanceIdentifier(instance);

		ImageIcon icon = null;
		
		ColorPair colorPair = colorByTraitInstanceId.get(instanceIdentifier);
		if (colorPair != null) {
			icon = iconByTraitInstanceId.get(instanceIdentifier);
			if (icon == null) {
				BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
				Graphics2D graphics = image.createGraphics();
				Color bg = colorPair.getBackground();
				graphics.setBackground(bg);
				graphics.setPaint(bg);
				graphics.fillRect(0, 0, ICON_SIZE, ICON_SIZE);
				
				graphics.setColor(bg.darker());
				graphics.drawRect(0, 0, ICON_SIZE, ICON_SIZE);

				icon = new ImageIcon(image);
				iconByTraitInstanceId.put(instanceIdentifier, icon);
			}
		}
		
		return icon;
	}

	public void generateColorMap(Collection<TraitInstance> instances) {
		traitInstances.clear();
		colorByTraitInstanceId.clear();
		iconByTraitInstanceId.clear();
		if (instances != null) {
			traitInstances.addAll(instances);	
		}		
		generateColorMap();
	}
	
}
