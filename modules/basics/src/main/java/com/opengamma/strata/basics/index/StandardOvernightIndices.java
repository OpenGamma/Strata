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
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.CHZU;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendars.NYFD;

/**
 * Standard Overnight index implementations.
 * <p>
 * See {@link OvernightIndices} for the description of each.
 */
final class StandardOvernightIndices {
  // http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf

  private static final String GBP_SONIA_NAME = "GBP-SONIA";
  private static final String CHF_TOIS_NAME = "CHF-TOIS";
  private static final String EUR_EONIA_NAME = "EUR-EONIA";
  private static final String JPY_TONAR_NAME = "JPY-TONAR";
  private static final String USD_FED_FUND_NAME = "USD-FED-FUND";

  // GBP SONIA
  public static final OvernightIndex GBP_SONIA = ImmutableOvernightIndex.builder()
      .name(GBP_SONIA_NAME)
      .currency(GBP)
      .fixingCalendar(GBLO)
      .publicationDateOffset(0)
      .effectiveDateOffset(0)
      .dayCount(ACT_365F)
      .build();

  // CHF TOIS
  public static final OvernightIndex CHF_TOIS = ImmutableOvernightIndex.builder()
      .name(CHF_TOIS_NAME)
      .currency(CHF)
      .fixingCalendar(CHZU)
      .publicationDateOffset(0)
      .effectiveDateOffset(1)
      .dayCount(ACT_360)
      .build();

  // EUR EONIA
  public static final OvernightIndex EUR_EONIA = ImmutableOvernightIndex.builder()
      .name(EUR_EONIA_NAME)
      .currency(EUR)
      .fixingCalendar(EUTA)
      .publicationDateOffset(0)
      .effectiveDateOffset(0)
      .dayCount(ACT_360)
      .build();

  // JPY TONAR
  public static final OvernightIndex JPY_TONAR = ImmutableOvernightIndex.builder()
      .name(JPY_TONAR_NAME)
      .currency(JPY)
      .fixingCalendar(JPTO)
      .publicationDateOffset(1)
      .effectiveDateOffset(0)
      .dayCount(ACT_365F)
      .build();

  // USD FedFund
  public static final OvernightIndex USD_FED_FUND = ImmutableOvernightIndex.builder()
      .name(USD_FED_FUND_NAME)
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
