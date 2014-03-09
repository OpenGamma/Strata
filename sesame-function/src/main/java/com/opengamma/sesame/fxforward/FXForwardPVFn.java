/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import com.opengamma.financial.analytics.CurrencyLabelledMatrix1D;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.result.Result;

/**
 * Calculate FX PV for an FX Forward.
 */
public interface FXForwardPVFn {

  @Output(OutputNames.FX_PRESENT_VALUE)
  Result<CurrencyLabelledMatrix1D> calculatePV(Environment env, FXForwardSecurity security);
}
