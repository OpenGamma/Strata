/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.index;

import static com.opengamma.basics.currency.Currency.CHF;
import static com.opengamma.basics.currency.Currency.EUR;
import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.JPY;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.date.DayCounts.ACT_360;
import static com.opengamma.basics.date.DayCounts.ACT_365F;
import static com.opengamma.basics.date.HolidayCalendars.CHZU;
import static com.opengamma.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.basics.date.HolidayCalendars.JPTO;
import static com.opengamma.basics.date.HolidayCalendars.NYFD;

/**
 * Standard Overnight index implementations.
 * <p>
 * See {@link RateIndices} for the description of each.
 */
final class StandardOvernightIndices {
  // http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf

  // GBP SONIA
  public static final OvernightIndex GBP_SONIA = OvernightIndex.builder()
      .name("GBP-SONIA")
      .currency(GBP)
      .fixingCalendar(GBLO)
      .publicationDateOffset(0)
      .effectiveDateOffset(0)
      .dayCount(ACT_365F)
      .build();

  // CHF TOIS
  public static final OvernightIndex CHF_TOIS = OvernightIndex.builder()
      .name("CHF-TOIS")
      .currency(CHF)
      .fixingCalendar(CHZU)
      .publicationDateOffset(0)
      .effectiveDateOffset(1)
      .dayCount(ACT_360)
      .build();

  // EUR EONIA
  public static final OvernightIndex EUR_EONIA = OvernightIndex.builder()
      .name("EUR-EONIA")
      .currency(EUR)
      .fixingCalendar(EUTA)
      .publicationDateOffset(0)
      .effectiveDateOffset(0)
      .dayCount(ACT_360)
      .build();

  // JPY TONAR
  public static final OvernightIndex JPY_TONAR = OvernightIndex.builder()
      .name("JPY-TONAR")
      .currency(JPY)
      .fixingCalendar(JPTO)
      .publicationDateOffset(1)
      .effectiveDateOffset(0)
      .dayCount(ACT_365F)
      .build();

  // USD FedFund
  public static final OvernightIndex USD_FED_FUND = OvernightIndex.builder()
      .name("USD-FED-FUND")
      .currency(USD)
      .fixingCalendar(NYFD)
      .publicationDateOffset(1)
      .effectiveDateOffset(0)
      .dayCount(ACT_360)
      .build();

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardOvernightIndices() {
  }

}
