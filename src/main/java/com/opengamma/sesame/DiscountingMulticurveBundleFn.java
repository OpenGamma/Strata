/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.sesame.cache.Cache;
import com.opengamma.sesame.example.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.result.FunctionResult;
import com.opengamma.util.tuple.Pair;

public interface DiscountingMulticurveBundleFn {

  @Cache
  @Output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE)
  FunctionResult<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> generateBundle(String curveConstructionConfigurationName);
}
