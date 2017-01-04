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

import java.awt.Component;

import javax.swing.Box;
import javax.swing.JSeparator;

public enum Orientation {
    HORZ,
    VERT;

    public Orientation other() {
        return this==HORZ ? VERT : HORZ;
    }
    public Box createBox() {
        return this==HORZ ? Box.createHorizontalBox() : Box.createVerticalBox();
    }
    public Box createOtherBox() {
        return this==HORZ ? Box.createVerticalBox() : Box.createHorizontalBox();
    }
    public JSeparator createJSeparator() {
        return new JSeparator(this==HORZ ? JSeparator.VERTICAL : JSeparator.HORIZONTAL);
    }
    public Component createGlue() {
        return this==HORZ ? Box.createHorizontalGlue() : Box.createVerticalGlue();
    }
    public Component createStrut(int size) {
        return this==HORZ ? Box.createHorizontalStrut(size) : Box.createVerticalStrut(size);
    }
}
