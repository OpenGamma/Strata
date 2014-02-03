/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fxforward;

import static com.opengamma.util.result.ResultGenerator.failure;
import static com.opengamma.util.result.ResultGenerator.propagateFailure;
import static com.opengamma.util.result.ResultGenerator.success;

import java.util.Map;

import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.financial.analytics.DoubleLabelledMatrix1D;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.model.multicurve.MultiCurveUtils;
import com.opengamma.financial.security.fx.FXForwardSecurity;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

public class DiscountingFXForwardYieldCurveNodeSensitivitiesFn implements FXForwardYieldCurveNodeSensitivitiesFn {

  private final FXForwardCalculatorFn _fxForwardCalculatorFn;

  private final CurveDefinition _curveDefinition;

  public DiscountingFXForwardYieldCurveNodeSensitivitiesFn(FXForwardCalculatorFn fxForwardCalculatorFn,
                                                           CurveDefinition curveDefinition) {
    _fxForwardCalculatorFn = fxForwardCalculatorFn;
    _curveDefinition = curveDefinition;
  }

  @Override
  public Result<DoubleLabelledMatrix1D> calculateYieldCurveNodeSensitivities(FXForwardSecurity security) {

    Result<FXForwardCalculator> forwardCalculatorResult =
        _fxForwardCalculatorFn.generateCalculator(security);

    if (forwardCalculatorResult.isValueAvailable()) {

      FXForwardCalculator fxForwardCalculator = forwardCalculatorResult.getValue();
      final MultipleCurrencyParameterSensitivity sensitivities = fxForwardCalculator.generateBlockCurveSensitivities();

      return findMatchingSensitivities(sensitivities);
    } else {
      return propagateFailure(forwardCalculatorResult);
    }
  }

  private Result<DoubleLabelledMatrix1D> findMatchingSensitivities(MultipleCurrencyParameterSensitivity sensitivities) {

    final String curveName = _curveDefinition.getName();
    for (final Map.Entry<Pair<String, Currency>, DoubleMatrix1D> entry : sensitivities.getSensitivities().entrySet()) {
      if (curveName.equals(entry.getKey().getFirst())) {
        return success(MultiCurveUtils.getLabelledMatrix(entry.getValue(), _curveDefinition));
      }
    }
    return failure(FailureStatus.MISSING_DATA, "No sensitivities found for curve name: {}", curveName);
  }
}
