/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017,2018  Diversity Arrays Technology, Pty Ltd.
    
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
package com.diversityarrays.kdxplore.editing;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import net.pearcan.ui.GuiUtil;

import org.apache.commons.collections15.Closure;

import android.content.Context;
import android.util.Log;

import com.diversityarrays.kdsmart.KDSmartApplication;
import com.diversityarrays.kdsmart.db.entities.PlotIdentOption;
import com.diversityarrays.kdsmart.db.entities.TraitNameStyle;
import com.diversityarrays.kdsmart.db.entities.Trial;
import com.diversityarrays.util.DatePickerDialog;

public abstract class EntityPropertiesTable<T> extends JTable {
	
//	static class EditableFlagRenderer extends DefaultTableCellRenderer {
//		@Override
//		protected void setValue(Object value) {
//			if (value instanceof Boolean) {
//				Boolean b = (Boolean) value;
//				super.setValue(b ? "Double click 'Value' to edit" : "");
//				return;
//			}
//			super.setValue(value);
//		}
//	}
//	static private final EditableFlagRenderer EDITABLE_RENDERER = new EditableFlagRenderer();
	
	static class AttributeNameRenderer extends DefaultTableCellRenderer {
		
		private final Font normalFont;
		private final Font boldFont;

		AttributeNameRenderer() {
			normalFont = getFont();
			boldFont = normalFont.deriveFont(Font.BOLD);
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column)
		{
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			TableModel model = table.getModel();
			if (model instanceof EntityPropertiesTableModel) {
				int valueColumnIndex = ((EntityPropertiesTableModel<?>) model).getAttributeValueColumnIndex();
				if (model.isCellEditable(row, valueColumnIndex)) {
					setFont(boldFont);
					if (isSelected) {
						setForeground(table.getSelectionForeground());
					}
					else {
						setForeground(table.getSelectionBackground());
					}
				}
				else {
					setFont(normalFont);
					if (isSelected) {
						setForeground(table.getSelectionForeground());
					}
					else {
						setForeground(table.getForeground());
					}
				}
			}
			return c;
		}
	}
	
	static private final AttributeNameRenderer ATTRIBUTE_NAME_RENDERER = new AttributeNameRenderer();

	static public interface PropertyChangeConfirmer {
		public boolean isChangeAllowed(PropertyDescriptor pd);
		
		/**
		 * @return true if the change is allowed to "commit"
		 */
		public boolean valueChangeCanCommit(PropertyDescriptor pd, Object newValue);

		public void setValueBeforeChange(Object oldValue);
	}

	private final String TAG = this.getClass().getName();
	
	private final PropertyChangeConfirmer propertyChangeConfirmer;
	
	public EntityPropertiesTable(EntityPropertiesTableModel<T> tm) {
		this(tm, null);
	}
	
	public EntityPropertiesTable(EntityPropertiesTableModel<T> tm, PropertyChangeConfirmer pcc) {
		super(tm);
		
		this.propertyChangeConfirmer = pcc;
		getTableHeader().setReorderingAllowed(false);
		
		getColumnModel().getColumn(tm.getAttributeNameColumnIndex())
			.setCellRenderer(ATTRIBUTE_NAME_RENDERER);
		
//		getColumnModel().getColumn(tm.getEditableFlagColumnIndex())
//			.setCellRenderer(EDITABLE_RENDERER);
	}
	
	/**
	 * 
	 * @param tm
	 * @param row
	 * @param column
	 * @return true if handled
	 */
	abstract protected boolean handleEditCellAt(EntityPropertiesTableModel<T> tm, int row, int column);
	
	// return false if cannot be edited
	@Override
	public boolean editCellAt(int row, int column, EventObject e) {
		if (e instanceof MouseEvent) {
			MouseEvent me = (MouseEvent) e;
			if (SwingUtilities.isLeftMouseButton(me) && 2 != me.getClickCount()) {
				return false;
			}			
			me.consume();
		}
		
		@SuppressWarnings("unchecked")
		EntityPropertiesTableModel<T> eptm = (EntityPropertiesTableModel<T>) getModel();
		if (! eptm.isCellEditable(row, column)) {
			return false;
		}

		if (handleEditCellAt(eptm, row, column)) {
			return false;
		}
		
		PropertyDescriptor pd = eptm.getPropertyDescriptor(row);
		if (pd == null) {
			return super.editCellAt(row, column);
		}
		
		Class<?> propertyClass = pd.getPropertyType();
		
		if (propertyChangeConfirmer != null && ! propertyChangeConfirmer.isChangeAllowed(pd)) {
			return false;
		}
		
		if (java.util.Date.class.isAssignableFrom(propertyClass)) {
			try {
				java.util.Date dateValue = (Date) pd.getReadMethod().invoke(eptm.getEntity());

				if (propertyChangeConfirmer != null) {
					propertyChangeConfirmer.setValueBeforeChange(dateValue);
				}
				
				Closure<Date> onComplete = new Closure<Date>() {
					@Override
					public void execute(Date result) {
						if (result != null) {
							if (propertyChangeConfirmer == null || propertyChangeConfirmer.valueChangeCanCommit(pd, result)) {
								getModel().setValueAt(result, row, column);
							}
						}
					}
				};
				
				String title = pd.getDisplayName();
				DatePickerDialog datePicker = new DatePickerDialog(
						GuiUtil.getOwnerWindow(this), title,
						onComplete);
				datePicker.setDate(dateValue);
				datePicker.setVisible(true);
				
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e1)
			{
				e1.printStackTrace();
			}
			return false;
		}
		// else if (Enum.class.isAssignableFrom(propertyClass)) {
		// }

		Log.d(TAG, 
				"editCellAt(" + row + "," + column + ") No Editor override provided for " + propertyClass.getName());
		return super.editCellAt(row, column, e);
	}

	static class TNS_Editor extends DefaultCellEditor {
		
		static TNS_Editor create(Trial trial) {
			Map<String,TraitNameStyle> valueToTns = new HashMap<>();
			Map<TraitNameStyle,String> tnsToValue = new HashMap<>();
			
			TraitNameStyle current = trial.getTraitNameStyle();
			
			JComboBox<String> comboBox = new JComboBox<String>();
			
			Context ctx = KDSmartApplication.getInstance();
			if (TraitNameStyle.NO_INSTANCES == current) {
			    // Cannot change from NO_INSTANCES
                String s = "<HTML>" + ctx.getString(current.htmlStringResourceId);
                valueToTns.put(s, current);
                tnsToValue.put(current, s);
                comboBox.addItem(s);
			}
			else {
	            for (TraitNameStyle tns : TraitNameStyle.values()) {
	                if (TraitNameStyle.NO_INSTANCES == tns) {
	                    continue;
	                }
	                if (tns.getFirstInstanceNumber() == current.getFirstInstanceNumber()) {
	                    String s = "<HTML>" + ctx.getString(tns.htmlStringResourceId);
	                    valueToTns.put(s, tns);
	                    tnsToValue.put(tns, s);
	                    comboBox.addItem(s);
	                }
	            }
			}
			
			return new TNS_Editor(comboBox, valueToTns, tnsToValue);
		}


		private TNS_Editor(JComboBox<String> comboBox,
				Map<String,TraitNameStyle> valueToTns, 
				Map<TraitNameStyle,String> tnsToValue) 
		{
			super(comboBox);
			
			comboBox.removeActionListener(delegate);
			delegate = new EditorDelegate() {
	            public void setValue(Object value) {
	            	if (value instanceof TraitNameStyle) {
	            		String s = tnsToValue.get(value);
	            		comboBox.setSelectedItem(s);
	            	}
	            	else {
		                comboBox.setSelectedItem(value);
	            	}
	            }

	            public Object getCellEditorValue() {
	                Object item = comboBox.getSelectedItem();
	                return valueToTns.get(item);
	            }

	            public boolean shouldSelectCell(EventObject anEvent) {
	                if (anEvent instanceof MouseEvent) {
	                    MouseEvent e = (MouseEvent)anEvent;
	                    return e.getID() != MouseEvent.MOUSE_DRAGGED;
	                }
	                return true;
	            }
	            public boolean stopCellEditing() {
	                if (comboBox.isEditable()) {
	                    // Commit edited value.
	                    comboBox.actionPerformed(new ActionEvent(
	                    		TNS_Editor.this, 0, ""));
	                }
	                return super.stopCellEditing();
	            }
	        };
	        comboBox.addActionListener(delegate);
		}
		
	}
	
	@Override
	public TableCellEditor getCellEditor(int row, int column) {
		EntityPropertiesTableModel<?> tatm = (EntityPropertiesTableModel<?>) getModel();
		PropertyDescriptor pd = tatm.getPropertyDescriptor(row);

		Class<?> pdClass = pd.getPropertyType();
		
		if (TraitNameStyle.class == pdClass) {
		    if (Trial.class != tatm.entityClass) {
		        throw new RuntimeException("Internal error: " + tatm.entityClass.getName());
		    }
		    Trial trial = (Trial) tatm.getEntity();
			return TNS_Editor.create(trial);
		}
		
		if (Enum.class.isAssignableFrom(pdClass)) {
			if (PlotIdentOption.class.equals(pdClass)) {
				List<PlotIdentOption> list = new ArrayList<>();
				for (PlotIdentOption pio : PlotIdentOption.values()) {
					if (PlotIdentOption.NO_X_Y_OR_PLOT_ID != pio) {
						list.add(pio);
					}
				}
				return new DefaultCellEditor(new JComboBox<PlotIdentOption>(
						list.toArray(new PlotIdentOption[list.size()])));
			}
			else {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				Class<Enum> eclass = (Class<Enum>) pdClass;
				@SuppressWarnings({ "rawtypes", "unchecked" })
				EnumSet allOf = EnumSet.allOf(eclass);
				return new DefaultCellEditor(new JComboBox<>(allOf.toArray()));
			}			
		}
		return super.getCellEditor(row, column);
	}

	@Override
	public boolean editCellAt(int row, int column) {
		System.out.println("editCellAt(" + row + "," + column + ")");
		return super.editCellAt(row, column);
	}
	
}
