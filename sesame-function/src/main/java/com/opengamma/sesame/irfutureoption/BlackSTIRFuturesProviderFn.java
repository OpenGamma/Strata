/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irfutureoption;

import com.opengamma.analytics.financial.provider.description.interestrate.BlackSTIRFuturesProviderInterface;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.trade.IRFutureOptionTrade;
import com.opengamma.util.result.Result;

/**
 * Function to return an instance of {@link BlackSTIRFuturesProviderInterface} for a given interest rate future option.
 */
public interface BlackSTIRFuturesProviderFn {

  /**
   * Returns the black volatility provider for a STIR future option.
   * @param env the environment to return the black volatility provider for, not null.
   * @param trade the trade to return the black volatility provider for, not null.
   * @return the black volatility provider for a STIR future option trade.
   */
  Result<BlackSTIRFuturesProviderInterface> getBlackSTIRFuturesProvider(Environment env, IRFutureOptionTrade trade);
}
