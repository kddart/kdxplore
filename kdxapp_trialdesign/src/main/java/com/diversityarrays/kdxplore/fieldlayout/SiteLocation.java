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
package com.diversityarrays.kdxplore.fieldlayout;

import java.awt.Dimension;
import java.util.UUID;

import com.diversityarrays.util.UUIDUtil;

@SuppressWarnings("nls")
public class SiteLocation implements Comparable<SiteLocation> {

    static public final int INITIAL_MIN_WIDTH_IN_CELLS = 20;
    static public final int INITIAL_MIN_HEIGHT_IN_CELLS = 10;

    private final UUID uuid;
    public final String name;

    public final int widthInCells;
    public final int heightInCells;

    public final double widthInMetres;
    public final double heightInMetres;

    private boolean sizeEditable;

    public SiteLocation(String name, int width, int height) {
        this(UUIDUtil.getTimeBasedUUID(), name, new Dimension(width, height), true, 0, 0);
    }
    public SiteLocation(String name, int width, int height, boolean editable) {
        this(UUIDUtil.getTimeBasedUUID(), name, new Dimension(width, height), editable, 0, 0);
    }
    private SiteLocation(UUID id, String n, Dimension cells, boolean editable, double w, double h) {
        this.uuid = id;
        this.name = n;
        this.widthInCells = cells.width;
        this.heightInCells = cells.height;
        this.sizeEditable = editable;
        this.widthInMetres = w;
        this.heightInMetres = h;
    }

    @Override
    public String toString() {
        return "Location[" + name + "]";
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this==o) return true;
        if (! (o instanceof SiteLocation)) return false;
        SiteLocation other = (SiteLocation) o;
        return this.uuid.equals(other.uuid);
    }

    public SiteLocation copy(SiteLocation loc, String n, Dimension d, boolean editable,
            double wMetres, double hMetres)
    {
        return new SiteLocation(loc.uuid, n, d, editable, wMetres, hMetres);
    }
    @Override
    public int compareTo(SiteLocation o) {
        return name.compareToIgnoreCase(o.name);
    }

    public Dimension getSizeInCells() {
        return new Dimension(widthInCells, heightInCells);
    }
    public boolean isSizeEditable() {
        return sizeEditable;
    }
}
