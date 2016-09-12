/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Constants and implementations for standard swap indices.
 * <p>
 * Each constant returns a standard definition of the specified index.
 */
public final class SwapIndices {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<SwapIndex> ENUM_LOOKUP = ExtendedEnum.of(SwapIndex.class);

  //-------------------------------------------------------------------------
  /**
   * USD Rates 1100 for tenor of 1 year.
   */
  public static final SwapIndex USD_LIBOR_1100_1Y = SwapIndex.of("USD-LIBOR-1100-1Y");
  /**
   * USD Rates 1100 for tenor of 2 years.
   */
  public static final SwapIndex USD_LIBOR_1100_2Y = SwapIndex.of("USD-LIBOR-1100-2Y");
  /**
   * USD Rates 1100 for tenor of 3 years.
   */
  public static final SwapIndex USD_LIBOR_1100_3Y = SwapIndex.of("USD-LIBOR-1100-3Y");
  /**
   * USD Rates 1100 for tenor of 4 years.
   */
  public static final SwapIndex USD_LIBOR_1100_4Y = SwapIndex.of("USD-LIBOR-1100-4Y");
  /**
   * USD Rates 1100 for tenor of 5 years.
   */
  public static final SwapIndex USD_LIBOR_1100_5Y = SwapIndex.of("USD-LIBOR-1100-5Y");
  /**
   * USD Rates 1100 for tenor of 6 years.
   */
  public static final SwapIndex USD_LIBOR_1100_6Y = SwapIndex.of("USD-LIBOR-1100-6Y");
  /**
   * USD Rates 1100 for tenor of 7 years.
   */
  public static final SwapIndex USD_LIBOR_1100_7Y = SwapIndex.of("USD-LIBOR-1100-7Y");
  /**
   * USD Rates 1100 for tenor of 8 years.
   */
  public static final SwapIndex USD_LIBOR_1100_8Y = SwapIndex.of("USD-LIBOR-1100-8Y");
  /**
   * USD Rates 1100 for tenor of 9 years.
   */
  public static final SwapIndex USD_LIBOR_1100_9Y = SwapIndex.of("USD-LIBOR-1100-9Y");
  /**
   * USD Rates 1100 for tenor of 10 years.
   */
  public static final SwapIndex USD_LIBOR_1100_10Y = SwapIndex.of("USD-LIBOR-1100-10Y");
  /**
   * USD Rates 1100 for tenor of 15 years.
   */
  public static final SwapIndex USD_LIBOR_1100_15Y = SwapIndex.of("USD-LIBOR-1100-15Y");
  /**
   * USD Rates 1100 for tenor of 20 years.
   */
  public static final SwapIndex USD_LIBOR_1100_20Y = SwapIndex.of("USD-LIBOR-1100-20Y");
  /**
   * USD Rates 1100 for tenor of 30 years.
   */
  public static final SwapIndex USD_LIBOR_1100_30Y = SwapIndex.of("USD-LIBOR-1100-30Y");

  //-------------------------------------------------------------------------
  /**
   * USD Rates 1500 for tenor of 1 year.
   */
  public static final SwapIndex USD_LIBOR_1500_1Y = SwapIndex.of("USD-LIBOR-1500-1Y");

  //-------------------------------------------------------------------------
  /**
   * EUR Rates 1100 for tenor of 1 year.
   */
  public static final SwapIndex EUR_EURIBOR_1100_1Y = SwapIndex.of("EUR-EURIBOR-1100-1Y");
  /**
   * EUR Rates 1100 for tenor of 2 years.
   */
  public static final SwapIndex EUR_EURIBOR_1100_2Y = SwapIndex.of("EUR-EURIBOR-1100-2Y");
  /**
   * EUR Rates 1100 for tenor of 3 years.
   */
  public static final SwapIndex EUR_EURIBOR_1100_3Y = SwapIndex.of("EUR-EURIBOR-1100-3Y");
  /**
   * EUR Rates 1100 for tenor of 4 years.
   */
  public static final SwapIndex EUR_EURIBOR_1100_4Y = SwapIndex.of("EUR-EURIBOR-1100-4Y");
  /**
   * EUR Rates 1100 for tenor of 5 years.
   */
  public static final SwapIndex EUR_EURIBOR_1100_5Y = SwapIndex.of("EUR-EURIBOR-1100-5Y");
  /**
   * EUR Rates 1100 for tenor of 6 years.
   */
  public static final SwapIndex EUR_EURIBOR_1100_6Y = SwapIndex.of("EUR-EURIBOR-1100-6Y");
  /**
   * EUR Rates 1100 for tenor of 7 years.
   */
  public static final SwapIndex EUR_EURIBOR_1100_7Y = SwapIndex.of("EUR-EURIBOR-1100-7Y");
  /**
   * EUR Rates 1100 for tenor of 8 years.
   */
  public static final SwapIndex EUR_EURIBOR_1100_8Y = SwapIndex.of("EUR-EURIBOR-1100-8Y");
  /**
   * EUR Rates 1100 for tenor of 9 years.
   */
  public static final SwapIndex EUR_EURIBOR_1100_9Y = SwapIndex.of("EUR-EURIBOR-1100-9Y");
  /**
   * EUR Rates 1100 for tenor of 10 years.
   */
  public static final SwapIndex EUR_EURIBOR_1100_10Y = SwapIndex.of("EUR-EURIBOR-1100-10Y");
  /**
   * EUR Rates 1100 for tenor of 12 years.
   */
  public static final SwapIndex EUR_EURIBOR_1100_12Y = SwapIndex.of("EUR-EURIBOR-1100-12Y");
  /**
   * EUR Rates 1100 for tenor of 15 years.
   */
  public static final SwapIndex EUR_EURIBOR_1100_15Y = SwapIndex.of("EUR-EURIBOR-1100-15Y");
  /**
   * EUR Rates 1100 for tenor of 20 years.
   */
  public static final SwapIndex EUR_EURIBOR_1100_20Y = SwapIndex.of("EUR-EURIBOR-1100-20Y");
  /**
   * EUR Rates 1100 for tenor of 25 years.
   */
  public static final SwapIndex EUR_EURIBOR_1100_25Y = SwapIndex.of("EUR-EURIBOR-1100-25Y");
  /**
   * EUR Rates 1100 for tenor of 30 years.
   */
  public static final SwapIndex EUR_EURIBOR_1100_30Y = SwapIndex.of("EUR-EURIBOR-1100-30Y");

  //-------------------------------------------------------------------------
  /**
   * EUR Rates 1200 for tenor of 1 year.
   */
  public static final SwapIndex EUR_EURIBOR_1200_1Y = SwapIndex.of("EUR-EURIBOR-1200-1Y");
  /**
   * EUR Rates 1200 for tenor of 2 years.
   */
  public static final SwapIndex EUR_EURIBOR_1200_2Y = SwapIndex.of("EUR-EURIBOR-1200-2Y");
  /**
   * EUR Rates 1200 for tenor of 3 years.
   */
  public static final SwapIndex EUR_EURIBOR_1200_3Y = SwapIndex.of("EUR-EURIBOR-1200-3Y");
  /**
   * EUR Rates 1200 for tenor of 4 years.
   */
  public static final SwapIndex EUR_EURIBOR_1200_4Y = SwapIndex.of("EUR-EURIBOR-1200-4Y");
  /**
   * EUR Rates 1200 for tenor of 5 years.
   */
  public static final SwapIndex EUR_EURIBOR_1200_5Y = SwapIndex.of("EUR-EURIBOR-1200-5Y");
  /**
   * EUR Rates 1200 for tenor of 6 years.
   */
  public static final SwapIndex EUR_EURIBOR_1200_6Y = SwapIndex.of("EUR-EURIBOR-1200-6Y");
  /**
   * EUR Rates 1200 for tenor of 7 years.
   */
  public static final SwapIndex EUR_EURIBOR_1200_7Y = SwapIndex.of("EUR-EURIBOR-1200-7Y");
  /**
   * EUR Rates 1200 for tenor of 8 years.
   */
  public static final SwapIndex EUR_EURIBOR_1200_8Y = SwapIndex.of("EUR-EURIBOR-1200-8Y");
  /**
   * EUR Rates 1200 for tenor of 9 years.
   */
  public static final SwapIndex EUR_EURIBOR_1200_9Y = SwapIndex.of("EUR-EURIBOR-1200-9Y");
  /**
   * EUR Rates 1200 for tenor of 10 years.
   */
  public static final SwapIndex EUR_EURIBOR_1200_10Y = SwapIndex.of("EUR-EURIBOR-1200-10Y");
  /**
   * EUR Rates 1200 for tenor of 12 years.
   */
  public static final SwapIndex EUR_EURIBOR_1200_12Y = SwapIndex.of("EUR-EURIBOR-1200-12Y");
  /**
   * EUR Rates 1200 for tenor of 15 years.
   */
  public static final SwapIndex EUR_EURIBOR_1200_15Y = SwapIndex.of("EUR-EURIBOR-1200-15Y");
  /**
   * EUR Rates 1200 for tenor of 20 years.
   */
  public static final SwapIndex EUR_EURIBOR_1200_20Y = SwapIndex.of("EUR-EURIBOR-1200-20Y");
  /**
   * EUR Rates 1200 for tenor of 25 years.
   */
  public static final SwapIndex EUR_EURIBOR_1200_25Y = SwapIndex.of("EUR-EURIBOR-1200-25Y");
  /**
   * EUR Rates 1200 for tenor of 30 years.
   */
  public static final SwapIndex EUR_EURIBOR_1200_30Y = SwapIndex.of("EUR-EURIBOR-1200-30Y");

  //-------------------------------------------------------------------------
  /**
   * GBP Rates 1100 for tenor of 1 year.
   */
  public static final SwapIndex GBP_LIBOR_1100_1Y = SwapIndex.of("GBP-LIBOR-1100-1Y");
  /**
   * GBP Rates 1100 for tenor of 2 years.
   */
  public static final SwapIndex GBP_LIBOR_1100_2Y = SwapIndex.of("GBP-LIBOR-1100-2Y");
  /**
   * GBP Rates 1100 for tenor of 3 years.
   */
  public static final SwapIndex GBP_LIBOR_1100_3Y = SwapIndex.of("GBP-LIBOR-1100-3Y");
  /**
   * GBP Rates 1100 for tenor of 4 years.
   */
  public static final SwapIndex GBP_LIBOR_1100_4Y = SwapIndex.of("GBP-LIBOR-1100-4Y");
  /**
   * GBP Rates 1100 for tenor of 5 years.
   */
  public static final SwapIndex GBP_LIBOR_1100_5Y = SwapIndex.of("GBP-LIBOR-1100-5Y");
  /**
   * GBP Rates 1100 for tenor of 6 years.
   */
  public static final SwapIndex GBP_LIBOR_1100_6Y = SwapIndex.of("GBP-LIBOR-1100-6Y");
  /**
   * GBP Rates 1100 for tenor of 7 years.
   */
  public static final SwapIndex GBP_LIBOR_1100_7Y = SwapIndex.of("GBP-LIBOR-1100-7Y");
  /**
   * GBP Rates 1100 for tenor of 8 years.
   */
  public static final SwapIndex GBP_LIBOR_1100_8Y = SwapIndex.of("GBP-LIBOR-1100-8Y");
  /**
   * GBP Rates 1100 for tenor of 9 years.
   */
  public static final SwapIndex GBP_LIBOR_1100_9Y = SwapIndex.of("GBP-LIBOR-1100-9Y");
  /**
   * GBP Rates 1100 for tenor of 10 years.
   */
  public static final SwapIndex GBP_LIBOR_1100_10Y = SwapIndex.of("GBP-LIBOR-1100-10Y");
  /**
   * GBP Rates 1100 for tenor of 12 years.
   */
  public static final SwapIndex GBP_LIBOR_1100_12Y = SwapIndex.of("GBP-LIBOR-1100-12Y");
  /**
   * GBP Rates 1100 for tenor of 15 years.
   */
  public static final SwapIndex GBP_LIBOR_1100_15Y = SwapIndex.of("GBP-LIBOR-1100-15Y");
  /**
   * GBP Rates 1100 for tenor of 20 years.
   */
  public static final SwapIndex GBP_LIBOR_1100_20Y = SwapIndex.of("GBP-LIBOR-1100-20Y");
  /**
   * GBP Rates 1100 for tenor of 25 years.
   */
  public static final SwapIndex GBP_LIBOR_1100_25Y = SwapIndex.of("GBP-LIBOR-1100-25Y");
  /**
   * GBP Rates 1100 for tenor of 30 years.
   */
  public static final SwapIndex GBP_LIBOR_1100_30Y = SwapIndex.of("GBP-LIBOR-1100-30Y");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private SwapIndices() {
  }
}
