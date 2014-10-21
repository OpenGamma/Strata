/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.trade;

/**
 * High level categorization of asset.
 * <p>
 * A trade is a transaction that occurred at a specific instant in time.
 */
public enum AssetClass {

  /**
   * Equities.
   */
  EQUITY,
  /**
   * Bonds.
   */
  BOND,
  /**
   * Foreign Exchange.
   */
  FX,
  /**
   * Swaps.
   */
  SWAP,
  /**
   * Options.
   */
  OPTION,
  /**
   * Futures.
   */
  FUTURE,
  /**
   * Credit.
   */
  CREDIT,
  ;

}
