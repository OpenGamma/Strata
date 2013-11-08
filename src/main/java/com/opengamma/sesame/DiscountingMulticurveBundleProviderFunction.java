/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.sesame.cache.Cache;
import com.opengamma.sesame.example.OutputNames;
import com.opengamma.sesame.function.Output;

public interface DiscountingMulticurveBundleProviderFunction {

  @Cache
  @Output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE)
  FunctionResult<MulticurveProviderDiscount> generateBundle(String curveConstructionConfigurationName);
}
