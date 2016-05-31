/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import com.opengamma.strata.data.MarketDataId;

/**
 * Market data identifier used by functions that need access to objects containing market data for multiple scenarios.
 * <p>
 * Many functions are written to calculate a single value at a time, for example a single present value for a trade.
 * These functions need to consume single values of market data, for example a curve or a quoted price.
 * <p>
 * However, it may be more efficient in some cases to calculate values for all scenarios at the same time.
 * To do this efficiently it may be helpful to package the market data into a structure that is suitable for
 * bulk calculations. Implementations of this interface allow these values to be requested.
 * 
 * @param <T>  the type of data referred to by the identifier
 * @param <U>  the type of the multi-scenario data
 */
public interface ScenarioMarketDataId<T, U extends ScenarioArray<T>> {

  /**
   * Gets the market data identifier of the market data value.
   *
   * @return the market data identifier of the market data value
   */
  public abstract MarketDataId<T> getMarketDataId();

  /**
   * Gets the type of the object containing the market data for all scenarios.
   *
   * @return the type of the object containing the market data for all scenarios
   */
  public abstract Class<U> getScenarioMarketDataType();

  /**
   * Creates an instance of the scenario market data object from a box containing data of the same underlying
   * type.
   * <p>
   * There are many possible ways to store scenario market data for a data type. For example, if the single
   * values are doubles, the scenario value might simply be a {@code List<Double>} or it might be a wrapper
   * class that stores the values more efficiently in a {@code double[]}.
   * <p>
   * This method allows a scenario value of the required type to be created from a different type of
   * scenario value or from a single value.
   * <p>
   * Normally this method will not be used. It is assumed the required scenario values will be created by the
   * perturbations that create scenario data. However there is no mechanism in the market data system to guarantee
   * that scenario values of a particular type are available. If they are not, this method creates them on demand.
   * <p>
   * Values returned from this method might be cached in the market data containers for efficiency.
   *
   * @param marketDataBox  a market data box containing single market data values of the same type as the
   *   scenario value identified by this key
   * @param scenarioCount  the number of scenarios for which data is required in the returned value
   * @return an object containing market data for multiple scenarios built from the data in the market data box
   */
  public abstract U createScenarioValue(MarketDataBox<T> marketDataBox, int scenarioCount);

}
