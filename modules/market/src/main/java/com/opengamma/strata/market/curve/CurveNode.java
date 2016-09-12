/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.time.LocalDate;
import java.util.Set;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.Trade;

/**
 * A node in the configuration specifying how to calibrate a curve.
 * <p>
 * A curve node is associated with an instrument and provides a method to create a trade representing the instrument.
 */
public interface CurveNode {

  /**
   * Gets the label to use for the node.
   * 
   * @return the label, not empty
   */
  public abstract String getLabel();

  /**
   * Gets the date order rules that apply to this node within the curve.
   * <p>
   * Each curve node has an associated date which defines the x-value in the curve,
   * available via {@link #date(LocalDate, ReferenceData)}. Restrictions may be placed
   * on the node to prevent it from being too close, before or after another node.
   * 
   * @return the date order rules
   */
  public abstract CurveNodeDateOrder getDateOrder();

  /**
   * Determines the market data that is required by the node.
   * <p>
   * This returns the market data needed to build the trade that the node represents.
   *
   * @return requirements for the market data needed to build a trade representing the instrument at the node
   */
  public abstract Set<? extends MarketDataId<?>> requirements();

  /**
   * Calculates the date associated with the node.
   * <p>
   * Each curve node has an associated date which defines the x-value in the curve.
   * This date is visible in the {@linkplain DatedParameterMetadata parameter metadata}.
   *
   * @param valuationDate  the valuation date used when calibrating the curve
   * @param refData  the reference data to use to resolve the trade
   * @return the date associated with the node
   */
  public abstract LocalDate date(LocalDate valuationDate, ReferenceData refData);

  /**
   * Returns metadata for the node.
   * <p>
   * This provides curve metadata for the node at the specified valuation date.
   *
   * @param valuationDate  the valuation date used when calibrating the curve
   * @param refData  the reference data to use to resolve the trade
   * @return metadata for the node
   */
  public abstract DatedParameterMetadata metadata(LocalDate valuationDate, ReferenceData refData);

  /**
   * Creates a trade representing the instrument at the node.
   * <p>
   * This uses the observed market data to build the trade that the node represents.
   * The reference data is typically used to find the start date of the trade from the valuation date.
   * The resulting trade is not resolved.
   * The notional of the trade is taken from the 'quantity' variable.
   * The quantity is signed and will affect whether the trade is Buy or Sell.
   * The valuation date is defined by the market data.
   *
   * @param quantity  the quantity or notional of the trade
   * @param marketData  the market data required to build a trade for the instrument, including the valuation date
   * @param refData  the reference data, used to resolve the trade dates
   * @return a trade representing the instrument at the node
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @throws RuntimeException if unable to resolve due to an invalid definition
   */
  public abstract Trade trade(double quantity, MarketData marketData, ReferenceData refData);

  /**
   * Creates a resolved trade representing the instrument at the node.
   * <p>
   * This uses the observed market data to build the trade that the node represents.
   * The trade is then resolved using the specified reference data if necessary.
   * The valuation date is defined by the market data.
   * <p>
   * Resolved objects may be bound to data that changes over time, such as holiday calendars.
   * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
   * Care must be taken when placing the resolved form in a cache or persistence layer.
   *
   * @param quantity  the quantity or notional of the trade
   * @param marketData  the market data required to build a trade for the instrument, including the valuation date
   * @param refData  the reference data, used to resolve the trade
   * @return a trade representing the instrument at the node
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @throws RuntimeException if unable to resolve due to an invalid definition
   */
  public abstract ResolvedTrade resolvedTrade(double quantity, MarketData marketData, ReferenceData refData);

  /**
   * Gets the initial guess used for calibrating the node.
   * <p>
   * This uses the observed market data to select a suitable initial guess.
   * For example, a Fixed-Ibor swap would return the market quote, which is the fixed rate,
   * providing that the value type is 'ZeroRate'.
   * The valuation date is defined by the market data.
   * <p>
   * This is primarily used as a performance hint. Since the guess is refined by
   * calibration, in most cases any suitable number can be returned, such as zero.
   *
   * @param marketData  the market data required to build a trade for the instrument, including the valuation date
   * @param valueType  the type of y-value that the curve will contain
   * @return the initial guess of the calibrated value
   */
  public abstract double initialGuess(MarketData marketData, ValueType valueType);

}
