/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getPrevIMMDate;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Arrays;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.MersenneTwister64;
import cern.jet.random.engine.RandomEngine;
import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;

/**
 * 
 */
@Test
public class HedgeRatioCalculatorTest extends IsdaBaseTest {
  protected static final RandomEngine RANDOM = new MersenneTwister64(MersenneTwister.DEFAULT_SEED);
  protected static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1, RANDOM);

  private static final CdsAnalyticFactory CDS_FACTORY = new CdsAnalyticFactory(0.4);

  private static final LocalDate TRADE_DATE = LocalDate.of(2013, Month.NOVEMBER, 13);
  private static final LocalDate MATURITY = LocalDate.of(2016, Month.MARCH, 20);
  private static final Period[] TENORS = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3), Period.ofYears(5), Period.ofYears(7), Period.ofYears(10) };
  private static final HedgeRatioCalculator HEDGE_CAL = new HedgeRatioCalculator();
  private static final CdsAnalytic[] HEDGE_CDS;
  private static final IsdaCompliantYieldCurve YIELD_CURVE;
  private static final IsdaCompliantCreditCurve CREDIT_CURVE;

  static {
    final LocalDate spotDate = DEFAULT_CALENDAR.shift(TRADE_DATE.minusDays(1), 3);
    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "9M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "11Y", "12Y", "15Y", "20Y", "25Y", "30Y" };
    final String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
    final double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033, 0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231,
      0.03367, 0.03419, 0.03411, 0.03412 };
    YIELD_CURVE = makeYieldCurve(TRADE_DATE, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofYears(1));

    HEDGE_CDS = CDS_FACTORY.makeImmCds(TRADE_DATE, TENORS);
    final double[] spreads = new double[] {0.00886315689995649, 0.00886315689995649, 0.0133044689825873, 0.0171490070952563, 0.0183903639181293, 0.0194721890639724 };
    final CreditCurveCalibrator calibrator = new CreditCurveCalibrator(HEDGE_CDS, YIELD_CURVE);
    CREDIT_CURVE = calibrator.calibrate(spreads);
  }

  public void test() {
    final LocalDate accStart = FOLLOWING.adjust(getPrevIMMDate(TRADE_DATE), DEFAULT_CALENDAR);
    final CdsAnalytic cds = CDS_FACTORY.makeCds(TRADE_DATE, accStart, MATURITY);
    final double cdsCoupon = 0.01;

    final int n = HEDGE_CDS.length;
    final double[] hedgeCoupons = new double[n];
    Arrays.fill(hedgeCoupons, cdsCoupon);
    final DoubleMatrix1D w = HEDGE_CAL.getHedgeRatios(cds, cdsCoupon, HEDGE_CDS, hedgeCoupons, CREDIT_CURVE, YIELD_CURVE);
    final double[] expected = new double[] {-1.1842173839714448E-6, 0.36244465818986815, 0.6376106050590048, 0.0, 0.0, 0.0 };
    //regression test
    for (int i = 0; i < n; i++) {
      assertEquals("", expected[i], w.getEntry(i), 1e-15);
    }

    //value portfolio
    double pv = PRICER.pv(cds, YIELD_CURVE, CREDIT_CURVE, cdsCoupon);
    for (int i = 0; i < n; i++) {
      pv -= w.getEntry(i) * PRICER.pv(HEDGE_CDS[i], YIELD_CURVE, CREDIT_CURVE, hedgeCoupons[i]);
    }

    //perturb the credit curve 
    final double[] t = CREDIT_CURVE.getKnotTimes();
    final double[] h = CREDIT_CURVE.getKnotZeroRates();
    final double sigma = 0.01; //would expect to see up to a 4% shift in hazard rates 
    for (int k = 0; k < 200; k++) {
      final double[] bumpedH = new double[n];
      System.arraycopy(h, 0, bumpedH, 0, n);
      for (int i = 0; i < n; i++) {
        //this may induce an arbitrage - it doesn't matter here 
        bumpedH[i] *= Math.exp(sigma * NORMAL.nextRandom() - sigma * sigma / 2);
      }
      final IsdaCompliantCreditCurve cc = new IsdaCompliantCreditCurve(t, bumpedH);
      double pvBumped = PRICER.pv(cds, YIELD_CURVE, cc, cdsCoupon);
      for (int i = 0; i < n; i++) {
        pvBumped -= w.getEntry(i) * PRICER.pv(HEDGE_CDS[i], YIELD_CURVE, cc, hedgeCoupons[i]);
      }
      final double change = pvBumped - pv;
      assertTrue(change > 0 && change < 3e-7); //position has positive gamma, so change should always be positive 
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void constHazardRateTest() {
    final IsdaCompliantCreditCurve flatCC = new IsdaCompliantCreditCurve(5.0, 0.02);
    final LocalDate accStart = FOLLOWING.adjust(getPrevIMMDate(TRADE_DATE), DEFAULT_CALENDAR);
    final CdsAnalytic cds = CDS_FACTORY.makeCds(TRADE_DATE, accStart, MATURITY);
    final double cdsCoupon = 0.01;

    final int n = HEDGE_CDS.length;
    final double[] hedgeCoupons = new double[n];
    Arrays.fill(hedgeCoupons, cdsCoupon);
    final DoubleMatrix1D w = HEDGE_CAL.getHedgeRatios(cds, cdsCoupon, HEDGE_CDS, hedgeCoupons, flatCC, YIELD_CURVE);
    System.out.println(w);
  }

  public void lessCDStest() {
    final LocalDate accStart = FOLLOWING.adjust(getPrevIMMDate(TRADE_DATE), DEFAULT_CALENDAR);
    final CdsAnalytic cds = CDS_FACTORY.makeCds(TRADE_DATE, accStart, MATURITY);
    final double cdsCoupon = 0.01;

    final CdsAnalytic[] hedgeCDS = CDS_FACTORY.makeImmCds(TRADE_DATE, new Period[] {Period.ofYears(1), Period.ofYears(5) });
    final int n = hedgeCDS.length;
    final double[] hedgeCoupons = new double[n];
    Arrays.fill(hedgeCoupons, cdsCoupon);
    final DoubleMatrix1D w = HEDGE_CAL.getHedgeRatios(cds, cdsCoupon, hedgeCDS, hedgeCoupons, CREDIT_CURVE, YIELD_CURVE);
    final double[] expected = new double[] {0.3877847710928422, 0.026594401620818442 };
    for (int i = 0; i < n; i++) {
      assertEquals("", expected[i], w.getEntry(i), 1e-15);
    }
  }

}
