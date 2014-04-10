/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fedfundsfuture;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.FedFundsFutureTrade;
import com.opengamma.util.result.Result;

/**
 * Federal funds future calculator factory.
 */
public interface FedFundsFutureCalculatorFactory {

  Result<FedFundsFutureCalculator> createCalculator(Environment env, FedFundsFutureTrade trade);
}
