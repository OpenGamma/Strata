/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;

import java.time.Period;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.opengamma.analytics.convention.daycount.DayCount;
import com.opengamma.analytics.convention.daycount.DayCountFactory;
import com.opengamma.analytics.financial.instrument.index.IborIndex;
import com.opengamma.analytics.financial.instrument.index.IndexON;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndices;

/**
 * Static utilities to convert types to legacy types.
 */
public final class LegacyIndices {

  private static final DayCount ACT_360 = DayCountFactory.of("Actual/360");
  private static final DayCount ACT_365F = DayCountFactory.of("Actual/365");

  /**
   * Reference to the AUD BBSW 3M index.
   */
  public static final IborIndex AUDBB3M = new IborIndex(
      Currency.AUD, Period.ofMonths(3), 1, ACT_365F, MODIFIED_FOLLOWING, true, "AUDBB3M");
  /**
   * Reference to the AUD BBSW 6M index.
   */
  public static final IborIndex AUDBB6M = new IborIndex(
      Currency.AUD, Period.ofMonths(6), 1, ACT_365F, MODIFIED_FOLLOWING, true, "AUDBB6M");
  /**
   * Reference to the CAD CDOR 3M index.
   */
  public static final IborIndex CADCDOR3M = new IborIndex(
      Currency.CAD, Period.ofMonths(3), 0, ACT_365F, MODIFIED_FOLLOWING, true, "CADCDOR3M");
  /**
   * Reference to the DKK CIBOR 3M index.
   */
  public static final IborIndex DKKCIBOR3M = new IborIndex(
      Currency.DKK, Period.ofMonths(3), 2, ACT_360, MODIFIED_FOLLOWING, true, "DKKCIBOR3M");
  /**
   * Reference to the DKK CIBOR 6M index.
   */
  public static final IborIndex DKKCIBOR6M = new IborIndex(
      Currency.DKK, Period.ofMonths(6), 2, ACT_360, MODIFIED_FOLLOWING, true, "DKKCIBOR6M");

  /**
   * Reference to the GBP LIBOR 1M index.
   */
  public static final IborIndex CHFLIBOR1M = new IborIndex(
      Currency.CHF, Period.ofMonths(1), 2, ACT_360, MODIFIED_FOLLOWING, true, "CHFLIBOR1M");
  /**
   * Reference to the GBP LIBOR 3M index.
   */
  public static final IborIndex CHFLIBOR3M = new IborIndex(
      Currency.CHF, Period.ofMonths(3), 2, ACT_360, MODIFIED_FOLLOWING, true, "CHFLIBOR3M");
  /**
   * Reference to the GBP LIBOR 6M index.
   */
  public static final IborIndex CHFLIBOR6M = new IborIndex(
      Currency.CHF, Period.ofMonths(6), 2, ACT_360, MODIFIED_FOLLOWING, true, "CHFLIBOR6M");
  /**
   * Reference to the GBP LIBOR 12M index.
   */
  public static final IborIndex CHFLIBOR12M = new IborIndex(
      Currency.CHF, Period.ofMonths(12), 2, ACT_360, MODIFIED_FOLLOWING, true, "CHFLIBOR12M");

  /**
   * Reference to the EUR EURIBOR 1M index.
   */
  public static final IborIndex EURIBOR1M = new IborIndex(
      Currency.EUR, Period.ofMonths(1), 2, ACT_360, MODIFIED_FOLLOWING, true, "EURIBOR1M");
  /**
   * Reference to the EUR EURIBOR 3M index.
   */
  public static final IborIndex EURIBOR3M = new IborIndex(
      Currency.EUR, Period.ofMonths(3), 2, ACT_360, MODIFIED_FOLLOWING, true, "EURIBOR3M");
  /**
   * Reference to the EUR EURIBOR 6M index.
   */
  public static final IborIndex EURIBOR6M = new IborIndex(
      Currency.EUR, Period.ofMonths(6), 2, ACT_360, MODIFIED_FOLLOWING, true, "EURIBOR6M");
  /**
   * Reference to the EUR EURIBOR 12M index.
   */
  public static final IborIndex EURIBOR12M = new IborIndex(
      Currency.EUR, Period.ofMonths(12), 2, ACT_360, MODIFIED_FOLLOWING, true, "EURIBOR12M");

  /**
   * Reference to the GBP LIBOR 1M index.
   */
  public static final IborIndex GBPLIBOR1M = new IborIndex(
      Currency.GBP, Period.ofMonths(1), 0, ACT_365F, MODIFIED_FOLLOWING, true, "GBPLIBOR1M");
  /**
   * Reference to the GBP LIBOR 3M index.
   */
  public static final IborIndex GBPLIBOR3M = new IborIndex(
      Currency.GBP, Period.ofMonths(3), 0, ACT_365F, MODIFIED_FOLLOWING, true, "GBPLIBOR3M");
  /**
   * Reference to the GBP LIBOR 6M index.
   */
  public static final IborIndex GBPLIBOR6M = new IborIndex(
      Currency.GBP, Period.ofMonths(6), 0, ACT_365F, MODIFIED_FOLLOWING, true, "GBPLIBOR6M");
  /**
   * Reference to the GBP LIBOR 12M index.
   */
  public static final IborIndex GBPLIBOR12M = new IborIndex(
      Currency.GBP, Period.ofMonths(12), 0, ACT_365F, MODIFIED_FOLLOWING, true, "GBPLIBOR12M");

  /**
   * Reference to the JPY LIBOR 1M index.
   */
  public static final IborIndex JPYLIBOR1M = new IborIndex(
      Currency.JPY, Period.ofMonths(1), 2, ACT_365F, MODIFIED_FOLLOWING, true, "JPYLIBOR1M");
  /**
   * Reference to the JPY LIBOR 3M index.
   */
  public static final IborIndex JPYLIBOR3M = new IborIndex(
      Currency.JPY, Period.ofMonths(3), 2, ACT_365F, MODIFIED_FOLLOWING, true, "JPYLIBOR3M");
  /**
   * Reference to the JPY LIBOR 6M index.
   */
  public static final IborIndex JPYLIBOR6M = new IborIndex(
      Currency.JPY, Period.ofMonths(6), 2, ACT_365F, MODIFIED_FOLLOWING, true, "JPYLIBOR6M");
  /**
   * Reference to the JPY LIBOR 12M index.
   */
  public static final IborIndex JPYLIBOR12M = new IborIndex(
      Currency.JPY, Period.ofMonths(12), 2, ACT_365F, MODIFIED_FOLLOWING, true, "JPYLIBOR12M");

  /**
   * Reference to the USD LIBOR 1M index.
   */
  public static final IborIndex USDLIBOR1M = new IborIndex(
      Currency.USD, Period.ofMonths(1), 2, ACT_360, MODIFIED_FOLLOWING, true, "USDLIBOR1M");
  /**
   * Reference to the USD LIBOR 3M index.
   */
  public static final IborIndex USDLIBOR3M = new IborIndex(
      Currency.USD, Period.ofMonths(3), 2, ACT_360, MODIFIED_FOLLOWING, true, "USDLIBOR3M");
  /**
   * Reference to the USD LIBOR 6M index.
   */
  public static final IborIndex USDLIBOR6M = new IborIndex(
      Currency.USD, Period.ofMonths(6), 2, ACT_360, MODIFIED_FOLLOWING, true, "USDLIBOR6M");
  /**
   * Reference to the USD LIBOR 12M index.
   */
  public static final IborIndex USDLIBOR12M = new IborIndex(
      Currency.USD, Period.ofMonths(12), 2, ACT_360, MODIFIED_FOLLOWING, true, "USDLIBOR12M");

  /**
   * Map of new to old IBOR-like indices.
   */
  public static final BiMap<String, com.opengamma.analytics.financial.instrument.index.IborIndex> IBOR =
      ImmutableBiMap.<String, com.opengamma.analytics.financial.instrument.index.IborIndex>builder()
          .put(IborIndices.CHF_LIBOR_1M.getName(), CHFLIBOR1M)
          .put(IborIndices.CHF_LIBOR_3M.getName(), CHFLIBOR3M)
          .put(IborIndices.CHF_LIBOR_6M.getName(), CHFLIBOR6M)
          .put(IborIndices.CHF_LIBOR_12M.getName(), CHFLIBOR12M)
          .put(IborIndices.EUR_EURIBOR_1M.getName(), EURIBOR1M)
          .put(IborIndices.EUR_EURIBOR_3M.getName(), EURIBOR3M)
          .put(IborIndices.EUR_EURIBOR_6M.getName(), EURIBOR6M)
          .put(IborIndices.EUR_EURIBOR_12M.getName(), EURIBOR12M)
          .put(IborIndices.GBP_LIBOR_1M.getName(), GBPLIBOR1M)
          .put(IborIndices.GBP_LIBOR_3M.getName(), GBPLIBOR3M)
          .put(IborIndices.GBP_LIBOR_6M.getName(), GBPLIBOR6M)
          .put(IborIndices.GBP_LIBOR_12M.getName(), GBPLIBOR12M)
          .put(IborIndices.JPY_LIBOR_1M.getName(), JPYLIBOR1M)
          .put(IborIndices.JPY_LIBOR_3M.getName(), JPYLIBOR3M)
          .put(IborIndices.JPY_LIBOR_6M.getName(), JPYLIBOR6M)
          .put(IborIndices.JPY_LIBOR_12M.getName(), JPYLIBOR12M)
          .put(IborIndices.USD_LIBOR_1M.getName(), USDLIBOR1M)
          .put(IborIndices.USD_LIBOR_3M.getName(), USDLIBOR3M)
          .put(IborIndices.USD_LIBOR_6M.getName(), USDLIBOR6M)
          .put(IborIndices.USD_LIBOR_12M.getName(), USDLIBOR12M)
          .build();

  //-------------------------------------------------------------------------
  /**
   * Reference to the EUR EONIA index.
   */
  public static final IndexON CHF_TOIS = new IndexON("TOIS", Currency.CHF, ACT_360, 1);
  /**
   * Reference to the EUR EONIA index.
   */
  public static final IndexON EUR_EONIA = new IndexON("EONIA", Currency.EUR, ACT_360, 0);
  /**
   * Reference to the GBP SONIA index.
   */
  public static final IndexON GBP_SONIA = new IndexON("SONIA", Currency.GBP, ACT_365F, 0);
  /**
   * Reference to the JPY TONAR index.
   */
  public static final IndexON JPY_TONAR = new IndexON("TONAR", Currency.JPY, ACT_365F, 0);
  /**
   * Reference to the USD FED FUND index.
   */
  public static final IndexON USD_FED_FUND = new IndexON("FED FUND", Currency.USD, ACT_360, 1);

  /**
   * Map of new to old Overnight indices.
   */
  public static final BiMap<String, IndexON> OVERNIGHT =
      ImmutableBiMap.<String, IndexON>builder()
          .put(OvernightIndices.CHF_TOIS.getName(), CHF_TOIS)
          .put(OvernightIndices.EUR_EONIA.getName(), EUR_EONIA)
          .put(OvernightIndices.GBP_SONIA.getName(), GBP_SONIA)
          .put(OvernightIndices.JPY_TONAR.getName(), JPY_TONAR)
          .put(OvernightIndices.USD_FED_FUND.getName(), USD_FED_FUND)
          .build();

  /**
   * Restricted constructor.
   */
  private LegacyIndices() {
  }

}
