/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.market;

import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.security.bond.BondSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

/**
 * Interface for resolving a credit key from a bond security. The key can
 * be used for resolving a credit curve for pricing against.
 */
public interface BondCreditMarketDataResolverFn {
  
  /**
   * Resolve a credit key for a given bond.
   * 
   * @param env the pricing environment
   * @param bondSecurity the bond security
   * @return a credit key
   */
  Result<CreditCurveDataKey> resolve(Environment env, BondSecurity bondSecurity);
  
}
