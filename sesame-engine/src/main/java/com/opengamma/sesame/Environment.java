/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Map;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.sesame.marketdata.MarketDataSource;

/**
 * The execution environment for functions, includes the valuation time and market data.
 */
public interface Environment {

  /**
   * Gets the valuation date.
   * <p>
   * This method should be used in preference to {@link #getValuationTime()} if only the date is required.
   * 
   * @return the valuation date, not null
   */
  LocalDate getValuationDate();

  /**
   * Gets the valuation time.
   * <p>
   * Use {@link #getValuationDate()} in preference to this method if you only need the date.
   * 
   * @return the valuation time, not null
   */
  ZonedDateTime getValuationTime();

  /**
   * Gets the source used to access market data.
   *
   * @return the market data source, not null
   */
  MarketDataSource getMarketDataSource();

  /**
   * Returns the scenario argument for a function, possibly null
   *
   * @param function the function whose scenario argument is required
   * @return the scenario argument for the specified function, possibly null
   * TODO should this return a list? that would allow composition. is that needed?
   */
  Object getScenarioArgument(Object function);

  /**
   * @return the scenario arguments for all functions, keyed by the function implementation type
   * that consumes the argument.
   * TODO is there a nice way to avoid exposing this to the functions?
   * make Environment an abstract class and make this package private? would have to re-jig the packages
   * move to a subtype and downcast in the proxy?
   */
  Map<Class<?>, Object> getScenarioArguments();

  /**
   * Returns a new environment copied from this environment but with a different valuation time.
   *
   * @param valuationTime  the valuation time for the new environment, not null
   * @return a new environment copied from this environment but with the specified valuation time, not null
   */
  Environment withValuationTime(ZonedDateTime valuationTime);

  /**
   * Returns a new environment copied from this environment but with a different valuation time.
   *
   * @param marketData  the market data for the new environment, not null
   * @return a new environment copied from this environment but with the specified market data, not null
   */
  Environment withMarketData(MarketDataSource marketData);

  /**
   * Returns a new environment with a different valuation time and market data.
   *
   * @param marketData  the market data for the new environment, not null
   * @param valuationTime  the valuation time for the new environment, not null
   * @return a new environment, not null
   */
  Environment with(ZonedDateTime valuationTime, MarketDataSource marketData);

  /**
   * Returns a new environment copied from this one but with different scenario arguments.
   *
   * @param scenarioArguments the scenario arguments
   * @return an environment copied from this one but with difference scenario arguments
   */
  Environment withScenarioArguments(Map<Class<?>, Object> scenarioArguments);
}
