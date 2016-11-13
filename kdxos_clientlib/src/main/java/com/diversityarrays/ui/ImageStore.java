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
package com.diversityarrays.ui;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class ImageStore {

	static public enum ImageId {
		Cross16("cross-16x16.png"),
		Cross24("cross-24x24.png"),
		CrossButton("cross-61x24.png"),
		
		GroupButton("group-61x24.png"),
		
		UrlButton("folder-web-green-41x32.png"),
		HttpButton("http-41x32.png"),
		
		LoginBlackButton("login-61x24.png"),
		LoginButton24("loginblue-64x24.png"),
		LoginButton32("loginblue-64x32.png"),
		
		Password32("password-41x32.png"),
		
		Person32("person-41x32.png"),
		
		Trash16("trash-16.png"),
		Trash16B("trash-16b.png"),
		Trash24("trash-24.png"),
		
		TrashQuestion("trash-question-72x24.png"),
		
		;
		public final String resourceName;
		ImageId(String rname) {
			this.resourceName = rname;
		}
	}
	
	static private ImageStore singleton;
	
	static public ImageStore getImageStore() {
		if (singleton==null) {
			synchronized (ImageStore.class) {
				if (singleton==null) {
					singleton = new ImageStore();
				}
			}
		}
		return singleton;
	}
	
	private final Map<ImageId,Image> imagesById = new HashMap<ImageId,Image>();
	private final Map<ImageId,ImageIcon> imageIconsById = new HashMap<ImageId,ImageIcon>();

	private ImageStore() {
	}
	
	public ImageIcon getImageIcon(ImageId id) {
		ImageIcon result = imageIconsById.get(id);
		if (result==null) {
			result = new ImageIcon(getImage(id));
			imageIconsById.put(id, result);
		}
		return result;
	}
	
	public Image getImage(ImageId id) {
		Image result = imagesById.get(id);
		if (result==null) {
			InputStream is = ImageStore.class.getResourceAsStream(id.resourceName);
			if (is==null) {
				throw new RuntimeException("Missing resource: "+ImageStore.class.getName()+"/"+id.resourceName);
			}
			try {
				result = ImageIO.read(is);
				imagesById.put(id, result);
			} catch (IOException e) {
				throw new RuntimeException("Resource("+ImageStore.class.getName()+"/"+id.resourceName+") read failure: "+e.getMessage());
			}
		}
		return result;
	}
}
