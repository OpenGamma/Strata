/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.ParameterIssuerProviderInterface;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.sesame.cache.CacheLifetime;
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

public interface IssuerProviderBundleFn {

  @Cacheable(CacheLifetime.NEXT_FUTURE_ROLL)
  @Output(OutputNames.ISSUER_PROVIDER_BUNDLE)
  Result<Pair<ParameterIssuerProviderInterface, CurveBuildingBlockBundle>> generateBundle(
      Environment env, CurveConstructionConfiguration curveConfig);

}
