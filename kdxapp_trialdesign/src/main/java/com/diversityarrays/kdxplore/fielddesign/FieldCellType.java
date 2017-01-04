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
package com.diversityarrays.kdxplore.fielddesign;

import java.awt.Color;

@SuppressWarnings("nls")
public enum FieldCellType {

    BORDER_PLOT("Border Plots", Color.decode("#006600")),
    // TODO re-introduce this when we know what we want/need
//    UNUSABLE("Unusable", Color.PINK),
    ;

    static public final FieldCellType DEFAULT_FIELD_CELL_TYPE = FieldCellType.BORDER_PLOT;
    static public final Color ERROR_PLOT_COLOR = Color.RED;

    public final String visible;
    public final Color defaultColor;
    FieldCellType(String s, Color c) {
        visible = s;
        defaultColor = c;
    }

    @Override
    public String toString() {
        return visible;
    }
}
