/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

/**
 * Function providing market data to clients. When data is requested a {@link MarketDataFunctionResult}
 * is returned which contains status (and potentially value) for every item that has been requested.
 * TODO blocking variant
 */
public interface MarketDataProviderFunction {

  /**
   * Request a single item of market data.
   *
   * @param requirement the item of market data being requested
   * @return a result object containing an indication of whether the data is (currently)
   * available and the value if it is.
   */
  MarketDataFunctionResult requestData(MarketDataRequirement requirement);

  /**
   * Request multiple item,s of market data.
   *
   * @param requirements the items of market data being requested
   * @return a result object containing an indication of whether each item of data is (currently)
   * available and the value if it is.
   */
  MarketDataFunctionResult requestData(Set<MarketDataRequirement> requirements);
}
