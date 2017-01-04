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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.pearcan.ui.widget.PromptTextField;

public class FilterTextField extends PromptTextField {

    private String cross;
    private int crossWidth;
    private int charHeight;
    
    private MouseListener mouseListener = new MouseListener() {
        int crossLeft;
        @Override
        public void mouseReleased(MouseEvent e) {}
        
        @Override
        public void mousePressed(MouseEvent e) {}
        
        @Override
        public void mouseExited(MouseEvent e) {}
        
        @Override
        public void mouseEntered(MouseEvent e) {
            Rectangle bounds = getBounds();
            crossLeft = bounds.width - crossWidth;
            Insets insets = getInsets();
            if (insets != null) {
                crossLeft -= insets.right;
            }
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if (! Check.isEmpty(getText()) && crossLeft > crossWidth) {
                Point pt = e.getPoint();
                if (pt.x >= crossLeft) {
                    setText(null);
                }
            }
        }
    };

    public FilterTextField() {
        super();
        initialiseCross();
        initialiseLocal();
    }
    public FilterTextField(String prompt) {
        super(prompt);
        initialiseCross();
        initialiseLocal();
    }
    public FilterTextField(int nColumns) {
        super(nColumns);
        initialiseCross();
        initialiseLocal();
    }
    public FilterTextField(String prompt, int nColumns) {
        super(prompt, nColumns);
        initialiseCross();
        initialiseLocal();
    }

    private DocumentListener documentListener = new DocumentListener() {
        @Override
        public void removeUpdate(DocumentEvent e) {
            fireFilterChangeHandlers();
        }
        @Override
        public void insertUpdate(DocumentEvent e) {
            fireFilterChangeHandlers();
        }
        @Override
        public void changedUpdate(DocumentEvent e) {
            fireFilterChangeHandlers();
        }
    };

    private final List<Consumer<String>> filterChangeHandlers = new ArrayList<>();
    public void addFilterChangeHandler(Consumer<String> consumer) {
        filterChangeHandlers.add(consumer);
    }

    protected void fireFilterChangeHandlers() {
        if (! filterChangeHandlers.isEmpty()) {
            String text = getText().trim();
            @SuppressWarnings("unchecked")
            Consumer<String>[] consumers = (Consumer<String>[]) Array.newInstance(Consumer.class, filterChangeHandlers.size());
            consumers = filterChangeHandlers.toArray(consumers);
            for (Consumer<String> c : consumers) {
                c.accept(text);
            }
        }
    }
    public void removeFilterChangeHandler(Consumer<String> consumer) {
        filterChangeHandlers.remove(consumer);
    }

    private void initialiseLocal() {
        addMouseListener(mouseListener);
        getDocument().addDocumentListener(documentListener);
    }
    
    protected char getDefaultCrossCharacter() {
        return '\u2715';
    }
    
    protected void initialiseCross() {
        char ch = getDefaultCrossCharacter();
        cross = new String("| " + ch); //$NON-NLS-1$
        FontMetrics fm = getFontMetrics(getFont());
        crossWidth = fm.stringWidth(cross);
        charHeight = fm.getMaxAscent();
    }
    
    @Override
    public void setFont(Font font) {
        super.setFont(font);
        initialiseCross();
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        String t = getText();
        if (! Check.isEmpty(t)) {
            Rectangle bounds = getBounds();
            int x = bounds.width - crossWidth;
            int y = charHeight;
            Insets insets = getInsets();
            if (insets != null) {
                x -= insets.right;
                y += insets.top;
            }
            Color save = g.getColor();
            g.setColor(Color.LIGHT_GRAY);
            g.drawString(cross, x, y);
            g.setColor(save);
        }
    }
}
