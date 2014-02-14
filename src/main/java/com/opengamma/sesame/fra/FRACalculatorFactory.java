/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.conversion.FRASecurityConverter;
import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.sesame.ValuationTimeFn;

public class FRACalculatorFactory {

  /**
   * Converter for a FRA.
   */
  private final FRASecurityConverter _fraConverter;

  /**
   * Provides the valuation time to perform calculations as at.
   */
  private final ValuationTimeFn _valuationTimeFn;

  public FRACalculatorFactory(FRASecurityConverter fraConverter,
                              ValuationTimeFn valuationTimeFn) {
    _fraConverter = fraConverter;
    _valuationTimeFn = valuationTimeFn;
  }

  public FRACalculator createCalculator(FRASecurity security,
                                        MulticurveProviderDiscount bundle,
                                        CurveBuildingBlockBundle mergedJacobianBundle) {
    return new FRACalculator(security, bundle, mergedJacobianBundle, _fraConverter, _valuationTimeFn);
  }
}
