/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016  Diversity Arrays Technology, Pty Ltd.

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
package com.diversityarrays.util;

import java.util.Arrays;

import org.apache.commons.collections15.Closure;

public class CombinationsAndPermutations {

	static public <T> void combinations(T[] names, int nPerCombination, Closure<T[]> collector) {
		T[] namesBeforeNameIndex = Arrays.copyOf(names, nPerCombination);
		
		for (int i = 0; i < names.length; ++i) {
			namesBeforeNameIndex[0] = names[i];
			combinations(namesBeforeNameIndex, 1, names, i+1, collector);
		}
	}
	
	static private <T> void combinations(T[] namesBeforeNameIndex, int fillIndex, T[] names, int nameIndex, Closure<T[]> collector) {
		if ( fillIndex >= namesBeforeNameIndex.length) {
			collector.execute(namesBeforeNameIndex);
		}
		else {
			int nNames = names.length;
			for (int i = nameIndex; i < nNames; ++i) {
				namesBeforeNameIndex[fillIndex] = names[i];
				combinations(namesBeforeNameIndex, fillIndex+1, names, i+1, collector);
			}
			namesBeforeNameIndex[fillIndex] = null;
		}
	}
	
	/*
	procedure generate(n : integer, A : array of any):
    if n = 1 then
          output(A)
    else
        for i := 1; i ≤ n; i += 1 do
            generate(n - 1, A)
            if n is odd then
                j ← 1
            else
                j ← i
            swap(A[j], A[n])
	 */
	
	static public <T> void permutations(T[] names, Closure<T[]> collector) {
		permutations(names.length, names, collector);
	}
	
	static private <T> void permutations(int nNames, T[] names, Closure<T[]> collector) {
		if (nNames == 1) {
			collector.execute(names);
		}
		else {
			for (int i = 1; i <= nNames; ++i) {
				permutations(nNames-1, names, collector);
				int jj = (1==(nNames & 1)) ? 1 : i;
				
				int j = jj-1;
				
				T tmp = names[j];
				names[j] = names[nNames-1];
				names[nNames-1] = tmp;
//				collector.execute(names);
			}
		}
	}

	private CombinationsAndPermutations() {
	}
}
