/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.local;

import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.INTERPOLATOR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.NATURAL_SPLINE;
import static org.testng.Assert.assertEquals;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.DeformedSurface;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;

/**
 * Test {@link DupireLocalVolatilityCalculator}.
 */
@Test
public class DupireLocalVolatilityCalculatorTest {

  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(
      NATURAL_SPLINE, INTERPOLATOR, NATURAL_SPLINE, INTERPOLATOR);
  private static final DoubleArray TIMES =
      DoubleArray.of(0.25, 0.25, 0.25, 0.5, 0.5, 0.5, 0.75, 0.75, 0.75, 1, 1, 1);
  private static final DoubleArray STRIKES =
      DoubleArray.of(0.8, 1.4, 2, 0.8, 1.4, 2, 0.8, 1.4, 2, 0.8, 1.4, 2);
  private static final DoubleArray VOLS =
      DoubleArray.of(0.21, 0.17, 0.185, 0.17, 0.15, 0.16, 0.15, 0.14, 0.14, 0.14, 0.13, 0.13);
  private static final InterpolatedNodalSurface VOL_SURFACE =
      InterpolatedNodalSurface.of(DefaultSurfaceMetadata.of("Test"), TIMES, STRIKES, VOLS, INTERPOLATOR_2D);
  private static final DoubleArray PRICES = DoubleArray.of(
      0.59600, 0.04868, 2.3012E-6,
      0.59201, 0.06138, 4.7919E-5,
      0.58812, 0.07063, 1.1365E-4,
      0.58413, 0.07626, 2.4524E-4);
  private static final InterpolatedNodalSurface PRICE_SURFACE =
      InterpolatedNodalSurface.of(DefaultSurfaceMetadata.of("Test"), TIMES, STRIKES, PRICES, INTERPOLATOR_2D);
  private static final double SPOT = 1.40;
  private static final double[] TEST_STRIKES = new double[] {1.1, 1.4, 2.2 };
  private static final double[] TEST_TIMES = new double[] {0.1, 0.6, 1.1 };
  private static final double FD_EPS = 1.0e-5;

  private static final DupireLocalVolatilityCalculator CALC = new DupireLocalVolatilityCalculator();

  public void flatVolTest() {
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
    double[] strikes = new double[] {90d, 100d, 115d };
    for (double strike : strikes) {
      for (double time : TEST_TIMES) {
        DeformedSurface localVolSurface =
            CALC.localVolatilityFromImpliedVolatility(impliedVolSurface, 100d, zeroRate, zeroRate1);
        assertEquals(localVolSurface.zValue(time, strike), constantVol);
      }
    }
  }

  public void test_localVolatilityFromImpliedVolatility() {
    double r = 0.05;
    double q = 0.01;
    Function<Double, Double> interestRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return r;
      }
    };
    Function<Double, Double> dividendRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return q;
      }
    };
    for (double strike : TEST_STRIKES) {
      for (double time : TEST_TIMES) {
        double computedVol = CALC
            .localVolatilityFromImpliedVolatility(VOL_SURFACE, SPOT, interestRate, dividendRate)
            .zValue(time, strike);
        double expectedVol = volFromFormula(r, q, time, strike, VOL_SURFACE);
        assertEquals(computedVol, expectedVol, FD_EPS);
        UnitParameterSensitivity computedSensi =
            CALC.localVolatilityFromImpliedVolatility(VOL_SURFACE, SPOT, interestRate, dividendRate)
            .zValueParameterSensitivity(time, strike);
        for (int i = 0; i < VOLS.size(); ++i) {
          InterpolatedNodalSurface surfaceUp = VOL_SURFACE.withZValues(VOLS.with(i, VOLS.get(i) + FD_EPS));
          InterpolatedNodalSurface surfaceDw = VOL_SURFACE.withZValues(VOLS.with(i, VOLS.get(i) - FD_EPS));
          double volUp = CALC.localVolatilityFromImpliedVolatility(
              surfaceUp, SPOT, interestRate, dividendRate).zValue(time, strike);
          double volDw = CALC.localVolatilityFromImpliedVolatility(
              surfaceDw, SPOT, interestRate, dividendRate).zValue(time, strike);
          double expectedSensi = 0.5 * (volUp - volDw) / FD_EPS;
          assertEquals(computedSensi.getSensitivity().get(i), expectedSensi, FD_EPS * 10d);
        }
      }
    }
  }

  public void test_localVolatilityFromImpliedVolatility_smallStrike() {
    double r = 0.05;
    double q = 0.01;
    Function<Double, Double> interestRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return r;
      }
    };
    Function<Double, Double> dividendRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return q;
      }
    };
    double strike = 1.0e-11;
    for (double time : TEST_TIMES) {
      double computedVol = CALC
          .localVolatilityFromImpliedVolatility(VOL_SURFACE, SPOT, interestRate, dividendRate)
          .zValue(time, strike);
      double expectedVol = volFromFormula(r, q, time, strike, VOL_SURFACE);
      assertEquals(computedVol, expectedVol, FD_EPS);
      UnitParameterSensitivity computedSensi =
          CALC.localVolatilityFromImpliedVolatility(VOL_SURFACE, SPOT, interestRate, dividendRate)
              .zValueParameterSensitivity(time, strike);
        for (int i = 0; i < VOLS.size(); ++i) {
        InterpolatedNodalSurface surfaceUp = VOL_SURFACE.withZValues(VOLS.with(i, VOLS.get(i) + FD_EPS));
        InterpolatedNodalSurface surfaceDw = VOL_SURFACE.withZValues(VOLS.with(i, VOLS.get(i) - FD_EPS));
        double volUp = CALC.localVolatilityFromImpliedVolatility(
            surfaceUp, SPOT, interestRate, dividendRate).zValue(time, strike);
        double volDw = CALC.localVolatilityFromImpliedVolatility(
            surfaceDw, SPOT, interestRate, dividendRate).zValue(time, strike);
        double expectedSensi = 0.5 * (volUp - volDw) / FD_EPS;
        assertEquals(computedSensi.getSensitivity().get(i), expectedSensi, FD_EPS * 10d);
      }
    }
  }

  public void test_localVolatilityFromPrice() {
    double r = 0.03;
    double q = 0.02;
    Function<Double, Double> interestRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return r;
      }
    };
    Function<Double, Double> dividendRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return q;
      }
    };
    for (double strike : TEST_STRIKES) {
      for (double time : TEST_TIMES) {
        double computedVol = CALC
            .localVolatilityFromPrice(PRICE_SURFACE, SPOT, interestRate, dividendRate)
            .zValue(time, strike);
        double expectedVol = volFromFormulaPrice(r, q, time, strike, PRICE_SURFACE);
        assertEquals(computedVol, expectedVol, FD_EPS);
        UnitParameterSensitivity computedSensi =
            CALC.localVolatilityFromPrice(PRICE_SURFACE, SPOT, interestRate, dividendRate)
                .zValueParameterSensitivity(time, strike);
        for (int i = 0; i < PRICES.size(); ++i) {
          InterpolatedNodalSurface surfaceUp = PRICE_SURFACE.withZValues(PRICES.with(i, PRICES.get(i) + FD_EPS));
          InterpolatedNodalSurface surfaceDw = PRICE_SURFACE.withZValues(PRICES.with(i, PRICES.get(i) - FD_EPS));
          double priceUp = CALC.localVolatilityFromPrice(
              surfaceUp, SPOT, interestRate, dividendRate).zValue(time, strike);
          double priceDw = CALC.localVolatilityFromPrice(
              surfaceDw, SPOT, interestRate, dividendRate).zValue(time, strike);
          double expectedSensi = 0.5 * (priceUp - priceDw) / FD_EPS;
          assertEquals(computedSensi.getSensitivity().get(i), expectedSensi, FD_EPS * 100d); // tiny call price
        }
      }
    }
  }

  private double volFromFormula(double r, double q, double time, double strike, NodalSurface surface) {
    double vol = surface.zValue(time, strike);
    double volT = 0.5 / FD_EPS * (surface.zValue(time + FD_EPS, strike) - surface.zValue(time - FD_EPS, strike));
    double volK = 0.5 / FD_EPS * (surface.zValue(time, strike + FD_EPS) - surface.zValue(time, strike - FD_EPS));
    double volKK =
        (surface.zValue(time, strike + FD_EPS) + surface.zValue(time, strike - FD_EPS) - 2d * vol) / FD_EPS / FD_EPS;
    double rootT = Math.sqrt(time);
    double d1 = (Math.log(SPOT / strike) + (r - q + 0.5 * vol * vol) * time) / vol / rootT;
    double d2 = (Math.log(SPOT / strike) + (r - q - 0.5 * vol * vol) * time) / vol / rootT;
    double den = 1d + 2d * d1 * strike * rootT * volK + strike * strike * time * (d1 * d2 * volK * volK + vol * volKK);
    double var = (vol * vol + 2d * vol * time * (volT + (r - q) * strike * volK)) / den;
    return Math.sqrt(var);
  }

  private double volFromFormulaPrice(double r, double q, double time, double strike, NodalSurface surface) {
    double p = surface.zValue(time, strike);
    double pT = 0.5 / FD_EPS * (surface.zValue(time + FD_EPS, strike) - surface.zValue(time - FD_EPS, strike));
    double pK = 0.5 / FD_EPS * (surface.zValue(time, strike + FD_EPS) - surface.zValue(time, strike - FD_EPS));
    double pKK =
        (surface.zValue(time, strike + FD_EPS) + surface.zValue(time, strike - FD_EPS) - 2d * p) / FD_EPS / FD_EPS;
    double var = 2d * (pT + (r - q) * strike * pK + q * p) / (strike * strike * pKK);
    return Math.sqrt(var);
  }

}
