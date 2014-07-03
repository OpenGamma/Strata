/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.List;

import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.analytics.curve.CurveConstructionConfiguration;
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.sesame.function.Output;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.Tenor;
import com.opengamma.util.tuple.Triple;


/**
 * Function implementation that for a particular curve config, determines which
 * curves need to go into a multicurve bundle and coordinates the building
 * of them. This ensures that a particular curve only needs to get built once.
 */
public interface DiscountingMulticurveBundleResolverFn {

  /**
   * Generates a multicurve bundle for the supplied curve config.
   *
   * @param env the environment
   * @param curveConfig the curve to build the multicurve bundle for
   * @return result containing the multicurve data, if successful
   */
  @Cacheable
  @Output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE)
  Result<MulticurveBundle> generateBundle(Environment env, CurveConstructionConfiguration curveConfig);

  /**
   * Extracts the implied deposit curve data for the supplied curve config.
   *
   * @param env the environment
   * @param curveConfig the curve to extract the implied deposit curve data for
   * @return result containing the implied deposit curve data, if successful
   */
  @Cacheable
  Result<Triple<List<Tenor>,List<Double>,List<InstrumentDerivative>>> extractImpliedDepositCurveData(
      Environment env, CurveConstructionConfiguration curveConfig);
}
