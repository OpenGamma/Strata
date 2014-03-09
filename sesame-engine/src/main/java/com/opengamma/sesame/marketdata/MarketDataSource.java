/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.Result;

/**
 * A source of market data.
 * This may be a source of live market data or backed by a database of historical data or data snapshots.
 * This is intended to be a low level interface, functions are expected to use {@link MarketDataFn}.
 */
public interface MarketDataSource {

  /**
   * Returns an item of market data.
   *
   * @param id the ID of the data
   * @param fieldName the name of the field in the market data record
   * @return the data value
   */
  Result<?> get(ExternalIdBundle id, FieldName fieldName);

  // TODO do we need other versions of get taking sets of IDs and / or field names?
}
