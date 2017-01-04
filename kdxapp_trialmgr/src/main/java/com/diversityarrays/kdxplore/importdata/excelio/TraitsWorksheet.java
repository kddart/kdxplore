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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.diversityarrays.daldb.InvalidRuleException;
import com.diversityarrays.daldb.ValidationRule;
import com.diversityarrays.daldb.ValidationRuleType;
import com.diversityarrays.daldb.ValidationRule.Calculated;
import com.diversityarrays.daldb.ValidationRule.Choice;
import com.diversityarrays.daldb.ValidationRule.ElapsedDays;
import com.diversityarrays.daldb.ValidationRule.Range;
import com.diversityarrays.kdsmart.db.KDSmartDbUtil;
import com.diversityarrays.kdsmart.db.entities.Trait;
import com.diversityarrays.kdsmart.db.entities.TraitDataType;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.Pair;

import net.pearcan.excel.ExcelUtil;

@SuppressWarnings("nls")
class TraitsWorksheet extends KdxploreWorksheet {

    static private final String HDG_TRAIT_VALIDATION = "Trait Validation";

    static private final List<ImportField> TRAIT_FIELDS = new ArrayList<>();

    static {
        TRAIT_FIELDS.add(new ImportField(Trait.class, "traitName", "Trait Name"));
        TRAIT_FIELDS.add(new ImportField(Trait.class, "traitDescription", "Trait Description"));
        TRAIT_FIELDS.add(new ImportField(Trait.class, "traitLevel", "Trait Level"));
        TRAIT_FIELDS.add(new ImportField(Trait.class, "traitAlias", "Trait Alias"));
        TRAIT_FIELDS.add(new ImportField(Trait.class, "traitUnit", "Trait Unit"));
        TRAIT_FIELDS.add(new ImportField(Trait.class, "barcode", "Barcode"));
        TRAIT_FIELDS.add(new ImportField(Trait.class, "idDownloaded", "Database Id"));
        TRAIT_FIELDS.add(new ImportField(Trait.class, "traitDataType", "Trait Data Type"));
        TRAIT_FIELDS.add(new ImportField(Trait.class, "traitValRule", HDG_TRAIT_VALIDATION));

    }
    
    public TraitsWorksheet() {
        super(new WorksheetInfo(WorksheetId.TRAITS, TRAIT_FIELDS));
    }

    @Override
    public DataError processWorksheet(Sheet sheet, WorkbookReadResult wrr) {

        int nRows = ExcelUtil.getRowCount(sheet);
        
        HeadingRow headingRow = null;
        int lastColumnIndex = -1;
        
        for (int rowIndex = 0; rowIndex < nRows; ++rowIndex) {
            Row rrow = sheet.getRow(rowIndex);
            if (rrow == null) {
                continue;
            }
            
            List<String> cellValues = getCellValuesIfAnyNonBlank(rrow);
            if (cellValues == null) {
                continue;
            }
            
            if (headingRow==null) {
                Either<List<Pair<Integer,String>>, HeadingRow> either = worksheetInfo.scanHeadingRow(cellValues);
                if (either.isLeft()) {
                    String errmsg = either.left().stream()
                        .map(pair -> pair.first + ":" + pair.second)
                        .collect(Collectors.joining(","));
                    
                    return new DataError(rowIndex, errmsg);
                }
                headingRow = either.right();
                if (headingRow.importFieldByColumnIndex.isEmpty()) {
                    return new DataError(rowIndex,
                            "No Column Headings found in worksheet '" + sheet.getSheetName() + "'");
                }
                List<Integer> columnIndices = new ArrayList<>(headingRow.importFieldByColumnIndex.keySet());
                Collections.sort(columnIndices, Collections.reverseOrder());

                ImportField lastImportField = headingRow.importFieldByColumnIndex.get(lastColumnIndex);
                if (! "traitValRule".equals(lastImportField.fieldName)) {
                    return new DataError(rowIndex,
                            "Last Column Heading must be '" + HDG_TRAIT_VALIDATION + "'");
                }
            }
            else {
                Either<DataError,Trait> either = getTraitFromCellValues(worksheetInfo, headingRow, rowIndex, cellValues, lastColumnIndex);
                if (either.isLeft()) {
                    return either.left();
                }
                String errmsg = wrr.addTrait(either.right());
                if (! Check.isEmpty(errmsg)) {
                    return new DataError(rowIndex, errmsg);
                }
            }
            
        }
        return null;
    }
    
    private final Predicate<ImportField> requiredIfNotTraitLevelAndNonNull = new Predicate<ImportField>() {
        @Override
        public boolean test(ImportField importField) {
            if ("traitLevel".equals(importField.fieldName)) {
                return false;
            }
            return ! KdxploreDatabase.Util.canBeNull(importField.field);
        }
    };
    
    private Either<DataError,Trait> getTraitFromCellValues(
            WorksheetInfo wsi, 
            HeadingRow headingRow, 
            int rowIndex, 
            List<String> cellValues,
            int lastColumnIndex)
    {
        Set<ImportField> required = wsi.getRequiredFields(requiredIfNotTraitLevelAndNonNull);
        
        RowData rowData = wsi.collectImportFields(headingRow.importFieldByColumnIndex, cellValues);

        required.removeAll(rowData.importFieldsAndCellValues.stream()
                .map(p -> p.first)
                .collect(Collectors.toList()));

        if (! required.isEmpty()) {
            String msg = required.stream()
                .map(f -> f.wsHeading)
                .collect(Collectors.joining(",", "Missing: ",""));
            return Either.left(new DataError(rowIndex, msg));
        }
        // All required ones are present
        
        Trait trait = new Trait();
        // Force it null so we can detect if it isn't set by the user.
        trait.setTraitLevel(null);

        for (Pair<ImportField,String> pair : rowData.importFieldsAndCellValues) {
            ImportField importField = pair.first;
            String value = pair.second;
            
            Either<String, Object> either = KDSmartDbUtil.convertValueOrError(Trait.class, importField.field, value);
            
            if (either.isLeft()) {
                return Either.left(new DataError(rowIndex, either.left()));
            }
            
            try {
                importField.field.set(trait, either.right());
            }
            catch (IllegalArgumentException | IllegalAccessException e) {
                String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
                return Either.left(new DataError(rowIndex, msg));
            }
        }
        
        TraitDataType tdt = trait.getTraitDataType();
        if (tdt == null) {
            return Either.left(new DataError(rowIndex, "Missing Trait Data Type"));
        }
        String traitValRule = trait.getTraitValRule();
        switch (tdt) {
        case CATEGORICAL:
            // First see if they provided the rule directly
            try {
                Pattern pattern = Pattern.compile("^([^|]+|).*$");
                Matcher m = pattern.matcher(traitValRule);
                ValidationRule vrule;
                if (m.matches()) {
                    vrule = ValidationRule.create("CHOICE(" + traitValRule + ")");
                }
                else {
                    vrule = ValidationRule.create(traitValRule);
                }
                trait.setTraitValRule(vrule.asNormalisedRuleString());
            }
            catch (InvalidRuleException e) {
                int nCells = cellValues.size();
                // Nope .. lets see if the group of cells will give it to us
                StringBuilder sb = new StringBuilder("CHOICE(");
                sb.append(traitValRule);
                for (int cellIndex = lastColumnIndex + 1; cellIndex < nCells; ++cellIndex) {
                    String cellValue = cellValues.get(cellIndex);
                    if (Check.isEmpty(cellValue)) {
                        break;
                    }
                    sb.append('|').append(cellValue);
                }
                sb.append(')');
                
                try {
                    ValidationRule vrule = ValidationRule.create(traitValRule);
                    if (! (vrule instanceof Choice)) {
                        return Either.left(new DataError(rowIndex, "Rule mismatch (looking for CATEGORICAL)"));
                    }
                    trait.setTraitValRule(vrule.asNormalisedRuleString());
                }
                catch (InvalidRuleException e1) {
                    return Either.left(new DataError(rowIndex, "Unable to get CATEGORICAL rule"));
                }
            }
            break;
        case TEXT:
            if (! Check.isEmpty(traitValRule)) {
                return Either.left(new DataError(rowIndex, "TEXT does not have validation"));
            }
            break;
        case CALC:
        case DATE:
        case DECIMAL:
        case ELAPSED_DAYS:
        case INTEGER:
            Pair<ValidationRuleType, String> pair = ValidationRuleType.parseTypeAndExpression(trait.getTraitValRule());
            if (pair == null) {
                return Either.left(new DataError(rowIndex, "Missing or incorrect validation rule"));
            }

            try {
                ValidationRule vrule = ValidationRule.create(traitValRule);
                
                boolean matched = false;
                switch (tdt) {
                case CALC:
                    matched = vrule instanceof Calculated;
                    break;
                case DATE:
                    matched = "CHOICE(date)".equalsIgnoreCase(vrule.getExpression());
                    break;
                case DECIMAL:
                case INTEGER:
                    matched = (vrule instanceof Range) && ! (vrule instanceof ElapsedDays);
                    break;
                case ELAPSED_DAYS:
                    matched = vrule instanceof ElapsedDays;
                    break;

                case CATEGORICAL:
                case TEXT:
                default:
                    throw new IllegalStateException();
                }
                
                if (! matched) {
                    return Either.left(new DataError(rowIndex, "Validation Rule does not match the TraitDataType"));
                }
                
                trait.setTraitValRule(vrule.asNormalisedRuleString());
            }
            catch (InvalidRuleException e) {
                return Either.left(new DataError(rowIndex, e.getMessage()));
            }
            
            break;
        default:
            return Either.left(new DataError(rowIndex, "Unsupported TraitDataType: " + tdt.name()));
        }
        
        return Either.right(trait);
    }
}
