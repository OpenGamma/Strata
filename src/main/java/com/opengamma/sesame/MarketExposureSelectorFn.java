/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.sesame.cache.CacheLifetime;
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.util.result.Result;

/**
 * Function capable of providing a market exposure selector.
 */
public interface MarketExposureSelectorFn {

  /**
   * Gets a market exposure selector.
   * 
   * @return the selector, a failure result if not found
   */
  @Cacheable(CacheLifetime.FOREVER) // TODO is this lifetime correct?
  Result<MarketExposureSelector> getMarketExposureSelector();

}
