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
package com.diversityarrays.kdxplore.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.JLabel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.border.EmptyBorder;

public class Toast {
    
    static private Color PALE_YELLOW = Color.decode("#ffff66"); //$NON-NLS-1$
    static public final long SHORT = 2000;
    static public final long LONG = 8000;

    private final Component owner;
    private final Component contents;
    private final Point where;
    private long durationMillis;

    public Toast(Component owner, Point where, Component contents, long durationMillis) {
        this.owner = owner;
        this.where = where;
        this.contents = contents;
        this.durationMillis = durationMillis;
    }

    public Toast(Point where, String message, long durationMillis) {
        this(null, where, message, durationMillis);
    }
    
    public Toast(Component owner, String message, long durationMillis) {
        this(owner, null, message, durationMillis);
    }
    
    public Toast(String message, long durationMillis) {
        this(null, null, message, durationMillis);
    }
    
    public Toast(Component owner, Point where, String message, long durationMillis) {
        this.owner = owner;
        this.where = where;

        JLabel label = new JLabel(message);
        label.setBorder(new EmptyBorder(4,4,4,4));
        label.setForeground(Color.BLUE);
        label.setBackground(PALE_YELLOW);
        label.setOpaque(true);
        this.contents = label;
        
        this.durationMillis = durationMillis;
    }
    
    private Point computeXY() {
        Point xy = where;
        if (xy == null) {
            if (owner == null) {
                Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
                xy = new Point(
                            (int) (ss.getWidth() / 4),
                            (int) (ss.getHeight() / 3));
            }
            else {
                xy = owner.getLocationOnScreen();
            }
        }
        return xy;
    }
    
    public void showAsError() {
        if (contents != null) {
            contents.setForeground(Color.RED);
            contents.setBackground(Color.WHITE);
        }
        show();
    }
    
    public void show() {
        
        Point xy = computeXY();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                
                Popup popup = null;
                try {
                    popup = PopupFactory.getSharedInstance().getPopup(owner, contents, xy.x, xy.y);
                    popup.show();
                    Thread.sleep(durationMillis);
                }
                catch (InterruptedException ignore) { }
                finally {
                    if (popup != null) {
                        popup.hide();
                    }
                }
                
            }
        };
        
        new Thread(runnable).start();
    }
    

}
