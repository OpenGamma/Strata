/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static com.opengamma.util.result.ResultGenerator.propagateFailure;
import static com.opengamma.util.result.ResultGenerator.success;

import com.opengamma.financial.analytics.CurrencyLabelledMatrix1D;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

public class DiscountingFXForwardPVFn implements FXForwardPVFn {

  private final FXForwardCalculatorFn _fxForwardCalculatorFn;

  public DiscountingFXForwardPVFn(FXForwardCalculatorFn fxForwardCalculatorFn) {
    _fxForwardCalculatorFn = fxForwardCalculatorFn;
  }

  @Override
  public Result<CurrencyLabelledMatrix1D> calculatePV(Environment env, FXForwardSecurity security) {
    Result<FXForwardCalculator> result = _fxForwardCalculatorFn.generateCalculator(env, security);

    if (result.isValueAvailable()) {
      return success(result.getValue().calculatePV(env));
    } else {
      return propagateFailure(result);
    }
  }
}
