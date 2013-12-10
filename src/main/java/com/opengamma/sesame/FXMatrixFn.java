/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.sesame.cache.Cache;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FunctionResult;

/**
 * Provides an FX Matrix.
 */
public interface FXMatrixFn {

  @Cache
  FunctionResult<FXMatrix> getFXMatrix(Set<Currency> currencies);

  @Cache
  FunctionResult<FXMatrix> getFXMatrix(CurveConstructionConfiguration configuration);
}
