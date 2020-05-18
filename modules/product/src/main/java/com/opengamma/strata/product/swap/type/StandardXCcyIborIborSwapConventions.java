/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * Market standard cross-currency Ibor-Ibor swap conventions.
 * <p>
 * https://quant.opengamma.io/Interest-Rate-Instruments-and-Market-Conventions.pdf
 * <p>
 * For the cross currency swap convention we have used the following approach to the naming: the first part
 * of the name refers to the leg on which the spread is paid and the second leg is the flat leg. 
 * For example, the EUR_xxx_USD_xxx name should be interpreted as the cross-currency swap with the spread above Ibor
 * paid on the EUR leg and a flat USD Ibor leg. 
 */
final class StandardXCcyIborIborSwapConventions {

  // Join calendar with the main currencies
  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);
  private static final HolidayCalendarId EUTA_GBLO = EUTA.combinedWith(GBLO);
  private static final HolidayCalendarId JPTO_GBLO = JPTO.combinedWith(GBLO);

  /**
   * EUR EURIBOR 3M v USD LIBOR 3M.
   * The spread is on the EUR leg.
   */
  public static final XCcyIborIborSwapConvention EUR_EURIBOR_3M_USD_LIBOR_3M =
      ImmutableXCcyIborIborSwapConvention.builder()
          .name("EUR-EURIBOR-3M-USD-LIBOR-3M")
          .spreadLeg(IborRateSwapLegConvention.builder()
              .index(IborIndices.EUR_EURIBOR_3M)
              .stubConvention(StubConvention.SMART_INITIAL)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY))
              .notionalExchange(true)
              .build())
          .flatLeg(IborRateSwapLegConvention.builder()
              .index(IborIndices.USD_LIBOR_3M)
              .stubConvention(StubConvention.SMART_INITIAL)
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
              .stubConvention(StubConvention.SMART_INITIAL)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY))
              .notionalExchange(true)
              .build())
          .flatLeg(IborRateSwapLegConvention.builder()
              .index(IborIndices.USD_LIBOR_3M)
              .stubConvention(StubConvention.SMART_INITIAL)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY))
              .notionalExchange(true)
              .build())
          .spotDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO_USNY))
          .build();

  /**
   * GBP LIBOR 3M v EUR EURIBOR 3M.
   * The spread is on the GBP leg.
   */
  public static final XCcyIborIborSwapConvention GBP_LIBOR_3M_EUR_EURIBOR_3M =
      ImmutableXCcyIborIborSwapConvention.builder()
          .name("GBP-LIBOR-3M-EUR-EURIBOR-3M")
          .spreadLeg(IborRateSwapLegConvention.builder()
              .index(IborIndices.GBP_LIBOR_3M)
              .stubConvention(StubConvention.SMART_INITIAL)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_GBLO))
              .notionalExchange(true)
              .build())
          .flatLeg(IborRateSwapLegConvention.builder()
              .index(IborIndices.EUR_EURIBOR_3M)
              .stubConvention(StubConvention.SMART_INITIAL)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_GBLO))
              .notionalExchange(true)
              .build())
          .spotDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA_GBLO))
          .build();

  /**
   * GBP LIBOR 3M v JPY LIBOR 3M.
   * The spread is on the GBP leg.
   */
  public static final XCcyIborIborSwapConvention GBP_LIBOR_3M_JPY_LIBOR_3M =
      ImmutableXCcyIborIborSwapConvention.builder()
          .name("GBP-LIBOR-3M-JPY-LIBOR-3M")
          .spreadLeg(IborRateSwapLegConvention.builder()
              .index(IborIndices.GBP_LIBOR_3M)
              .stubConvention(StubConvention.SMART_INITIAL)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO_GBLO))
              .notionalExchange(true)
              .build())
          .flatLeg(IborRateSwapLegConvention.builder()
              .index(IborIndices.JPY_LIBOR_3M)
              .stubConvention(StubConvention.SMART_INITIAL)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO_GBLO))
              .notionalExchange(true)
              .build())
          .spotDateOffset(DaysAdjustment.ofBusinessDays(2, JPTO_GBLO))
          .build();

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardXCcyIborIborSwapConventions() {
  }

}
