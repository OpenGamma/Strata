/*
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
 * <p>
 * If a floating rate has a constant here, then it is fully supported by Strata
 * with example holiday calendar data.
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
   * 
   * @deprecated Not published as of 2018-12-03
   */
  @Deprecated
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
   * 
   * @deprecated Not published as of 2018-12-03
   */
  @Deprecated
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
   * 
   * @deprecated Not published as of 2018-12-03
   */
  @Deprecated
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
   * 
   * @deprecated Not published as of 2019-04-01
   */
  @Deprecated
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
   * 
   * @deprecated Not published as of 2019-04-01
   */
  @Deprecated
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
   * 
   * @deprecated Not published as of 2021-05-17
   */
  @Deprecated
  public static final IborIndex CAD_CDOR_6M = IborIndex.of("CAD-CDOR-6M");
  /**
   * The 12 month CDOR index.
   * <p>
   * The "Canadian Dollar Offered Rate".
   * 
   * @deprecated Not published as of 2021-05-17
   */
  @Deprecated
  public static final IborIndex CAD_CDOR_12M = IborIndex.of("CAD-CDOR-12M");

  //-------------------------------------------------------------------------
  /**
   * The 1 week PRIBOR index.
   * <p>
   * The "Prague Interbank Offered Rate".
   */
  public static final IborIndex CZK_PRIBOR_1W = IborIndex.of("CZK-PRIBOR-1W");
  /**
   * The 2 week PRIBOR index.
   * <p>
   * The "Prague Interbank Offered Rate".
   */
  public static final IborIndex CZK_PRIBOR_2W = IborIndex.of("CZK-PRIBOR-2W");
  /**
   * The 1 month PRIBOR index.
   * <p>
   * The "Prague Interbank Offered Rate".
   */
  public static final IborIndex CZK_PRIBOR_1M = IborIndex.of("CZK-PRIBOR-1M");
  /**
   * The 2 month PRIBOR index.
   * <p>
   * The "Prague Interbank Offered Rate".
   */
  public static final IborIndex CZK_PRIBOR_2M = IborIndex.of("CZK-PRIBOR-2M");
  /**
   * The 3 month PRIBOR index.
   * <p>
   * The "Prague Interbank Offered Rate".
   */
  public static final IborIndex CZK_PRIBOR_3M = IborIndex.of("CZK-PRIBOR-3M");
  /**
   * The 6 month PRIBOR index.
   * <p>
   * The "Prague Interbank Offered Rate".
   */
  public static final IborIndex CZK_PRIBOR_6M = IborIndex.of("CZK-PRIBOR-6M");
  /**
   * The 9 month PRIBOR index.
   * <p>
   * The "Prague Interbank Offered Rate".
   */
  public static final IborIndex CZK_PRIBOR_9M = IborIndex.of("CZK-PRIBOR-9M");
  /**
   * The 12 month PRIBOR index.
   * <p>
   * The "Prague Interbank Offered Rate".
   */
  public static final IborIndex CZK_PRIBOR_12M = IborIndex.of("CZK-PRIBOR-12M");

  //-------------------------------------------------------------------------
  /**
   * The 1 week CIBOR index.
   * <p>
   * The "Copenhagen Interbank Offered Rate".
   */
  public static final IborIndex DKK_CIBOR_1W = IborIndex.of("DKK-CIBOR-1W");
  /**
   * The 2 week CIBOR index.
   * <p>
   * The "Copenhagen Interbank Offered Rate".
   */
  public static final IborIndex DKK_CIBOR_2W = IborIndex.of("DKK-CIBOR-2W");
  /**
   * The 1 month CIBOR index.
   * <p>
   * The "Copenhagen Interbank Offered Rate".
   */
  public static final IborIndex DKK_CIBOR_1M = IborIndex.of("DKK-CIBOR-1M");
  /**
   * The 2 month CIBOR index.
   * <p>
   * The "Copenhagen Interbank Offered Rate".
   */
  public static final IborIndex DKK_CIBOR_2M = IborIndex.of("DKK-CIBOR-2M");
  /**
   * The 3 month CIBOR index.
   * <p>
   * The "Copenhagen Interbank Offered Rate".
   */
  public static final IborIndex DKK_CIBOR_3M = IborIndex.of("DKK-CIBOR-3M");
  /**
   * The 6 month CIBOR index.
   * <p>
   * The "Copenhagen Interbank Offered Rate".
   */
  public static final IborIndex DKK_CIBOR_6M = IborIndex.of("DKK-CIBOR-6M");
  /**
   * The 9 month CIBOR index.
   * <p>
   * The "Copenhagen Interbank Offered Rate".
   */
  public static final IborIndex DKK_CIBOR_9M = IborIndex.of("DKK-CIBOR-9M");
  /**
   * The 12 month CIBOR index.
   * <p>
   * The "Copenhagen Interbank Offered Rate".
   */
  public static final IborIndex DKK_CIBOR_12M = IborIndex.of("DKK-CIBOR-12M");

  //-------------------------------------------------------------------------
  /**
   * The 1 week BUBOR index.
   * <p>
   * The "Budapest Interbank Offered Rate".
   */
  public static final IborIndex HUF_BUBOR_1W = IborIndex.of("HUF-BUBOR-1W");
  /**
   * The 2 week BUBOR index.
   * <p>
   * The "Budapest Interbank Offered Rate".
   */
  public static final IborIndex HUF_BUBOR_2W = IborIndex.of("HUF-BUBOR-2W");
  /**
   * The 1 month BUBOR index.
   * <p>
   * The "Budapest Interbank Offered Rate".
   */
  public static final IborIndex HUF_BUBOR_1M = IborIndex.of("HUF-BUBOR-1M");
  /**
   * The 2 month BUBOR index.
   * <p>
   * The "Budapest Interbank Offered Rate".
   */
  public static final IborIndex HUF_BUBOR_2M = IborIndex.of("HUF-BUBOR-2M");
  /**
   * The 3 month BUBOR index.
   * <p>
   * The "Budapest Interbank Offered Rate".
   */
  public static final IborIndex HUF_BUBOR_3M = IborIndex.of("HUF-BUBOR-3M");
  /**
   * The 6 month BUBOR index.
   * <p>
   * The "Budapest Interbank Offered Rate".
   */
  public static final IborIndex HUF_BUBOR_6M = IborIndex.of("HUF-BUBOR-6M");
  /**
   * The 9 month BUBOR index.
   * <p>
   * The "Budapest Interbank Offered Rate".
   */
  public static final IborIndex HUF_BUBOR_9M = IborIndex.of("HUF-BUBOR-9M");
  /**
   * The 12 month BUBOR index.
   * <p>
   * The "Budapest Interbank Offered Rate".
   */
  public static final IborIndex HUF_BUBOR_12M = IborIndex.of("HUF-BUBOR-12M");

  //-------------------------------------------------------------------------
  /**
   * The 4 week TIIE index.
   * <p>
   * The "Interbank Equilibrium Interest Rate".
   */
  public static final IborIndex MXN_TIIE_4W = IborIndex.of("MXN-TIIE-4W");
  /**
   * The 13 week TIIE index.
   * <p>
   * The "Interbank Equilibrium Interest Rate".
   */
  public static final IborIndex MXN_TIIE_13W = IborIndex.of("MXN-TIIE-13W");
  /**
   * The 26 week TIIE index.
   * <p>
   * The "Interbank Equilibrium Interest Rate".
   */
  public static final IborIndex MXN_TIIE_26W = IborIndex.of("MXN-TIIE-26W");

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
   * The 1 month BKBM index.
   * <p>
   * The "New Zealand Bank Bill Benchmark Rate".
   */
  public static final IborIndex NZD_BKBM_1M = IborIndex.of("NZD-BKBM-1M");
  /**
   * The 2 month BKBM index.
   * <p>
   * The "New Zealand Bank Bill Benchmark Rate".
   */
  public static final IborIndex NZD_BKBM_2M = IborIndex.of("NZD-BKBM-2M");
  /**
   * The 3 month BKBM index.
   * <p>
   * The "New Zealand Bank Bill Benchmark Rate".
   */
  public static final IborIndex NZD_BKBM_3M = IborIndex.of("NZD-BKBM-3M");
  /**
   * The 4 month BKBM index.
   * <p>
   * The "New Zealand Bank Bill Benchmark Rate".
   */
  public static final IborIndex NZD_BKBM_4M = IborIndex.of("NZD-BKBM-4M");
  /**
   * The 5 month BKBM index.
   * <p>
   * The "New Zealand Bank Bill Benchmark Rate".
   */
  public static final IborIndex NZD_BKBM_5M = IborIndex.of("NZD-BKBM-5M");
  /**
   * The 6 month BKBM index.
   * <p>
   * The "New Zealand Bank Bill Benchmark Rate".
   */
  public static final IborIndex NZD_BKBM_6M = IborIndex.of("NZD-BKBM-6M");

  //-------------------------------------------------------------------------
  /**
   * The 1 week WIBOR index.
   * <p>
   * The "Polish Interbank Offered Rate".
   */
  public static final IborIndex PLN_WIBOR_1W = IborIndex.of("PLN-WIBOR-1W");
  /**
   * The 1 month WIBOR index.
   * <p>
   * The "Polish Interbank Offered Rate".
   */
  public static final IborIndex PLN_WIBOR_1M = IborIndex.of("PLN-WIBOR-1M");
  /**
   * The 3 month WIBOR index.
   * <p>
   * The "Polish Interbank Offered Rate".
   */
  public static final IborIndex PLN_WIBOR_3M = IborIndex.of("PLN-WIBOR-3M");
  /**
   * The 6 month WIBOR index.
   * <p>
   * The "Polish Interbank Offered Rate".
   */
  public static final IborIndex PLN_WIBOR_6M = IborIndex.of("PLN-WIBOR-6M");
  /**
   * The 12 month WIBOR index.
   * <p>
   * The "Polish Interbank Offered Rate".
   */
  public static final IborIndex PLN_WIBOR_12M = IborIndex.of("PLN-WIBOR-12M");

  //-------------------------------------------------------------------------
  /**
   * The 1 week STIBOR index.
   * <p>
   * The "Swedish Interbank Offered Rate".
   */
  public static final IborIndex SEK_STIBOR_1W = IborIndex.of("SEK-STIBOR-1W");
  /**
   * The 1 month STIBOR index.
   * <p>
   * The "Swedish Interbank Offered Rate".
   */
  public static final IborIndex SEK_STIBOR_1M = IborIndex.of("SEK-STIBOR-1M");
  /**
   * The 2 month STIBOR index.
   * <p>
   * The "Swedish Interbank Offered Rate".
   */
  public static final IborIndex SEK_STIBOR_2M = IborIndex.of("SEK-STIBOR-2M");
  /**
   * The 3 month STIBOR index.
   * <p>
   * The "Swedish Interbank Offered Rate".
   */
  public static final IborIndex SEK_STIBOR_3M = IborIndex.of("SEK-STIBOR-3M");
  /**
   * The 6 month STIBOR index.
   * <p>
   * The "Swedish Interbank Offered Rate".
   */
  public static final IborIndex SEK_STIBOR_6M = IborIndex.of("SEK-STIBOR-6M");

  //-------------------------------------------------------------------------
  /**
   * The 1 month JIBAR index.
   * <p>
   * The "Johannnesburg Interbank Average Rate".
   */
  public static final IborIndex ZAR_JIBAR_1M = IborIndex.of("ZAR-JIBAR-1M");
  /**
   * The 3 month JIBAR index.
   * <p>
   * The "Johannnesburg Interbank Average Rate".
   */
  public static final IborIndex ZAR_JIBAR_3M = IborIndex.of("ZAR-JIBAR-3M");
  /**
   * The 6 month JIBAR index.
   * <p>
   * The "Johannnesburg Interbank Average Rate".
   */
  public static final IborIndex ZAR_JIBAR_6M = IborIndex.of("ZAR-JIBAR-6M");
  /**
   * The 12 month JIBAR index.
   * <p>
   * The "Johannnesburg Interbank Average Rate".
   */
  public static final IborIndex ZAR_JIBAR_12M = IborIndex.of("ZAR-JIBAR-12M");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private IborIndices() {
  }

}
