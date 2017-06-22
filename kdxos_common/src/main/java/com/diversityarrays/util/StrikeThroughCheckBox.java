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

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;

public class StrikeThroughCheckBox extends JCheckBox {
    private final Font normal;
    private final Font strikeThrough;

    public StrikeThroughCheckBox(String label, boolean selected) {
        super(label, selected);

        Map<AttributedCharacterIterator.Attribute, Object> strikeThroughAttributes = new HashMap<>();
        strikeThroughAttributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);

        normal = getFont();
        strikeThrough = normal.deriveFont(strikeThroughAttributes);
    }

    @Override
    public void setEnabled(boolean b) {
        setFont(b ? normal : strikeThrough);
        super.setEnabled(b);
    }
}