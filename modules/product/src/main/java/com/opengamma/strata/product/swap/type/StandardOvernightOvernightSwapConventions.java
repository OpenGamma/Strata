/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_SOFR;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;

import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Market standard Overnight-Overnight swap conventions.
 */
final class StandardOvernightOvernightSwapConventions {

  /**
   * USD SOFR vs USD Fed Fund 3M swap.
   * <p>
   * The spot date offset is 2 days.
   */
  public static final OvernightOvernightSwapConvention USD_SOFR_3M_FED_FUND_3M =
      makeConvention("USD-SOFR-3M-FED-FUND-3M", USD_SOFR, USD_FED_FUND, P3M, 2, 2);

  //-------------------------------------------------------------------------
  // build conventions
  private static OvernightOvernightSwapConvention makeConvention(
      String name,
      OvernightIndex spreadIndex,
      OvernightIndex flatIndex,
      Frequency frequency,
      int paymentLag,
      int spotLag) {

    HolidayCalendarId calendar = spreadIndex.getFixingCalendar();
    DaysAdjustment spotDateOffset = DaysAdjustment.ofBusinessDays(spotLag, calendar);
    return ImmutableOvernightOvernightSwapConvention.of(
        name,
        OvernightRateSwapLegConvention.of(spreadIndex, frequency, paymentLag),
        OvernightRateSwapLegConvention.of(flatIndex, frequency, paymentLag),
        spotDateOffset);
  }

  //-------------------------------------------------------------------------

  /**
   * Restricted constructor.
   */
  private StandardOvernightOvernightSwapConventions() {
  }

}
