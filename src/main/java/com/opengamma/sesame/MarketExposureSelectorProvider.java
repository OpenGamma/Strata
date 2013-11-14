/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.sesame.cache.Cache;

public interface MarketExposureSelectorProvider {

  @Cache
  FunctionResult<MarketExposureSelector> getMarketExposureSelector();
}
