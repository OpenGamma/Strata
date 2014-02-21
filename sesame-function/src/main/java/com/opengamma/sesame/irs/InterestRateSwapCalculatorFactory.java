/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.conversion.InterestRateSwapSecurityConverter;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.sesame.ValuationTimeFn;
import com.opengamma.util.ArgumentChecker;

public class InterestRateSwapCalculatorFactory {

  /**
   * Converter for a Swap
   */
  private final InterestRateSwapSecurityConverter _swapConverter;

  /**
   * Provides the valuation time to perform calculations as at.
   */
  private final ValuationTimeFn _valuationTimeFn;

  public InterestRateSwapCalculatorFactory(InterestRateSwapSecurityConverter swapConverter,
                                           ValuationTimeFn valuationTimeFn) {
    _swapConverter = ArgumentChecker.notNull(swapConverter, "swapConverter");
    _valuationTimeFn = ArgumentChecker.notNull(valuationTimeFn, "valuationTimeFn");
  }

  public InterestRateSwapCalculator createCalculator(InterestRateSwapSecurity security, MulticurveProviderDiscount bundle) {
    return new InterestRateSwapCalculator(security, bundle, _swapConverter, _valuationTimeFn);
  }

}
