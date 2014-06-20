/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.List;
import java.util.Map;

import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.sesame.cache.CacheLifetime;
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.Tenor;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Triple;

/**
 * Function capable of providing a discounting multi-curve bundle.
 */
public interface DiscountingMulticurveBundleFn {

  /**
   * Generates a multicurve bundle for the supplied curve config.
   *
   * @param env the environment
   * @param curveConfig the curve to build the multicurve bundle for
   * @param builtCurves map of curves that are required to build the
   * bundle (i.e. exogenous curves). If the map does not contain all
   * the curves required, a failure will be returned
   * @return result containing the multicurve data, if successful
   */
  @Cacheable(CacheLifetime.NEXT_FUTURE_ROLL)
  Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> generateBundle(
      Environment env,
      CurveConstructionConfiguration curveConfig,
      Map<CurveConstructionConfiguration, Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>>> builtCurves);

  /**
   * Extracts the implied deposit curve data for the supplied curve config.
   *
   * @param env the environment
   * @param curveConfig the curve to extract the implied deposit curve data for
   * @param builtCurves map of curves that are required to build the
   * supplied curve (i.e. exogenous curves). If the map does not contain all
   * the curves required, a failure will be returned
   * @return result containing the implied deposit curve data, if successful
   */
  // REVIEW Chris 2014-03-14 this is crying out for a real class for the return type
  // TODO ideally the curve config would be a type indicting that it actually is an implied deposit curve
  @Cacheable(CacheLifetime.FOREVER)
  Result<Triple<List<Tenor>, List<Double>, List<InstrumentDerivative>>> extractImpliedDepositCurveData(
      Environment env,
      CurveConstructionConfiguration curveConfig,
      Map<CurveConstructionConfiguration, Result<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>>> builtCurves);

}

