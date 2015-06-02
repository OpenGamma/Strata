/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.config;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import org.joda.beans.ImmutableBean;

import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.market.curve.CurveParameterMetadata;

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
  public abstract Set<ObservableKey> requirements();

  /**
   * Returns a trade representing the instrument at the node.
   *
   * @param valuationDate the valuation date used when calibrating the curve
   * @param marketData the market data required to build a trade for the instrument
   * @return a trade representing the instrument at the node
   */
  public abstract Trade trade(LocalDate valuationDate, Map<ObservableKey, Double> marketData);

  /**
   * Returns metadata for the node.
   *
   * @param valuationDate  the valuation date used when calibrating the curve
   * @return metadata for the node
   */
  public abstract CurveParameterMetadata metadata(LocalDate valuationDate);
}
