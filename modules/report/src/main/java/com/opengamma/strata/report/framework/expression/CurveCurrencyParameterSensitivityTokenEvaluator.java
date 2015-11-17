/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;

/**
 * Token evaluator for curve currency parameter sensitivity.
 * <p>
 * Although there is a formatter for this type, users will traverse to a single sensitivity from
 * a list of sensitivities. This traversal may include redundant tokens, so the purpose of this
 * evaluator is to continue returning the same sensitivity object as long as the tokens are
 * consistent with the fields on this object.
 */
public class CurveCurrencyParameterSensitivityTokenEvaluator extends TokenEvaluator<CurveCurrencyParameterSensitivity> {

  @Override
  public Class<?> getTargetType() {
    return CurveCurrencyParameterSensitivity.class;
  }

  @Override
  public Set<String> tokens(CurveCurrencyParameterSensitivity sensitivity) {
    return ImmutableSet.of(
        sensitivity.getCurrency().getCode().toLowerCase(),
        sensitivity.getCurveName().toString().toLowerCase());
  }

  @Override
  public EvaluationResult evaluate(
      CurveCurrencyParameterSensitivity sensitivity,
      String firstToken,
      List<String> remainingTokens) {

    if (firstToken.equalsIgnoreCase(sensitivity.getCurrency().getCode()) ||
        firstToken.equalsIgnoreCase(sensitivity.getCurveName().toString())) {
      return EvaluationResult.success(sensitivity, remainingTokens);
    } else {
      return invalidTokenFailure(sensitivity, firstToken);
    }
  }

}
