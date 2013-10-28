/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

/**
 * Stores the results of a market data request.
 */
public interface MarketDataResult {

  MarketDataStatus getStatus(MarketDataRequirement requirement);

  MarketDataValue getMarketValue(MarketDataRequirement requirement);

  MarketDataResult combine(MarketDataResult result);
}
