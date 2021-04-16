/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swaption;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesId;

/**
 * The lookup that provides access to swaption volatilities in market data.
 * <p>
 * The swaption market lookup provides access to the volatilities used to price swaptions.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface SwaptionMarketDataLookup extends CalculationParameter {

  /**
   * Obtains an instance based on a single mapping from index to volatility identifier.
   * <p>
   * The lookup provides volatilities for the specified index.
   *
   * @param index  the index
   * @param volatilityId  the volatility identifier
   * @return the swaption lookup containing the specified mapping
   */
  public static SwaptionMarketDataLookup of(RateIndex index, SwaptionVolatilitiesId volatilityId) {
    return DefaultSwaptionMarketDataLookup.of(ImmutableMap.of(index, volatilityId));
  }

  /**
   * Obtains an instance based on a map of volatility identifiers.
   * <p>
   * The map is used to specify the appropriate volatilities to use for each index.
   *
   * @param volatilityIds  the volatility identifiers, keyed by index
   * @return the swaption lookup containing the specified volatilities
   */
  public static SwaptionMarketDataLookup of(Map<RateIndex, SwaptionVolatilitiesId> volatilityIds) {
    return DefaultSwaptionMarketDataLookup.of(volatilityIds);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the type that the lookup will be queried by.
   * <p>
   * This returns {@code SwaptionMarketLookup.class}.
   * When querying parameters using {@link CalculationParameters#findParameter(Class)},
   * {@code SwaptionMarketLookup.class} must be passed in to find the instance.
   * 
   * @return the type of the parameter implementation
   */
  @Override
  public default Class<? extends CalculationParameter> queryType() {
    return SwaptionMarketDataLookup.class;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the set of indices that volatilities are provided for.
   *
   * @return the set of indices
   */
  public abstract ImmutableSet<RateIndex> getVolatilityIndices();

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
  public abstract ImmutableSet<MarketDataId<?>> getVolatilityIds(RateIndex index);

  //-------------------------------------------------------------------------
  /**
   * Creates market data requirements for the specified indices.
   * 
   * @param indices  the indices, for which volatilities are required
   * @return the requirements
   */
  public default FunctionRequirements requirements(RateIndex... indices) {
    return requirements(ImmutableSet.copyOf(indices));
  }

  /**
   * Creates market data requirements for the specified indices.
   * 
   * @param indices  the indices, for which volatilities are required
   * @return the requirements
   */
  public abstract FunctionRequirements requirements(Set<RateIndex> indices);

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
  public default SwaptionScenarioMarketData marketDataView(ScenarioMarketData marketData) {
    return DefaultSwaptionScenarioMarketData.of(this, marketData);
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
  public default SwaptionMarketData marketDataView(MarketData marketData) {
    return DefaultSwaptionMarketData.of(this, marketData);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains swaption volatilities based on the specified market data.
   * <p>
   * This provides {@link SwaptionVolatilities} suitable for pricing a swaption.
   * Although this method can be used directly, it is typically invoked indirectly
   * via {@link SwaptionMarketData}:
   * <pre>
   *  // bind the baseData to this lookup
   *  SwaptionMarketData view = lookup.marketDataView(baseData);
   *  
   *  // pass around SwaptionMarketData within the function to use in pricing
   *  SwaptionVolatilities vols = view.volatilities(index);
   * </pre>
   * 
   * @param index  the index
   * @param marketData  the complete set of market data for one scenario
   * @return the volatilities
   * @throws MarketDataNotFoundException if the index is not found
   */
  public abstract SwaptionVolatilities volatilities(RateIndex index, MarketData marketData);

}
