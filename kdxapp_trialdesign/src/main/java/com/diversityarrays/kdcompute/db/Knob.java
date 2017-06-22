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
package com.diversityarrays.kdcompute.db;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import com.diversityarrays.util.Check;

/**
 * Note: a <i>Value</i> type.
 * The <code>@Column(nullable = false)</code> annotations
 * improve Hibernate performance when the Knob is a member of an <i>ElementCollection</i>.
 * <p>
 * Note that two Knobs are the same if they have the same name (case-insensitive).
 * @author brianp
 *
 */
@SuppressWarnings("nls")
@Embeddable
public class Knob implements Comparable<Knob> {

    static public final KnobDataType DEFAULT_KNOB_DATA_TYPE = KnobDataType.TEXT;
    
    /**
     * Knob names MUST start with a letter and consist only of letters, digits and underscore.
     */
    
    @Column(name = "knobName" ,nullable = false)
    private String knobName = "";
    
    @Column(name = "description", nullable = false)
    private String description = "";
    
    @Column(name = "required" ,nullable = false)
    private boolean required = true;
    
    @Column(name = "knobDatatype" ,nullable = false)
    private KnobDataType knobDataType = DEFAULT_KNOB_DATA_TYPE;

    @Column(name = "defaultValue", nullable = false)
    private String defaultValue;

    @Column(name = "validationRule" , nullable = false)
    private String validationRule = "";

    /**
     * Strictly speaking this should be part of the UI construction
     * but it's in the legacy plugin Spec so I'm capturing it here
     * while the legacy plugin is being loaded.
     */
    @Column(nullable = false)
    private String tooltip = "";

    public Knob() {}
    
    public Knob(String name) {
        knobName = errorIfInvalid(name);
    }
    public Knob(String name, String desc, KnobDataType dt, String vr) {
        this.knobName = errorIfInvalid(name);
        this.description = desc;
        this.knobDataType = dt;
        this.validationRule = vr;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Knob(");
        sb.append(knobName).append(' ').append(knobDataType);
        sb.append("  valrule='").append(validationRule).append('\'');
        sb.append("  '").append(description).append('\'');
        sb.append(')');
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        return knobName.toLowerCase().hashCode();
    }

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (! (o instanceof Knob)) return false;
        Knob other = (Knob) o;
        return this.knobName.equalsIgnoreCase(other.knobName);
    }
    
    /**
     * Return the description unless it is empty in which case return the knobName.
     * The idea is to give a better "user" visible name.
     * @return
     */
    @Transient
    public String getVisibleName() {
        if (Check.isEmpty(description)) {
            return knobName;
        }
        return description;
    }

    public String getKnobName() {
        return knobName;
    }

    public void setKnobName(String n) {
        this.knobName = n==null ? "" : n;
    }
    
    public KnobDataType getKnobDataType() {
        return knobDataType;
    }

    public void setKnobDataType(KnobDataType knobDataType) {
        this.knobDataType = knobDataType;
    }
    
    public void setDefaultValue(String v) {
        this.defaultValue = v == null ? "" : v;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
    
    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean reqd) {
        this.required = reqd;
    }

    public String getValidationRule() {
        return validationRule;
    }

    public void setValidationRule(String v) {
        this.validationRule = v==null ? "" : v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d==null ? "" : d;
    }

    @Override
    public int compareTo(Knob o) {
        int diff = knobName.compareTo(o.knobName);
        if (diff == 0) {
            diff = knobDataType.compareTo(o.knobDataType);
            if (diff == 0) {
                diff = validationRule.compareTo(o.validationRule);
                if (diff == 0) {
                    diff = description.compareTo(o.description);
                }
            }
        }
        return diff;
    }

    @Transient
    public String getBriefString() {
        return "Knob(" + knobName + " : " + knobDataType + ")";   
    }
    
    public void setTooltip(String s) {
        tooltip = s==null ? "" : s;
    }

    public String getTooltip() {
        return tooltip;
    }

    /**
     * Throw an exception if the name is not valid.
     * @param name
     * @return the input name if of
     * @throws IllegalArgumentException
     */
    public static String errorIfInvalid(String name) throws IllegalArgumentException {
        if (Check.isEmpty(name)) {
            throw new IllegalArgumentException("Name must not be empty");
        }
        if (! Character.isLetter(name.charAt(0))) {
            throw new IllegalArgumentException("Name must begin with a letter");
        }
        String tmp = name.replaceAll("[a-zA-Z_0-9]", "");
        if (! tmp.isEmpty()) {
            throw new IllegalArgumentException("Name may only contain letters, digits or '_'");
        }
        return name;
    }

    /**
     * 
     * @param input
     * @return null if no error else the description of the problem
     */
    public static String errorTextIfInvalidKnobName(String name) {
        String errtext = null;
        try {
            errorIfInvalid(name);
        }
        catch (IllegalArgumentException e) {
            errtext = e.getMessage();
        }
        return errtext;
    }

}
