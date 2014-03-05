/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.fra;

import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.conversion.FRASecurityConverter;
import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.util.ArgumentChecker;

public class FRACalculatorFactory {

  /**
   * Converter for a FRA.
   */
  private final FRASecurityConverter _fraConverter;

  public FRACalculatorFactory(FRASecurityConverter fraConverter) {
    _fraConverter = ArgumentChecker.notNull(fraConverter, "fraConverter");
  }

  public FRACalculator createCalculator(Environment env, FRASecurity security, MulticurveProviderDiscount bundle) {
    return new FRACalculator(security, bundle, _fraConverter, env.getValuationTime());
  }
}
