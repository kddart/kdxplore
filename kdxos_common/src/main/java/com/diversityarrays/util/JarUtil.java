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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class JarUtil {

    static public void getManifestAttributes(
            ClassLoader classLoader, 
            Predicate<URL> predicate,
            BiConsumer<URL,Either<IOException,Attributes>> consumer) 
    throws IOException
    {
        try {
            Enumeration<URL> url_enum;
            try {
                url_enum = classLoader.getResources("META-INF/MANIFEST.MF"); //$NON-NLS-1$;
            }
            catch (IOException e) {
                throw e;
            }
            while (url_enum.hasMoreElements()) {
                URL url = url_enum.nextElement();
                if (predicate.test(url)) {
                    try {
                        Manifest manifest = new Manifest(url.openStream());
                        Attributes mainAttributes = manifest.getMainAttributes();
                        consumer.accept(url, Either.right(mainAttributes));
                    }
                    catch (IOException e) {
                        consumer.accept(url, Either.left(e));
                    }
                }
            }
        } finally {
            
        }
    }
    
    private JarUtil() {}
}
