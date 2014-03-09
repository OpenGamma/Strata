/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.irs;

import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.conversion.InterestRateSwapSecurityConverter;
import com.opengamma.financial.security.irs.InterestRateSwapSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.ArgumentChecker;

public class InterestRateSwapCalculatorFactory {

  /**
   * Converter for a Swap
   */
  private final InterestRateSwapSecurityConverter _swapConverter;

  public InterestRateSwapCalculatorFactory(InterestRateSwapSecurityConverter swapConverter) {
    _swapConverter = ArgumentChecker.notNull(swapConverter, "swapConverter");
  }

  public InterestRateSwapCalculator createCalculator(Environment env,
                                                     InterestRateSwapSecurity security,
                                                     MulticurveProviderDiscount bundle) {
    return new InterestRateSwapCalculator(security, bundle, _swapConverter, env.getValuationTime());
  }

}
