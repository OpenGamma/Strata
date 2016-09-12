/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.product.swap.OvernightAccrualMethod.AVERAGED;
import static com.opengamma.strata.product.swap.OvernightAccrualMethod.COMPOUNDED;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;

/**
 * Market standard Fixed-Overnight swap conventions.
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
final class StandardOvernightIborSwapConventions {

  /**
   * USD Fed Fund AA v LIBOR 3M swap .
   * <p>
   * Both legs use day count 'Act/360'.
   * The spot date offset is 2 days and the cut-off period is 2 days.
   */
  public static final OvernightIborSwapConvention USD_FED_FUND_AA_LIBOR_3M =
      makeConvention("USD-FED-FUND-AA-LIBOR-3M", USD_FED_FUND, USD_LIBOR_3M, ACT_360, P3M, 0, 2, AVERAGED, 2);

  /**
   * GBP Sonia compounded 1Y v LIBOR 3M .
   * <p>
   * Both legs use day count 'Act/365F'.
   * The spot date offset is 0 days and payment offset is 0 days.
   */
  public static final OvernightIborSwapConvention GBP_SONIA_OIS_1Y_LIBOR_3M =
      makeConvention("GBP-SONIA-OIS-1Y-LIBOR-3M", GBP_SONIA, GBP_LIBOR_3M, ACT_365F, P12M, 0, 0, COMPOUNDED, 0);

  //-------------------------------------------------------------------------
  // build conventions
  private static OvernightIborSwapConvention makeConvention(
      String name,
      OvernightIndex onIndex,
      IborIndex iborIndex,
      DayCount dayCount,
      Frequency frequency,
      int paymentLag,
      int cutOffDays,
      OvernightAccrualMethod accrual,
      int spotLag) {

    HolidayCalendarId calendarOn = onIndex.getFixingCalendar();
    DaysAdjustment paymentDateOffset = DaysAdjustment.ofBusinessDays(paymentLag, calendarOn);
    DaysAdjustment spotDateOffset = DaysAdjustment.ofBusinessDays(spotLag, calendarOn);
    return ImmutableOvernightIborSwapConvention.of(
        name,
        OvernightRateSwapLegConvention.builder()
            .index(onIndex)
            .accrualMethod(accrual)
            .accrualFrequency(frequency)
            .paymentFrequency(frequency)
            .paymentDateOffset(paymentDateOffset)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .rateCutOffDays(cutOffDays)
            .build(),
        IborRateSwapLegConvention.of(iborIndex),
        spotDateOffset);
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardOvernightIborSwapConventions() {
  }

}
