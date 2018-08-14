/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Constants and implementations for standard Floating rate names.
 * <p>
 * Each constant refers to a standard definition of the specified index.
 * <p>
 * If a floating rate has a constant here, then it is fully supported by Strata
 * with example holiday calendar data.
 */
public final class FloatingRateNames {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<FloatingRateName> ENUM_LOOKUP = ExtendedEnum.of(FloatingRateName.class);

  //-------------------------------------------------------------------------
  /**
   * Constant for GBP-LIBOR.
   */
  public static final FloatingRateName GBP_LIBOR = FloatingRateName.of("GBP-LIBOR");
  /**
   * Constant for USD-LIBOR.
   */
  public static final FloatingRateName USD_LIBOR = FloatingRateName.of("USD-LIBOR");
  /**
   * Constant for CHF-LIBOR.
   */
  public static final FloatingRateName CHF_LIBOR = FloatingRateName.of("CHF-LIBOR");
  /**
   * Constant for EUR-LIBOR.
   */
  public static final FloatingRateName EUR_LIBOR = FloatingRateName.of("EUR-LIBOR");
  /**
   * Constant for JPY-LIBOR.
   */
  public static final FloatingRateName JPY_LIBOR = FloatingRateName.of("JPY-LIBOR");
  /**
   * Constant for EUR-EURIBOR.
   */
  public static final FloatingRateName EUR_EURIBOR = FloatingRateName.of("EUR-EURIBOR");
  /**
   * Constant for AUD-BBSW.
   */
  public static final FloatingRateName AUD_BBSW = FloatingRateName.of("AUD-BBSW");
  /**
   * Constant for CAD-CDOR.
   */
  public static final FloatingRateName CAD_CDOR = FloatingRateName.of("CAD-CDOR");
  /**
   * Constant for CZK-PRIBOR.
   */
  public static final FloatingRateName CZK_PRIBOR = FloatingRateName.of("CZK-PRIBOR");
  /**
   * Constant for DKK-CIBOR.
   */
  public static final FloatingRateName DKK_CIBOR = FloatingRateName.of("DKK-CIBOR");
  /**
   * Constant for HUF-BUBOR.
   */
  public static final FloatingRateName HUF_BUBOR = FloatingRateName.of("HUF-BUBOR");
  /**
   * Constant for MXN-TIIE.
   */
  public static final FloatingRateName MXN_TIIE = FloatingRateName.of("MXN-TIIE");
  /**
   * Constant for NOK-NIBOR.
   */
  public static final FloatingRateName NOK_NIBOR = FloatingRateName.of("NOK-NIBOR");
  /**
   * Constant for NZD-BKBM.
   */
  public static final FloatingRateName NZD_BKBM = FloatingRateName.of("NZD-BKBM");
  /**
   * Constant for PLN-WIBOR.
   */
  public static final FloatingRateName PLN_WIBOR = FloatingRateName.of("PLN-WIBOR");
  /**
   * Constant for SEK-STIBOR.
   */
  public static final FloatingRateName SEK_STIBOR = FloatingRateName.of("SEK-STIBOR");
  /**
   * Constant for ZAR-JIBAR.
   */
  public static final FloatingRateName ZAR_JIBAR = FloatingRateName.of("ZAR-JIBAR");

  /**
   * Constant for GBP-SONIA Overnight index.
   */
  public static final FloatingRateName GBP_SONIA = FloatingRateName.of("GBP-SONIA");
  /**
   * Constant for USD-FED-FUND Overnight index.
   */
  public static final FloatingRateName USD_FED_FUND = FloatingRateName.of("USD-FED-FUND");
  /**
   * Constant for USD-SOFR Overnight index.
   */
  public static final FloatingRateName USD_SOFR = FloatingRateName.of("USD-SOFR");
  /**
   * Constant for CHF-SARON Overnight index.
   */
  public static final FloatingRateName CHF_SARON = FloatingRateName.of("CHF-SARON");
  /**
   * Constant for CHF-TOIS Overnight index.
   */
  public static final FloatingRateName CHF_TOIS = FloatingRateName.of("CHF-TOIS");
  /**
   * Constant for EUR-EONIA Overnight index.
   */
  public static final FloatingRateName EUR_EONIA = FloatingRateName.of("EUR-EONIA");
  /**
   * Constant for JPY-TONAR Overnight index.
   */
  public static final FloatingRateName JPY_TONAR = FloatingRateName.of("JPY-TONAR");
  /**
   * Constant for AUD-AONIA Overnight index.
   */
  public static final FloatingRateName AUD_AONIA = FloatingRateName.of("AUD-AONIA");
  /**
   * Constant for BRL-CDI Overnight index.
   */
  public static final FloatingRateName BRL_CDI = FloatingRateName.of("BRL-CDI");
  /**
   * Constant for CAD-CORRA Overnight index.
   */
  public static final FloatingRateName CAD_CORRA = FloatingRateName.of("CAD-CORRA");
  /**
   * Constant for DKK-TNR Overnight index.
   */
  public static final FloatingRateName DKK_TNR = FloatingRateName.of("DKK-TNR");
  /**
   * Constant for NOK-NOWA Overnight index.
   */
  public static final FloatingRateName NOK_NOWA = FloatingRateName.of("NOK-NOWA");
  /**
   * Constant for PLN-POLONIA Overnight index.
   */
  public static final FloatingRateName PLN_POLONIA = FloatingRateName.of("PLN-POLONIA");
  /**
   * Constant for SEK-SIOR Overnight index.
   */
  public static final FloatingRateName SEK_SIOR = FloatingRateName.of("SEK-SIOR");

  /**
   * Constant for USD-FED-FUND Overnight index using averaging.
   */
  public static final FloatingRateName USD_FED_FUND_AVG = FloatingRateName.of("USD-FED-FUND-AVG");

  /**
   * Constant for GB-RPI Price index.
   */
  public static final FloatingRateName GB_RPI = FloatingRateName.of("GB-RPI");
  /**
   * Constant for EU-EXT-CPI Price index.
   */
  public static final FloatingRateName EU_EXT_CPI = FloatingRateName.of("EU-EXT-CPI");
  /**
   * Constant for US-CPI-U Price index.
   */
  public static final FloatingRateName US_CPI_U = FloatingRateName.of("US-CPI-U");
  /**
   * Constant for FR-EXT-CPI Price index.
   */
  public static final FloatingRateName FR_EXT_CPI = FloatingRateName.of("FR-EXT-CPI");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FloatingRateNames() {
  }

}
