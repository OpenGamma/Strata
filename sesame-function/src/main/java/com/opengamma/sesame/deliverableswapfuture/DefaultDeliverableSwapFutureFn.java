/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.deliverableswapfuture;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.DeliverableSwapFutureTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * Default implementation of the {@link DeliverableSwapFutureFn} that uses a specified calculator function to calculate
 * requested values.
 */
public class DefaultDeliverableSwapFutureFn implements DeliverableSwapFutureFn {
  
  /**
   * The calculator used to provide the OG-Analytics representation of the security and the necessary market data requirements
   * to calculate the requested values.
   */
  private final DeliverableSwapFutureCalculatorFactory _deliverableSwapFutureCalculatorFactory;
  
  public DefaultDeliverableSwapFutureFn(DeliverableSwapFutureCalculatorFactory deliverableSwapFutureCalculatorFactory) {
    _deliverableSwapFutureCalculatorFactory = ArgumentChecker.notNull(deliverableSwapFutureCalculatorFactory, "deliverableSwapFutureCalculatorFactory");
  }
   
  @Override
  public Result<Double> calculateSecurityModelPrice(Environment env, DeliverableSwapFutureTrade trade) {
    Result<DeliverableSwapFutureCalculator> calculatorResult = _deliverableSwapFutureCalculatorFactory.createCalculator(env, trade);
    if (!calculatorResult.isSuccess()) {
      return Result.failure(calculatorResult);
    }
    return calculatorResult.getValue().calculateSecurityModelPrice();
  }
}
