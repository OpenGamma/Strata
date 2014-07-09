/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.converter;

import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalytic;
import com.opengamma.financial.security.credit.LegacyCDSSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.IsdaCreditCurve;
import com.opengamma.util.result.Result;

/**
 * Converts a {@link LegacyCDSSecurity} to a its equivalent analytics type.
 */
public interface LegacyCdsConverterFn {
  
  /**
   * Convert the given legacy cds to its equivalent analytics type.
   * 
   * @param env the pricing environment
   * @param legacyCds the legacy cds
   * @param curve the curve resolved for the cds
   * @return the constructed cds analytic
   */
  Result<CDSAnalytic> toCdsAnalytic(Environment env, LegacyCDSSecurity legacyCds, IsdaCreditCurve curve);
  
}
