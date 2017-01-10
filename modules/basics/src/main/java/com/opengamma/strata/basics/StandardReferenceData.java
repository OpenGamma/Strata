/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;

/**
 * Provides standard reference data for holiday calendars in common currencies.
 */
final class StandardReferenceData {

  /**
   * Standard reference data.
   */
  static final ImmutableReferenceData STANDARD;
  static {
    Map<ReferenceDataId<?>, Object> map = new HashMap<>();
    for (HolidayCalendar cal : HolidayCalendars.extendedEnum().lookupAllNormalized().values()) {
      map.put(cal.getId(), cal);
    }
    STANDARD = ImmutableReferenceData.of(map);
  }
  /**
   * Minimal reference data.
   */
  static final ImmutableReferenceData MINIMAL;
  static {
    ImmutableMap.Builder<ReferenceDataId<?>, Object> builder = ImmutableMap.builder();
    builder.put(HolidayCalendars.NO_HOLIDAYS.getId(), HolidayCalendars.NO_HOLIDAYS);
    builder.put(HolidayCalendars.SAT_SUN.getId(), HolidayCalendars.SAT_SUN);
    builder.put(HolidayCalendars.FRI_SAT.getId(), HolidayCalendars.FRI_SAT);
    builder.put(HolidayCalendars.THU_FRI.getId(), HolidayCalendars.THU_FRI);
    MINIMAL = ImmutableReferenceData.of(builder.build());
  }

  // restricted constructor
  private StandardReferenceData() {
  }

}
