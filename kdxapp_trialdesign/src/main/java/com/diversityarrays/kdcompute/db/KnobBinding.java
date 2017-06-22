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
import javax.persistence.Embedded;

@SuppressWarnings("nls")
@Embeddable
public class KnobBinding {

    @Embedded
    @Column(name= "knob" , nullable = false)
    private final Knob knob;

    @Column(name = "knobValue", nullable = false)
    private String knobValue = ""; //$NON-NLS-1$
    
    public KnobBinding() {
        knob = null;
    }
    
    public KnobBinding(Knob k) {
        knob = k;
    }
    
    public KnobBinding(Knob k, String v) {
        knob = k;
        knobValue = v;
    }
    
    public KnobBinding(KnobBinding kb) {
        knob = kb.getKnob();
        knobValue = kb.getKnobValue();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("KnobBinding(");
        sb.append(knob.getBriefString()).append(" IS '" ).append(knobValue).append('\'');
        sb.append(')');
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        return this.knob.hashCode() * 17 + knobValue.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (! (o instanceof KnobBinding)) return false;
        KnobBinding other = (KnobBinding) o;
        return this.knob.equals(other.knob)
                &&
               this.knobValue.equals(other.knobValue);
    }

    public Knob getKnob() {
        return knob;
    }

    public String getKnobValue() {
        return knobValue;
    }
    
    public void setKnobValue(String v) {
        knobValue = v==null ? "" : v; //$NON-NLS-1$
    }
}
