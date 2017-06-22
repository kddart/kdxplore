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
package com.diversityarrays.kdcompute.designer.editor;

import java.text.DecimalFormat;

import javax.swing.JComponent;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.diversityarrays.kdcompute.db.helper.KnobValidationRule.Range;
import com.diversityarrays.util.Check;

import net.pearcan.ui.widget.NumberSpinner;

/**
 * Presents the user with a JSpinner to select an Integer or Decimal value.
 * <p>
 * On the web page this will have to be a library or custom version of a TEXT input
 * that validates according to the speficications of the supplied <code>Range</code>.
 * @author brianp
 *
 */
public class RangeValueEditor extends AbstractValueEditor {

    private final SpinnerNumberModel model;
    private final NumberSpinner spinner;
    private final DecimalFormat decimalFormat;
    private final boolean integerRange;
    private final Range range;

    public RangeValueEditor(String initialValue, Range range) {

        this.range = range;
        this.decimalFormat = range.getDecimalFormat();
        Double[] limits = range.getInclusiveLimits();

        integerRange = 0 == range.getNumberOfDecimalPlaces();

        if (integerRange) {
            int min = limits[0] == null ? Integer.MIN_VALUE : limits[0].intValue();
            int max = limits[1] == null ? Integer.MAX_VALUE : limits[1].intValue();

            Integer value = null;
            if (Check.isEmpty(initialValue)) {
                try {
                    value = new Integer(initialValue);
                    if (value < min || max < value) {
                        value = null; // else we will get an error !
                    }
                }
                catch (NumberFormatException e) {
                }
            }
            if (value == null) {
                value = limits[0] == null ? 0 : limits[0].intValue();
            }
            model = new SpinnerNumberModel(value.intValue(), min, max, 1);
        }
        else {
            double min = limits[0] == null ? (Double.MIN_VALUE / 2): limits[0];
            double max = limits[1] == null ? (Double.MAX_VALUE / 2) : limits[1];

            Double value = null;
            if (Check.isEmpty(initialValue)) {
                try {
                    value = new Double(initialValue);
                }
                catch (NumberFormatException e) {
                }
            }
            if (value == null) {
                value = limits[0]== null ? 0 : min;
            }
            model = new SpinnerNumberModel(value.doubleValue(), min, max, range.getIncrement());
        }

        model.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                fireStateChanged();
            }
        });
        spinner = new NumberSpinner(model, range.getNumberFormat());
    }

    public Range getRange() {
        return range;
    }

    @Override
    public String getValue() {
        return decimalFormat.format(model.getNumber().doubleValue());
    }

    @Override
    public JComponent getVisualComponent() {
        return spinner;
    }

    @Override
    public void setValue(String value) {
        if (integerRange) {
            try {
                model.setValue(Integer.valueOf(value));
            }
            catch (NumberFormatException e) {
            }
        }
        else {
            try {
                model.setValue(Double.valueOf(value));
            }
            catch (NumberFormatException e) {
            }
        }
    }

}
