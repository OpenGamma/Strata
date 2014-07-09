/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.market;

import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

/**
 * A function which generates a {@link CreditCurveDataKey} for the given security.
 * This key can then be used for resolving credit market data.
 * 
 * @param <T> the security type
 */
public interface CreditMarketDataResolverFn<T> {

  /**
   * Resolve a {@link CreditCurveDataKey} for the given security.
   * 
   * @param security the security to generate the key for
   * @param env the pricing environment
   * @return the credit curve data key
   */
  Result<CreditCurveDataKey> resolve(Environment env, T security);
  
}
