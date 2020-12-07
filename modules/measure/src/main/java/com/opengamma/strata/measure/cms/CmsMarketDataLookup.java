/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.cms;

import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.swaption.SwaptionMarketDataLookup;

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
