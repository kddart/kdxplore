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

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import com.diversityarrays.kdsmart.db.entities.TraitValue;
import com.diversityarrays.kdxplore.data.dal.SampleType;
import com.diversityarrays.kdxplore.data.kdx.DeviceIdentifier;
import com.diversityarrays.kdxplore.data.kdx.DeviceType;
import com.diversityarrays.kdxplore.data.kdx.KdxSample;
import com.diversityarrays.util.ObjectUtil;

import net.pearcan.util.StringUtil;

public class TypedSampleMeasurement implements Comparable<TypedSampleMeasurement> {
	
	public final Map<Integer, SampleType> sampleTypeById;
	public final DeviceIdentifier deviceIdentifier;
	public final KdxSample sample;
	public final TraitValue traitValue;
	public final Date sampleGroupDate;
	public final boolean moreRecent;
			
	public TypedSampleMeasurement(
			DeviceIdentifier did,
			KdxSample sm, 
			Map<Integer, SampleType> sampleTypeById,
			TraitValue traitValue, 
			Date sampleGroupDate,
			boolean moreRecent)
	{
		this.deviceIdentifier = did;
		this.sample = sm;
		this.sampleTypeById = sampleTypeById;
		this.traitValue = traitValue;
		this.sampleGroupDate = sampleGroupDate;
		this.moreRecent = moreRecent;
	}

	@Override
	public String toString() {
		String displayValue = traitValue==null
				? "?"
				: traitValue.displayValue;
//				TraitValue.transformSimpleValue(
//				KDSmartApplication.getInstance(), sample.getTraitValue());
		if (deviceIdentifier == null) {
			return displayValue;
		}
		return "From " + deviceIdentifier.getDeviceName() + displayValue;
	}
	
	public DeviceIdentifier getDeviceIdentifier() {
		return deviceIdentifier;
	}
	
	public Integer getInstanceNumber() {
		return sample.getTraitInstanceNumber();
	}
	
	public java.util.Date getMeasureDateTime() {
		Date result = sample.getMeasureDateTime();
		return result; // TODO check if using a renderer for this
	}

	public String getTraitDisplayValue() {
		return traitValue==null ? sample.getTraitValue() : traitValue.displayValue;
	}

	public SampleType getSampleType() {
		SampleType st = sampleTypeById.get(sample.getSampleTypeId());
		return st;
	}

	@Override
	public int compareTo(TypedSampleMeasurement o) {
		int diff = ObjectUtil.safeCompare(deviceIdentifier, o.deviceIdentifier);
		if (diff == 0) {
			diff = ObjectUtil.safeCompare(sampleGroupDate, o.sampleGroupDate);
		}
		return diff;
	}

	public String asDeviceNameString(DateFormat dateFormat) {
		
		if (deviceIdentifier == null) {
			return "";
		}

		StringBuilder html = new StringBuilder();
		
		if (DeviceType.KDSMART == deviceIdentifier.getDeviceType()) {
			if (sampleGroupDate == null || dateFormat == null) {
				html.append(deviceIdentifier.getDeviceName());
			} else {
				html.append("<HTML>")
				.append(StringUtil.htmlEscape(deviceIdentifier.getDeviceName()))
				.append("<br>@")
				.append(dateFormat.format(sampleGroupDate));
			}
		} else {
			html.append(deviceIdentifier.getDeviceName());
		}

		return html.toString();
	}
	
	/**
	 * Return null if all the DeviceNames are unique else
	 * the shortFormat if it is sufficient to uniquely identify the TypedSampleMeasurements
	 * @param coll
	 * @param shortFormat
	 * @param longFormat
	 * @return
	 */
	static public DateFormat getDateFormatForUniqueIdent(Collection<TypedSampleMeasurement> coll, 
			DateFormat shortFormat, 
			DateFormat longFormat) 
	{
		Map<String, Bag<String>> datesByDeviceName = new HashMap<>();
		
		boolean foundDuplicateDeviceName = false;
		for (TypedSampleMeasurement tsm : coll) {
			if (DeviceType.KDSMART == tsm.deviceIdentifier.getDeviceType()) {
				String deviceName = tsm.deviceIdentifier.getDeviceName();
				if (datesByDeviceName.containsKey(deviceName)) {
					foundDuplicateDeviceName = true;	
				}
				else {
					datesByDeviceName.put(deviceName, new HashBag<>());
				}
			}
		}
		if (! foundDuplicateDeviceName) {
			return null;
		}
		
		for (TypedSampleMeasurement tsm : coll) {
			if (DeviceType.KDSMART == tsm.deviceIdentifier.getDeviceType()) {
				String deviceName = tsm.deviceIdentifier.getDeviceName();
				
				Bag<String> bag = datesByDeviceName.get(deviceName);
				if (tsm.sampleGroupDate != null) {
					String s = longFormat.format(tsm.sampleGroupDate);
					bag.add(s);
				}
			}
		}

		for (Bag<String> bag : datesByDeviceName.values()) {
			for (String key : bag.uniqueSet()) {
				if (bag.getCount(key) > 1) {
					return longFormat;
				}
			}
		}
		
		return shortFormat;
	}
}
