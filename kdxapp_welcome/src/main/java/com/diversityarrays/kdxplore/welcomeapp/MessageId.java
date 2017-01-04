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
package com.diversityarrays.kdxplore.welcomeapp;

@SuppressWarnings("nls")
public class MessageId {
    
    static public final MessageId UNABLE_TO_OPEN_URL = new MessageId("Unable to open URL");
    static public final MessageId WELCOME_TO_KDXPLORE = new MessageId("Welcome to KDXplore");
    static public final MessageId ALSO_HIDE_ON_NEXT_START = new MessageId("Also Hide on next start");
    static public final MessageId APPNAME_WELCOME = new MessageId("Welcome");
    static public final MessageId HIDE_FROM_VIEW = new MessageId("Hide from View");

    public final String name;

    private MessageId(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
