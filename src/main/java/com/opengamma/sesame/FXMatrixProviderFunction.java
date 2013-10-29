/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.analytics.financial.forex.method.FXMatrix;

/**
 * Provides an FX Matrix.
 */
public interface FXMatrixProviderFunction {

  FunctionResult<FXMatrix> getFXMatrix(MarketData marketData, String curveConfigurationName);
}
