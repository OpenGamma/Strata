/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static com.opengamma.strata.pricer.impl.credit.isda.YieldCurveProvider.makeUSDBuilder;
import static org.testng.AssertJUnit.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

/**
 * 
 */
@Test
public class InterestRateSensitviityTest extends IsdaBaseTest {

  private static final LocalDate TRADE_DATE = LocalDate.of(2014, 3, 3);
  private static IsdaCompliantYieldCurveBuild YC_BUILDER = makeUSDBuilder(TRADE_DATE);
  private static double[] RATES = new double[] {0.001555, 0.001975, 0.002357, 0.003305, 0.005533, 0.004465, 0.00817, 0.012215, 0.016075, 0.019335, 0.02203, 0.02423, 0.02608, 0.02766, 0.030085,
    0.03243, 0.03441, 0.0353, 0.03579 };
  private static IsdaCompliantYieldCurve YIELD_CURVE = YC_BUILDER.build(RATES);
  private static double COUPON = 0.01;
  private static CdsAnalyticFactory FACTORY = new CdsAnalyticFactory();
  private static InterestRateSensitivityCalculator IRDV01_CAL = new InterestRateSensitivityCalculator();

  public void zeroPUFTest() {
    final CdsAnalytic cds = FACTORY.makeImmCds(TRADE_DATE, Period.ofYears(5));
    final double puf = 0.0;
    final double qs = COUPON;

    final double irdv01_puf = IRDV01_CAL.parallelIR01(cds, new PointsUpFront(COUPON, puf), YC_BUILDER, RATES);
    final double irdv01_spread = IRDV01_CAL.parallelIR01(cds, new CdsQuotedSpread(COUPON, qs), YC_BUILDER, RATES);

    assertEquals(0, irdv01_puf, 1e-16);
    assertEquals(0, irdv01_spread, 1e-16);
  }

  public void spreadTest() {

    final double notional = 1e15;
    final CdsAnalytic cds = FACTORY.makeImmCds(TRADE_DATE, Period.ofYears(5));

    final double[] spreads = new double[] {70, 90, 110, 130 };
    final double[] expIR01 = new double[] {3751044644., 1236090509., -1222016033., -3624366827. };
    final int n = spreads.length;
    for (int i = 0; i < n; i++) {
      final double irdv01_spread = notional * IRDV01_CAL.parallelIR01(cds, new CdsQuotedSpread(COUPON, spreads[i] * ONE_BP), YC_BUILDER, RATES);
      assertEquals(expIR01[i], irdv01_spread, 1); //one part in 1e15
    }
  }

  public void pufTest() {

    final double notional = 1e15;
    final CdsAnalytic cds = FACTORY.makeImmCds(TRADE_DATE, Period.ofYears(5));

    final double[] puf = new double[] {-3, -1, 1, 3, 5 };
    final double[] expIR01 = new double[] {7795781794., 2581999744., -2564865638., -7641468784., -12644163245. };
    final int n = puf.length;
    for (int i = 0; i < n; i++) {
      final double irdv01_spread = notional * IRDV01_CAL.parallelIR01(cds, new PointsUpFront(COUPON, puf[i] * ONE_PC), YC_BUILDER, RATES);

      assertEquals(expIR01[i], irdv01_spread, 1); //one part in 1e15
    }
  }

  /**
   * This uses a calculator that bumps the yield curve directly, so we only approximate the 'true' IR DV01 numbers 
   */
  public void spread2Test() {

    final double notional = 1e15;
    final CdsAnalytic cds = FACTORY.makeImmCds(TRADE_DATE, Period.ofYears(5));

    final double[] spreads = new double[] {70, 90, 110, 130 };
    final double[] expIR01 = new double[] {3751044644., 1236090509., -1222016033., -3624366827. };
    final int n = spreads.length;
    for (int i = 0; i < n; i++) {
      final double irdv01_spread = notional * IRDV01_CAL.parallelIR01(cds, new CdsQuotedSpread(COUPON, spreads[i] * ONE_BP), YIELD_CURVE, RATES);
      assertEquals(expIR01[i], irdv01_spread, 1e-2 * Math.abs(expIR01[i]));
    }
  }
}
