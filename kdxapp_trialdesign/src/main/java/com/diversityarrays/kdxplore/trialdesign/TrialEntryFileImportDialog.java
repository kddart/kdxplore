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
package com.diversityarrays.kdxplore.trialdesign;

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.diversityarrays.kdxplore.design.EntryFileException;
import com.diversityarrays.kdxplore.design.EntryFileImportDialog;
import com.diversityarrays.kdxplore.design.HeadingRoleTableModel;
import com.diversityarrays.kdxplore.design.RowDataProvider;
import com.diversityarrays.util.Either;

public class TrialEntryFileImportDialog extends EntryFileImportDialog<TrialEntryFile,TrialHeading>{

    private static final Function<String, Optional<TrialHeading>> CLASSIFIER = new Function<String, Optional<TrialHeading>>() {
        @Override
        public Optional<TrialHeading> apply(String in_hdg) {
            String hdg = in_hdg.toLowerCase().replaceAll(" *", "");  //$NON-NLS-1$//$NON-NLS-2$
            for (TrialHeading h : TrialHeading.values()) {
                if (hdg.equalsIgnoreCase(h.display)) {
                    return Optional.of(h);
                }

                if (h.matchWords != null) {
                    for (String w : h.matchWords) {
                        if (hdg.equalsIgnoreCase(w)) {
                            return Optional.of(h);
                        }
                    }
                }

                if (TrialHeading.NESTING == h) {
                    if (in_hdg.toLowerCase().startsWith("nest ")) { //$NON-NLS-1$
                        return Optional.of(TrialHeading.NESTING);
                    }
                }
            }
            return Optional.empty();
        }
    };

    public TrialEntryFileImportDialog(Window owner, String title, File inputFile) {
        super(owner, title, inputFile);
    }

    @Override
    protected Either<String, Set<String>> checkValidity(Map<String, TrialHeading> roleByHeading) {
        return TrialEntryFile.checkValidity(roleByHeading);
    }

    @Override
    protected HeadingRoleTableModel<TrialHeading> createHeadingRoleTableModel() {
        return new HeadingRoleTableModel<>(TrialHeading.class, TrialHeading.values(), TrialHeading.DONT_USE, CLASSIFIER);
    }

    @Override
    protected TrialEntryFile createEntryFile(RowDataProvider rdp,
            Map<String, TrialHeading> roleByHeading) throws IOException, EntryFileException
    {
        TrialEntryFileParams entryParams = new TrialEntryFileParams(rdp, roleByHeading);
        return new TrialEntryFile(entryParams);
    }

}
