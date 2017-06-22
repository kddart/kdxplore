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

public enum KnobDataType {
    CHOICE, // set of text values  "choice(a|b|c)"
//    DATE,  // date (possibly constrained) "date"
    DECIMAL, // with n decimal places  various forms
    INTEGER,
    TEXT,
    FORMULA,

    // TODO: something that gives a set of column numbers, the "prompts" are the
    
    FILE_UPLOAD;
    
    public String getDefaultValidationRuleString() {
        switch (this) {
        case CHOICE:
            return "a|b|c";
//        case DATE:
//            return "";
        case DECIMAL:
            return "0..100.0";
        case TEXT:
            return "text";
        case FILE_UPLOAD:
            return "";
        case FORMULA:
            return "javascript: return 1";
        case INTEGER:
            return "0..100";
        default:
            break;
        }
        throw new RuntimeException("Not yet supported: " + this.name());
    }

    public boolean requiresValidationRule() {
        switch (this) {
//        case DATE:
        case TEXT:
            return false;
        case CHOICE:
        case DECIMAL:
        case FILE_UPLOAD:
        case FORMULA:
        case INTEGER:
        default:
            break;
        }
        return ! getDefaultValidationRuleString().isEmpty();
    }

    // TODO replace with a Msg translation
    public String getDisplayName() {
        return name();
    }
}
