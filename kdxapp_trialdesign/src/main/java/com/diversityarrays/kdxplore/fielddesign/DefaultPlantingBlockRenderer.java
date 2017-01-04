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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.function.BiPredicate;
import java.util.function.Function;

import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock;
import com.diversityarrays.kdxplore.fieldlayout.Side;
import com.diversityarrays.util.UnicodeChars;

import net.pearcan.color.ColorPair;

@SuppressWarnings("nls")
public class DefaultPlantingBlockRenderer<E> implements PlantingBlockRenderer<E> {

    static public final Color DEFAULT_BLOCK_COLOR = new Color(150, 150, 150);
    static public final Color DEFAULT_BORDER_COLOR = Color.LIGHT_GRAY;

    static private boolean DEBUG = Boolean.getBoolean(
            DefaultPlantingBlockRenderer.class.getSimpleName() + ".DEBUG");

    /**
     * If set, is used to obtain the colours for the <code>PlantingBlock</code>.
     * The foreground of the ColorPair is used to paint the border portions of the PlantingBlock
     * and the background for the main content.
     *
     */
    private Function<PlantingBlock<E>, ColorPair> blockColorPairProvider;

    /**
     * If non-null, will be used to override the <code>contentColorProvider</code> for the
     * <code>contentRenderer</code>.
     */
    private Function<E, Color> contentColorProvider;
    /**
     * If set, use this ContentRenderer implementation to draw the content of each PlantingBlock.
     */
    private ContentRenderer<E> contentRenderer;

    private Color activeBlockBorderColor = Color.RED;
    private float activeBlockBorderWidth = 3.0f;

    private Color selectedBlockBorderColor = Color.BLUE;
    private float selectedBlockBorderWidth = 3.0f;
    private float normalBlockBorderWidth = 1.0f;

    private boolean drawingEntries;
    private boolean entriesAsString;

    public DefaultPlantingBlockRenderer() { }

    public DefaultPlantingBlockRenderer(
            Function<PlantingBlock<E>, ColorPair> block_cpp,
            Function<E, Color> content_cp)
    {
        this.blockColorPairProvider = block_cpp;
        this.contentColorProvider = content_cp;
    }

    public boolean getEntriesAsString() {
        return entriesAsString;
    }
    public void setEntriesAsString(boolean b) {
        entriesAsString = b;
    }

    public ContentRenderer<E> getContentRenderer() {
        return contentRenderer;
    }

    public void setContentRenderer(ContentRenderer<E> renderer) {
        this.contentRenderer = renderer;
    }

    @Override
    public boolean isDrawingEntries() {
        return drawingEntries;
    }
    @Override
    public void setDrawingEntries(boolean draw) {
        drawingEntries = draw;
    }

    public void setBlockColorPairProvider(Function<PlantingBlock<E>, ColorPair> cpp) {
        this.blockColorPairProvider = cpp;
    }
    public void setContentColorProvider(Function<E, Color> cp) {
        this.contentColorProvider = cp;
    }

    @Override
    public void draw(Graphics2D g2d,
            Composite transparent,
            int lineHeight,
            FieldView<E> view,
            PlantingBlock<E> block,
            int width,
            int height,
            boolean isActiveBlock,
            boolean isSelected,
            boolean intersectsOtherBlocks)
    {
        Composite save = g2d.getComposite();

        ColorPair cp = blockColorPairProvider==null ? null : blockColorPairProvider.apply(block);

        Color blockColor;
        Color borderColor;
        if (cp != null) {
            blockColor = cp.getBackground();
            borderColor = cp.getForeground();
        }
        else {
            blockColor = DEFAULT_BLOCK_COLOR;
            borderColor = DEFAULT_BORDER_COLOR;
        }

        g2d.setComposite(transparent);
        g2d.setColor(blockColor);
        g2d.fillRect(0, 0, width, height);

        drawBorders(g2d, view, block, width, height, borderColor);

        g2d.setComposite(save);
        if (isActiveBlock) {
//            g2d.setComposite(save);
            g2d.setColor(activeBlockBorderColor);
            g2d.setStroke(new BasicStroke(activeBlockBorderWidth));
            g2d.draw(new Rectangle2D.Float(0, 0, width, height));
        }
        else {
            Color fg = isSelected ? selectedBlockBorderColor : Color.DARK_GRAY; // cp.getForeground();
            float strokeWidth = isSelected ? selectedBlockBorderWidth : normalBlockBorderWidth;
//            g2d.setComposite(transparent);
            g2d.setColor(fg);
            g2d.setStroke(new BasicStroke(strokeWidth));
            g2d.draw(new Rectangle2D.Float(0, 0, width, height));
        }

        g2d.setComposite(save);
        if (drawingEntries && block.getContentCount() > 0) {
            if (entriesAsString) {
                drawEntriesAsString(g2d, lineHeight, view, block);
            }
            else {
//                Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
//              g2d.setComposite(comp);
                drawEntries(g2d, transparent, lineHeight, view, block);
            }
        }

        // Last - the name - not transparent
        Color nameColor = intersectsOtherBlocks ? Color.RED : Color.BLACK;

        g2d.setColor(nameColor);
        int sx = 2;
        int sy = Math.max(height - lineHeight, lineHeight);
        g2d.drawString(block.getName(), sx, sy);

//        g2d.setComposite(save);
    }

    private void drawEntries(Graphics2D g2d,
            Composite transparent,
            int lineHeight,
            FieldView<E> view,
            PlantingBlock<E> block)
    {
        int cellWidth = view.getCellWidth();
        int cellHeight = view.getCellHeight();

        Function<E, Color> saveColorProvider = null;

        BiPredicate<Point, E> visitor;
        if (contentRenderer != null) {
            if (contentColorProvider != null) {
                saveColorProvider = contentRenderer.getContentColorProvider();
                contentRenderer.setContentColorProvider(contentColorProvider);
            }

            visitor = new BiPredicate<Point, E>() {
                @Override
                public boolean test(Point p, E e) {
                    if (e != null) {
                        // Transform to content location
                        Point vp = new Point(p.x * cellWidth, p.y * cellHeight);
                        contentRenderer.draw(g2d, transparent, vp, e, cellWidth, cellHeight, false);
                    }
                    return true;
                }
            };
        }
        else {
            visitor = new BiPredicate<Point, E>() {
                @Override
                public boolean test(Point p, E e) {
                    // convert to view offsets
                    if (e != null) {
                        Point vp = new Point(p.x * cellWidth, p.y * cellHeight);

                        Color c = null;
                        if (contentColorProvider != null) {
                            c = contentColorProvider.apply(e);
                        }
                        else {
                            int hc = e.toString().hashCode();
                            c = new Color(hc & 0x00ffffff);
                        }
                        g2d.setColor(c);
                        g2d.fillRect(vp.x, vp.y, cellWidth, cellHeight);
                    }
                    return true;
                }
            };
        }

        try {
            block.visitContentByPoint(visitor);
        }
        finally {
            if (saveColorProvider != null) {
                contentRenderer.setContentColorProvider(saveColorProvider);
            }
        }
    }

    private void drawEntriesAsString(Graphics2D g2d,
            int lineHeight,
            FieldView<E> view,
            PlantingBlock<E> block)
    {
        FontMetrics fm = g2d.getFontMetrics(g2d.getFont());
        int maxAdvance = fm.stringWidth("m");
        int cellWidth = view.getCellWidth();
        int cellHeight = view.getCellHeight();
        int maxChars = Math.max(1, Math.floorDiv(cellWidth, maxAdvance));

        int partHeight = (cellHeight - lineHeight) / 2 + lineHeight; // fm.getMaxAscent();

        if (DEBUG) {
            System.out.println(String.format(
                    "maxAdvance=%d  cellDim=%d x %d  maxChars=%d  lineOffset=%d",
                    maxAdvance,
                    cellWidth, cellHeight,
                    maxChars,
                    partHeight));
        }
        BiPredicate<Point, E> visitor = new BiPredicate<Point, E>() {
            @Override
            public boolean test(Point p, E e) {
                String s = e.toString();

                Point vp = new Point(p.x * cellWidth, p.y * cellHeight); // t.view.modelToView(p);
                int x = vp.x;
                int y = vp.y + partHeight;

                if (DEBUG) {
                    System.out.println(String.format(
                            "\tdraw EntryType(%s) @ (%d,%d)\tentryPos: model=%d,%d   view=%d,%d",
                            s,
                            x,y,
                            p.x, p.y,
                            vp.x, vp.y
                            ));
                }

                if (s.length() > maxChars) {
                    if (maxChars <= 1) {
                        s = s.substring(0,1);
                    }
                    else {
                        s = s.substring(0, maxChars-1) + UnicodeChars.ELLIPSIS;
                    }
                }
                int swid = fm.stringWidth(s);

                if (swid < cellWidth) {
                    x += (cellWidth - swid) / 2;
                }
                String ds = s; // String.format("%d,%d: %s", p.x, p.y, s);
                g2d.drawString(ds, x, y);
                return true;
            }
        };
        block.visitContentByPoint(visitor);
        if (DEBUG) System.out.println("-----------------------------");
    }

    private void drawBorders(Graphics2D g2d,
            FieldView<E> view,
            PlantingBlock<E> block,
            int width,
            int height,
            Color borderColor) {
        int cw = view.getCellWidth();
        int ch = view.getCellHeight();

        g2d.setColor(borderColor);
        int[] borderCounts = block.getBorderCountBySide();
        for (Side side : Side.values()) {
            int x, y;
            int w, h;
            switch (side) {
            case NORTH:
                h = borderCounts[side.ordinal()] * ch;
                w = width;
                x = 0;
                y = 0 - h;

                x -= borderCounts[Side.WEST.ordinal()] * cw;

                w += borderCounts[Side.WEST.ordinal()] * cw;
                w += borderCounts[Side.EAST.ordinal()] * cw;
                break;
            case SOUTH:
                h = borderCounts[side.ordinal()] * ch;
                w = width;
                x = 0;
                y = 0 + height;

                x -= borderCounts[Side.WEST.ordinal()] * cw;

                w += borderCounts[Side.WEST.ordinal()] * cw;
                w += borderCounts[Side.EAST.ordinal()] * cw;
                break;
            case WEST:
                w = borderCounts[side.ordinal()] * cw;
                h = height;
                x = 0 - w;
                y = 0;
                break;
            case EAST:
                w = borderCounts[side.ordinal()] * cw;
                h = height;
                x = 0 + width;
                y = 0;
                break;
            default:
                throw new RuntimeException("Side=" + side);
            }
            g2d.fillRect(x, y, w, h);
        }
    }

}
