/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static com.opengamma.sesame.StandardResultGenerator.propagateFailure;
import static com.opengamma.sesame.StandardResultGenerator.success;

import com.opengamma.financial.analytics.CurrencyLabelledMatrix1D;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.sesame.FunctionResult;

public class DiscountingFXForwardPVFn implements FXForwardPVFn {

  private final FxForwardCalculatorFn _fxForwardCalculatorFn;

  public DiscountingFXForwardPVFn(FxForwardCalculatorFn fxForwardCalculatorFn) {

    _fxForwardCalculatorFn = fxForwardCalculatorFn;
  }

  @Override
  public FunctionResult<CurrencyLabelledMatrix1D> calculatePV(FXForwardSecurity security) {

    FunctionResult<FxForwardCalculator> result = _fxForwardCalculatorFn.generateCalculator(security);
    if (result.isResultAvailable()) {
      return success(result.getResult().calculatePV());
    } else {
      return propagateFailure(result);
    }
  }
}
