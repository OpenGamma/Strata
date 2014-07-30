/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit;

import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantCreditCurve;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.result.Result;

/**
 * Function for producing {@link ISDACompliantCreditCurve}s, the analytics object
 * for representing credit curves.
 */
public interface IsdaCompliantCreditCurveFn {
  
  /**
   * Builds the credit curve for the specified {@link CreditCurveDataKey}.
   * @param env the pricing environment
   * @param creditCurveKey the credit key
   * @return an {@link ISDACompliantCreditCurve} result
   */
  @Cacheable
  @Output(OutputNames.ISDA_CREDIT_CURVE)
  Result<IsdaCreditCurve> buildIsdaCompliantCreditCurve(Environment env, CreditCurveDataKey creditCurveKey);
  
}
