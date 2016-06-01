/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

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
  static ReferenceData INSTANCE;
  static {
    ImmutableMap.Builder<ReferenceDataId<?>, Object> builder = ImmutableMap.builder();
    for (HolidayCalendar cal : HolidayCalendars.extendedEnum().lookupAll().values()) {
      builder.put(cal.getId(), cal);
    }
    INSTANCE = ReferenceData.of(builder.build());
  }

  // restricted constructor
  private StandardReferenceData() {
  }

}
