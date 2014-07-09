/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.market;

import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.security.credit.LegacyCDSSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

/**
 * Resolves a {@link CreditCurveDataKey} for the given {@link LegacyCDSSecurity}.
 * This can be used to resolve market data for pricing.
 */
public interface LegacyCdsMarketDataResolverFn
                                extends CreditMarketDataResolverFn<LegacyCDSSecurity> {

  /**
   * Resolves the credit curve data key for pricing this legacy CDS.
   * 
   * @param security the security to price
   * @param env the pricing environment
   * @return the credit curve data key
   */
  @Override
  Result<CreditCurveDataKey> resolve(Environment env, LegacyCDSSecurity security);
  
}
