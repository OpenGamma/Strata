/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;

public interface DiscountingMulticurveBundleProviderFunction {

  FunctionResult<MulticurveProviderDiscount> generateBundle(String curveConstructionConfigurationName);
}
