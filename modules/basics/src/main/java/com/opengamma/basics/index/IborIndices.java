/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.index;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Constants and implementations for standard Ibor indices.
 * <p>
 * Each constant returns a standard definition of the specified index.
 */
public final class IborIndices {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<IborIndex> ENUM_LOOKUP = ExtendedEnum.of(IborIndex.class);

  //-------------------------------------------------------------------------
  /**
   * The 1 week LIBOR index for GBP.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex GBP_LIBOR_1W = IborIndex.of(StandardIborIndices.GBP_LIBOR_1W.getName());
  /**
   * The 1 month LIBOR index for GBP.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex GBP_LIBOR_1M = IborIndex.of(StandardIborIndices.GBP_LIBOR_1M.getName());
  /**
   * The 2 month LIBOR index for GBP.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex GBP_LIBOR_2M = IborIndex.of(StandardIborIndices.GBP_LIBOR_2M.getName());
  /**
   * The 3 month LIBOR index for GBP.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex GBP_LIBOR_3M = IborIndex.of(StandardIborIndices.GBP_LIBOR_3M.getName());
  /**
   * The 6 month LIBOR index for GBP.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex GBP_LIBOR_6M = IborIndex.of(StandardIborIndices.GBP_LIBOR_6M.getName());
  /**
   * The 12 month LIBOR index for GBP.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex GBP_LIBOR_12M = IborIndex.of(StandardIborIndices.GBP_LIBOR_12M.getName());

  //-------------------------------------------------------------------------
  /**
   * The 1 week LIBOR index for CHF.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex CHF_LIBOR_1W = IborIndex.of(StandardIborIndices.CHF_LIBOR_1W.getName());
  /**
   * The 1 month LIBOR index for CHF.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex CHF_LIBOR_1M = IborIndex.of(StandardIborIndices.CHF_LIBOR_1M.getName());
  /**
   * The 2 month LIBOR index for CHF.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex CHF_LIBOR_2M = IborIndex.of(StandardIborIndices.CHF_LIBOR_2M.getName());
  /**
   * The 3 month LIBOR index for CHF.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex CHF_LIBOR_3M = IborIndex.of(StandardIborIndices.CHF_LIBOR_3M.getName());
  /**
   * The 6 month LIBOR index for CHF.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex CHF_LIBOR_6M = IborIndex.of(StandardIborIndices.CHF_LIBOR_6M.getName());
  /**
   * The 12 month LIBOR index for CHF.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex CHF_LIBOR_12M = IborIndex.of(StandardIborIndices.CHF_LIBOR_12M.getName());

  //-------------------------------------------------------------------------
  /**
   * The 1 week LIBOR index for EUR.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex EUR_LIBOR_1W = IborIndex.of(StandardIborIndices.EUR_LIBOR_1W.getName());
  /**
   * The 1 month LIBOR index for EUR.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex EUR_LIBOR_1M = IborIndex.of(StandardIborIndices.EUR_LIBOR_1M.getName());
  /**
   * The 2 month LIBOR index for EUR.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex EUR_LIBOR_2M = IborIndex.of(StandardIborIndices.EUR_LIBOR_2M.getName());
  /**
   * The 3 month LIBOR index for EUR.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex EUR_LIBOR_3M = IborIndex.of(StandardIborIndices.EUR_LIBOR_3M.getName());
  /**
   * The 6 month LIBOR index for EUR.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex EUR_LIBOR_6M = IborIndex.of(StandardIborIndices.EUR_LIBOR_6M.getName());
  /**
   * The 12 month LIBOR index for EUR.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex EUR_LIBOR_12M = IborIndex.of(StandardIborIndices.EUR_LIBOR_12M.getName());

  //-------------------------------------------------------------------------
  /**
   * The 1 week LIBOR index for JPY.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex JPY_LIBOR_1W = IborIndex.of(StandardIborIndices.JPY_LIBOR_1W.getName());
  /**
   * The 1 month LIBOR index for JPY.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex JPY_LIBOR_1M = IborIndex.of(StandardIborIndices.JPY_LIBOR_1M.getName());
  /**
   * The 2 month LIBOR index for JPY.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex JPY_LIBOR_2M = IborIndex.of(StandardIborIndices.JPY_LIBOR_2M.getName());
  /**
   * The 3 month LIBOR index for JPY.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex JPY_LIBOR_3M = IborIndex.of(StandardIborIndices.JPY_LIBOR_3M.getName());
  /**
   * The 6 month LIBOR index for JPY.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex JPY_LIBOR_6M = IborIndex.of(StandardIborIndices.JPY_LIBOR_6M.getName());
  /**
   * The 12 month LIBOR index for JPY.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex JPY_LIBOR_12M = IborIndex.of(StandardIborIndices.JPY_LIBOR_12M.getName());

  //-------------------------------------------------------------------------
  /**
   * The 1 week LIBOR index for USD.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex USD_LIBOR_1W = IborIndex.of(StandardIborIndices.USD_LIBOR_1W.getName());
  /**
   * The 1 month LIBOR index for USD.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex USD_LIBOR_1M = IborIndex.of(StandardIborIndices.USD_LIBOR_1M.getName());
  /**
   * The 2 month LIBOR index for USD.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex USD_LIBOR_2M = IborIndex.of(StandardIborIndices.USD_LIBOR_2M.getName());
  /**
   * The 3 month LIBOR index for USD.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex USD_LIBOR_3M = IborIndex.of(StandardIborIndices.USD_LIBOR_3M.getName());
  /**
   * The 6 month LIBOR index for USD.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex USD_LIBOR_6M = IborIndex.of(StandardIborIndices.USD_LIBOR_6M.getName());
  /**
   * The 12 month LIBOR index for USD.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex USD_LIBOR_12M = IborIndex.of(StandardIborIndices.USD_LIBOR_12M.getName());

  //-------------------------------------------------------------------------
  /**
   * The 1 week EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_1W = IborIndex.of(StandardIborIndices.EUR_EURIBOR_1W.getName());
  /**
   * The 2 week EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_2W = IborIndex.of(StandardIborIndices.EUR_EURIBOR_2W.getName());
  /**
   * The 1 month EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_1M = IborIndex.of(StandardIborIndices.EUR_EURIBOR_1M.getName());
  /**
   * The 2 month EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_2M = IborIndex.of(StandardIborIndices.EUR_EURIBOR_2M.getName());
  /**
   * The 3 month EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_3M = IborIndex.of(StandardIborIndices.EUR_EURIBOR_3M.getName());
  /**
   * The 6 month EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_6M = IborIndex.of(StandardIborIndices.EUR_EURIBOR_6M.getName());
  /**
   * The 9 month EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_9M = IborIndex.of(StandardIborIndices.EUR_EURIBOR_9M.getName());
  /**
   * The 12 month EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_12M = IborIndex.of(StandardIborIndices.EUR_EURIBOR_12M.getName());

  //-------------------------------------------------------------------------
  /**
   * The 1 week TIBOR (Japan) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  public static final IborIndex JPY_TIBOR_JAPAN_1W = IborIndex.of(StandardIborIndices.JPY_TIBOR_JAPAN_1W.getName());
  /**
   * The 1 month TIBOR (Japan) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  public static final IborIndex JPY_TIBOR_JAPAN_1M = IborIndex.of(StandardIborIndices.JPY_TIBOR_JAPAN_1M.getName());
  /**
   * The 2 month TIBOR (Japan) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  public static final IborIndex JPY_TIBOR_JAPAN_2M = IborIndex.of(StandardIborIndices.JPY_TIBOR_JAPAN_2M.getName());
  /**
   * The 3 month TIBOR (Japan) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  public static final IborIndex JPY_TIBOR_JAPAN_3M = IborIndex.of(StandardIborIndices.JPY_TIBOR_JAPAN_3M.getName());
  /**
   * The 6 month TIBOR (Japan) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  public static final IborIndex JPY_TIBOR_JAPAN_6M = IborIndex.of(StandardIborIndices.JPY_TIBOR_JAPAN_6M.getName());
  /**
   * The 12 month TIBOR (Japan) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  public static final IborIndex JPY_TIBOR_JAPAN_12M = IborIndex.of(StandardIborIndices.JPY_TIBOR_JAPAN_12M.getName());

  //-------------------------------------------------------------------------
  /**
   * The 1 week TIBOR (Euroyen) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  public static final IborIndex JPY_TIBOR_EUROYEN_1W = IborIndex.of(StandardIborIndices.JPY_TIBOR_EUROYEN_1W.getName());
  /**
   * The 1 month TIBOR (Euroyen) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  public static final IborIndex JPY_TIBOR_EUROYEN_1M = IborIndex.of(StandardIborIndices.JPY_TIBOR_EUROYEN_1M.getName());
  /**
   * The 2 month TIBOR (Euroyen) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  public static final IborIndex JPY_TIBOR_EUROYEN_2M = IborIndex.of(StandardIborIndices.JPY_TIBOR_EUROYEN_2M.getName());
  /**
   * The 3 month TIBOR (Euroyen) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  public static final IborIndex JPY_TIBOR_EUROYEN_3M = IborIndex.of(StandardIborIndices.JPY_TIBOR_EUROYEN_3M.getName());
  /**
   * The 6 month TIBOR (Euroyen) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  public static final IborIndex JPY_TIBOR_EUROYEN_6M = IborIndex.of(StandardIborIndices.JPY_TIBOR_EUROYEN_6M.getName());
  /**
   * The 12 month TIBOR (Euroyen) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  public static final IborIndex JPY_TIBOR_EUROYEN_12M = IborIndex.of(StandardIborIndices.JPY_TIBOR_EUROYEN_12M.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private IborIndices() {
  }

}
