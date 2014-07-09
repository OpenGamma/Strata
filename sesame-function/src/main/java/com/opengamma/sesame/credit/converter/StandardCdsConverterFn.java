/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.converter;

import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalytic;
import com.opengamma.financial.security.credit.StandardCDSSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.IsdaCreditCurve;
import com.opengamma.util.result.Result;

/**
 * Converts a {@link StandardCDSSecurity} to a its equivalent analytics type.
 */
public interface StandardCdsConverterFn {
  
  /**
   * Convert the given standard cds to its equivalent analytics type.
   * 
   * @param env the pricing environment
   * @param cds the standard cds
   * @param curve the curve resolved for the cds
   * @return the constructed cds analytic
   */
  Result<CDSAnalytic> toCdsAnalytic(Environment env, StandardCDSSecurity cds, IsdaCreditCurve curve);
  
}
