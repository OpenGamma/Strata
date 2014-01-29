/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.List;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.sesame.cache.CacheLifetime;
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.sesame.example.OutputNames;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.Tenor;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Triple;

public interface DiscountingMulticurveBundleFn {

  @Cacheable(CacheLifetime.NEXT_FUTURE_ROLL)
  @Output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE)
  Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> generateBundle(
      CurveConstructionConfiguration curveConfig);

  @Cacheable(CacheLifetime.FOREVER)
  Result<Triple<List<Tenor>, List<Double>, List<InstrumentDerivative>>> extractImpliedDepositCurveData(
      CurveConstructionConfiguration curveConfig, ZonedDateTime valuationTime);
}
