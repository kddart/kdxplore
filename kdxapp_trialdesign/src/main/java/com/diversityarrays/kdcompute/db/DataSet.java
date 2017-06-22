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
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.diversityarrays.util.Check;

/**
 * Provides the definition and description of either an input
 * or an output of an <i>Algorithm</i>.
 * <p>
 * Note that this is a <i>Value</i> type.
 * The <code>@Column(nullable = false)</code>
 * improves performance of Hibernate modification of any
 * ElementCollection this Embeddable is a member of.
 * <p>
 * Note: equality means same <code>dataSetName</code>.
 * @author brianp
 *
 */
@SuppressWarnings("nls")
@Embeddable
@Table
public class DataSet {

//    public static final String COLNAME_DATASET_ORDER = "dataset_order";

//    @Column(name = COLNAME_DATASET_ORDER, nullable = false)
//    private int displayOrder;
	
	
    
    @Column(name = "dataSetName" , nullable = false)
    private String dataSetName = "";

    @Column(name = "dataSetDescription", nullable = false)
    private String dataSetDescription = "";

    @Column(name = "dataSetType", nullable = false)
    private DataSetType dataSetType = DataSetType.ANY;

    /**
     * Strictly speaking this should be part of the UI construction
     * but it's in the legacy plugin Spec so I'm capturing it here
     * while the legacy plugin is being loaded.
     */
    @Lob
    @Column(name = "tooltip",nullable = false,length = 1024) // Half a page
    private String tooltip = "";

    @Column(name = "required" , nullable = false)
    private boolean required = true;

    public DataSet() {
    }
    
    public DataSet(String name, String desc) {
        if (Check.isEmpty(name)) {
            throw new IllegalArgumentException("DataSet cannot have an Empty name");
        }
        dataSetName = name;
        dataSetDescription = desc==null ? "" : desc;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DataSet(");
        sb.append(dataSetName);
        if (! Check.isEmpty(dataSetDescription)) {
            sb.append(": ").append(dataSetDescription);
        }
        sb.append(')');
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        return dataSetName.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (! (o instanceof DataSet)) return false;
        return this.dataSetName.equals(((DataSet) o).dataSetName);
    }
    
//    public int getDisplayOrder() {
//        return displayOrder;
//    }
    
    public String getDataSetName() {
        return dataSetName;
    }

    public void setDataSetName(String n) {
        this.dataSetName = n == null ? "" : n;
    }
    
    @Transient
    public String getVisibleName() {
        if (Check.isEmpty(dataSetDescription)) {
            return dataSetName;
        }
        return dataSetDescription;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean reqd) {
        this.required = reqd;
    }

    public String getDataSetDescription() {
        return dataSetDescription;
    }

    public void setDataSetDescription(String d) {
        this.dataSetDescription = d==null ? "" : d;
    }
    
    public DataSetType getDataSetType() {
        return dataSetType;
    }

    public void setDataSetType(DataSetType type) {
        this.dataSetType = type;
    }

    public void setTooltip(String tt) {
        this.tooltip = tt == null ? "" : tt;
    }
    
    public String getTooltip() {
        return tooltip;
    }

}
