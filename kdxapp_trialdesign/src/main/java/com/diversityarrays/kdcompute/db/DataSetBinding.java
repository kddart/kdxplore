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

/**
 * Note: equality means same inputDataSet.
 * @author brianp
 *
 */
@Embeddable
public class DataSetBinding {

    @Embedded
    @Column(name= "dataSet")
    private DataSet dataSet;

    @Column(name = "dataSetUrl", nullable = false)
    private String dataSetUrl = "";
    
    public DataSetBinding() {}
    
    public DataSetBinding(DataSet ds) {
        this(ds, "");
    }
    
    public DataSetBinding(DataSet ds, String url) {
        this.dataSet = ds;
        this.dataSetUrl = url;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DataSetBinding(ds=");
        sb.append(dataSet==null ? "<null>" : dataSet.getDataSetName());
        if (dataSetUrl != null) {
            sb.append(", url=").append(dataSetUrl);
        }
        
        sb.append(')');
        return sb.toString();
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public String getDataSetUrl() {
        return dataSetUrl;
    }
    
    public void setDataSetUrl(String s) {
        dataSetUrl = s;
    }
    
    @Override
    public int hashCode() {
        return dataSet.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (! (o instanceof DataSetBinding)) return false;
        return this.dataSet.equals(((DataSetBinding) o).dataSet);
    }
}
