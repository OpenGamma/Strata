/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit;

import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.financial.analytics.curve.ISDAYieldCurveDefinition;
import com.opengamma.sesame.Environment;
import com.opengamma.util.result.Result;

/**
 * Function for producing {@link ISDACompliantYieldCurve}s, the analytics object for
 * representing yield curves in ISDA compliant credit calculations.
 */
public interface ISDACompliantYieldCurveFn {
  
  
  /**
   * Builds an {@link ISDACompliantYieldCurve} from the passed {@link CurveConstructionConfiguration}.
   * @param env the pricing environment
   * @param definition the curve config to use
   * @return a result containing an {@link ISDACompliantCurve} if successful, or failure
   */
  Result<ISDACompliantYieldCurve> buildISDACompliantCurve(Environment env, ISDAYieldCurveDefinition definition);
  
  
}
