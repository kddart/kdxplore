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
package com.diversityarrays.kdxplore.fielddesign;

import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.diversityarrays.kdxplore.fieldlayout.ChangeDimensionDialog;
import com.diversityarrays.kdxplore.fieldlayout.DesignParametersPanel;
import com.diversityarrays.util.ImageId;
import com.diversityarrays.util.KDClientUtils;

import net.pearcan.color.ColorGroups;
import net.pearcan.color.ColorPairFactory;
import net.pearcan.ui.GuiUtil;

@SuppressWarnings("nls")
public class ZoomAndDimensionWidget {

    static final int MAX_ROW_COUNT = 10000;
    static final int MAX_COLUMN_COUNT = 10000;

    private static final boolean USE_SEPARATORS = false;

    static private Icon getColourEntriesIcon(boolean fill) {

        List<Color> list = new ArrayList<>();
        Collections.addAll(list, ColorGroups.COLOURS_GROUPED_BY_BRIGHTNESS);
        Collections.reverse(list);
        ColorPairFactory colorPairFactory = new ColorPairFactory(list.toArray(new Color[list.size()]));

        int wh = 12;
        Point[] points = new Point[] { new Point(8,8), new Point(2,2), new Point(10,4) };
        BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics g = img.getGraphics();
        g.setColor(Color.GRAY);
        g.drawRect(0, 0, 23, 23);
        for (Point pt : points) {
            g.setColor(colorPairFactory.getNextColorPair().getBackground());
            if (fill) {
                g.fillRect(pt.x, pt.y, wh, wh);
            }
            else {
                g.drawRect(pt.x, pt.y, wh, wh);
            }
        }
        g.dispose();
        return new ImageIcon(img);
    }


    private final JCheckBox drawEntriesOption = new JCheckBox();
    private boolean changingDrawingEntries;
    private final PropertyChangeListener fieldViewPropertyChangeListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (FieldView.PROPERTY_DRAWING_ENTRIES.equals(evt.getPropertyName())) {
                changingDrawingEntries = true;
                try {
                    drawEntriesOption.setSelected(fieldView.isDrawingEntries());
                }
                finally {
                    changingDrawingEntries = false;
                }
            }
        }
    };

    private final FieldView<?> fieldView;

    private final JCheckBox autoFieldSize = new JCheckBox("Auto Size", false);

    private final Action zoomInAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            fieldView.zoomIn();
            updateZoomActions();
        }
    };

    private final Action zoomOutAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            fieldView.zoomOut();
            updateZoomActions();
        }
    };

    private final Action changeDimensionAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            doChangeDimension();
        }
    };

    private final String[] changeDimensionLines;
    private final JButton changeDimensionButton = new JButton(changeDimensionAction) {

        @Override
        protected void paintComponent(Graphics g) {
//            super.paintComponent(g);

            if (changeDimensionLines != null && changeDimensionLines.length > 0) {
                FontMetrics fm = g.getFontMetrics();
                int linehyt = fm.getMaxAscent() + fm.getMaxDescent();
                int y_drop = fm.getAscent();
                int maxWid = 0;
                for (String line : changeDimensionLines) {
                    maxWid = Math.max(maxWid, fm.stringWidth(line));
                }
                Rectangle bounds = getBounds();
                Insets insets = getInsets();

//                g.setColor(Color.LIGHT_GRAY);
//                g.fillRect(0, 0, bounds.width, bounds.height);

                int x = 0; //bounds.x + insets.left;
                int y = 0; //bounds.y + insets.top;
//                int w = bounds.width - (insets.left + insets.right);
                int h = bounds.height - (insets.top + insets.bottom);

                if (maxWid < h) {
                    x += (h - maxWid) / 2;
                }

                int yreqd = linehyt * changeDimensionLines.length;
                if (yreqd < h) {
                    y += (h - yreqd) / 2;
                }

                g.setColor(Color.BLACK);
                for (int i = 0; i < changeDimensionLines.length; ++i) {
                    String line = changeDimensionLines[i];
                    g.drawString(line, x + (i==1?2:0), y + y_drop);
                    y += linehyt;
                }
            }
        }

    };

    private final IntSupplier minimumFieldSizeSupplier;
    private final Box widgetComponent;
    private final Orientation orientation;
    private final JSlider opacitySlider;

    public ZoomAndDimensionWidget(FieldView<?> fieldView,
            Orientation orientation,
            boolean showSizeControls,
            boolean allowAutoSize,
            IntSupplier minimumFieldSizeSupplier)
    {
        this.fieldView = fieldView;
        this.orientation = orientation;

        changeDimensionAction.putValue(Action.SHORT_DESCRIPTION, "Click to change Site size");
        switch (orientation) {
        case HORZ:
            changeDimensionLines = new String[1];
            changeDimensionAction.putValue(Action.NAME, "x");
            break;
        case VERT:
        default:
            changeDimensionLines = new String[3];
            changeDimensionAction.putValue(Action.NAME, "<HTML>w<BR>x<BR>h");
            break;
        }
        changeDimensionButton.setForeground(changeDimensionButton.getBackground());


        this.minimumFieldSizeSupplier = minimumFieldSizeSupplier;

        autoFieldSize.setToolTipText("Check to alter size using " + DesignParametersPanel.DESIGN_PARAMS_HEADING);

        fieldView.addPropertyChangeListener(FieldView.PROPERTY_DRAWING_ENTRIES, fieldViewPropertyChangeListener);
        KDClientUtils.initAction(ImageId.ZOOM_IN_24, zoomInAction, "Zoom In");
        KDClientUtils.initAction(ImageId.ZOOM_OUT_24, zoomOutAction, "Zoom Out");
        updateZoomActions();

        setFieldSize(fieldView.getModel().getSize());

        drawEntriesOption.setSelected(fieldView.isDrawingEntries());
        drawEntriesOption.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (! changingDrawingEntries) {
                    fieldView.setDrawingEntries(drawEntriesOption.isSelected());
                }
            }
        });

        drawEntriesOption.setSelectedIcon(getColourEntriesIcon(true));
        drawEntriesOption.setIcon(getColourEntriesIcon(false));
        drawEntriesOption.setToolTipText("Draw Plots");

        if (Orientation.VERT == orientation) {
            changeDimensionButton.setHorizontalAlignment(SwingConstants.CENTER);
            Font font = changeDimensionButton.getFont();
            changeDimensionButton.setFont(font
                    .deriveFont(font.getSize2D() * 0.8f)
                    .deriveFont(Font.BOLD));
        }

        float opacity = fieldView.getPlantingBlockOpacity();
        int opacityValue = (int) Math.floor(10 * opacity);
        opacitySlider = new JSlider(JSlider.VERTICAL, 1, 9, opacityValue);
        drawEntriesOption.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSliderEnabled();
            }
        });
        updateSliderEnabled();

        opacitySlider.setMajorTickSpacing(1);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPaintLabels(true);

        opacitySlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int value = opacitySlider.getValue();
                float opacity = (value * 1.0f) / 10.0f;
                fieldView.setPlantingBlockOpacity(opacity);
            }
        });

        widgetComponent = orientation.createBox();

        if (showSizeControls && allowAutoSize) {
            widgetComponent.add(autoFieldSize);
        }

        if (Orientation.VERT == orientation) {
            changeDimensionButton.setBorder(new CompoundBorder(new EmptyBorder(0,2,0,2), new LineBorder(Color.GRAY)));
            changeDimensionButton.setMaximumSize(new Dimension(32, 48));
        }

        widgetComponent.add(changeDimensionButton);
        if (USE_SEPARATORS) {
            widgetComponent.add(orientation.createJSeparator());
        }
        widgetComponent.add(new JButton(zoomInAction));
        widgetComponent.add(new JButton(zoomOutAction));

        widgetComponent.add(drawEntriesOption);
        widgetComponent.add(opacitySlider);

        if (USE_SEPARATORS) {
            widgetComponent.add(orientation.createJSeparator());
        }

    }

    protected void updateSliderEnabled() {
        opacitySlider.setEnabled(drawEntriesOption.isSelected());
    }

    private void doChangeDimension() {

        Consumer<Dimension> undoChanges = new Consumer<Dimension>() {
            @Override
            public void accept(Dimension size) {
                fieldView.getModel().setColumnRowCount(size.width, size.height);
            }
        };

        Dimension size = new Dimension(fieldView.getModel().getColumnCount(),
                fieldView.getModel().getRowCount());

        ChangeDimensionDialog dlg = new ChangeDimensionDialog(
                GuiUtil.getOwnerWindow(widgetComponent),
                "Change Size",
                ModalityType.MODELESS,
                size,
                minimumFieldSizeSupplier.getAsInt(),
                undoChanges
                )
        {
            @Override
            protected void applyChanges(int wid, int hyt) {
                fieldView.getModel().setColumnRowCount(wid, hyt);
            }
        };
        dlg.setLocationRelativeTo(changeDimensionButton);
        dlg.setVisible(true);
    }

    private void updateZoomActions() {
        zoomInAction.setEnabled(fieldView.canZoomIn());
        zoomOutAction.setEnabled(fieldView.canZoomOut());
    }

    public void setFieldSize(Dimension sz) {
        switch (orientation) {
        case HORZ:
            changeDimensionLines[0] = String.format("%d x %d", sz.width, sz.height);
//            changeDimensionAction.putValue(Action.NAME,
//                    String.format("%d x %d", sz.width, sz.height));
            break;
        case VERT:
            changeDimensionLines[0] = Integer.toString(sz.width);
            changeDimensionLines[1] = "x";
            changeDimensionLines[2] = Integer.toString(sz.height);
//            changeDimensionAction.putValue(Action.NAME,
//                    String.format("<HTML>%d<BR>x<BR>%d", sz.width, sz.height));
            break;
        default:
            break;
        }
    }

    public JComponent getWidgetComponent() {
        return widgetComponent;
    }

}
