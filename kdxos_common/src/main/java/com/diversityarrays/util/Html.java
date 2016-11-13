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

import net.pearcan.util.StringUtil;

public class Html {

	static public String buildHtmlLines(String wordText, int lineLengthLimit) {
		if (wordText == null || wordText.isEmpty()) {
			return ""; //$NON-NLS-1$
		}
		StringBuilder html = new StringBuilder();
		appendHtmlLines(html, wordText, lineLengthLimit);
		return html.toString();
	}

	static public void appendHtmlLines(StringBuilder html, String wordText, int lineLengthLimit) {
		if (wordText != null) {
			String[] lines = wordText.split("\n"); //$NON-NLS-1$
			for (String line : lines) {
				StringBuilder line_sb = new StringBuilder();

				for (String wd : line.split(" +")) { //$NON-NLS-1$
					if ((line_sb.length() + 1 + wd.length()) > lineLengthLimit) {
						html.append("<BR>").append(StringUtil.htmlEscape(line_sb.toString())); //$NON-NLS-1$
						line_sb = new StringBuilder();
					}
					else {
						line_sb.append(' ');
					}
					line_sb.append(wd);				
				}
				if (line_sb.length() > 0) {
					html.append("<BR>") //$NON-NLS-1$
						.append(StringUtil.htmlEscape(line_sb.toString()));
				}
			}
		}
	}
	
	private Html() {}
}

