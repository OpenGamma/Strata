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

import com.google.common.collect.Iterables;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;

/**
 * Evaluates a token against curve currency parameter sensitivities.
 */
public class CurveCurrencyParameterSensitivitiesTokenEvaluator
    extends TokenEvaluator<CurveCurrencyParameterSensitivities> {

  private final BeanTokenEvaluator beanEvaluator = new BeanTokenEvaluator();
  
  @Override
  public Class<?> getTargetType() {
    return CurveCurrencyParameterSensitivities.class;
  }

  @Override
  public Set<String> tokens(CurveCurrencyParameterSensitivities sensitivities) {
    Set<String> tokens = new HashSet<String>();
    for (CurveCurrencyParameterSensitivity sensitivity : sensitivities.getSensitivities()) {
      tokens.add(sensitivity.getCurrency().getCode().toLowerCase());
      tokens.add(sensitivity.getMetadata().getCurveName().toString().toLowerCase());
    }
    if (sensitivities.getSensitivities().size() == 1) {
      CurveCurrencyParameterSensitivity sensitivity = Iterables.getOnlyElement(sensitivities.getSensitivities());
      tokens.addAll(beanEvaluator.tokens(sensitivity));
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
      return Result.success(CurveCurrencyParameterSensitivities.of(candidates));
    } else if (sensitivities.getSensitivities().size() == 1) {
      CurveCurrencyParameterSensitivity sensitivity = Iterables.getOnlyElement(sensitivities.getSensitivities());
      return beanEvaluator.evaluate(sensitivity, token);
    } else {
      return invalidTokenFailure(sensitivities, token);
    }
  }

}
