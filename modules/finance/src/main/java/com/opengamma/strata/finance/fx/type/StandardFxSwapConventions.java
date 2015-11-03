/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx.type;

import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;

/**
 * Market standard Fx swap conventions.
 */
public class StandardFxSwapConventions {

  // Join calendar with the main currencies
  private static final HolidayCalendar EUTA_USNY = EUTA.combineWith(USNY);
  private static final HolidayCalendar GBLO_EUTA = GBLO.combineWith(EUTA);
  private static final HolidayCalendar GBLO_USNY = GBLO.combineWith(USNY);

  /**
   * EUR/USD convention with 2 days spot date.
   */
  public static final FxSwapConvention EUR_USD =
      ImmutableFxSwapConvention.of(
          "EUR-USD",
          CurrencyPair.of(EUR, USD),
          DaysAdjustment.ofBusinessDays(2, EUTA_USNY),
          BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUTA_USNY));

  /**
   * GBP/EUR convention with 2 days spot date.
   */
  public static final FxSwapConvention GBP_EUR =
      ImmutableFxSwapConvention.of(
          "GBP-EUR",
          CurrencyPair.of(GBP, EUR),
          DaysAdjustment.ofBusinessDays(2, GBLO_EUTA),
          BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, GBLO_EUTA));

  /**
   * GBP/USD convention with 2 days spot date.
   */
  public static final FxSwapConvention GBP_USD =
      ImmutableFxSwapConvention.of(
          "GBP-USD",
          CurrencyPair.of(GBP, USD),
          DaysAdjustment.ofBusinessDays(2, GBLO_USNY),
          BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, GBLO_USNY));

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardFxSwapConventions() {
  }

}
