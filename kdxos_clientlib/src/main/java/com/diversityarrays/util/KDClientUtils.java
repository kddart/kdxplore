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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableRowSorter;

import net.pearcan.ui.GuiUtil;
import net.pearcan.util.Util;

public class KDClientUtils {

	static private Map<ImageId,BufferedImage> IMAGE_CACHE = new HashMap<>();

	static private Map<String, Cursor> CURSOR_CACHE = new HashMap<>();

	static public Cursor getEraserCursor(boolean small) {
        return getCursor(ImageId.ERASER_24, small, "Eraer",
                (sz) -> new Point(8,sz.height-1));
	}

	static public Cursor getOpenHandCursor(boolean small) {
        return getCursor(ImageId.HAND_OPEN_24, small, "OpenHand",
                (sz) -> new Point(sz.width/2, sz.height/2));
	}

    static public Cursor getClosedHandCursor(boolean small) {
        return KDClientUtils.getCursor(ImageId.HAND_CLOSED_24, small, "ClosedHand",
                (sz) -> new Point(sz.width/2, sz.height/2));
    }

    static public Cursor getCursor(ImageId id, boolean small, String name,
            Function<Dimension, Point> sizeToHotspot)
    {
        String cursorName = small ? name + "-small" : name;
        Cursor result = CURSOR_CACHE.get(cursorName);
        if (result == null) {
            BufferedImage image = getImage(id);
            int imageWid = image.getWidth();
            int imageHyt = image.getHeight();
            if (small) {
                int wid = imageWid / 2;
                int hyt = imageHyt / 2;
                BufferedImage bi = new BufferedImage(wid, hyt, image.getType());
                Graphics g = bi.getGraphics();
                g.drawImage(image,
                        0, 0, wid-1, hyt-1,
                        0, 0, image.getWidth()-1, image.getHeight()-1,
                        null);
                g.dispose();
                image = bi;
                imageWid = image.getWidth();
                imageHyt = image.getHeight();
            }
            Point hotspot = sizeToHotspot.apply(new Dimension(imageWid, imageHyt));
            result = Toolkit.getDefaultToolkit().createCustomCursor(image, hotspot, cursorName);

            CURSOR_CACHE.put(cursorName, result);
        }
        return result;
    }

	static public BufferedImage getImage(ImageId imageId) {
		BufferedImage result = IMAGE_CACHE.get(imageId);
		if (result==null) {
			InputStream is = null;
			try {
				is = KDClientUtils.class.getResourceAsStream("images/"+imageId.resourceName);
				if (is!=null) {
					result = ImageIO.read(is);
					IMAGE_CACHE.put(imageId, result);
				}
			} catch (IOException e) {
			} finally {
				if (is!=null) {
					try { is.close(); } catch (IOException ignore) {}
				}
			}

			if (result==null) {
				System.err.println("Missing resource: " +
						KDClientUtils.class.getName() + "/images/" + imageId.resourceName);
			}
		}
		return result;
	}

	static private final Map<ImageId,ImageIcon> ICON_CACHE = new HashMap<>();

	static public ImageIcon getIcon(ImageId imageId) {
		ImageIcon icon = ICON_CACHE.get(imageId);
		if (icon==null) {
			BufferedImage img = getImage(imageId);
			if (img != null) {
				icon = new ImageIcon(img);
				ICON_CACHE.put(imageId, icon);
			}
		}
		return icon;
	}

	static public void initAction(ImageId imageId, Action action, String ttt) {
		initAction(imageId, action, ttt, false);
	}


	static public void initAction(ImageId imageId, Action action, String ttt, boolean keepName) {
		ImageIcon icon = getIcon(imageId);
		initAction(icon, action, ttt, keepName);
	}

	public static void initAction(ImageIcon icon, Action action, String ttt, boolean keepName) {
		if (icon != null) {
			action.putValue(Action.SMALL_ICON, icon);
			if (! keepName) {
				action.putValue(Action.NAME, null);
			}
		}
		action.putValue(Action.SHORT_DESCRIPTION, ttt);
	}


	static public enum ResizeOption {
		OFF("Scrollbar", JTable.AUTO_RESIZE_OFF),
		ALL("Resize All Columns", JTable.AUTO_RESIZE_ALL_COLUMNS),
		NEXT("Resize Next Column", JTable.AUTO_RESIZE_NEXT_COLUMN),
		SUBSEQUENT("Resize Subsequent Columns", JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS),
		LAST("Resize Last Column", JTable.AUTO_RESIZE_LAST_COLUMN),
		;

		public final String displayName;
		public final int resizeMode;
		ResizeOption(String s, int resizeMode) {
			this.displayName = s;
			this.resizeMode = resizeMode;
		}

		@Override
		public String toString() {
			return displayName;
		}
	}

	static public Font makeFont(Component comp, float mult) {
		Font font = comp.getFont();
		return font.deriveFont(font.getSize2D() *mult);
	}

	static public JScrollPane createTableScrollPane(final JTable table)
	{
		table.setRowSorter(new TableRowSorter<>(table.getModel()));
		return new JScrollPane(table);
	}

	static public final String RESIZE_OPTIONS_COMPONENT_NAME = "resizeOptions";

	@SuppressWarnings("unchecked")
	static public JComboBox<ResizeOption> findResizeCombo(Box box) {
		for (Component c : box.getComponents()) {
			if ((c instanceof JComboBox) && RESIZE_OPTIONS_COMPONENT_NAME.equals(c.getName())) {
				return (JComboBox<ResizeOption>) c;
			}
		}
		return null;
	}

	static public Box createResizeControls(final JTable table, Font fontForResizeControls,
			Component... moreComponents)
	{
		return createResizeControls(table, fontForResizeControls, ResizeOption.ALL, moreComponents);
	}

	static public Box createResizeControls(final JTable table, Font fontForResizeControls,
			ResizeOption initial,
			Component... moreComponents)
	{
		Box resizeControls = Box.createHorizontalBox();

		if (moreComponents != null && moreComponents.length > 0) {
			for (Component c : moreComponents) {
			    if (c != null) {
	                resizeControls.add(c);
			    }
			}
		}

		final JButton resizeColumns = new JButton(getIcon(ImageId.RESIZE_HORZ));
		resizeColumns.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GuiUtil.initialiseTableColumnWidths(table);
			}
		});
		resizeColumns.setToolTipText("Redistribute column widths");

		@SuppressWarnings({ "rawtypes", "unchecked" })
		final JComboBox resizeChoices = new JComboBox(ResizeOption.values());
		resizeChoices.setName(RESIZE_OPTIONS_COMPONENT_NAME);
		resizeChoices.setEditable(false);
		resizeChoices.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				ResizeOption option = (ResizeOption) resizeChoices.getSelectedItem();
				table.setAutoResizeMode(option.resizeMode);
			}
		});
		resizeChoices.setSelectedItem(initial);
		table.setAutoResizeMode(initial.resizeMode);

		table.getModel().addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (table.getModel().getRowCount() > 0) {
					table.getModel().removeTableModelListener(this);
					GuiUtil.initialiseTableColumnWidths(table);
				}
			}
		});

		JLabel label = new JLabel("Column sizing");

		if (fontForResizeControls != null) {
			resizeChoices.setFont(fontForResizeControls);
			label.setFont(fontForResizeControls);
		}

		resizeControls.add(Box.createHorizontalGlue());
		resizeControls.add(resizeColumns);
		resizeControls.add(Box.createHorizontalStrut(4));
		resizeControls.add(label);
		resizeControls.add(resizeChoices);

		return resizeControls;
	}

	static public Font makeSmallFont(JComponent c) {
		Font result;
		if (Util.isMacOS()) {
			result = makeFont(c, 0.8f);
		}
		else {
			result = c.getFont();
		}
		return result;
	}


	static public void initLookAndFeel() {
		try {
			UIManager.setLookAndFeel(
					(LookAndFeel) Class.forName(
							UIManager.getSystemLookAndFeelClassName()).newInstance());
		} catch (Exception warn) {
			System.err.println(KDClientUtils.class.getName()+".initLookAndFeel: " + warn.getMessage());
		}
	}

	static public void initUncaughtExceptionHandler() {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);

				pw.println("Uncaught exception in Thread "+t);
				pw.println("=============");
				e.printStackTrace(pw);
				pw.close();

				String msg = sw.toString();
				System.err.println(msg);

				JOptionPane.showMessageDialog(null, msg, "Unexpected Error - Please report to IT Support", JOptionPane.ERROR_MESSAGE);
			}
		});
	}
}
