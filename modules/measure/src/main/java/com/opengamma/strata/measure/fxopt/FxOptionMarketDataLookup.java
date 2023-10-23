/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilities;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilitiesId;

/**
 * The lookup that provides access to FX options volatilities in market data.
 * <p>
 * The FX options market lookup provides access to the volatilities used to price FX options.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface FxOptionMarketDataLookup extends CalculationParameter {

  /**
   * Obtains an instance based on a single mapping from currency pair to volatility identifier.
   * <p>
   * The lookup provides volatilities for the specified currency pair.
   *
   * @param currencyPair  the currency pair
   * @param volatilityId  the volatility identifier
   * @return the FX options lookup containing the specified mapping
   */
  public static FxOptionMarketDataLookup of(CurrencyPair currencyPair, FxOptionVolatilitiesId volatilityId) {
    return DefaultFxOptionMarketDataLookup.of(ImmutableMap.of(currencyPair, volatilityId));
  }

  /**
   * Obtains an instance based on a map of volatility identifiers.
   * <p>
   * The map is used to specify the appropriate volatilities to use for each currency pair.
   *
   * @param volatilityIds  the volatility identifiers, keyed by currency pair
   * @return the FX options lookup containing the specified volatilities
   */
  public static FxOptionMarketDataLookup of(Map<CurrencyPair, FxOptionVolatilitiesId> volatilityIds) {
    return DefaultFxOptionMarketDataLookup.of(volatilityIds);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the type that the lookup will be queried by.
   * <p>
   * This returns {@code FxOptionMarketLookup.class}.
   * When querying parameters using {@link CalculationParameters#findParameter(Class)},
   * {@code FxOptionMarketLookup.class} must be passed in to find the instance.
   * 
   * @return the type of the parameter implementation
   */
  @Override
  public default Class<? extends CalculationParameter> queryType() {
    return FxOptionMarketDataLookup.class;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the set of currency pairs that volatilities are provided for.
   *
   * @return the set of currency pairs
   */
  public abstract ImmutableSet<CurrencyPair> getVolatilityCurrencyPairs();

  /**
   * Gets the identifiers used to obtain the volatilities for the specified currency pair.
   * <p>
   * The result will typically refer to a surface or cube.
   * If the currency pair is not found, an empty set is returned.
   *
   * @param currencyPair  the currency pair for which identifiers are required
   * @return the set of market data identifiers, that can be empty
   */
  public abstract ImmutableSet<MarketDataId<?>> getVolatilityIds(CurrencyPair currencyPair);

  //-------------------------------------------------------------------------
  /**
   * Creates market data requirements for the specified currency pairs.
   * 
   * @param currencyPairs  the currency pairs, for which volatilities are required
   * @return the requirements
   */
  public default FunctionRequirements requirements(CurrencyPair... currencyPairs) {
    return requirements(ImmutableSet.copyOf(currencyPairs));
  }

  /**
   * Creates market data requirements for the specified currency pairs.
   * 
   * @param currencyPairs  the currency pairs, for which volatilities are required
   * @return the requirements
   */
  public abstract FunctionRequirements requirements(Set<CurrencyPair> currencyPairs);

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
  public default FxOptionScenarioMarketData marketDataView(ScenarioMarketData marketData) {
    return DefaultFxOptionScenarioMarketData.of(this, marketData);
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
  public default FxOptionMarketData marketDataView(MarketData marketData) {
    return DefaultFxOptionMarketData.of(this, marketData);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains FX options volatilities based on the specified market data.
   * <p>
   * This provides {@link FxOptionVolatilities} suitable for pricing FX options.
   * Although this method can be used directly, it is typically invoked indirectly
   * via {@link FxOptionMarketData}:
   * <pre>
   *  // bind the baseData to this lookup
   *  FxOptionMarketData view = lookup.marketDataView(baseData);
   *  
   *  // pas around FxOptionMarketData within the function to use in pricing
   *  FxOptionVolatilities vols = view.volatilities(currencyPair);
   * </pre>
   * 
   * @param currencyPair  the currency pair
   * @param marketData  the complete set of market data for one scenario
   * @return the volatilities
   * @throws MarketDataNotFoundException if the currency pair is not found
   */
  public abstract FxOptionVolatilities volatilities(CurrencyPair currencyPair, MarketData marketData);

}
