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
package com.diversityarrays.update;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import com.diversityarrays.util.KDXploreUpdate;
import com.diversityarrays.util.Msg;

import net.pearcan.util.GBH;

public class UpdatePanel extends JPanel{


	public UpdatePanel(UpdateCheckRequest updateCheckRequest, long daysToGo) {

	    KDXploreUpdate kdxploreUpdate = updateCheckRequest.kdxploreUpdate;

		StringBuilder sb = new StringBuilder("<HTML>"); //$NON-NLS-1$
		if (kdxploreUpdate.versionCode <= updateCheckRequest.versionCode) {
			sb.append(Msg.HTML_NO_NEW_UPDATES_CURRENTLY_AVAILABLE());
		}
		else {
			sb.append(Msg.HTML_A_NEW_UPDATE_FOR_XX_IS_AVAILABLE("KDXplore")); //$NON-NLS-1$
		}
		if (daysToGo > 0) {
			sb.append(Msg.HTML_THIS_VERSION_EXPIRES_IN_N_DAYS((int) daysToGo));
		}

		GBH gbh = new GBH(this);

		String updateInformation = "<html>" + kdxploreUpdate.helpHtml + "</html>"; //$NON-NLS-1$ //$NON-NLS-2$

		JPanel informationBox = new JPanel();
		JLabel helpInfo = new JLabel(updateInformation);
		informationBox.add(helpInfo);

		int y =0;
		gbh.add(1, y, 1, 1, GBH.NONE, 1, 0, GBH.NORTH, sb.toString());
		y++;
		gbh.add(1, y, 1, 1, GBH.HORZ, 1, 0, GBH.NORTH, new JSeparator());
		y++;
		gbh.add(1, y, 1, 1, GBH.NONE, 1, 0, GBH.NORTH, Msg.TEXT_CURRENT_VERSION(updateCheckRequest.versionName));
		y++;

		if (kdxploreUpdate.versionCode != updateCheckRequest.versionCode) {
			gbh.add(1, y, 1, 1, GBH.NONE, 1, 0, GBH.NORTH, Msg.TEXT_UPDATE_VERSION(kdxploreUpdate.versionName));
			y++;
			gbh.add(1, y, 1, 1, GBH.NONE, 1, 0, GBH.NORTH, Msg.TEXT_UPDATE_SIZE(kdxploreUpdate.updateSize));
			y++;
		}

		gbh.add(1, y, 1, 1, GBH.HORZ, 1, 0, GBH.CENTER, new JSeparator());
		y++;
		gbh.add(1, y, 1, 1, GBH.NONE, 1, 0, GBH.CENTER, "<html>" + Msg.HTML_PREFIX_UPDATE_DETAILS()); //$NON-NLS-1$
		y++;
		gbh.add(1, y, 1, 2, GBH.BOTH, 1, 0, GBH.SOUTH, informationBox);

	}

}
