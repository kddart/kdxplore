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
package com.diversityarrays.kdxplore.design;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.diversityarrays.util.Check;

import net.pearcan.ui.table.BspAbstractTableModel;

public class FileTableModel extends BspAbstractTableModel {

    private final List<File> files = new ArrayList<>();
    private final Map<File,FileType> typeByFile = new HashMap<>();
    
    FileTableModel() {
        super("Name", "Info");
    }
    
    public void addData(List<File> list) {
        for (File f : list) {
            if (! files.contains(f)) {
                int r = files.size();
                files.add(f);
                fireTableRowsInserted(r, r);
            }
        }
    }

    @Override
    public int getRowCount() {
        return files.size();
    }
    
    @Override
    public Class<?> getColumnClass(int col) {
        switch (col) {
        case 0: return String.class;
        case 1: return FileType.class;
        }
        return Object.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        File f = files.get(rowIndex);
        switch (columnIndex) {
        case 0: return f.getName();
        case 1: return typeByFile.get(f);
        }
        return null;
    }

    public void removeFilesAt(List<Integer> rowIndices) {
        if (Check.isEmpty(rowIndices)) {
            return;
        }
        List<File> filesToRemove = rowIndices.stream()
            .map(r -> files.get(r))
            .collect(Collectors.toList());
        for (File f : filesToRemove) {
            int rowIndex = files.indexOf(f);
            if (rowIndex >= 0) {
                files.remove(f);
                fireTableRowsDeleted(rowIndex, rowIndex);
            }
        }
    }
}
