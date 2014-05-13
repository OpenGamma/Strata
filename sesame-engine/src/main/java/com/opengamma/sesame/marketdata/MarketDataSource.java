/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * A source of market data.
 * <p>
 * This may be a source of live market data or backed by a database of historical data or snapshots.
 * This is intended to be a low level interface, functions are expected to use {@link MarketDataFn}.
 */
public interface MarketDataSource {

  /**
   * Returns an item of market data.
   * <p>
   * If the market data is not present, the result will be returned with a
   * status of {@link FailureStatus#MISSING_DATA}.
   *
   * @param id  the external identifier of the data
   * @param fieldName  the name of the field in the market data record
   * @return a success result of the market data value, or a failure result of {@code MISSING_DATA}
   */
  Result<?> get(ExternalIdBundle id, FieldName fieldName);

}
