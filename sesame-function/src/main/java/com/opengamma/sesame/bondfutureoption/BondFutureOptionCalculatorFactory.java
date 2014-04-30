/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bondfutureoption;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.BondFutureOptionTrade;
import com.opengamma.util.result.Result;

/**
 * Interface for a factory that creates bond future option calculators.
 */
public interface BondFutureOptionCalculatorFactory {

  /**
   * Creates a calculator for the bond future option trade.
   * @param env the environment, not null.
   * @param trade the bond future option trade, not null.
   * @return a calculator for bond future options.
   */
  Result<BondFutureOptionCalculator> createCalculator(Environment env, BondFutureOptionTrade trade);
}
