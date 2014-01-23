/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.sesame.cache.CacheLifetime;
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;

/**
 * Provides an FX Matrix.
 */
public interface FXMatrixFn {

  @Cacheable(CacheLifetime.FOREVER)
  Result<FXMatrix> getFXMatrix(Set<Currency> currencies);

  @Cacheable(CacheLifetime.FOREVER)
  Result<FXMatrix> getFXMatrix(CurveConstructionConfiguration configuration, ZonedDateTime valuationTime);
}
