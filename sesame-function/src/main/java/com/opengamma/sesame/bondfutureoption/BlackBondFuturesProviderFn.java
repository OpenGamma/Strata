/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bondfutureoption;

import com.opengamma.analytics.financial.provider.description.interestrate.BlackBondFuturesProviderInterface;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.BondFutureOptionTrade;
import com.opengamma.util.result.Result;

/**
 * Function to return instances of {@link BlackBondFuturesProviderInterface}.
 */
public interface BlackBondFuturesProviderFn {

  /**
   * Returns the black volatility provider for a bond future trade.
   * @param env the environment to create the black provider for, not null.
   * @param trade the trade to create the black provider for, not null.
   * @return the black volatility provider for a bond future trade.
   */
  Result<BlackBondFuturesProviderInterface> getBlackBondFuturesProvider(Environment env, BondFutureOptionTrade trade);
}
