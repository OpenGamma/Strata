/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.pricer.bond.BondFutureVolatilities;
import com.opengamma.strata.pricer.bond.BondFutureVolatilitiesId;
import com.opengamma.strata.product.SecurityId;

/**
 * The lookup that provides access to bond future volatilities in market data.
 * <p>
 * The bond future option market lookup provides access to the volatilities used to price bond future options.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface BondFutureOptionMarketDataLookup extends CalculationParameter {

  /**
   * Obtains an instance based on a single mapping from security ID to volatility identifier.
   * <p>
   * The lookup provides volatilities for the specified security ID.
   *
   * @param securityId  the security ID
   * @param volatilityId  the volatility identifier
   * @return the bond future options lookup containing the specified mapping
   */
  public static BondFutureOptionMarketDataLookup of(SecurityId securityId, BondFutureVolatilitiesId volatilityId) {
    return DefaultBondFutureOptionMarketDataLookup.of(ImmutableMap.of(securityId, volatilityId));
  }

  /**
   * Obtains an instance based on a map of volatility identifiers.
   * <p>
   * The map is used to specify the appropriate volatilities to use for each security ID.
   *
   * @param volatilityIds  the volatility identifiers, keyed by security ID
   * @return the bond future options lookup containing the specified volatilities
   */
  public static BondFutureOptionMarketDataLookup of(Map<SecurityId, BondFutureVolatilitiesId> volatilityIds) {
    return DefaultBondFutureOptionMarketDataLookup.of(volatilityIds);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the type that the lookup will be queried by.
   * <p>
   * This returns {@code BondFutureOptionMarketLookup.class}.
   * When querying parameters using {@link CalculationParameters#findParameter(Class)},
   * {@code BondFutureOptionMarketLookup.class} must be passed in to find the instance.
   * 
   * @return the type of the parameter implementation
   */
  @Override
  default Class<? extends CalculationParameter> queryType() {
    return BondFutureOptionMarketDataLookup.class;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the set of security IDs that volatilities are provided for.
   *
   * @return the set of security IDs
   */
  public abstract ImmutableSet<SecurityId> getVolatilitySecurityIds();

  /**
   * Gets the identifiers used to obtain the volatilities for the specified security ID.
   * <p>
   * The result will typically refer to a surface or cube.
   * If the security ID is not found, an exception is thrown.
   *
   * @param securityId  the security ID for which identifiers are required
   * @return the set of market data identifiers 
   * @throws IllegalArgumentException if the security ID is not found
   */
  public abstract ImmutableSet<MarketDataId<?>> getVolatilityIds(SecurityId securityId);

  //-------------------------------------------------------------------------
  /**
   * Creates market data requirements for the specified security IDs.
   * 
   * @param securityIds  the security IDs, for which volatilities are required
   * @return the requirements
   */
  public default FunctionRequirements requirements(SecurityId... securityIds) {
    return requirements(ImmutableSet.copyOf(securityIds));
  }

  /**
   * Creates market data requirements for the specified security IDs.
   * 
   * @param securityIds  the security IDs, for which volatilities are required
   * @return the requirements
   */
  public abstract FunctionRequirements requirements(Set<SecurityId> securityIds);

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
  public default BondFutureOptionScenarioMarketData marketDataView(ScenarioMarketData marketData) {
    return DefaultBondFutureOptionScenarioMarketData.of(this, marketData);
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
  public default BondFutureOptionMarketData marketDataView(MarketData marketData) {
    return DefaultBondFutureOptionMarketData.of(this, marketData);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains bond future volatilities based on the specified market data.
   * <p>
   * This provides {@link BondFutureVolatilities} suitable for pricing bond future options.
   * Although this method can be used directly, it is typically invoked indirectly
   * via {@link BondFutureOptionMarketData}:
   * <pre>
   *  // bind the baseData to this lookup
   *  BondFutureOptionMarketData view = lookup.marketDataView(baseData);
   *  
   *  // pas around BondFutureOptionMarketData within the function to use in pricing
   *  BondFutureVolatilities vols = view.volatilities(securityId);
   * </pre>
   * 
   * @param securityId  the security ID
   * @param marketData  the complete set of market data for one scenario
   * @return the volatilities
   * @throws MarketDataNotFoundException if the security ID is not found
   */
  public abstract BondFutureVolatilities volatilities(SecurityId securityId, MarketData marketData);

}
