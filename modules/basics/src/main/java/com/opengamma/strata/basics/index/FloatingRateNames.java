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
   * Constant for GBP-SONIA Overnight index.
   */
  public static final FloatingRateName GBP_SONIA = FloatingRateName.of("GBP-SONIA");
  /**
   * Constant for USD-FED-FUND Overnight index.
   */
  public static final FloatingRateName USD_FED_FUND = FloatingRateName.of("USD-FED-FUND");
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
