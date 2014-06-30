/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit;

import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;

/**
 * Function for producing {@link ISDACompliantYieldCurve}s, the analytics object for
 * representing yield curves in ISDA compliant credit calculations.
 */
public interface IsdaCompliantYieldCurveFn {
  
  /**
   * Builds an {@link ISDACompliantYieldCurve} for the given currency.
   * @param env the pricing environment
   * @param ccy the currency of the curve to bootstrap
   * @return a result containing an {@link ISDACompliantCurve} if successful, or failure
   */
  @Cacheable
  Result<ISDACompliantYieldCurve> buildIsdaCompliantCurve(Environment env, Currency ccy);
  
}
