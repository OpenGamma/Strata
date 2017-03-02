/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.local;

import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.INTERPOLATOR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.NATURAL_SPLINE;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.NATURAL_SPLINE_NONNEGATIVITY_CUBIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.TIME_SQUARE;
import static org.testng.Assert.assertEquals;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.DeformedSurface;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;

/**
 * Test {@link ImpliedTrinomialTreeLocalVolatilityCalculator}.
 */
@Test
public class ImpliedTrinomialTreeLocalVolatilityCalculatorTest {

  private static final GridSurfaceInterpolator INTERP_LINEAR = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final GridSurfaceInterpolator INTERP_TIMESQ_LINEAR = GridSurfaceInterpolator.of(TIME_SQUARE, LINEAR);
  private static final GridSurfaceInterpolator INTERP_CUBIC = GridSurfaceInterpolator.of(
      NATURAL_SPLINE, INTERPOLATOR, NATURAL_SPLINE, INTERPOLATOR);
  private static final GridSurfaceInterpolator INTERP_CUBIC_NN = GridSurfaceInterpolator.of(
      NATURAL_SPLINE_NONNEGATIVITY_CUBIC, INTERPOLATOR, NATURAL_SPLINE_NONNEGATIVITY_CUBIC, INTERPOLATOR);

  private static final DoubleArray TIMES =
      DoubleArray.of(0.25, 0.25, 0.25, 0.5, 0.5, 0.5, 0.75, 0.75, 0.75, 1, 1, 1);
  private static final DoubleArray STRIKES =
      DoubleArray.of(0.8, 1.4, 2, 0.8, 1.4, 2, 0.8, 1.4, 2, 0.8, 1.4, 2);
  private static final DoubleArray VOLS =
      DoubleArray.of(0.21, 0.17, 0.185, 0.17, 0.15, 0.16, 0.15, 0.14, 0.14, 0.14, 0.13, 0.13);
  private static final InterpolatedNodalSurface VOL_SURFACE =
      InterpolatedNodalSurface.ofUnsorted(DefaultSurfaceMetadata.of("Test"), TIMES, STRIKES, VOLS, INTERP_CUBIC);
  private static final DoubleArray PRICES = DoubleArray.of(
      0.6024819282312833, 0.0507874597232295, 2.598419834431295E-6, 0.6049279456317715, 0.06581419934686354,
      5.691088908182669E-5, 0.607338423139487, 0.07752243330525914, 1.4290312009415014E-4, 0.6097138918063894,
      0.0856850744439275, 3.218460178780302E-4);
  private static final InterpolatedNodalSurface PRICE_SURFACE =
      InterpolatedNodalSurface.ofUnsorted(DefaultSurfaceMetadata.of("Test"), TIMES, STRIKES, PRICES, INTERP_CUBIC_NN);
  private static final double SPOT = 1.40;

  public void flatVolTest() {
    double tol = 2.0e-2;
    double constantVol = 0.15;
    ConstantSurface impliedVolSurface = ConstantSurface.of("impliedVol", constantVol);
    Function<Double, Double> zeroRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return 0.05d;
      }
    };
    Function<Double, Double> zeroRate1 = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return 0.02d;
      }
    };
    ImpliedTrinomialTreeLocalVolatilityCalculator calc =
        new ImpliedTrinomialTreeLocalVolatilityCalculator(45, 1d, INTERP_TIMESQ_LINEAR);
    InterpolatedNodalSurface localVolSurface =
        calc.localVolatilityFromImpliedVolatility(impliedVolSurface, 100d, zeroRate, zeroRate1);
    assertEquals(localVolSurface.getZValues().stream().filter(d -> !DoubleMath.fuzzyEquals(d, constantVol, tol)).count(), 0);
  }

  public void flatVolPriceTest() {
    double tol = 2.0e-2;
    double constantVol = 0.15;
    double spot = 100d;
    double maxTime = 1d;
    int nSteps = 9;
    ConstantSurface impliedVolSurface = ConstantSurface.of("impliedVol", constantVol);
    Function<Double, Double> zeroRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return 0d;
      }
    };
    Function<DoublesPair, ValueDerivatives> func = new Function<DoublesPair, ValueDerivatives>() {
      @Override
      public ValueDerivatives apply(DoublesPair x) {
        double price = BlackFormulaRepository.price(spot, x.getSecond(), x.getFirst(), constantVol, true);
        return ValueDerivatives.of(price, DoubleArray.EMPTY);
      }
    };
    DeformedSurface priceSurface = DeformedSurface.of(DefaultSurfaceMetadata.of("price"), impliedVolSurface, func);
    ImpliedTrinomialTreeLocalVolatilityCalculator calc =
        new ImpliedTrinomialTreeLocalVolatilityCalculator(nSteps, maxTime, INTERP_TIMESQ_LINEAR);
    InterpolatedNodalSurface localVolSurface = calc.localVolatilityFromPrice(priceSurface, spot, zeroRate, zeroRate);
    assertEquals(localVolSurface.getZValues().stream().filter(d -> !DoubleMath.fuzzyEquals(d, constantVol, tol)).count(), 0);
  }

  public void comparisonDupireVolTest() {
    double tol = 1.0e-2;
    ImpliedTrinomialTreeLocalVolatilityCalculator calc =
        new ImpliedTrinomialTreeLocalVolatilityCalculator(28, 1.45d, INTERP_LINEAR);
    Function<Double, Double> interestRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return 0.03d;
      }
    };
    Function<Double, Double> dividendRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return 0.01d;
      }
    };
    InterpolatedNodalSurface resTri = calc.localVolatilityFromImpliedVolatility(VOL_SURFACE, SPOT, interestRate, dividendRate);
    DeformedSurface resDup = (new DupireLocalVolatilityCalculator())
        .localVolatilityFromImpliedVolatility(VOL_SURFACE, SPOT, interestRate, dividendRate);
    double[][] sampleStrikes = new double[][] {
      {0.7 * SPOT, SPOT, 1.1 * SPOT, 1.4 * SPOT, }, {0.5 * SPOT, 0.9 * SPOT, SPOT, 1.3 * SPOT, 1.9 * SPOT } };
    double[] sampleTimes = new double[] {0.8, 1.1 };
    for (int i = 0; i < sampleTimes.length; ++i) {
      double time = sampleTimes[i];
      for (double strike : sampleStrikes[i]) {
        double volTri = resTri.zValue(time, strike);
        double volDup = resDup.zValue(time, strike);
        assertEquals(volTri, volDup, tol);
      }
    }
  }

  public void comparisonDupirePriceTest() {
    double tol = 7.0e-2;
    ImpliedTrinomialTreeLocalVolatilityCalculator calc =
        new ImpliedTrinomialTreeLocalVolatilityCalculator(22, 1.1d, INTERP_LINEAR);
    Function<Double, Double> interestRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return 0.003d;
      }
    };
    Function<Double, Double> dividendRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return 0.01d;
      }
    };
    InterpolatedNodalSurface resTri = calc.localVolatilityFromPrice(PRICE_SURFACE, SPOT, interestRate, dividendRate);
    DeformedSurface resDup = (new DupireLocalVolatilityCalculator())
        .localVolatilityFromPrice(PRICE_SURFACE, SPOT, interestRate, dividendRate);
    // limited range due to interpolation/extrapolation of price surface -> negative call/put price reached
    double[][] sampleStrikes = new double[][] { {0.95 * SPOT, 1.05 * SPOT, }, {0.9 * SPOT, SPOT, 1.1 * SPOT } };
    double[] sampleTimes = new double[] {0.7, 1.05 };
    for (int i = 0; i < sampleTimes.length; ++i) {
      double time = sampleTimes[i];
      for (double strike : sampleStrikes[i]) {
        double volTri = resTri.zValue(time, strike);
        double volDup = resDup.zValue(time, strike);
        assertEquals(volTri, volDup, tol);
      }
    }
  }

}
