/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfutureoption;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.IRFutureOptionTrade;
import com.opengamma.util.result.Result;

/**
 * Interface for a factory that creates interest rate future option calculators.
 */
public interface IRFutureOptionCalculatorFactory {

  /**
   * Creates a calculator for the specified interest rate future option trade.
   * @param env the current environment, not null.
   * @param trade the interest rate future option trade, not null.
   * @return result containing the calculator if successful, a failure result otherwise.
   */
  Result<IRFutureOptionCalculator> createCalculator(Environment env, IRFutureOptionTrade trade);
}
