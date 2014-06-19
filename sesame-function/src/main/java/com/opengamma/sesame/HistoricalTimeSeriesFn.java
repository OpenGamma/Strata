/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.analytics.timeseries.HistoricalTimeSeriesBundle;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.sesame.cache.CacheLifetime;
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.util.result.Result;

/**
 * The functionality on this interface will move. see SSM-215
 */
public interface HistoricalTimeSeriesFn {
  /**
   * Finds the fixing requirements for the security.
   * 
   * @param env the environment that the fixing requirements are needed for. 
   * @param security the security to return the fixings for.
   * @return the bundle of fixing requirements.
   */
  @Cacheable(CacheLifetime.DAY)
  Result<HistoricalTimeSeriesBundle> getFixingsForSecurity(Environment env, FinancialSecurity security);
}
