/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;

/**
 * Provides standard reference data for holiday calendars and indices in common currencies.
 */
final class StandardReferenceData {

  /**
   * Standard reference data.
   */
  static ReferenceData INSTANCE;
  static {
    ImmutableMap.Builder<ReferenceDataId<?>, Object> builder = ImmutableMap.builder();
    for (HolidayCalendar cal : HolidayCalendar.extendedEnum().lookupAll().values()) {
      builder.put(cal.getId(), cal);
    }
    for (IborIndex index : IborIndex.extendedEnum().lookupAll().values()) {
      builder.put(index.getId(), index);
    }
    for (OvernightIndex index : OvernightIndex.extendedEnum().lookupAll().values()) {
      builder.put(index.getId(), index);
    }
    for (PriceIndex index : PriceIndex.extendedEnum().lookupAll().values()) {
      builder.put(index.getId(), index);
    }
    for (FxIndex index : FxIndex.extendedEnum().lookupAll().values()) {
      builder.put(index.getId(), index);
    }
    INSTANCE = ReferenceData.of(builder.build());
  }

  // restricted constructor
  private StandardReferenceData() {
  }

}
