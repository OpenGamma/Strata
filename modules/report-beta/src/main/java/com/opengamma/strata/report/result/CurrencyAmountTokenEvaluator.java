/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Evaluates a token against a currency amount.
 */
public class CurrencyAmountTokenEvaluator implements TokenEvaluator<CurrencyAmount> {

  private final String CURRENCY_FIELD = "currency";
  private final String AMOUNT_FIELD = "amount";

  @Override
  public Class<CurrencyAmount> getTargetType() {
    return CurrencyAmount.class;
  }

  @Override
  public ImmutableSet<String> tokens(CurrencyAmount amount) {
    return ImmutableSet.of(CURRENCY_FIELD, AMOUNT_FIELD);
  }

  @Override
  public Object evaluate(CurrencyAmount amount, String token) {
    if (token.equals(CURRENCY_FIELD)) {
      return amount.getCurrency();
    }
    if (token.equals(AMOUNT_FIELD)) {
      // Can be rendered directly - retains the currency for formatting purposes
      return amount;
    }
    throw new TokenException(token, TokenError.INVALID, tokens(amount));
  }

}
