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
package com.diversityarrays.util;

import java.util.Stack;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Generate a sequence of "names" of the form:<pre>
 * A,   B,  C, ...  Z,
 * AA, AB, AC, ... AZ,
 * BA, BB, BC, ...
 * </pre>
 * @author brianp
 *
 */
public class NameMaker implements Supplier<String>, Function<Integer, String> {

    private int count = 0;
    private final int cycleAfter;
    private final int char0;

    /**
     * Create a NameMaker starting at 'A' and cycling after 26 letters.
     */
    public NameMaker() {
        this('A', 26);
    }

    /**
     * Create a NameMaker starting at the given character
     * and cycling after 26 letters.
     * <br>
     * Usually used with <code>'a'</code> as the parameter.
     * @param base
     */
    public NameMaker(char base) {
        this(base, 26);
    }

    /**
     * Create a NameMaker starting at the given character
     * and cycling after <code>cycleAfter</code> letters.
     * @param base
     */
    public NameMaker(char base, int cyleAfter) {
        this.char0 = base;
        this.cycleAfter = cyleAfter;
    }

    public void reset() {
        count = 0;
    }

    @Override
    public String get() {
        return apply(count++);
    }

    @Override
    public String apply(Integer t) {
        Stack<Integer> stack = new Stack<>();
        int n = t;

        while (n >= cycleAfter) {
            int ch = n % cycleAfter;
            stack.push(ch);
            n = (n / cycleAfter) - 1;
        }
        stack.push(n);

        StringBuilder sb = new StringBuilder();
        while (! stack.isEmpty()) {
            int val = stack.pop();
            sb.append((char) (char0 + val));
        }

        return sb.toString();
    }
}
