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
package com.diversityarrays.kdxplore.todo;


public class TodoItemImpl implements TodoItem {
	
	private static int nextSeq = 1;

	private final Integer seq = nextSeq++;
	
	private final Object identifier;
	
	private Boolean done;
	
	private final Integer priority;
	
	private final String name;

	private final String description;
	
	public TodoItemImpl(Object identifier, String name, String desc) {
		this(identifier, 1, name, desc);
	}
	
	public TodoItemImpl(Object identifier, int p, String name, String desc) {
		this.identifier = identifier;
		this.priority = p;
		this.name = name;
		this.description = desc;
	}

	@Override
	public String toString() {
		return priority+": "+name;
	}
	
	@Override
	public Object getIdentifier() {
		return identifier;
	}
	
	@Override
	public Integer getSeq() {
		return seq;
	}
	
	
	@Override
	public Boolean getDone() {
		return done;
	}

	@Override
	public void setDone(Boolean done) {
		this.done = done;
	}

	@Override
	public Integer getPriority() {
		return priority;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}
	
	
}
