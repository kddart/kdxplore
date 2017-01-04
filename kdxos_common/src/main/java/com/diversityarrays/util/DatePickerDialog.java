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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.UIManager;

import net.pearcan.ui.GuiUtil;

import org.apache.commons.collections15.Closure;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.plaf.basic.CalendarHeaderHandler;
import org.jdesktop.swingx.plaf.basic.SpinningCalendarHeaderHandler;

@SuppressWarnings("nls")
public class DatePickerDialog extends JDialog {

	static {
		UIManager
				.put(CalendarHeaderHandler.uiControllerID,
						"org.jdesktop.swingx.plaf.basic.SpinningCalendarHeaderHandler");
		UIManager.put(SpinningCalendarHeaderHandler.FOCUSABLE_SPINNER_TEXT, Boolean.TRUE);
        UIManager.put(SpinningCalendarHeaderHandler.ARROWS_SURROUND_MONTH, Boolean.TRUE);
	}
	
	static public DateFormat[] getDefaultDateFormats() {
		List<DateFormat> tmp = new ArrayList<DateFormat>();
		
		tmp.add(new SimpleDateFormat("yyyy-MM-dd"));

		tmp.add(new SimpleDateFormat("dd/MMM/yyyy"));
		tmp.add(new SimpleDateFormat("dd/MM/yyyy"));
		
		tmp.add(new SimpleDateFormat("dd/MMM/yyyy"));
		tmp.add(new SimpleDateFormat("dd/MM/yyyy"));

		tmp.add(new SimpleDateFormat("yyyy/MMM/dd"));
		tmp.add(new SimpleDateFormat("yyyy/MM/dd"));
		
		return tmp.toArray(new DateFormat[tmp.size()]);
	}

	private final JXDatePicker datePicker = new JXDatePicker();

	private final Action cancel = new AbstractAction(Msg.ACTION_CANCEL()) {
		@Override
		public void actionPerformed(ActionEvent e) {
			dispose();
			onComplete.execute(null);
		}
	};

	private final Action save = new AbstractAction(Msg.ACTION_SAVE()) {
		@Override
		public void actionPerformed(ActionEvent e) {
			dispose();
			onComplete.execute(datePicker.getDate());
		}
	};

	private final Closure<Date> onComplete;

	public DatePickerDialog(Window owner, String title,
			Closure<Date> onComplete) {
		super(owner, title, ModalityType.APPLICATION_MODAL);

		this.onComplete = onComplete;
		
		datePicker.getMonthView().setZoomable(true);
		datePicker.setLinkPanel(null);
		datePicker.setFormats(getDefaultDateFormats());
		
		Box buttons = Box.createHorizontalBox();
		buttons.add(Box.createHorizontalStrut(10));
		buttons.add(new JButton(cancel));
		buttons.add(Box.createHorizontalStrut(10));
		buttons.add(new JButton(save));
		buttons.add(Box.createHorizontalGlue());

		Container cp = getContentPane();
		cp.add(datePicker.getMonthView(), BorderLayout.CENTER);
		cp.add(buttons, BorderLayout.SOUTH);

		pack();
	}

	@Override
	public void setVisible(boolean b) {
		GuiUtil.centreOnOwner(DatePickerDialog.this);
		super.setVisible(b);
	}

	public void setDateBounds(Date min, Date max) {
		if (min != null) {
			datePicker.getMonthView().setLowerBound(min);
		}
		if (max != null) {
			datePicker.getMonthView().setUpperBound(max);
		}
	}

	public void setDate(Date d) {
		if (d != null) {
			datePicker.setDate(d);
		}
	}
}
