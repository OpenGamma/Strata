/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.id.ExternalIdBundle;

/**
 * TODO not sure if this is a good idea, just trying it on for size
 */
public interface RawMarketDataSource {

  // TODO is dataField needed for all impls? what about live?
  <T> MarketDataValue<T> get(ExternalIdBundle idBundle, String dataField);
}
