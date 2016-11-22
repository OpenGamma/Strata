/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import java.util.BitSet;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.minimization.DoubleRangeLimitTransform;
import com.opengamma.strata.math.impl.minimization.NonLinearParameterTransforms;
import com.opengamma.strata.math.impl.minimization.ParameterLimitsTransform;
import com.opengamma.strata.math.impl.minimization.ParameterLimitsTransform.LimitType;
import com.opengamma.strata.math.impl.minimization.SingleRangeLimitTransform;
import com.opengamma.strata.math.impl.minimization.UncoupledParameterTransforms;
import com.opengamma.strata.pricer.model.SabrVolatilityFormula;

/**
 * SABR model fitter.
 * <p>
 * Attempts to calibrate SABR model to the implied volatilities of European vanilla options, by minimizing the sum of 
 * squares between the market and model implied volatilities.
 * <p>
 * All the options must be for the same expiry and (implicitly) on the same underlying.
 */
public final class SabrModelFitter extends SmileModelFitter<SabrFormulaData> {

  private static final double RHO_LIMIT = 0.999;
  // Allowing for rho to be equal to 1 or -1 does not make sense from a financial point of view and creates numerical instability
  private static final ParameterLimitsTransform[] DEFAULT_TRANSFORMS;
  static {
    DEFAULT_TRANSFORMS = new ParameterLimitsTransform[4];
    DEFAULT_TRANSFORMS[0] = new SingleRangeLimitTransform(0, LimitType.GREATER_THAN); // alpha > 0
    DEFAULT_TRANSFORMS[1] = new DoubleRangeLimitTransform(0, 1.0); // 0 <= beta <= 1
    DEFAULT_TRANSFORMS[2] = new DoubleRangeLimitTransform(-RHO_LIMIT, RHO_LIMIT); // -RHO_LIMIT <= rho <= RHO_LIMIT
    DEFAULT_TRANSFORMS[3] = new DoubleRangeLimitTransform(0.01d, 2.50d);
    // nu > 0  and limit on Nu to avoid numerical instability in formula for large nu.
  }

  //-------------------------------------------------------------------------
  /**
   * Constructs SABR model fitter from forward, strikes, time to expiry, implied volatilities and error values.
   * <p>
   * {@code strikes}, {@code impliedVols} and {@code error} should be the same length and ordered coherently.
   * 
   * @param forward  the forward value of the underlying
   * @param strikes  the ordered values of strikes
   * @param timeToExpiry  the time-to-expiry
   * @param impliedVols  the market implied volatilities
   * @param error  the 'measurement' error to apply to the market volatility of a particular option
   * @param sabrVolatilityFormula  the volatility formula
   */
  @SuppressWarnings("unchecked")
  public SabrModelFitter(
      double forward,
      DoubleArray strikes,
      double timeToExpiry,
      DoubleArray impliedVols,
      DoubleArray error,
      SabrVolatilityFormula sabrVolatilityFormula) {

    super(
        forward,
        strikes,
        timeToExpiry,
        impliedVols,
        error,
        (VolatilityFunctionProvider<SabrFormulaData>) sabrVolatilityFormula);
  }

  /**
   * Constructs SABR model fitter from forward, strikes, time to expiry, implied volatilities and error values.
   * <p>
   * {@code strikes}, {@code impliedVols} and {@code error} should be the same length and ordered coherently.
   * 
   * @param forward  the forward value of the underlying
   * @param strikes  the ordered values of strikes
   * @param timeToExpiry  the time-to-expiry
   * @param impliedVols  the market implied volatilities
   * @param error  the 'measurement' error to apply to the market volatility of a particular option
   * @param model  the volatility function provider
   */
  public SabrModelFitter(
      double forward,
      DoubleArray strikes,
      double timeToExpiry,
      DoubleArray impliedVols,
      DoubleArray error,
      VolatilityFunctionProvider<SabrFormulaData> model) {

    super(forward, strikes, timeToExpiry, impliedVols, error, model);
  }

  //-------------------------------------------------------------------------
  @Override
  public SabrFormulaData toSmileModelData(DoubleArray modelParameters) {
    return SabrFormulaData.of(modelParameters.toArray());
  }

  @Override
  protected NonLinearParameterTransforms getTransform(DoubleArray start) {
    final BitSet fixed = new BitSet();
    return new UncoupledParameterTransforms(start, DEFAULT_TRANSFORMS, fixed);
  }

  @Override
  protected NonLinearParameterTransforms getTransform(DoubleArray start, BitSet fixed) {
    return new UncoupledParameterTransforms(start, DEFAULT_TRANSFORMS, fixed);
  }

  @Override
  protected DoubleArray getMaximumStep() {
    return null;
  }

}
