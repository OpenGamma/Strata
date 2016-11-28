/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.List;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.calc.runner.CalculationFunctions;

/**
 * Evaluates a token against a currency amount.
 */
public class CurrencyAmountTokenEvaluator extends TokenEvaluator<CurrencyAmount> {

  private static final String CURRENCY_FIELD = "currency";
  private static final String AMOUNT_FIELD = "amount";

  @Override
  public Class<CurrencyAmount> getTargetType() {
    return CurrencyAmount.class;
  }

  @Override
  public ImmutableSet<String> tokens(CurrencyAmount amount) {
    return ImmutableSet.of(CURRENCY_FIELD, AMOUNT_FIELD);
  }

  @Override
  public EvaluationResult evaluate(
      CurrencyAmount amount,
      CalculationFunctions functions,
      String firstToken,
      List<String> remainingTokens) {

    if (firstToken.equalsIgnoreCase(CURRENCY_FIELD)) {
      return EvaluationResult.success(amount.getCurrency(), remainingTokens);
    }
    if (firstToken.equalsIgnoreCase(AMOUNT_FIELD)) {
      // Can be rendered directly - retains the currency for formatting purposes
      return EvaluationResult.success(amount, remainingTokens);
    }
    return invalidTokenFailure(amount, firstToken);
  }

}
