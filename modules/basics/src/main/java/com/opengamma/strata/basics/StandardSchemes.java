/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;

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
   * The scheme for exchange Tickers.
   * <p>
   * A ticker is the human-readable identifier used by the exchange for a security.
   * It is not a stable identifier, and each exchange defines their own tickers.
   * A company can change ticker over time, such as if the company merges or changes it's name.
   * If a company ceases to use a particular ticker, the ticker can be reused for an entirely different company.
   * Tickers are typically reused over time as companies change.
   * <p>
   * A ticker is unlikely to be useful as an identifier without an exchange, see {@link #TICMIC_SCHEME}.
   */
  public static final String TICKER_SCHEME = "TICKER";
  /**
   * The scheme for TICMICs combining the exchange Ticker with the exchange MIC.
   * <p>
   * A TICMIC is an identifier that combines the {@linkplain #TICKER_SCHEME Ticker}, as defined by the exchange,
   * with the MIC (Market Identifier Code) that identifies the exchange.
   * The format is {@code <ticker>@<exchangeMic>}.
   * For example, ULVR@XLON represents Unilever on the London Stock Exchange.
   */
  public static final String TICMIC_SCHEME = "TICMIC";
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
  /**
   * The scheme for OPRA option codes.
   * <p>
   * These codes have:
   * <ul>
   * <li>1 to 5 characters for the underlying root symbol
   * <li>1 letter representing the month and put/call
   * <li>2 digits for the day-of-month
   * <li>2 digits for the year
   * <li>1 character flag indicating the scale of the strike
   * <li>6 digits for the strike
   * </ul>
   * https://customers.refinitiv.com/wetfetch/index.aspx?CID=27348&doc=OSI_FAQ_18th_Feb_2010.pdf&base=/support/datasupport/option_symbology_change.aspx
   */
  public static final String OPRA_SCHEME = "OPRA";
  /**
   * The scheme for OCC option codes.
   * <p>
   * These codes have:
   * <ul>
   * <li>1 to 6 characters for the underlying root symbol
   * <li>2 digits for the year
   * <li>2 digits for the month
   * <li>2 digits for the day-of-month
   * <li>1 letter representing put/call
   * <li>8 digits for the strike multiplied by 1000
   * </ul>
   * https://customers.refinitiv.com/wetfetch/index.aspx?CID=27348&doc=OSI_FAQ_18th_Feb_2010.pdf&base=/support/datasupport/option_symbology_change.aspx
   * https://ibkr.info/node/972
   */
  public static final String OCC_SCHEME = "OCC";

  // restricted constructor
  private StandardSchemes() {
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a TICMIC identifier.
   * <p>
   * A TICMIC is an identifier that combines the {@linkplain #TICKER_SCHEME Ticker}, as defined by the exchange,
   * with the MIC (Market Identifier Code) that defines the exchange.
   * 
   * @param ticker the ticker, as defined by the exchange
   * @param exchangeMic the MIC code of the exchange, four characters
   * @return the TICMIC identifier
   */
  public static StandardId createTicMic(String ticker, String exchangeMic) {
    ArgChecker.notNull(ticker, "ticker");
    ArgChecker.notNull(exchangeMic, "exchangeMic");
    ArgChecker.isTrue(exchangeMic.length() == 4, "MIC must have 4 characters, but was {}", exchangeMic);
    return StandardId.of(TICMIC_SCHEME, ticker + '@' + exchangeMic);
  }

  /**
   * Splits a TICMIC identifier.
   * <p>
   * This method extracts the Ticker and exchange MIC from the identifier.
   * 
   * @param ticMic the TICMIC identifier
   * @return the pair, holding the Ticker and MIC
   * @throws IllegalArgumentException if unable to split the identifier
   */
  public static Pair<String, String> splitTicMic(StandardId ticMic) {
    ArgChecker.notNull(ticMic, "ticMic");
    int splitPos = ticMic.getValue().lastIndexOf('@');
    if (splitPos < 0 || ticMic.getValue().length() < 6 || splitPos != ticMic.getValue().length() - 5) {
      throw new IllegalArgumentException("Invalid TICMIC identifier: " + ticMic);
    }
    String ticker = ticMic.getValue().substring(0, splitPos);
    String mic = ticMic.getValue().substring(splitPos + 1);
    return Pair.of(ticker, mic);
  }

}
