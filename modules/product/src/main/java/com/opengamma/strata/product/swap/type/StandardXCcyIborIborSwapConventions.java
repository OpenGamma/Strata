/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.IborIndices;

/**
 * Market standard cross-currency Ibor-Ibor swap conventions.
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
final class StandardXCcyIborIborSwapConventions {

  // Join calendar with the main currencies
  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);

  /**
   * EUR EURIBOR 3M v USD LIBOR 3M.
   * The spread is on the EUR leg.
   */
  public static final XCcyIborIborSwapConvention EUR_EURIBOR_3M_USD_LIBOR_3M =
      ImmutableXCcyIborIborSwapConvention.builder()
          .name("EUR-EURIBOR-3M-USD-LIBOR-3M")
          .spreadLeg(IborRateSwapLegConvention.builder()
              .index(IborIndices.EUR_EURIBOR_3M)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY))
              .notionalExchange(true)
              .build())
          .flatLeg(IborRateSwapLegConvention.builder()
              .index(IborIndices.USD_LIBOR_3M)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY))
              .notionalExchange(true)
              .build())
          .spotDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA_USNY))
          .build();

  /**
   * GBP LIBOR 3M v USD LIBOR 3M.
   * The spread is on the GBP leg.
   */
  public static final XCcyIborIborSwapConvention GBP_LIBOR_3M_USD_LIBOR_3M =
      ImmutableXCcyIborIborSwapConvention.builder()
          .name("GBP-LIBOR-3M-USD-LIBOR-3M")
          .spreadLeg(IborRateSwapLegConvention.builder()
              .index(IborIndices.GBP_LIBOR_3M)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY))
              .notionalExchange(true)
              .build())
          .flatLeg(IborRateSwapLegConvention.builder()
              .index(IborIndices.USD_LIBOR_3M)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY))
              .notionalExchange(true)
              .build())
          .spotDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO_USNY))
          .build();

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardXCcyIborIborSwapConventions() {
  }

}
