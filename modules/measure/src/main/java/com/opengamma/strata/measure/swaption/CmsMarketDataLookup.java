/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swaption;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesId;

/**
 * The lookup that provides access to swaption volatilities in market data.
 * <p>
 * The swaption market lookup provides access to the volatilities used to price CMS. Separate interface to
 * {@link SwaptionMarketDataLookup} to allow different vols to price CMS and Swaptions in a portfolio.
 * <p>
 * The lookup implements {@link CalculationParameter} and is used by passing it
 * as an argument to {@link CalculationRules}. It provides the link between the
 * data that the function needs and the data that is available in {@link ScenarioMarketData}.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface CmsMarketDataLookup extends SwaptionMarketDataLookup {

  /**
   * Obtains an instance based on a single mapping from index to volatility identifier.
   * <p>
   * The lookup provides volatilities for the specified index.
   *
   * @param index  the Ibor index
   * @param volatilityId  the volatility identifier
   * @return the CMS lookup containing the specified mapping
   */
  public static CmsMarketDataLookup of(IborIndex index, SwaptionVolatilitiesId volatilityId) {
    return DefaultCmsMarketDataLookup.of(ImmutableMap.of(index, volatilityId));
  }

  /**
   * Obtains an instance based on a map of volatility identifiers.
   * <p>
   * The map is used to specify the appropriate volatilities to use for each index.
   *
   * @param volatilityIds  the volatility identifiers, keyed by index
   * @return the CMS lookup containing the specified volatilities
   */
  public static CmsMarketDataLookup of(Map<IborIndex, SwaptionVolatilitiesId> volatilityIds) {
    return DefaultCmsMarketDataLookup.of(volatilityIds);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the type that the lookup will be queried by.
   * <p>
   * This returns {@code CmsMarketDataLookup.class}.
   * When querying parameters using {@link CalculationParameters#findParameter(Class)},
   * {@code CmsMarketDataLookup.class} must be passed in to find the instance.
   *
   * @return the type of the parameter implementation
   */
  @Override
  public default Class<? extends CalculationParameter> queryType() {
    return CmsMarketDataLookup.class;
  }

}
