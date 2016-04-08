/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.CHZU;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.FRPA;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Fixed-Inflation swap conventions.
 */
final class StandardFixedInflationSwapConventions {

  /**
   * GBP vanilla fixed vs UK HCIP swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention GBP_FIXED_6M_GB_HCIP =
      ImmutableFixedInflationSwapConvention.of(
          "GBP-FIXED-6M-GB-HCIP",
          FixedRateSwapLegConvention.of(GBP, THIRTY_U_360, Frequency.TERM, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)),
          InflationRateSwapLegConvention.of(PriceIndices.GB_HICP),
          BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO),
          DaysAdjustment.ofBusinessDays(2, GBLO));
  
  /**
   * GBP vanilla fixed vs UK RPI swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention GBP_FIXED_6M_GB_RPI =
      ImmutableFixedInflationSwapConvention.of(
          "GBP-FIXED-6M-GB-RPI",
          FixedRateSwapLegConvention.of(GBP, THIRTY_U_360, Frequency.TERM, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)),
          InflationRateSwapLegConvention.of(PriceIndices.GB_RPI),
          BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO),
          DaysAdjustment.ofBusinessDays(2, GBLO));
  
  /**
   * GBP vanilla fixed vs UK RPIX swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention GBP_FIXED_6M_GB_RPIX =
      ImmutableFixedInflationSwapConvention.of(
          "GBP-FIXED-6M-GB-RPIX",
          FixedRateSwapLegConvention.of(GBP, THIRTY_U_360, Frequency.TERM, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)),
          InflationRateSwapLegConvention.of(PriceIndices.GB_RPIX),
          BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO),
          DaysAdjustment.ofBusinessDays(2, GBLO));
  
  /**
   * CHF vanilla fixed vs Switzerland CPI swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention CHF_FIXED_6M_CH_CPI =
      ImmutableFixedInflationSwapConvention.of(
          "CHF-FIXED-6M-CH-CPI",
          FixedRateSwapLegConvention.of(CHF, THIRTY_U_360, Frequency.TERM, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CHZU)),
          InflationRateSwapLegConvention.of(PriceIndices.CH_CPI),
          BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CHZU),
          DaysAdjustment.ofBusinessDays(2, CHZU));
  
  /**
   * EUR vanilla fixed vs Europe CPI swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention EUR_FIXED_6M_EU_AI_CPI =
      ImmutableFixedInflationSwapConvention.of(
          "EUR-FIXED-6M-EU-AI-CPI",
          FixedRateSwapLegConvention.of(EUR, THIRTY_U_360, Frequency.TERM, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)),
          InflationRateSwapLegConvention.of(PriceIndices.EU_AI_CPI),
          BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA),
          DaysAdjustment.ofBusinessDays(2, EUTA));
  
  /**
   * EUR vanilla fixed vs Europe (Excluding Tobacco) CPI swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention EUR_FIXED_6M_EU_EXT_CPI =
      ImmutableFixedInflationSwapConvention.of(
          "EUR-FIXED-6M-EU-EXT-CPI",
          FixedRateSwapLegConvention.of(EUR, THIRTY_U_360, Frequency.TERM, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)),
          InflationRateSwapLegConvention.of(PriceIndices.EU_EXT_CPI),
          BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA),
          DaysAdjustment.ofBusinessDays(2, EUTA));
  
  /**
   * JPY vanilla fixed vs Japan (Excluding Fresh Food) CPI swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention JPY_FIXED_6M_JP_CPI =
      ImmutableFixedInflationSwapConvention.of(
          "JPY-FIXED-6M-JP-CPI",
          FixedRateSwapLegConvention.of(JPY, THIRTY_U_360, Frequency.TERM, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO)),
          InflationRateSwapLegConvention.of(PriceIndices.JP_CPI_EXF),
          BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO),
          DaysAdjustment.ofBusinessDays(2, JPTO));
  
  /**
   * USD(NY) vanilla fixed vs US Urban consumers CPI swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention USD_FIXED_6M_US_CPI =
      ImmutableFixedInflationSwapConvention.of(
          "USD-FIXED-6M-US-CPI",
          FixedRateSwapLegConvention.of(USD, THIRTY_U_360, Frequency.TERM, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY)),
          InflationRateSwapLegConvention.of(PriceIndices.US_CPI_U),
          BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY),
          DaysAdjustment.ofBusinessDays(2, USNY));
  
  /**
   * EUR vanilla fixed vs France CPI swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedInflationSwapConvention EUR_FIXED_6M_FR_CPI =
      ImmutableFixedInflationSwapConvention.of(
          "Eur-FIXED-6M-FR-CPI",
          FixedRateSwapLegConvention.of(EUR, THIRTY_U_360, Frequency.TERM, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, FRPA)),
          InflationRateSwapLegConvention.of(PriceIndices.FR_EXT_CPI),
          BusinessDayAdjustment.of(MODIFIED_FOLLOWING, FRPA),
          DaysAdjustment.ofBusinessDays(2, FRPA));
  
  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardFixedInflationSwapConventions() {
  }

}
