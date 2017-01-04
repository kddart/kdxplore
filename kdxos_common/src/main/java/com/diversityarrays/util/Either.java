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

import org.apache.commons.collections15.Closure;

public class Either<L,R> {
	
	public static <L,R> Either<L,R> left(L l) {
		return new Either<L,R>(l, null);
	}

	public static <L,R> Either<L,R> right(R r) {
		return new Either<L,R>(null, r);
	}
	
	public void execute(Closure<L> leftOption, Closure<R> rightOption) {
		if (right == null) {
			leftOption.execute(left);
		}
		else {
			rightOption.execute(right);
		}
	}
	
	private final L left;
	private final R right;

	protected Either(L l, R r) {
		left = l;
		right = r;
	}
	
	@Override
	public String toString() {
		return left != null ? "Left:"+left : "Right:"+right;
	}

	public L left() {
		return left;
	}

	public boolean isLeft() {
		return left != null;
	}
	
	public R right() {
		return right;
	}
	
	public boolean isRight() {
		// so it's ok to have a null value for right
		return left == null;
	}

}
