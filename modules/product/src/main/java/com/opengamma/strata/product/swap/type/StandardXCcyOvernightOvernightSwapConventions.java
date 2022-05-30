/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * Market standard cross-currency Overnight-overnight swap conventions.
 * <p>
 * For the cross currency swap convention we have used the following approach to the naming: the first part
 * of the name refers to the leg on which the spread is paid and the second leg is the flat leg. 
 * For example, the EUR_xxx_USD_xxx name should be interpreted as the cross-currency swap with the spread above 
 * overnight paid on the EUR leg and a flat USD overnight leg. 
 */
final class StandardXCcyOvernightOvernightSwapConventions {

  // Join calendar with the main currencies
  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_EUTA = GBLO.combinedWith(EUTA);
  private static final HolidayCalendarId JPTO_USNY = JPTO.combinedWith(USNY);

  /**
   * EUR ESTR 3M v USD SOFR 3M.
   * The spread is on the EUR leg.
   */
  public static final XCcyOvernightOvernightSwapConvention EUR_ESTR_3M_USD_SOFR_3M =
      ImmutableXCcyOvernightOvernightSwapConvention.builder()
          .name("EUR-ESTR-3M-USD-SOFR-3M")
          .spreadLeg(OvernightRateSwapLegConvention.builder()
              .index(OvernightIndices.EUR_ESTR)
              .paymentFrequency(Frequency.P3M)
              .accrualFrequency(Frequency.P3M)
              .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA_USNY))
              .stubConvention(StubConvention.SMART_INITIAL)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY))
              .notionalExchange(true)
              .build())
          .flatLeg(OvernightRateSwapLegConvention.builder()
              .index(OvernightIndices.USD_SOFR)
              .paymentFrequency(Frequency.P3M)
              .accrualFrequency(Frequency.P3M)
              .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA_USNY))
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY))
              .notionalExchange(true)
              .build())
          .spotDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA_USNY))
          .build();

  /**
   * GBP SONIA 3M v USD SOFR 3M.
   * The spread is on the GBP leg.
   */
  public static final XCcyOvernightOvernightSwapConvention GBP_SONIA_3M_USD_SOFR_3M =
      ImmutableXCcyOvernightOvernightSwapConvention.builder()
          .name("GBP-SONIA-3M-USD-SOFR-3M")
          .spreadLeg(OvernightRateSwapLegConvention.builder()
              .index(OvernightIndices.GBP_SONIA)
              .paymentFrequency(Frequency.P3M)
              .accrualFrequency(Frequency.P3M)
              .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO_USNY))
              .stubConvention(StubConvention.SMART_INITIAL)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY))
              .notionalExchange(true)
              .build())
          .flatLeg(OvernightRateSwapLegConvention.builder()
              .index(OvernightIndices.USD_SOFR)
              .paymentFrequency(Frequency.P3M)
              .accrualFrequency(Frequency.P3M)
              .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO_USNY))
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY))
              .notionalExchange(true)
              .build())
          .spotDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO_USNY))
          .build();

  /**
   * GBP SONIA 3M v EUR ESTR 3M.
   * The spread is on the GBP leg.
   */
  public static final XCcyOvernightOvernightSwapConvention GBP_SONIA_3M_EUR_ESTR_3M =
      ImmutableXCcyOvernightOvernightSwapConvention.builder()
          .name("GBP-SONIA-3M-EUR-ESTR-3M")
          .spreadLeg(OvernightRateSwapLegConvention.builder()
              .index(OvernightIndices.GBP_SONIA)
              .paymentFrequency(Frequency.P3M)
              .accrualFrequency(Frequency.P3M)
              .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO_EUTA))
              .stubConvention(StubConvention.SMART_INITIAL)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_EUTA))
              .notionalExchange(true)
              .build())
          .flatLeg(OvernightRateSwapLegConvention.builder()
              .index(OvernightIndices.EUR_ESTR)
              .paymentFrequency(Frequency.P3M)
              .accrualFrequency(Frequency.P3M)
              .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO_EUTA))
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_EUTA))
              .notionalExchange(true)
              .build())
          .spotDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO_EUTA))
          .build();

  /**
   * JPY TONA 3M v USD SOFR 3M.
   * The spread is on the JPY leg.
   */
  public static final XCcyOvernightOvernightSwapConvention JPY_TONA_3M_USD_SOFR_3M =
      ImmutableXCcyOvernightOvernightSwapConvention.builder()
          .name("JPY-TONA-3M-USD-SOFR-3M")
          .spreadLeg(OvernightRateSwapLegConvention.builder()
              .index(OvernightIndices.JPY_TONAR)
              .paymentFrequency(Frequency.P3M)
              .accrualFrequency(Frequency.P3M)
              .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, JPTO_USNY))
              .stubConvention(StubConvention.SMART_INITIAL)
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO_USNY))
              .notionalExchange(true)
              .build())
          .flatLeg(OvernightRateSwapLegConvention.builder()
              .index(OvernightIndices.USD_SOFR)
              .paymentFrequency(Frequency.P3M)
              .accrualFrequency(Frequency.P3M)
              .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, JPTO_USNY))
              .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO_USNY))
              .notionalExchange(true)
              .build())
          .spotDateOffset(DaysAdjustment.ofBusinessDays(2, JPTO_USNY))
          .build();

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardXCcyOvernightOvernightSwapConventions() {
  }

}
