/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

/**
 * A set of schemes that can be used with {@code StandardId}.
 * <p>
 * The scheme part of a {@link StandardId} is freeform, but it obviously works best where
 * the scheme name is widely agreed. This class contains a set of useful constants.
 */
public final class StandardSchemes {

  /**
   * The OpenGamma scheme used to identify values in market data.
   */
  public static final String OG_TICKER_SCHEME = "OG-Ticker";
  /**
   * The OpenGamma scheme used to identify ETDs in market data.
   */
  public static final String OG_ETD_SCHEME = "OG-ETD";
  /**
   * The OpenGamma scheme used for trade identifiers.
   */
  public static final String OG_TRADE_SCHEME = "OG-Trade";
  /**
   * The OpenGamma scheme used for position identifiers.
   */
  public static final String OG_POSITION_SCHEME = "OG-Position";
  /**
   * The OpenGamma scheme used for portfolio sensitivity identifiers.
   */
  public static final String OG_SENSITIVITY_SCHEME = "OG-Sensitivity";
  /**
   * The OpenGamma scheme used for securities.
   */
  public static final String OG_SECURITY_SCHEME = "OG-Security";
  /**
   * The OpenGamma scheme used for counterparties.
   */
  public static final String OG_COUNTERPARTY = "OG-Counterparty";

  /**
   * The scheme for ISINs.
   * <p>
   * https://en.wikipedia.org/wiki/International_Securities_Identification_Number
   */
  public static final String ISIN_SCHEME = "ISIN";
  /**
   * The scheme for CUSIPs, the North American numbering system.
   * <p>
   * https://en.wikipedia.org/wiki/CUSIP
   */
  public static final String CUSIP_SCHEME = "CUSIP";
  /**
   * The scheme for SEDOLs, the United Kingdom numbering system.
   * <p>
   * https://en.wikipedia.org/wiki/SEDOL
   */
  public static final String SEDOL_SCHEME = "SEDOL";
  /**
   * The scheme for Wertpapierkennnummer, the German numbering system.
   * <p>
   * https://en.wikipedia.org/wiki/Wertpapierkennnummer
   */
  public static final String WKN_SCHEME = "WKN";
  /**
   * The scheme for VALOR numbers, the Swiss numbering system.
   * <p>
   * https://en.wikipedia.org/wiki/Valoren_number
   */
  public static final String VALOR_SCHEME = "VALOR";

  /**
   * The scheme for RICs, the Reuters Instrument Code.
   * <p>
   * https://en.wikipedia.org/wiki/Reuters_Instrument_Code
   */
  public static final String RIC_SCHEME = "RIC";
  /**
   * The scheme for Chain RICs, which identifies a set of linked RICs.
   */
  public static final String CHAIN_RIC_SCHEME = "CHAINRIC";
  /**
   * The scheme for Bloomberg Tickers.
   */
  public static final String BBG_SCHEME = "BBG";
  /**
   * The scheme for FIGIs, the Financial Instrument Global Identifier.
   * <p>
   * https://en.wikipedia.org/wiki/Financial_Instrument_Global_Identifier
   */
  public static final String FIGI_SCHEME = "FIGI";
  /**
   * The scheme for LEIs, the Legal Entity Identifier.
   * <p>
   * https://en.wikipedia.org/wiki/Legal_Entity_Identifier
   */
  public static final String LEI_SCHEME = "LEI";
  /**
   * The scheme for 6 character RED codes, the Reference Entity Data code.
   * <p>
   * https://ihsmarkit.com/products/red-cds.html
   */
  public static final String RED6_SCHEME = "RED6";
  /**
   * The scheme for 9 character RED codes, the Reference Entity Data code.
   * <p>
   * https://ihsmarkit.com/products/red-cds.html
   */
  public static final String RED9_SCHEME = "RED9";

  // restricted constructor
  private StandardSchemes() {
  }

}
