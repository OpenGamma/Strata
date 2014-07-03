/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.List;
import java.util.Map;

import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.sesame.cache.CacheLifetime;
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.Tenor;
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
   * @param requiredCurves map of curves that are required to build the
   * bundle (i.e. exogenous curves). If the map does not contain all
   * the curves required, a failure will be returned
   * @return result containing the multicurve data, if successful
   */
  @Cacheable(CacheLifetime.NEXT_FUTURE_ROLL)
  Result<MulticurveBundle> generateBundle(
      Environment env,
      CurveConstructionConfiguration curveConfig,
      Map<CurveConstructionConfiguration, Result<MulticurveBundle>> requiredCurves);

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
  @Cacheable(CacheLifetime.FOREVER)
  Result<ImpliedDepositCurveData> extractImpliedDepositCurveData(
      Environment env,
      CurveConstructionConfiguration curveConfig,
      Map<CurveConstructionConfiguration, Result<MulticurveBundle>> builtCurves);

}

