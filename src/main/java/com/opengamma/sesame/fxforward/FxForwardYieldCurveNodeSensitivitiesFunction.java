/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import com.opengamma.financial.analytics.DoubleLabelledMatrix1D;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.FunctionResult;

/**
 * Generates yield curve node sensitivities for an FX Forward security
 */
public interface FxForwardYieldCurveNodeSensitivitiesFunction {

  FunctionResult<DoubleLabelledMatrix1D> calculateYieldCurveNodeSensitivities(FXForwardSecurity security);
}
