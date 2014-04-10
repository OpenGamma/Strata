/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fedfundsfuture;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.FedFundsFutureTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;

/**
 * Default implementation for returning results for federal funds futures.
 */
public class DefaultFedFundsFutureFn implements FedFundsFutureFn {
  
  private final FedFundsFutureCalculatorFactory _fedFundsFutureCalculatorFactory;
  
  public DefaultFedFundsFutureFn(FedFundsFutureCalculatorFactory fedFundsFutureCalculatorFactory) {
    _fedFundsFutureCalculatorFactory = ArgumentChecker.notNull(fedFundsFutureCalculatorFactory, "fedFundsFutureCalculatorFactory");
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV(Environment env, FedFundsFutureTrade trade) {
    Result<FedFundsFutureCalculator> calculatorResult = _fedFundsFutureCalculatorFactory.createCalculator(env, trade);
    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return calculatorResult.getValue().calculatePV();
  }

}
