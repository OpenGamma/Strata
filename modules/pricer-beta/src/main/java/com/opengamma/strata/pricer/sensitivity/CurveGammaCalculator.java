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
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.differentiation.FiniteDifferenceType;
import com.opengamma.analytics.math.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.analytics.math.function.Function1D;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.analytics.math.matrix.DoubleMatrix2D;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Computes the cross-gamma and related figures to the rate curves parameters for rates provider.
 * <p>
 * All the curves in the provider should be in the same currency.
 * The curves should be represented by a YieldCurve with an InterpolatedDoublesCurve on the zero-coupon rates.
 * By default the gamma is computed using a one basis-point shift and a forward finite difference.
 * The results themselves are not scaled (they represent the second order derivative).
 * <p> Reference: Interest Rate Cross-gamma for Single and Multiple Curves. OpenGamma quantitative research 15, July 14
 */
public class CurveGammaCalculator {
  
  /** Default size of bump: 1 basis point. */
  private static final double BP1 = 1.0E-4;

  /** The default instance. Finite difference is forward and the shift is 1 basis point). **/
  public static final CurveGammaCalculator DEFAULT = new CurveGammaCalculator(FiniteDifferenceType.FORWARD, BP1);
  
  /** The first order finite difference calculator. **/
  private final VectorFieldFirstOrderDifferentiator fd;

  /**
   * Constructor.
   * @param fdType The finite difference type.
   * @param shift The shift to be applied to the curves.
   */
  public CurveGammaCalculator(FiniteDifferenceType fdType, double shift) {
    this.fd = new VectorFieldFirstOrderDifferentiator(fdType, shift);
  }
  
  /**
   * Computes the "sum-of-column gamma" or "semi-parallel gamma" for a sensitivity function.
   * <p>
   * See the documentation for the definition.
   * The curve provider should contain only one curve which should be of the 
   * type YieldCurve with an underlying InterpolatedDoublesCurve.
   * 
   * @param provider  the rate provider
   * @param sensitivitiesFn  the function from a rate provider to the point sensitivities
   * @return The "sum-of-columns" or "semi-parallel" gamma vector.
   */
  public double[] calculateSemiParallelGamma(
      ImmutableRatesProvider provider,
      Function<ImmutableRatesProvider, PointSensitivities> sensitivitiesFn) {
    ArgChecker.notNull(provider, "rates provider");
    ImmutableMap<Currency, YieldAndDiscountCurve> dsc = provider.getDiscountCurves();
    ImmutableMap<Index, YieldAndDiscountCurve> fwd = provider.getIndexCurves();
    // Check all curves are the same
    YieldAndDiscountCurve single = dsc.entrySet().iterator().next().getValue();
    for(Entry<Currency, YieldAndDiscountCurve> entry: dsc.entrySet()) {
      ArgChecker.isTrue(entry.getValue() == single, "provider should refer to only one curve");
    }
    for(Entry<Index, YieldAndDiscountCurve> entry: fwd.entrySet()) {
      ArgChecker.isTrue(entry.getValue() == single, "provider should refer to only one curve");
    }
    InterpolatedDoublesCurve curve = checkInterpolated(single);
    Delta deltaShift = new Delta(provider, curve, sensitivitiesFn);
    Function1D<DoubleMatrix1D, DoubleMatrix2D> gammaFn = fd.differentiate(deltaShift);
    double[][] gamma2 = gammaFn.evaluate(new DoubleMatrix1D(new double[1])).getData();
    double[] gamma = new double[gamma2.length];
    for (int i = 0; i < gamma2.length; i++) {
      gamma[i] = gamma2[i][0];
    }
    return gamma;
  }

  /**
   * Inner class to compute the delta for a given parallel shift of the curve.
   */
  class Delta extends Function1D<DoubleMatrix1D, DoubleMatrix1D> {
    private final double[] x;
    private final double[] y;
    private final int nbNode;
    private final ImmutableRatesProvider provider;
    private final InterpolatedDoublesCurve curve;
    private final Function<ImmutableRatesProvider, PointSensitivities> sensitivitiesFn;

    public Delta(ImmutableRatesProvider provider,
        InterpolatedDoublesCurve curve,
        Function<ImmutableRatesProvider, PointSensitivities> sensitivitiesFn) {
      this.provider = provider;
      this.curve = curve;
      y = curve.getYDataAsPrimitive();
      x = curve.getXDataAsPrimitive();
      nbNode = x.length;
      this.sensitivitiesFn = sensitivitiesFn;
    }

    @Override
    public DoubleMatrix1D evaluate(DoubleMatrix1D s) {
      double shift = s.getEntry(0);
      final double[] yieldBumped = y.clone();
      for (int loopnode = 0; loopnode < nbNode; loopnode++) {
        yieldBumped[loopnode] += shift;
      }
      final YieldAndDiscountCurve curveBumped = new YieldCurve(curve.getName(),
          new InterpolatedDoublesCurve(x, yieldBumped, curve.getInterpolator(), true));
      ImmutableMap<Currency, YieldAndDiscountCurve> dsc = provider.getDiscountCurves();
      Map<Currency, YieldAndDiscountCurve> dscBumped = new HashMap<>();
      for (Entry<Currency, YieldAndDiscountCurve> entry : dsc.entrySet()) {
        dscBumped.put(entry.getKey(), curveBumped);
      }
      ImmutableMap<Index, YieldAndDiscountCurve> fwd = provider.getIndexCurves();
      Map<Index, YieldAndDiscountCurve> fwdBumped = new HashMap<>(fwd);
      for (Entry<Index, YieldAndDiscountCurve> entry : fwd.entrySet()) {
        fwdBumped.put(entry.getKey(), curveBumped);
      }
      ImmutableRatesProvider providerBumped = provider.toBuilder().discountCurves(dsc).indexCurves(fwdBumped).build();
      PointSensitivities pts = sensitivitiesFn.apply(providerBumped);
      ImmutableMap<SensitivityKey, double[]> psObject = providerBumped.parameterSensitivity(pts).getSensitivities();
      double[] psArray = psObject.entrySet().iterator().next().getValue(); // Only one entry
      return new DoubleMatrix1D(psArray);
    }
  }

  // check that the curve is yield curve and the underlying is an InterpolatedDoublesCurve and returns the latter
  InterpolatedDoublesCurve checkInterpolated(YieldAndDiscountCurve curve) {
    ArgChecker.isTrue(curve instanceof YieldCurve, "Curve should be a YieldCurve");
    YieldCurve curveYield = (YieldCurve) curve;
    ArgChecker.isTrue(curveYield.getCurve() instanceof InterpolatedDoublesCurve,
        "Yield curve should be based on InterpolatedDoublesCurve");
    return (InterpolatedDoublesCurve) curveYield.getCurve();
  }

}
