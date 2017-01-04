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
package com.diversityarrays.kdxplore.importdata.excelio;

import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;

import com.diversityarrays.kdsmart.db.entities.PlotAttribute;
import com.diversityarrays.util.Either;

@SuppressWarnings("nls")
class PlotAttributesWorksheet extends KdxploreWorksheet {
    
    static List<ImportField> PLOT_ATTRIBUTE_FIELDS;
    static {
        PLOT_ATTRIBUTE_FIELDS.add(new ImportField(PlotAttribute.class, "plotAttributeName", "Name"));
        PLOT_ATTRIBUTE_FIELDS.add(new ImportField(PlotAttribute.class, "plotAttributeAlias", "Alias"));
    }
    
    public PlotAttributesWorksheet() {
        super(new WorksheetInfo(WorksheetId.PLOT_ATTRIBUTES, PLOT_ATTRIBUTE_FIELDS));
    }

    @Override
    public DataError processWorksheet(Sheet sheet, WorkbookReadResult wrr) {

        EntityProcessor<PlotAttribute> entityProcessor = new EntityProcessor<PlotAttribute>() {
            @Override
            public Either<DataError, PlotAttribute> createEntity(Integer rowIndex) {
                PlotAttribute pa = new PlotAttribute();
                DataError error = wrr.addPlotAttribute(rowIndex, pa);
                if (error != null) {
                    return Either.left(error);
                }
                return Either.right(pa);
            }
        };

        return processWorksheet(sheet, PlotAttribute.class, entityProcessor, wrr);
    }
}
