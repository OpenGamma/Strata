/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx.type;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;

/**
 * Market standard FX swap conventions.
 */
public final class StandardFxSwapConventions {

  // Join calendar with the main currencies
  private static final HolidayCalendarId EUTA_JPTO = EUTA.combinedWith(JPTO);
  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);
  private static final HolidayCalendarId EUTA_GBLO = EUTA.combinedWith(GBLO);
  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_JPTO = GBLO.combinedWith(JPTO);
  private static final HolidayCalendarId JPTO_USNY = JPTO.combinedWith(USNY);

  /**
   * EUR/USD convention with 2 days spot date.
   */
  public static final FxSwapConvention EUR_USD =
      ImmutableFxSwapConvention.of(
          CurrencyPair.of(EUR, USD),
          DaysAdjustment.ofBusinessDays(2, EUTA_USNY),
          BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUTA_USNY));

  /**
   * EUR/GBP convention with 2 days spot date.
   */
  public static final FxSwapConvention EUR_GBP =
      ImmutableFxSwapConvention.of(
          CurrencyPair.of(EUR, GBP),
          DaysAdjustment.ofBusinessDays(2, EUTA_GBLO),
          BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUTA_GBLO));

  /**
   * EUR/JPY convention with 2 days spot date.
   */
  public static final FxSwapConvention EUR_JPY =
      ImmutableFxSwapConvention.of(
          CurrencyPair.of(EUR, JPY),
          DaysAdjustment.ofBusinessDays(2, EUTA_JPTO),
          BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUTA_JPTO));

  /**
   * GBP/USD convention with 2 days spot date.
   */
  public static final FxSwapConvention GBP_USD =
      ImmutableFxSwapConvention.of(
          CurrencyPair.of(GBP, USD),
          DaysAdjustment.ofBusinessDays(2, GBLO_USNY),
          BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, GBLO_USNY));

  /**
   * GBP/JPY convention with 2 days spot date.
   */
  public static final FxSwapConvention GBP_JPY =
      ImmutableFxSwapConvention.of(
          CurrencyPair.of(GBP, JPY),
          DaysAdjustment.ofBusinessDays(2, GBLO_JPTO),
          BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, GBLO_JPTO));

  /**
   * USD/JPY convention with 2 days spot date.
   */
  public static final FxSwapConvention USD_JPY =
      ImmutableFxSwapConvention.of(
          CurrencyPair.of(USD, JPY),
          DaysAdjustment.ofBusinessDays(2, JPTO_USNY),
          BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, JPTO_USNY));

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardFxSwapConventions() {
  }

}
