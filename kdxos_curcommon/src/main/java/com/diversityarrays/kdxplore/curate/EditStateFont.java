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
package com.diversityarrays.kdxplore.curate;


public enum EditStateFont {
	NORMAL(""),
	ITALIC("i"),
	BOLD("b"),
	BOLD_ITALIC(null)
	;
	
	private final String htmlWrapEntity;
	EditStateFont(String h) {
		this.htmlWrapEntity = h;
	}
	
	public void wrap(StringBuilder sb, String html) {
		switch (this) {
		case BOLD_ITALIC:
			sb.append("<b><i>")
				.append(html)
				.append("</i></b>");
			break;

		case BOLD:
		case ITALIC:
		case NORMAL:
		default:
			sb.append('<').append(htmlWrapEntity).append('>');
			sb.append(html);
			sb.append("</").append(htmlWrapEntity).append('>');
			break;
		}
	}

	public String getWrapPrefix() {
		switch (this) {
		case BOLD_ITALIC:
			return "<b><i>";
		case BOLD:
		case ITALIC:
		case NORMAL:
		default:
			return "<" + htmlWrapEntity + ">";
		}
	}

	public String getWrapSuffix() {
		switch (this) {
		case BOLD_ITALIC:
			return "</i></b>";
		case BOLD:
		case ITALIC:
		case NORMAL:
		default:
			return "</" + htmlWrapEntity + ">";
		}
	}
}
