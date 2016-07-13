/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

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
  public static final IborIndex GBP_LIBOR_1W = IborIndex.of("GBP-LIBOR-1W");
  /**
   * The 1 month LIBOR index for GBP.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex GBP_LIBOR_1M = IborIndex.of("GBP-LIBOR-1M");
  /**
   * The 2 month LIBOR index for GBP.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex GBP_LIBOR_2M = IborIndex.of("GBP-LIBOR-2M");
  /**
   * The 3 month LIBOR index for GBP.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex GBP_LIBOR_3M = IborIndex.of("GBP-LIBOR-3M");
  /**
   * The 6 month LIBOR index for GBP.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex GBP_LIBOR_6M = IborIndex.of("GBP-LIBOR-6M");
  /**
   * The 12 month LIBOR index for GBP.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex GBP_LIBOR_12M = IborIndex.of("GBP-LIBOR-12M");

  //-------------------------------------------------------------------------
  /**
   * The 1 week LIBOR index for CHF.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex CHF_LIBOR_1W = IborIndex.of("CHF-LIBOR-1W");
  /**
   * The 1 month LIBOR index for CHF.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex CHF_LIBOR_1M = IborIndex.of("CHF-LIBOR-1M");
  /**
   * The 2 month LIBOR index for CHF.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex CHF_LIBOR_2M = IborIndex.of("CHF-LIBOR-2M");
  /**
   * The 3 month LIBOR index for CHF.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex CHF_LIBOR_3M = IborIndex.of("CHF-LIBOR-3M");
  /**
   * The 6 month LIBOR index for CHF.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex CHF_LIBOR_6M = IborIndex.of("CHF-LIBOR-6M");
  /**
   * The 12 month LIBOR index for CHF.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex CHF_LIBOR_12M = IborIndex.of("CHF-LIBOR-12M");

  //-------------------------------------------------------------------------
  /**
   * The 1 week LIBOR index for EUR.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex EUR_LIBOR_1W = IborIndex.of("EUR-LIBOR-1W");
  /**
   * The 1 month LIBOR index for EUR.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex EUR_LIBOR_1M = IborIndex.of("EUR-LIBOR-1M");
  /**
   * The 2 month LIBOR index for EUR.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex EUR_LIBOR_2M = IborIndex.of("EUR-LIBOR-2M");
  /**
   * The 3 month LIBOR index for EUR.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex EUR_LIBOR_3M = IborIndex.of("EUR-LIBOR-3M");
  /**
   * The 6 month LIBOR index for EUR.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex EUR_LIBOR_6M = IborIndex.of("EUR-LIBOR-6M");
  /**
   * The 12 month LIBOR index for EUR.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex EUR_LIBOR_12M = IborIndex.of("EUR-LIBOR-12M");

  //-------------------------------------------------------------------------
  /**
   * The 1 week LIBOR index for JPY.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex JPY_LIBOR_1W = IborIndex.of("JPY-LIBOR-1W");
  /**
   * The 1 month LIBOR index for JPY.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex JPY_LIBOR_1M = IborIndex.of("JPY-LIBOR-1M");
  /**
   * The 2 month LIBOR index for JPY.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex JPY_LIBOR_2M = IborIndex.of("JPY-LIBOR-2M");
  /**
   * The 3 month LIBOR index for JPY.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex JPY_LIBOR_3M = IborIndex.of("JPY-LIBOR-3M");
  /**
   * The 6 month LIBOR index for JPY.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex JPY_LIBOR_6M = IborIndex.of("JPY-LIBOR-6M");
  /**
   * The 12 month LIBOR index for JPY.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex JPY_LIBOR_12M = IborIndex.of("JPY-LIBOR-12M");

  //-------------------------------------------------------------------------
  /**
   * The 1 week LIBOR index for USD.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex USD_LIBOR_1W = IborIndex.of("USD-LIBOR-1W");
  /**
   * The 1 month LIBOR index for USD.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex USD_LIBOR_1M = IborIndex.of("USD-LIBOR-1M");
  /**
   * The 2 month LIBOR index for USD.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex USD_LIBOR_2M = IborIndex.of("USD-LIBOR-2M");
  /**
   * The 3 month LIBOR index for USD.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex USD_LIBOR_3M = IborIndex.of("USD-LIBOR-3M");
  /**
   * The 6 month LIBOR index for USD.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex USD_LIBOR_6M = IborIndex.of("USD-LIBOR-6M");
  /**
   * The 12 month LIBOR index for USD.
   * <p>
   * The "London Interbank Offered Rate".
   */
  public static final IborIndex USD_LIBOR_12M = IborIndex.of("USD-LIBOR-12M");

  //-------------------------------------------------------------------------
  /**
   * The 1 week EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_1W = IborIndex.of("EUR-EURIBOR-1W");
  /**
   * The 2 week EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_2W = IborIndex.of("EUR-EURIBOR-2W");
  /**
   * The 1 month EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_1M = IborIndex.of("EUR-EURIBOR-1M");
  /**
   * The 2 month EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_2M = IborIndex.of("EUR-EURIBOR-2M");
  /**
   * The 3 month EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_3M = IborIndex.of("EUR-EURIBOR-3M");
  /**
   * The 6 month EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_6M = IborIndex.of("EUR-EURIBOR-6M");
  /**
   * The 9 month EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_9M = IborIndex.of("EUR-EURIBOR-9M");
  /**
   * The 12 month EURIBOR index.
   * <p>
   * The "Euro Interbank Offered Rate".
   */
  public static final IborIndex EUR_EURIBOR_12M = IborIndex.of("EUR-EURIBOR-12M");

  //-------------------------------------------------------------------------
  /**
   * The 1 week TIBOR (Japan) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  public static final IborIndex JPY_TIBOR_JAPAN_1W = IborIndex.of("JPY-TIBOR-JAPAN-1W");
  /**
   * The 1 month TIBOR (Japan) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  public static final IborIndex JPY_TIBOR_JAPAN_1M = IborIndex.of("JPY-TIBOR-JAPAN-1M");
  /**
   * The 2 month TIBOR (Japan) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  public static final IborIndex JPY_TIBOR_JAPAN_2M = IborIndex.of("JPY-TIBOR-JAPAN-2M");
  /**
   * The 3 month TIBOR (Japan) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  public static final IborIndex JPY_TIBOR_JAPAN_3M = IborIndex.of("JPY-TIBOR-JAPAN-3M");
  /**
   * The 6 month TIBOR (Japan) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  public static final IborIndex JPY_TIBOR_JAPAN_6M = IborIndex.of("JPY-TIBOR-JAPAN-6M");
  /**
   * The 12 month TIBOR (Japan) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", unsecured call market.
   */
  public static final IborIndex JPY_TIBOR_JAPAN_12M = IborIndex.of("JPY-TIBOR-JAPAN-12M");

  //-------------------------------------------------------------------------
  /**
   * The 1 week TIBOR (Euroyen) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  public static final IborIndex JPY_TIBOR_EUROYEN_1W = IborIndex.of("JPY-TIBOR-EUROYEN-1W");
  /**
   * The 1 month TIBOR (Euroyen) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  public static final IborIndex JPY_TIBOR_EUROYEN_1M = IborIndex.of("JPY-TIBOR-EUROYEN-1M");
  /**
   * The 2 month TIBOR (Euroyen) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  public static final IborIndex JPY_TIBOR_EUROYEN_2M = IborIndex.of("JPY-TIBOR-EUROYEN-2M");
  /**
   * The 3 month TIBOR (Euroyen) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  public static final IborIndex JPY_TIBOR_EUROYEN_3M = IborIndex.of("JPY-TIBOR-EUROYEN-3M");
  /**
   * The 6 month TIBOR (Euroyen) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  public static final IborIndex JPY_TIBOR_EUROYEN_6M = IborIndex.of("JPY-TIBOR-EUROYEN-6M");
  /**
   * The 12 month TIBOR (Euroyen) index.
   * <p>
   * The "Tokyo Interbank Offered Rate", Japan offshore market.
   */
  public static final IborIndex JPY_TIBOR_EUROYEN_12M = IborIndex.of("JPY-TIBOR-EUROYEN-12M");

  //-------------------------------------------------------------------------
  /**
   * The 1 month BBSW index.
   * <p>
   * The AFMA Australian Bank Bill Short Term Rate.
   */
  public static final IborIndex AUD_BBSW_1M = IborIndex.of("AUD-BBSW-1M");
  /**
   * The 2 month BBSW index.
   * <p>
   * The AFMA Australian Bank Bill Short Term Rate.
   */
  public static final IborIndex AUD_BBSW_2M = IborIndex.of("AUD-BBSW-2M");
  /**
   * The 3 month BBSW index.
   * <p>
   * The AFMA Australian Bank Bill Short Term Rate.
   */
  public static final IborIndex AUD_BBSW_3M = IborIndex.of("AUD-BBSW-3M");
  /**
   * The 4 month BBSW index.
   * <p>
   * The AFMA Australian Bank Bill Short Term Rate.
   */
  public static final IborIndex AUD_BBSW_4M = IborIndex.of("AUD-BBSW-4M");
  /**
   * The 5 month BBSW index.
   * <p>
   * The AFMA Australian Bank Bill Short Term Rate.
   */
  public static final IborIndex AUD_BBSW_5M = IborIndex.of("AUD-BBSW-5M");
  /**
   * The 6 month BBSW index.
   * <p>
   * The AFMA Australian Bank Bill Short Term Rate.
   */
  public static final IborIndex AUD_BBSW_6M = IborIndex.of("AUD-BBSW-6M");

  //-------------------------------------------------------------------------
  /**
   * The 1 month CDOR index.
   * <p>
   * The "Canadian Dollar Offered Rate".
   */
  public static final IborIndex CAD_CDOR_1M = IborIndex.of("CAD-CDOR-1M");
  /**
   * The 2 month CDOR index.
   * <p>
   * The "Canadian Dollar Offered Rate".
   */
  public static final IborIndex CAD_CDOR_2M = IborIndex.of("CAD-CDOR-2M");
  /**
   * The 3 month CDOR index.
   * <p>
   * The "Canadian Dollar Offered Rate".
   */
  public static final IborIndex CAD_CDOR_3M = IborIndex.of("CAD-CDOR-3M");
  /**
   * The 6 month CDOR index.
   * <p>
   * The "Canadian Dollar Offered Rate".
   */
  public static final IborIndex CAD_CDOR_6M = IborIndex.of("CAD-CDOR-6M");
  /**
   * The 12 month CDOR index.
   * <p>
   * The "Canadian Dollar Offered Rate".
   */
  public static final IborIndex CAD_CDOR_12M = IborIndex.of("CAD-CDOR-12M");

  //-------------------------------------------------------------------------
  /**
   * The 1 week NIBOR index.
   * <p>
   * The "Norwegian Interbank Offered Rate".
   */
  public static final IborIndex NOK_NIBOR_1W = IborIndex.of("NOK-NIBOR-1W");
  /**
   * The 1 month NIBOR index.
   * <p>
   * The "Norwegian Interbank Offered Rate".
   */
  public static final IborIndex NOK_NIBOR_1M = IborIndex.of("NOK-NIBOR-1M");
  /**
   * The 2 month NIBOR index.
   * <p>
   * The "Norwegian Interbank Offered Rate".
   */
  public static final IborIndex NOK_NIBOR_2M = IborIndex.of("NOK-NIBOR-2M");
  /**
   * The 3 month NIBOR index.
   * <p>
   * The "Norwegian Interbank Offered Rate".
   */
  public static final IborIndex NOK_NIBOR_3M = IborIndex.of("NOK-NIBOR-3M");
  /**
   * The 6 month NIBOR index.
   * <p>
   * The "Norwegian Interbank Offered Rate".
   */
  public static final IborIndex NOK_NIBOR_6M = IborIndex.of("NOK-NIBOR-6M");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private IborIndices() {
  }

}
