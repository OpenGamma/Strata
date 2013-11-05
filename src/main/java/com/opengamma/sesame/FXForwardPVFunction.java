/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.analytics.CurrencyLabelledMatrix1D;
import com.opengamma.financial.security.fx.FXForwardSecurity;

/**
 * Calculate FX PV for an FX Forward.
 */
public interface FXForwardPVFunction {

  FunctionResult<CurrencyLabelledMatrix1D> calculatePV(FXForwardSecurity security);
}
