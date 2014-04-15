/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bondfuture;

import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Bond future calculator.
 */
public interface BondFutureCalculator {

  Result<MultipleCurrencyAmount> calculatePV();
  
  Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01();
}
