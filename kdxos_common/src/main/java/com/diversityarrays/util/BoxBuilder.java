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
package com.diversityarrays.util;

import java.awt.Component;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;

public class BoxBuilder {

    static public BoxBuilder horizontal() {
        return create(BoxOrientation.HORZ);
    }

    static public BoxBuilder vertical() {
        return create(BoxOrientation.VERT);
    }

    static public BoxBuilder create(BoxOrientation orientation) {
        return new BoxBuilder(orientation);
    }

    private final BoxOrientation orientation;
    private final Box result;

    private BoxBuilder(BoxOrientation orientation) {
        this.orientation = orientation;
        result = orientation.createBox();
    }

    public BoxBuilder add(Object ... parts) {
        if (parts != null) {
            for (Object part : parts) {
                if (part == null) {
                    result.add(orientation.createGlue());
                }
                else if (part instanceof BoxBuilder) {
                    result.add(((BoxBuilder) part).get());
                }
                else if (part instanceof Component) {
                    result.add((Component) part);
                }
                else if (part instanceof Action) {
                    result.add(new JButton((Action) part));
                }
                else if (part instanceof Integer) {
                    int size = ((Integer) part).intValue();
                    if (size < 0) {
                        result.add(orientation.createJSeparator());
                    }
                    else if(size == 0) {
                        result.add(orientation.createGlue());
                    }
                    else {
                        result.add(orientation.createStrut(size));
                    }
                }
                else {
                    result.add(new JLabel(part.toString()));
                }
            }
        }
        return this;
    }

    public Box get() {
        return result;
    }
}
