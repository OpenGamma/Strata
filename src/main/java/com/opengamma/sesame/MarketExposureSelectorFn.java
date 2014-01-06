/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.sesame.cache.CacheLifetime;
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.util.result.Result;

public interface MarketExposureSelectorFn {

  @Cacheable(CacheLifetime.FOREVER) // TODO is this lifetime correct?
  Result<MarketExposureSelector> getMarketExposureSelector();
}
