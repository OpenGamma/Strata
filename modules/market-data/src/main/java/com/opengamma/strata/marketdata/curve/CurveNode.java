/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.marketdata.curve;

import org.joda.beans.ImmutableBean;

import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.marketdata.CalculationRequirements;

/**
 * A node in the configuration specifying how to calibrate a curve.
 * <p>
 * A curve node is associated with an instrument and provides a method to create a trade representing the instrument.
 */
public interface CurveNode extends ImmutableBean {

  /**
   * Returns requirements for the market data needed to build a trade representing the instrument at the node.
   *
   * @return requirements for the market data needed to build a trade representing the instrument at the node
   */
  public abstract CalculationRequirements requirements();

  /**
   * Builds a trade representing the instrument at the node.
   *
   * @param marketData  the market data required to build a trade for the instrument
   * @return a trade representing the instrument at the node
   */
  public abstract Trade buildTrade(CurveNodeMarketData marketData);
}
