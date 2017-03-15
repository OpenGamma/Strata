/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;

/**
 * Token evaluator for currency parameter sensitivity.
 * <p>
 * Although there is a formatter for this type, users will traverse to a single sensitivity from
 * a list of sensitivities. This traversal may include redundant tokens, so the purpose of this
 * evaluator is to continue returning the same sensitivity object as long as the tokens are
 * consistent with the fields on this object.
 */
public class CurrencyParameterSensitivityTokenEvaluator extends TokenEvaluator<CurrencyParameterSensitivity> {

  @Override
  public Class<?> getTargetType() {
    return CurrencyParameterSensitivity.class;
  }

  @Override
  public Set<String> tokens(CurrencyParameterSensitivity sensitivity) {
    return ImmutableSet.of(
        sensitivity.getCurrency().getCode().toLowerCase(Locale.ENGLISH),
        sensitivity.getMarketDataName().getName().toLowerCase(Locale.ENGLISH));
  }

  @Override
  public EvaluationResult evaluate(
      CurrencyParameterSensitivity sensitivity,
      CalculationFunctions functions,
      String firstToken,
      List<String> remainingTokens) {

    if (firstToken.equalsIgnoreCase(sensitivity.getCurrency().getCode()) ||
        firstToken.equalsIgnoreCase(sensitivity.getMarketDataName().getName())) {
      return EvaluationResult.success(sensitivity, remainingTokens);
    } else {
      return invalidTokenFailure(sensitivity, firstToken);
    }
  }

}
