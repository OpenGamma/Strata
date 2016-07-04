/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

/**
 * Enumerates the supported types of underlying instruments on an ISDA yield curve.
 */
public enum IsdaYieldCurveUnderlyingType {

  /**
   * A money market instrument.
   */
  ISDA_MONEY_MARKET,
  /**
   * A swap.
   */
  ISDA_SWAP

}
