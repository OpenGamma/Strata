/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.financial.currency.CurrencyMatrixConfigPopulator;
import com.opengamma.sesame.engine.ComponentMap;

/**
 * 
 */
public class DefaultMarketDataFnFactory implements MarketDataFnFactory {

  private final ComponentMap _componentMap;
  
  public DefaultMarketDataFnFactory(ComponentMap componentMap) {
    _componentMap = componentMap;
  }
  
  @Override
  public MarketDataFn create(MarketDataSpecification spec) {
    RawMarketDataSource rawDataSource = SpecificationMarketDataFactory.createRawDataSource(_componentMap, spec);
    ConfigSource configSource = _componentMap.getComponent(ConfigSource.class);
    return new EagerMarketDataFn(rawDataSource, configSource, CurrencyMatrixConfigPopulator.BLOOMBERG_LIVE_DATA);
  }

}
