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
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.collections15.Transformer;

import com.diversityarrays.kdxplore.data.kdx.DateFormatSelector;
import com.diversityarrays.kdxplore.data.kdx.DeviceIdentifier;

public class TsmDateFormatSelector extends DateFormatSelector<TypedSampleMeasurement> {
	
	static public DateFormat LONG_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd'<BR> 'HH:mm:ss"); //$NON-NLS-1$
	static public DateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd"); //$NON-NLS-1$

	static private final Transformer<TypedSampleMeasurement, DeviceIdentifier> DEVID_PROVIDER = 
			new Transformer<TypedSampleMeasurement, DeviceIdentifier>() 
	{
		@Override
		public DeviceIdentifier transform(TypedSampleMeasurement tsm) {
			return tsm.deviceIdentifier;
		}
	};

	static private final Transformer<TypedSampleMeasurement, Date> DATE_PROVIDER =
			new Transformer<TypedSampleMeasurement, Date>() 
	{
		@Override
		public Date transform(TypedSampleMeasurement tsm) {
			return tsm.sampleGroupDate;
		}
	};
	
	public TsmDateFormatSelector() {
		super(DEVID_PROVIDER, DATE_PROVIDER);
	}
	
	public DateFormat getFormatForUniqueness(Collection<TypedSampleMeasurement> items) {
		return getFormatForUniqueness(items, SHORT_DATE_FORMAT, LONG_DATE_FORMAT);
	}
	
}
