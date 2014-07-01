/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.market;

import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.util.result.Result;

/**
 * Implements mapping logic for {@link CreditCurveDataKey}. This function adds 
 * a level of indirection when resolving credit curves. The input key is
 * inferred directly from the security being priced. The output key will be a 
 * reference to the target credit curve. This allows generic curves to be
 * used for pricing specified credit securities.
 */
public interface CreditKeyMapperFn {
  
  /**
   * Map the input key to a target key to be used for credit curve resolution.
   * 
   * A result will always be returned. If no mapping exists in the underlying
   * implementation, the input key will be returned.
   * 
   * @param key a credit key
   * @return a credit key result
   */
  Result<CreditCurveDataKey> map(CreditCurveDataKey key);
  
}
