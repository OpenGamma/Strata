/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ONE_ONE;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.CHZU;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.FRPA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;

import java.time.Period;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.product.swap.CompoundingMethod;

/**
 * Fixed-Inflation swap conventions.
 */
final class StandardFixedInflationSwapConventions {

  /**
   * Three month lag.
   */
  private static final Period LAG_3M = Period.ofMonths(3);

  /**
   * GBP vanilla fixed vs UK HCIP swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention GBP_FIXED_ZC_GB_HCIP =
      ImmutableFixedInflationSwapConvention.of(
          "GBP-FIXED-ZC-GB-HCIP",
          fixedLegZcConvention(GBP, GBLO),
          InflationRateSwapLegConvention.of(PriceIndices.GB_HICP, LAG_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)),
          DaysAdjustment.ofBusinessDays(2, GBLO));

  /**
   * GBP vanilla fixed vs UK RPI swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention GBP_FIXED_ZC_GB_RPI =
      ImmutableFixedInflationSwapConvention.of(
          "GBP-FIXED-ZC-GB-RPI",
          fixedLegZcConvention(GBP, GBLO),
          InflationRateSwapLegConvention.of(PriceIndices.GB_RPI, LAG_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)),
          DaysAdjustment.ofBusinessDays(2, GBLO));

  /**
   * GBP vanilla fixed vs UK RPIX swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention GBP_FIXED_ZC_GB_RPIX =
      ImmutableFixedInflationSwapConvention.of(
          "GBP-FIXED-ZC-GB-RPIX",
          fixedLegZcConvention(GBP, GBLO),
          InflationRateSwapLegConvention.of(PriceIndices.GB_RPIX, LAG_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)),
          DaysAdjustment.ofBusinessDays(2, GBLO));

  /**
   * CHF vanilla fixed vs Switzerland CPI swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention CHF_FIXED_ZC_CH_CPI =
      ImmutableFixedInflationSwapConvention.of(
          "CHF-FIXED-ZC-CH-CPI",
          fixedLegZcConvention(CHF, CHZU),
          InflationRateSwapLegConvention.of(PriceIndices.CH_CPI, LAG_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CHZU)),
          DaysAdjustment.ofBusinessDays(2, CHZU));

  /**
   * EUR vanilla fixed vs Europe CPI swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention EUR_FIXED_ZC_EU_AI_CPI =
      ImmutableFixedInflationSwapConvention.of(
          "EUR-FIXED-ZC-EU-AI-CPI",
          fixedLegZcConvention(EUR, EUTA),
          InflationRateSwapLegConvention.of(PriceIndices.EU_AI_CPI, LAG_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)),
          DaysAdjustment.ofBusinessDays(2, EUTA));

  /**
   * EUR vanilla fixed vs Europe (Excluding Tobacco) CPI swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention EUR_FIXED_ZC_EU_EXT_CPI =
      ImmutableFixedInflationSwapConvention.of(
          "EUR-FIXED-ZC-EU-EXT-CPI",
          fixedLegZcConvention(EUR, EUTA),
          InflationRateSwapLegConvention.of(PriceIndices.EU_EXT_CPI, LAG_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)),
          DaysAdjustment.ofBusinessDays(2, EUTA));

  /**
   * JPY vanilla fixed vs Japan (Excluding Fresh Food) CPI swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention JPY_FIXED_ZC_JP_CPI =
      ImmutableFixedInflationSwapConvention.of(
          "JPY-FIXED-ZC-JP-CPI",
          fixedLegZcConvention(JPY, JPTO),
          InflationRateSwapLegConvention.of(PriceIndices.JP_CPI_EXF, LAG_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO)),
          DaysAdjustment.ofBusinessDays(2, JPTO));

  /**
   * USD vanilla fixed vs US Urban consumers CPI swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention USD_FIXED_ZC_US_CPI =
      ImmutableFixedInflationSwapConvention.of(
          "USD-FIXED-ZC-US-CPI",
          fixedLegZcConvention(USD, USNY),
          InflationRateSwapLegConvention.of(PriceIndices.US_CPI_U, LAG_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY)),
          DaysAdjustment.ofBusinessDays(2, USNY));

  /**
   * EUR vanilla fixed vs France CPI swap.
   * Both legs are zero-coupon; the fixed rate is compounded.
   */
  public static final FixedInflationSwapConvention EUR_FIXED_ZC_FR_CPI =
      ImmutableFixedInflationSwapConvention.of(
          "EUR-FIXED-ZC-FR-CPI",
          fixedLegZcConvention(EUR, EUTA),
          InflationRateSwapLegConvention.of(PriceIndices.FR_EXT_CPI, LAG_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, FRPA)),
          DaysAdjustment.ofBusinessDays(2, EUTA));

  // Create a zero-coupon fixed leg convention
  private static FixedRateSwapLegConvention fixedLegZcConvention(Currency ccy, HolidayCalendarId cal) {
    return FixedRateSwapLegConvention.builder()
        .paymentFrequency(Frequency.TERM)
        .accrualFrequency(Frequency.P12M)
        .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, cal))
        .startDateBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, cal))
        .endDateBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, cal))
        .compoundingMethod(CompoundingMethod.STRAIGHT)
        .dayCount(ONE_ONE)
        .currency(ccy)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardFixedInflationSwapConventions() {
  }

}
