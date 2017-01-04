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
package com.diversityarrays.kdxplore.importdata.bms;

import org.apache.poi.ss.usermodel.IndexedColors;

public enum BmsCellColor {
	NO_COLOR(IndexedColors.BLACK, "0:0:0"), 
	TRIAL_NAME_ETC_BROWN(IndexedColors.BROWN,"9999:3333:0"), 
	CONDITION_OR_FACTOR_GREEN(IndexedColors.SEA_GREEN, "3333:9999:6666"), 
	CONSTANT_OR_VARIATE_PURPLE(IndexedColors.INDIGO, "3333:3333:9999"), 
	
	PLUM(IndexedColors.PLUM, "9999:3333:6666"),
	DARK_RED(IndexedColors.DARK_RED, "8080:0:0") // Looks a bit like brown :-)
	;

	public final String hexString;
	private byte[] rgbValues;
	public final IndexedColors indexedColor;

	BmsCellColor(IndexedColors ic, String rgb) {
		this.hexString = rgb;
		this.indexedColor = ic;
	}
	
	public byte[] getRgbValues() {
		if (rgbValues==null) {
			byte[] tmp = new byte[3];
			String[] parts = hexString.split(":");
			for (int i = 0; i < 3; ++i) {
				int b = Integer.parseInt(parts[i]);
				tmp[i] = (byte) (b & 0xff); 
			}
			rgbValues = tmp;
		}
		return rgbValues;
	}
}
