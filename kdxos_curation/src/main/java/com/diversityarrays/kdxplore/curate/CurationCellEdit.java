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
package com.diversityarrays.kdxplore.curate;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

public class CurationCellEdit implements UndoableEdit {

	private boolean dead;
	
	private final String presentationName;
	private final String undoPresentationName;
	private final String redoPresentationName;
	
	public CurationCellEdit(String p, String u, String r) {
		this.presentationName = p;
		this.undoPresentationName = u;
		this.redoPresentationName = r;
	}
	
	@Override
	public void undo() throws CannotUndoException {

		if (dead) {
			throw new CannotRedoException();
		}
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canUndo() {
		// TODO Auto-generated method stub
		return ! dead;
	}

	@Override
	public void redo() throws CannotRedoException {
		if (dead) {
			throw new CannotRedoException();
		}
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canRedo() {
		// TODO Auto-generated method stub
		return ! dead;
	}

	@Override
	public void die() {
		dead = true;
	}

	@Override
	public boolean addEdit(UndoableEdit anEdit) {
		// Each one stands by itself
		return false;
	}

	@Override
	public boolean replaceEdit(UndoableEdit anEdit) {
		// Each one stands by itself
		return false;
	}

	@Override
	public boolean isSignificant() {
		return true;
	}

	@Override
	public String getPresentationName() {
		return presentationName;
	}

	@Override
	public String getUndoPresentationName() {
		return undoPresentationName;
	}

	@Override
	public String getRedoPresentationName() {
		return redoPresentationName;
	}

}
