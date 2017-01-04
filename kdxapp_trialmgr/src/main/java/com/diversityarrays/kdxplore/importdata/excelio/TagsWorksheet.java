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

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;

import com.diversityarrays.kdsmart.db.entities.Tag;
import com.diversityarrays.util.Either;

@SuppressWarnings("nls")
class TagsWorksheet extends KdxploreWorksheet {
    
    static private final List<ImportField> TAG_FIELDS = new ArrayList<>();

    static {
        TAG_FIELDS.add(new ImportField(Tag.class, "label", "Label"));
        TAG_FIELDS.add(new ImportField(Tag.class, "description", "Description"));
    }

    public TagsWorksheet() {
        super(new WorksheetInfo(WorksheetId.TAGS, TAG_FIELDS));
    }

    @Override
    public DataError processWorksheet(Sheet sheet, WorkbookReadResult wrr) {

        EntityProcessor<Tag> entityProcessor = new EntityProcessor<Tag>() {
            @Override
            public Either<DataError, Tag> createEntity(Integer rowIndex) {
                Tag tag = new Tag();
                DataError error = wrr.addTag(rowIndex, tag);
                if (error != null) {
                    return Either.left(error);
                }
                return Either.right(tag);
            }
        };

        return processWorksheet(sheet, Tag.class, entityProcessor, wrr);
    }
}
