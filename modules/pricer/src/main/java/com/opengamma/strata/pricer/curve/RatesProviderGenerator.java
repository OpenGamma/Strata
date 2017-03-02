/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.JacobianCalibrationMatrix;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Generates a {@link RatesProvider} from a set of parameters.
 * <p>
 * This creates a new provider based on the specified parameters.
 */
public interface RatesProviderGenerator {

  /**
   * Generates a rates provider from a set of parameters.
   * <p>
   * The number of parameters passed has to match the total number of parameters in all the curves generated.
   * 
   * @param parameters  the parameters describing the provider
   * @return the provider
   */
  public default ImmutableRatesProvider generate(DoubleArray parameters) {
    return generate(parameters, ImmutableMap.of());
  }

  /**
   * Generates a rates provider from a set of parameters and calibration information.
   * <p>
   * The number of parameters passed has to match the total number of parameters in all the curves generated.
   * 
   * @param parameters  the parameters describing the provider
   * @param jacobians  the curve calibration info
   * @return the provider
   */
  public default ImmutableRatesProvider generate(
      DoubleArray parameters,
      Map<CurveName, JacobianCalibrationMatrix> jacobians) {
    return generate(parameters, jacobians, ImmutableMap.of());
  }

  /**
   * Generates a rates provider from a set of parameters and calibration information.
   * <p>
   * The number of parameters passed has to match the total number of parameters in all the curves generated.
   * 
   * @param parameters  the parameters describing the provider
   * @param jacobians  the curve calibration info
   * @param sensitivitiesMarketQuote  the PV sensitivities
   * @return the provider
   */
  public abstract ImmutableRatesProvider generate(
      DoubleArray parameters,
      Map<CurveName, JacobianCalibrationMatrix> jacobians,
      Map<CurveName, DoubleArray> sensitivitiesMarketQuote);

}
