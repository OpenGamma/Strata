/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.definition;

import java.time.LocalDate;
import java.util.Set;

import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.market.curve.DatedCurveParameterMetadata;
import com.opengamma.strata.market.value.ValueType;

/**
 * A node in the configuration specifying how to calibrate a curve.
 * <p>
 * A curve node is associated with an instrument and provides a method to create a trade representing the instrument.
 */
public interface CurveNode {

  /**
   * Determines the market data that is required by the node.
   * <p>
   * This returns the market data needed to build the trade that the node represents.
   *
   * @return requirements for the market data needed to build a trade representing the instrument at the node
   */
  public abstract Set<ObservableKey> requirements();

  /**
   * Returns metadata for the node.
   * <p>
   * This provides curve metadata for the node at the specified valuation date.
   *
   * @param valuationDate  the valuation date used when calibrating the curve
   * @return metadata for the node
   */
  public abstract DatedCurveParameterMetadata metadata(LocalDate valuationDate);

  /**
   * Creates a trade representing the instrument at the node.
   * <p>
   * This uses the observed market data to build the trade that the node represents.
   *
   * @param valuationDate  the valuation date used when calibrating the curve
   * @param marketData  the market data required to build a trade for the instrument
   * @return a trade representing the instrument at the node
   */
  public abstract Trade trade(LocalDate valuationDate, MarketData marketData);

  /**
   * Gets the initial guess used for calibrating the node.
   * <p>
   * This uses the observed market data to select a suitable initial guess.
   * For example, a Fixed-Ibor swap would return the market quote, which is the fixed rate,
   * providing that the value type is 'ZeroRate'.
   * <p>
   * This is primarily used as a performance hint. Since the guess is refined by
   * calibration, in most cases any suitable number can be returned, such as zero.
   *
   * @param valuationDate  the valuation date used when calibrating the curve
   * @param marketData  the market data required to build a trade for the instrument
   * @param valueType  the type of y-value that the curve will contain
   * @return the initial guess of the calibrated value
   */
  public abstract double initialGuess(LocalDate valuationDate, MarketData marketData, ValueType valueType);

}
