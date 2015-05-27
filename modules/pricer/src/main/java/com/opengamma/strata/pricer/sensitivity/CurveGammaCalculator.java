/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.math.differentiation.FiniteDifferenceType;
import com.opengamma.analytics.math.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.analytics.math.function.Function1D;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.analytics.math.matrix.DoubleMatrix2D;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.SensitivityKey;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Computes the cross-gamma and related figures to the rate curves parameters for rates provider.
 * <p>
 * All the curves in the provider should be in the same currency.
 * The curves should be represented by a {@link NodalCurve} on the zero-coupon rates.
 * By default the gamma is computed using a one basis-point shift and a forward finite difference.
 * The results themselves are not scaled (they represent the second order derivative).
 * <p>
 * Reference: Interest Rate Cross-gamma for Single and Multiple Curves. OpenGamma quantitative research 15, July 14
 */
public class CurveGammaCalculator {

  /**
   * Default implementation. Finite difference is forward and the shift is one basis point (0.0001).
   */
  public static final CurveGammaCalculator DEFAULT = new CurveGammaCalculator(FiniteDifferenceType.FORWARD, 1.0E-4);

  /**
   * The first order finite difference calculator.
   */
  private final VectorFieldFirstOrderDifferentiator fd;

  /**
   * Create an instance of the finite difference calculator.
   * 
   * @param fdType  the finite difference type
   * @param shift  the shift to be applied to the curves
   */
  public CurveGammaCalculator(FiniteDifferenceType fdType, double shift) {
    this.fd = new VectorFieldFirstOrderDifferentiator(fdType, shift);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the "sum-of-column gamma" or "semi-parallel gamma" for a sensitivity function.
   * <p>
   * See the class-level documentation for the definition.
   * The curve provider must contain only one curve which should be of type {@code NodalCurve}.
   * 
   * @param provider  the rate provider
   * @param sensitivitiesFn  the function from a rate provider to the point sensitivities
   * @return the "sum-of-columns" or "semi-parallel" gamma vector
   */
  public double[] calculateSemiParallelGamma(
      ImmutableRatesProvider provider,
      Function<ImmutableRatesProvider, PointSensitivities> sensitivitiesFn) {

    NodalCurve curve = findCurve(provider);
    Delta deltaShift = new Delta(provider, curve, sensitivitiesFn);
    Function1D<DoubleMatrix1D, DoubleMatrix2D> gammaFn = fd.differentiate(deltaShift);
    double[][] gamma2 = gammaFn.evaluate(new DoubleMatrix1D(new double[1])).getData();
    double[] gamma = new double[gamma2.length];
    for (int i = 0; i < gamma2.length; i++) {
      gamma[i] = gamma2[i][0];
    }
    return gamma;
  }

  // validate that there is only one curve and it is of correct type
  static NodalCurve findCurve(ImmutableRatesProvider provider) {
    ImmutableMap<Currency, Curve> dsc = provider.getDiscountCurves();
    ImmutableMap<Index, Curve> fwd = provider.getIndexCurves();
    Curve single = dsc.entrySet().iterator().next().getValue();
    for (Entry<Currency, Curve> entry : dsc.entrySet()) {
      ArgChecker.isTrue(entry.getValue() == single, "Provider should refer to only one curve");
    }
    for (Entry<Index, Curve> entry : fwd.entrySet()) {
      ArgChecker.isTrue(entry.getValue() == single, "Provider should refer to only one curve");
    }
    return checkInterpolated(single);
  }

  // check that the curve is a NodalCurve
  static NodalCurve checkInterpolated(Curve curve) {
    ArgChecker.isTrue(curve instanceof NodalCurve, "Curve must be a NodalCurve");
    return (InterpolatedNodalCurve) curve;
  }

  //-------------------------------------------------------------------------
  /**
   * Inner class to compute the delta for a given parallel shift of the curve.
   */
  static class Delta extends Function1D<DoubleMatrix1D, DoubleMatrix1D> {

    private final ImmutableRatesProvider provider;
    private final NodalCurve curve;
    private final Function<ImmutableRatesProvider, PointSensitivities> sensitivitiesFn;

    Delta(ImmutableRatesProvider provider,
        NodalCurve curve,
        Function<ImmutableRatesProvider, PointSensitivities> sensitivitiesFn) {
      this.provider = provider;
      this.curve = curve;
      this.sensitivitiesFn = sensitivitiesFn;
    }

    @Override
    public DoubleMatrix1D evaluate(DoubleMatrix1D s) {
      double shift = s.getEntry(0);
      double[] yieldBumped = curve.getYValues();
      for (int loopnode = 0; loopnode < curve.getParameterCount(); loopnode++) {
        yieldBumped[loopnode] += shift;
      }
      Curve curveBumped = curve.withYValues(yieldBumped);
      ImmutableMap<Currency, Curve> dsc = provider.getDiscountCurves();
      Map<Currency, Curve> dscBumped = new HashMap<>();
      for (Entry<Currency, Curve> entry : dsc.entrySet()) {
        dscBumped.put(entry.getKey(), curveBumped);
      }
      ImmutableMap<Index, Curve> fwd = provider.getIndexCurves();
      Map<Index, Curve> fwdBumped = new HashMap<>(fwd);
      for (Entry<Index, Curve> entry : fwd.entrySet()) {
        fwdBumped.put(entry.getKey(), curveBumped);
      }
      ImmutableRatesProvider providerBumped = provider.toBuilder()
          .discountCurves(dscBumped)
          .indexCurves(fwdBumped)
          .build();
      PointSensitivities pts = sensitivitiesFn.apply(providerBumped);
      ImmutableMap<SensitivityKey, double[]> psObject = providerBumped.parameterSensitivity(pts).getSensitivities();
      double[] psArray = psObject.entrySet().iterator().next().getValue(); // only one entry
      return new DoubleMatrix1D(psArray);
    }
  }

}
