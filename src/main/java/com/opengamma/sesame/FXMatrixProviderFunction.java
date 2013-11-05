/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.util.money.Currency;

/**
 * Provides an FX Matrix.
 */
public interface FXMatrixProviderFunction {

  FunctionResult<FXMatrix> getFXMatrix(Set<Currency> currencies);

  FunctionResult<FXMatrix> getFXMatrix(CurveConstructionConfiguration configuration);
}
