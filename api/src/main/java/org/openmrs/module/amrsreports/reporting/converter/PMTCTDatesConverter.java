/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.amrsreports.reporting.converter;

import org.openmrs.module.amrsreports.util.MOHReportUtil;
import org.openmrs.module.reporting.data.converter.DataConverter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class PMTCTDatesConverter implements DataConverter {

	/**
	 * converts a set of dates into proper formats for the PMTCT column in 361A
	 *
	 * @should convert a list of dates
	 * @should return a blank string if no dates exist
	 * @should return null if the parameter is null
	 */
	@Override
	public Object convert(Object original) {

		if (original == null)
			return null;

		Set<Date> o = (Set<Date>) original;

		List<String> out = new ArrayList<String>();
		for (Date d : o) {
			out.add(String.format("%s | PMTCT", MOHReportUtil.formatdates(d)));
		}

		return MOHReportUtil.joinAsSingleCell(out);
	}

	@Override
	public Class<?> getInputDataType() {
		return Set.class;
	}

	@Override
	public Class<?> getDataType() {
		return String.class;
	}
}
