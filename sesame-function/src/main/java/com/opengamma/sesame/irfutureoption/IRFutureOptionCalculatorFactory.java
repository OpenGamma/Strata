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

  Result<IRFutureOptionCalculator> createCalculator(Environment env, IRFutureOptionTrade trade);
}
