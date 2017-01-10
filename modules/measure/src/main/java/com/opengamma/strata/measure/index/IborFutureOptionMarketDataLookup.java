/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.pricer.index.IborFutureOptionVolatilities;
import com.opengamma.strata.pricer.index.IborFutureOptionVolatilitiesId;

/**
 * The lookup that provides access to Ibor future option volatilities in market data.
 * <p>
 * The Ibor future option market lookup provides access to the volatilities used to price Ibor future options.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface IborFutureOptionMarketDataLookup extends CalculationParameter {

  /**
   * Obtains an instance based on a single mapping from index to volatility identifier.
   * <p>
   * The lookup provides volatilities for the specified index.
   *
   * @param index  the Ibor index
   * @param volatilityId  the volatility identifier
   * @return the Ibor future option lookup containing the specified mapping
   */
  public static IborFutureOptionMarketDataLookup of(IborIndex index, IborFutureOptionVolatilitiesId volatilityId) {
    return DefaultIborFutureOptionMarketDataLookup.of(ImmutableMap.of(index, volatilityId));
  }

  /**
   * Obtains an instance based on a map of volatility identifiers.
   * <p>
   * The map is used to specify the appropriate volatilities to use for each index.
   *
   * @param volatilityIds  the volatility identifiers, keyed by index
   * @return the Ibor future option lookup containing the specified volatilities
   */
  public static IborFutureOptionMarketDataLookup of(Map<IborIndex, IborFutureOptionVolatilitiesId> volatilityIds) {
    return DefaultIborFutureOptionMarketDataLookup.of(volatilityIds);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the type that the lookup will be queried by.
   * <p>
   * This returns {@code IborFutureOptionMarketLookup.class}.
   * When querying parameters using {@link CalculationParameters#findParameter(Class)},
   * {@code IborFutureOptionMarketLookup.class} must be passed in to find the instance.
   * 
   * @return the type of the parameter implementation
   */
  @Override
  default Class<? extends CalculationParameter> queryType() {
    return IborFutureOptionMarketDataLookup.class;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the set of indices that volatilities are provided for.
   *
   * @return the set of indices
   */
  public abstract ImmutableSet<IborIndex> getVolatilityIndices();

  /**
   * Gets the identifiers used to obtain the volatilities for the specified currency.
   * <p>
   * The result will typically refer to a surface or cube.
   * If the index is not found, an exception is thrown.
   *
   * @param index  the index for which identifiers are required
   * @return the set of market data identifiers 
   * @throws IllegalArgumentException if the index is not found
   */
  public abstract ImmutableSet<MarketDataId<?>> getVolatilityIds(IborIndex index);

  //-------------------------------------------------------------------------
  /**
   * Creates market data requirements for the specified indices.
   * 
   * @param indices  the indices, for which volatilities are required
   * @return the requirements
   */
  public default FunctionRequirements requirements(IborIndex... indices) {
    return requirements(ImmutableSet.copyOf(indices));
  }

  /**
   * Creates market data requirements for the specified indices.
   * 
   * @param indices  the indices, for which volatilities are required
   * @return the requirements
   */
  public abstract FunctionRequirements requirements(Set<IborIndex> indices);

  //-------------------------------------------------------------------------
  /**
   * Obtains a filtered view of the complete set of market data.
   * <p>
   * This method returns an instance that binds the lookup to the market data.
   * The input is {@link ScenarioMarketData}, which contains market data for all scenarios.
   * 
   * @param marketData  the complete set of market data for all scenarios
   * @return the filtered market data
   */
  public default IborFutureOptionScenarioMarketData marketDataView(ScenarioMarketData marketData) {
    return DefaultIborFutureOptionScenarioMarketData.of(this, marketData);
  }

  /**
   * Obtains a filtered view of the complete set of market data.
   * <p>
   * This method returns an instance that binds the lookup to the market data.
   * The input is {@link MarketData}, which contains market data for one scenario.
   * 
   * @param marketData  the complete set of market data for one scenario
   * @return the filtered market data
   */
  public default IborFutureOptionMarketData marketDataView(MarketData marketData) {
    return DefaultIborFutureOptionMarketData.of(this, marketData);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains Ibor future option volatilities based on the specified market data.
   * <p>
   * This provides {@link IborFutureOptionVolatilities} suitable for pricing an Ibor future option.
   * Although this method can be used directly, it is typically invoked indirectly
   * via {@link IborFutureOptionMarketData}:
   * <pre>
   *  // bind the baseData to this lookup
   *  IborFutureOptionMarketData view = lookup.marketDataView(baseData);
   *  
   *  // pass around IborFutureOptionMarketData within the function to use in pricing
   *  IborFutureOptionVolatilities vols = view.volatilities(index);
   * </pre>
   * 
   * @param index  the Ibor index
   * @param marketData  the complete set of market data for one scenario
   * @return the volatilities
   * @throws MarketDataNotFoundException if the index is not found
   */
  public abstract IborFutureOptionVolatilities volatilities(IborIndex index, MarketData marketData);

}
