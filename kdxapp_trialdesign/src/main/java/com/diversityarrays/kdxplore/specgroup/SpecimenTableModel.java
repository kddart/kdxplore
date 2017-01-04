
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
package com.diversityarrays.kdxplore.specgroup;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.diversityarrays.daldb.core.Specimen;
import com.diversityarrays.kdxplore.gtools.searches.SpecimenSearcher;
import com.diversityarrays.kdxplore.gtools.searches.SpecimenSearcher.SpecimenSearchConsumer;
import com.diversityarrays.kdxplore.gtools.searches.SpecimenSearcher.SpecimenSearchParameter;

class SpecimenTableModel extends AbstractTableModel {

	private final List<Specimen> specimens = new ArrayList<>();

    private final SpecimenSearcher specimenSearcher;
	
	/**
	 * 
	 */
	public SpecimenTableModel(SpecimenSearcher searcher) {
	    specimenSearcher = searcher;
	}
	
	public void clear() {
	    specimens.clear();
	    fireTableDataChanged();
	}

	public void setData(List<Specimen> list) {
		this.specimens.clear();
		if (list != null) {
		    specimens.addAll(list);
		}
		this.fireTableDataChanged();
	}

	public void setData(List<Integer> specimenIds, SpecimenSearchParameter parameter) {
		
		SpecimenSearchConsumer consumer = new SpecimenSearchConsumer() {
            @Override
            public void handleError(Throwable error) {
            }
            
            @Override
            public void accept(List<Specimen> list) {
                for (Specimen spec : list) {
                    specimens.add(spec);
                }
                fireTableDataChanged(); 
            }
        };

        specimenSearcher.searchForSpecimens(parameter, specimenIds, consumer);
//			        "SpecimenName",
//					"SpecimenId", 
//					"BreedingMethodName");	
	}

	private static final String[] COLUMN_NAMES = {
		"Name",
		"ID",
		"Breeding Method"		
	};

	@Override
	public String getColumnName(int column) {
		return COLUMN_NAMES[column];	
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case 0: return String.class;
		case 1: return Integer.class;
		case 2: return String.class;
		}
		return Object.class;
	}

	@Override
	public int getRowCount() {
		if (specimens != null) {
			return specimens.size();
		}
		return 0;
	}

	@Override
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (rowIndex != -1 && columnIndex != -1) {
			Specimen specimen = specimens.get(rowIndex);
			if (specimen != null) {
				switch (columnIndex) {
				case 0 : 
					return specimen.getSpecimenName();
				case 1:
					return specimen.getSpecimenId();
				case 2:
				    return specimen.getExtraData("BreedingMethodName");
				}
			}
		}
		return null;
	}

	/**
	 * @param specimensTakeFromPlots
	 */
	public void addSpecimens(List<? extends Specimen> specimensTakeFromPlots) {
		for (Specimen specimen : specimensTakeFromPlots) {
			if (!specimens.contains(specimen)) {
				specimens.add(specimen);
			}
		}
		this.fireTableDataChanged();
	}

	/**
	 * @param row
	 * @return
	 */
	public Specimen getSpecimenAt(Integer row) {
		return specimens.get(row);
	}

	/**
	 * @param specimens2
	 */
	public void removeSpecimens(List<Specimen> specimens) {
		for (Specimen specimen : specimens) {
			if (this.specimens.contains(specimen)) {
				this.specimens.remove(specimen);
			}
		}
	}

	public List<Specimen> getSpecimens() {
		return this.specimens;
	}

}

