/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.market;

import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.security.credit.StandardCDSSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

/**
 * Resolves a {@link CreditCurveDataKey} to use for a passed CDS security.
 * This can then be used to resolve a credit curve in a market data
 * snapshot.
 */
public interface StandardCdsMarketDataResolverFn 
                    extends CreditMarketDataResolverFn<StandardCDSSecurity> {
  
  /**
   * Resolves the credit curve data key for pricing this standard CDS.
   * 
   * @param security the security to price
   * @param env the pricing environment
   * @return the credit curve data key
   */
  @Override
  Result<CreditCurveDataKey> resolve(Environment env, StandardCDSSecurity security);
  
}
