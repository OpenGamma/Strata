/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.CHZU;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.IborIndices;

/**
 * Market standard Fixed-Ibor swap conventions.
 * <p>
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
final class StandardFixedIborSwapConventions {

  // GBLO+USNY calendar
  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);
  // GBLO+CHZU calendar
  private static final HolidayCalendarId GBLO_CHZU = GBLO.combinedWith(CHZU);
  // GBLO+JPTO calendar
  private static final HolidayCalendarId GBLO_JPTO = GBLO.combinedWith(JPTO);

  /**
   * USD(NY) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays every 6 months with day count '30U/360'.
   */
  public static final FixedIborSwapConvention USD_FIXED_6M_LIBOR_3M =
      ImmutableFixedIborSwapConvention.of(
          "USD-FIXED-6M-LIBOR-3M",
          FixedRateSwapLegConvention.of(USD, THIRTY_U_360, P6M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY)),
          IborRateSwapLegConvention.of(IborIndices.USD_LIBOR_3M));

  /**
   * USD(London) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays yearly with day count 'Act/360'.
   */
  public static final FixedIborSwapConvention USD_FIXED_1Y_LIBOR_3M =
      ImmutableFixedIborSwapConvention.of(
          "USD-FIXED-1Y-LIBOR-3M",
          FixedRateSwapLegConvention.of(USD, ACT_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY)),
          IborRateSwapLegConvention.of(IborIndices.USD_LIBOR_3M));

  //-------------------------------------------------------------------------
  /**
   * EUR(1Y) vanilla fixed vs Euribor 3M swap.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final FixedIborSwapConvention EUR_FIXED_1Y_EURIBOR_3M =
      ImmutableFixedIborSwapConvention.of(
          "EUR-FIXED-1Y-EURIBOR-3M",
          FixedRateSwapLegConvention.of(EUR, THIRTY_U_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)),
          IborRateSwapLegConvention.of(IborIndices.EUR_EURIBOR_3M));

  /**
   * EUR(>1Y) vanilla fixed vs Euribor 6M swap.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final FixedIborSwapConvention EUR_FIXED_1Y_EURIBOR_6M =
      ImmutableFixedIborSwapConvention.of(
          "EUR-FIXED-1Y-EURIBOR-6M",
          FixedRateSwapLegConvention.of(EUR, THIRTY_U_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)),
          IborRateSwapLegConvention.of(IborIndices.EUR_EURIBOR_6M));

  //-------------------------------------------------------------------------
  /**
   * GBP(1Y) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays yearly with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention GBP_FIXED_1Y_LIBOR_3M =
      ImmutableFixedIborSwapConvention.of(
          "GBP-FIXED-1Y-LIBOR-3M",
          FixedRateSwapLegConvention.of(GBP, ACT_365F, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)),
          IborRateSwapLegConvention.of(IborIndices.GBP_LIBOR_3M));

  /**
   * GBP(>1Y) vanilla fixed vs LIBOR 6M swap.
   * The fixed leg pays every 6 months with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention GBP_FIXED_6M_LIBOR_6M =
      ImmutableFixedIborSwapConvention.of(
          "GBP-FIXED-6M-LIBOR-6M",
          FixedRateSwapLegConvention.of(GBP, ACT_365F, P6M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)),
          IborRateSwapLegConvention.of(IborIndices.GBP_LIBOR_6M));

  /**
   * GBP(>1Y) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays every 3 months with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention GBP_FIXED_3M_LIBOR_3M =
      ImmutableFixedIborSwapConvention.of(
          "GBP-FIXED-3M-LIBOR-3M",
          FixedRateSwapLegConvention.of(GBP, ACT_365F, P3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)),
          IborRateSwapLegConvention.of(IborIndices.GBP_LIBOR_3M));

  //-------------------------------------------------------------------------
  /**
   * CHF(1Y) vanilla fixed vs LIBOR 3M swap.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final FixedIborSwapConvention CHF_FIXED_1Y_LIBOR_3M =
      ImmutableFixedIborSwapConvention.of(
          "CHF-FIXED-1Y-LIBOR-3M",
          FixedRateSwapLegConvention.of(CHF, THIRTY_U_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_CHZU)),
          IborRateSwapLegConvention.of(IborIndices.CHF_LIBOR_3M));

  /**
   * CHF(>1Y) vanilla fixed vs LIBOR 6M swap.
   * The fixed leg pays yearly with day count '30U/360'.
   */
  public static final FixedIborSwapConvention CHF_FIXED_1Y_LIBOR_6M =
      ImmutableFixedIborSwapConvention.of(
          "CHF-FIXED-1Y-LIBOR-6M",
          FixedRateSwapLegConvention.of(CHF, THIRTY_U_360, P12M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_CHZU)),
          IborRateSwapLegConvention.of(IborIndices.CHF_LIBOR_6M));

  //-------------------------------------------------------------------------
  /**
   * JPY(Tibor) vanilla fixed vs Tibor 3M swap.
   * The fixed leg pays every 6 months with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention JPY_FIXED_6M_TIBORJ_3M =
      ImmutableFixedIborSwapConvention.of(
          "JPY-FIXED-6M-TIBOR-JAPAN-3M",
          FixedRateSwapLegConvention.of(JPY, ACT_365F, P6M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO)),
          IborRateSwapLegConvention.of(IborIndices.JPY_TIBOR_JAPAN_3M));

  /**
   * JPY(LIBOR) vanilla fixed vs LIBOR 6M swap.
   * The fixed leg pays every 6 months with day count 'Act/365F'.
   */
  public static final FixedIborSwapConvention JPY_FIXED_6M_LIBOR_6M =
      ImmutableFixedIborSwapConvention.of(
          "JPY-FIXED-6M-LIBOR-6M",
          FixedRateSwapLegConvention.of(JPY, ACT_365F, P6M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_JPTO)),
          IborRateSwapLegConvention.of(IborIndices.JPY_LIBOR_6M));

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardFixedIborSwapConventions() {
  }

}
