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

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.JOptionPane;

import com.diversityarrays.kdsmart.db.util.ItemConsumerHelper;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.data.KdxploreDatabase;

import net.pearcan.ui.table.BspAbstractTableModel;

public class EntityPropertiesTableModel<T> extends BspAbstractTableModel {

	private final String TAG = this.getClass().getSimpleName();

	protected final PropertyDescriptor[] propertyDescriptors;
	
	protected final Class<T> entityClass;
	protected T entity = null;
	
	protected KdxploreDatabase database;
	
	public EntityPropertiesTableModel(Class<T> clz, PropertyDescriptor[] pds) {
		super("Attribute", "Value");
		entityClass = clz;
		propertyDescriptors = pds;
	}
	
	protected Exception updateEntityInDatabase(Object aValue, int rowIndex, int columnIndex) {
		Exception result = null;;
		if (database != null) {
			try {
				ItemConsumerHelper itemConsumerHelper = database.getKDXploreKSmartDatabase().getItemConsumerHelper();
				itemConsumerHelper.updateItemInDatabase(entityClass, entity);
				itemConsumerHelper.notifyCreatedTTT();
			} catch (IOException e) {
				result = e;
			}
		}
		else {
			result = new Exception("No database to update " + entityClass.getName());
			Shared.Log.e(TAG, "updateEntityInDatabase(... ," + rowIndex + ", " + columnIndex, result);
		}
		return result;
	}

	public void setDatabase(KdxploreDatabase db) {
		this.database = db;
	}
	
	public int getAttributeNameColumnIndex() {
		return 0;
	}
	
	public int getAttributeValueColumnIndex() {
		return 1;
	}
	
	public Class<T> getEntityClass() {
		return entityClass;
	}

	public void clearData() {
		entity = null;
		fireTableDataChanged();
	}
	
	public T getEntity() {
		return entity;
	}
	
	public void setData(T t) {
		this.entity = t;
		fireTableDataChanged();
	}

	@Override
	public int getRowCount() {
		return entity == null ? 0 : propertyDescriptors.length;
	}

	@Override
	public Class<?> getColumnClass(int column) {
		switch (column) {
		case 0: return String.class;
		case 1: return Object.class;
		}
		return Object.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == 1 && rowIndex < propertyDescriptors.length) {
			PropertyDescriptor pd = propertyDescriptors[rowIndex];
			Method writeMethod = pd.getWriteMethod();
			
			return writeMethod != null;
		}
		return false;
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex != 1 || rowIndex >= propertyDescriptors.length) {
			return;
		}
		
		Exception error = null;
		PropertyDescriptor pd = propertyDescriptors[rowIndex];
		Method readMethod = pd.getReadMethod();
		Method writeMethod = pd.getWriteMethod();
		try {
			Object oldValue = readMethod.invoke(entity);
			writeMethod.invoke(entity, aValue);
			error = updateEntityInDatabase(aValue, rowIndex, columnIndex);
			if (error == null) {
				System.out.println("Updated " + entityClass.getName());
			}
			else {
				writeMethod.invoke(entity, oldValue);
				
			}
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) 
		{
			error = e;
		}
		
		if (error != null) {
			JOptionPane.showMessageDialog(null, error.getMessage(), 
					"Unable to save " + entityClass.getSimpleName(), 
					JOptionPane.WARNING_MESSAGE);
		}
	}
	
	public PropertyDescriptor getPropertyDescriptor(int rowIndex) {
		PropertyDescriptor result = null;
		if (rowIndex < propertyDescriptors.length) {
			result = propertyDescriptors[rowIndex];
		}
		return result;
	}

	public Class<?> getPropertyClass(int rowIndex) {
		Class<?> result = null;
		if (rowIndex < propertyDescriptors.length) {
			PropertyDescriptor pd = propertyDescriptors[rowIndex];
			result = pd.getPropertyType();
		}
		return result;
	}
	

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		
		if (rowIndex < propertyDescriptors.length) {
			PropertyDescriptor pd = propertyDescriptors[rowIndex];
			
			switch (columnIndex) {
			case 0:
				return pd.getDisplayName();
			case 1:
				Method method = pd.getReadMethod();
				Object value = null;
				try {
					value = method.invoke(entity);					
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					value = e.getMessage();
				}
				return value;
			}
		}	
		return null;
	}

}
