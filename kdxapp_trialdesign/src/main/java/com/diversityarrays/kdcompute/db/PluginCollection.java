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
package com.diversityarrays.kdcompute.db;

@SuppressWarnings("nls")
public class PluginCollection {
    public String pluginGroup;
    public java.util.Date lastUpdated;
    public int version;
    public String author;
    public String contact;
    public String[] plugins;

    public PluginCollection() {}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PluginCollection[");
        sb.append("version=").append(version)
            .append("grp=").append(pluginGroup)
            .append("\n\tupdated").append(lastUpdated)
            .append("\n\tby ").append(author)
            .append("\n\tvia ").append(contact);
        if (plugins != null) {
            sb.append("\n  plugins: ").append(plugins.length);
            for (String p : plugins) {
                sb.append("\n\t").append(p);
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
