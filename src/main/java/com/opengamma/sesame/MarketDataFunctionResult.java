/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Map;

public interface MarketDataFunctionResult extends FunctionResult<Map<MarketDataRequirement, MarketDataValue>> {

  boolean isFullyAvailable();

  MarketDataValue getSingleMarketDataValue();
}
