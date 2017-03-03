/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

/**
 * Identifiers for common exchanges.
 * <p>
 * The identifier names are ISO Market Identifier Codes (MICs).
 */
public final class ExchangeIds {

  /** Eurex Clearing AG. */
  public static final ExchangeId ECAG = ExchangeId.of("ECAG");

  /** Chicago Mercantile Exchange (CME). */
  public static final ExchangeId XCME = ExchangeId.of("XCME");

  /** Chicago Board of Trade (CBOT). */
  public static final ExchangeId XCBT = ExchangeId.of("XCBT");

  /** New York Mercantile Exchange (NYMEX). */
  public static final ExchangeId XNYM = ExchangeId.of("XNYM");

  /** Commodities Exchange Center (COMEX). */
  public static final ExchangeId XCEC = ExchangeId.of("XCEC");

  /** ICE Futures Europe - Equity Products Division. */
  public static final ExchangeId IFLO = ExchangeId.of("IFLO");

  /** ICE Futures Europe - Financial Products Division. */
  public static final ExchangeId IFLL = ExchangeId.of("IFLL");

  /** ICE Futures Europe - European Utilities Division. */
  public static final ExchangeId IFUT = ExchangeId.of("IFUT");

  /** ICE Futures Europe - Agricultural Products Division. */
  public static final ExchangeId IFLX = ExchangeId.of("IFLX");

  /** ICE Futures Europe - Oil and Refined Products Division. */
  public static final ExchangeId IFEN = ExchangeId.of("IFEN");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private ExchangeIds() {
  }

}
