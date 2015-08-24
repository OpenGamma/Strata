/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.HolidayCalendars.CHZU;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;

/**
 * Standard IBOR index implementations.
 * <p>
 * See {@link FxIndices} for the description of each.
 */
final class StandardFxIndices {

  // Euro foreign exchange reference rates
  public static final FxIndex ECB_EUR_CHF = ecb(EUR, CHF, EUTA, CHZU);
  public static final FxIndex ECB_EUR_GBP = ecb(EUR, GBP, EUTA, GBLO);
  public static final FxIndex ECB_EUR_JPY = ecb(EUR, JPY, EUTA, JPTO);
  public static final FxIndex ECB_EUR_USD = ecb(EUR, USD, EUTA, USNY);

  // WM company
  public static final FxIndex WM_USD_CHF = wm(USD, CHF, USNY, CHZU);
  public static final FxIndex WM_EUR_USD = wm(EUR, USD, USNY, EUTA);
  public static final FxIndex WM_GBP_USD = wm(GBP, USD, USNY, GBLO);
  public static final FxIndex WM_USD_JPY = wm(USD, JPY, USNY, JPTO);

  // ecb
  private static FxIndex ecb(Currency base, Currency counter, HolidayCalendar baseHoliday, HolidayCalendar counterHoliday) {
    DaysAdjustment maturity = DaysAdjustment.ofBusinessDays(2, baseHoliday.combineWith(counterHoliday));
    return ImmutableFxIndex.builder()
        .name("ECB-" + base.getCode() + "-" + counter.getCode())
        .currencyPair(CurrencyPair.of(base, counter))
        .fixingCalendar(EUTA)
        .maturityDateOffset(maturity)
        .build();
  }

  // wm
  private static FxIndex wm(Currency base, Currency counter, HolidayCalendar baseHoliday, HolidayCalendar counterHoliday) {
    DaysAdjustment maturity = DaysAdjustment.ofBusinessDays(2, baseHoliday.combineWith(counterHoliday));
    return ImmutableFxIndex.builder()
        .name("WM-" + base.getCode() + "-" + counter.getCode())
        .currencyPair(CurrencyPair.of(base, counter))
        .fixingCalendar(USNY)
        .maturityDateOffset(maturity)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardFxIndices() {
  }

}
