/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.sesame.engine.ComponentMap;

/**
 * TODO need to ensure impls can be compared, will need to know if data source changes for cache invalidation logic
 */
public interface MarketDataFactory {

  MarketDataProviderFunction create(ComponentMap components);
}
