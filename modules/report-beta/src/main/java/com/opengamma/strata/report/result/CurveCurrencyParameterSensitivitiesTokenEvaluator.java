/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;

/**
 * Evaluates a token against curve currency parameter sensitivities.
 */
public class CurveCurrencyParameterSensitivitiesTokenEvaluator
    extends TokenEvaluator<CurveCurrencyParameterSensitivities> {

  @Override
  public Class<?> getTargetType() {
    return CurveCurrencyParameterSensitivities.class;
  }

  @Override
  public Set<String> tokens(CurveCurrencyParameterSensitivities sensitivities) {
    Set<String> tokens = new HashSet<String>();
    for (CurveCurrencyParameterSensitivity sensitivity : sensitivities.getSensitivities()) {
      tokens.add(sensitivity.getCurrency().getCode().toLowerCase());
      tokens.add(sensitivity.getCurveName().toString().toLowerCase());
    }
    return tokens;
  }

  @Override
  public Result<?> evaluate(CurveCurrencyParameterSensitivities sensitivities, String token) {
    List<CurveCurrencyParameterSensitivity> candidates = new ArrayList<>();
    for (CurveCurrencyParameterSensitivity sensitivity : sensitivities.getSensitivities()) {
      if (token.equals(sensitivity.getCurrency().getCode().toLowerCase()) ||
          token.equals(sensitivity.getCurveName().toString().toLowerCase())) {
        candidates.add(sensitivity);
      }
    }
    if (!candidates.isEmpty()) {
      if (candidates.size() == 1) {
        CurveCurrencyParameterSensitivity sensitivity = candidates.get(0);
        return Result.success(sensitivity);
      } else {
        return Result.success(CurveCurrencyParameterSensitivities.of(candidates));
      }
    } else {
      return invalidTokenFailure(sensitivities, token);
    }
  }

}
