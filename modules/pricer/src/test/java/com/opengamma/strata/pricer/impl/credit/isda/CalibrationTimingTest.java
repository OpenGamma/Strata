/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.linearalgebra.CholeskyDecompositionCommons;
import com.opengamma.strata.math.impl.linearalgebra.CholeskyDecompositionResult;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;

import cern.jet.random.engine.MersenneTwister;

/**
 * This tests the time to calibrate the yield and credit curves. By default the tests are disabled.  
 */
@Test
public class CalibrationTimingTest extends IsdaBaseTest {

  private static ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1, new MersenneTwister(MersenneTwister.DEFAULT_SEED));
  private static final MatrixAlgebra MA = new OGMatrixAlgebra();
  private static final CholeskyDecompositionCommons CHOLESKY = new CholeskyDecompositionCommons();

  private static final int NUM_YIELD_CURVE_POINTS = 20;
  private static final Period SWAP_INTERVAL = Period.ofMonths(6);
  private static final IsdaInstrumentTypes[] YC_INST_TYPES;
  private static final Period[] YC_INST_TENOR;
  private static final double[] YC_MARKET_RATES = new double[] {0.00340055550701297, 0.00636929056400781, 0.0102617798438113, 0.0135851258907251, 0.0162809551414651, 0.020583125112332,
    0.0227369218210212, 0.0251978805237614, 0.0273223815467694, 0.0310882447627048, 0.0358397743454067, 0.036047665095421, 0.0415916567616181, 0.044066373237682, 0.046708518178509,
    0.0491196954851753, 0.0529297239911766, 0.0562025436376854, 0.0589772202773522, 0.0607471217692999 };
  private static final DoubleMatrix YC_COVAR;
  private static final DoubleMatrix YC_COVAR_SQR;

  private static final int NUM_CREDIT_CURVE_POINTS = 11;
  private static final LocalDate[] CC_DATES = new LocalDate[] {LocalDate.of(2013, 9, 20), LocalDate.of(2013, 12, 20), LocalDate.of(2014, 3, 20), LocalDate.of(2014, 6, 20), LocalDate.of(2014, 9, 20),
    LocalDate.of(2015, 9, 20), LocalDate.of(2016, 9, 20), LocalDate.of(2017, 9, 20), LocalDate.of(2018, 9, 20), LocalDate.of(2020, 9, 20), LocalDate.of(2023, 9, 20) };
  private static final DoubleMatrix CC_COVAR;
  private static final DoubleMatrix CC_COVAR_SQR;

  private static final LocalDate TODAY = LocalDate.of(2013, 6, 4);
  private static final LocalDate STEPIN_DATE = TODAY.plusDays(1); // aka effective date
  private static final LocalDate SPOTDATE = DEFAULT_CALENDAR.shift(TODAY, 3); // 3 working days on
  private static final LocalDate VALUEDATE = SPOTDATE;
  private static final LocalDate STARTDATE = TODAY; // have protection start now.
  private static final double[] MARKET_CREDIT_SPREADS = new double[] {40, 45, 50, 55, 70, 90, 130, 130, 130, 120, 115, 105, 90 };

  static {
    // setup yield curve stuff
    YC_INST_TYPES = new IsdaInstrumentTypes[NUM_YIELD_CURVE_POINTS];
    YC_INST_TENOR = new Period[NUM_YIELD_CURVE_POINTS];
    final int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12 };
    final int[] swapYears = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30 };
    final int nMoneyMarket = 6;
    final int nSwaps = 14;
    // check
    ArgChecker.isTrue(mmMonths.length == nMoneyMarket, "mmMonths");
    ArgChecker.isTrue(swapYears.length == nSwaps, "swapYears");

    for (int i = 0; i < nMoneyMarket; i++) {
      YC_INST_TYPES[i] = IsdaInstrumentTypes.MONEY_MARKET;
      YC_INST_TENOR[i] = Period.ofMonths(mmMonths[i]);
    }
    for (int i = nMoneyMarket; i < NUM_YIELD_CURVE_POINTS; i++) {
      YC_INST_TYPES[i] = IsdaInstrumentTypes.SWAP;
      YC_INST_TENOR[i] = Period.ofYears(swapYears[i - nMoneyMarket]);
    }

    final LocalDate[] ycMatDates = new LocalDate[NUM_YIELD_CURVE_POINTS];
    final double[] ycMatTimes = new double[NUM_YIELD_CURVE_POINTS];
    for (int i = 0; i < NUM_YIELD_CURVE_POINTS; i++) {
      ycMatDates[i] = SPOTDATE.plus(YC_INST_TENOR[i]);
      ycMatTimes[i] = ACT365F.yearFraction(SPOTDATE, ycMatDates[i]);
    }

    final double ycDecorrelation = -0.2;
    final double ycVar = 1 / 36.;
    double[][] temp = new double[NUM_YIELD_CURVE_POINTS][NUM_YIELD_CURVE_POINTS];
    for (int i = 0; i < NUM_YIELD_CURVE_POINTS; i++) {
      temp[i][i] = ycVar * FunctionUtils.square(YC_MARKET_RATES[i]);
      for (int j = 0; j < i; j++) {
        final double t1 = ycMatTimes[i];
        final double t2 = ycMatTimes[j];
        final double rho = t1 >= t2 ? Math.pow(t1 / t2, ycDecorrelation) : Math.pow(t2 / t1, ycDecorrelation);
        temp[i][j] = rho * ycVar * YC_MARKET_RATES[i] * YC_MARKET_RATES[j];
      }
    }
    for (int i = 0; i < NUM_YIELD_CURVE_POINTS; i++) {
      for (int j = i + 1; j < NUM_YIELD_CURVE_POINTS; j++) {
        temp[i][j] = temp[j][i];
      }
    }
    YC_COVAR = DoubleMatrix.copyOf(temp);
    CholeskyDecompositionResult res = CHOLESKY.apply(YC_COVAR);
    YC_COVAR_SQR = res.getL();

    // setup credit curve stuff
    final double[] coupons = new double[NUM_CREDIT_CURVE_POINTS];
    final int n = coupons.length;
    for (int i = 0; i < n; i++) {
      coupons[i] = MARKET_CREDIT_SPREADS[i] / 10000.0;
    }

    final double[] ccMatTimes = new double[NUM_CREDIT_CURVE_POINTS];
    for (int i = 0; i < NUM_CREDIT_CURVE_POINTS; i++) {
      ccMatTimes[i] = ACT365F.yearFraction(TODAY, CC_DATES[i]);
    }
    final double ccDecorrelation = -0.01;
    final double ccVar = 1 / 50.;
    temp = new double[NUM_CREDIT_CURVE_POINTS][NUM_CREDIT_CURVE_POINTS];
    for (int i = 0; i < NUM_CREDIT_CURVE_POINTS; i++) {
      temp[i][i] = ccVar * FunctionUtils.square(coupons[i]);
      for (int j = 0; j < i; j++) {
        final double t1 = ccMatTimes[i];
        final double t2 = ccMatTimes[j];
        final double rho = t1 >= t2 ? Math.pow(t1 / t2, ccDecorrelation) : Math.pow(t2 / t1, ccDecorrelation);
        temp[i][j] = rho * ccVar * coupons[i] * coupons[j];
      }
    }
    for (int i = 0; i < NUM_CREDIT_CURVE_POINTS; i++) {
      for (int j = i + 1; j < NUM_CREDIT_CURVE_POINTS; j++) {
        temp[i][j] = temp[j][i];
      }
    }
    CC_COVAR = DoubleMatrix.copyOf(temp);
    res = CHOLESKY.apply(CC_COVAR);
    CC_COVAR_SQR = res.getL();

  }

  @Test(enabled = false)
  public void yieldCurvePeturbTest() {
    System.out.println("CalibrationTimingTest - set enabled=false before push");

    final DoubleArray base = DoubleArray.copyOf(YC_MARKET_RATES);
    final int nSims = 10000;
    final IsdaCompliantYieldCurveBuild ycBuilder = new IsdaCompliantYieldCurveBuild(SPOTDATE, YC_INST_TYPES, YC_INST_TENOR, ACT360, D30360, SWAP_INTERVAL, MOD_FOLLOWING);

    final long startTime = System.nanoTime();
    int failed = 0;
    for (int count = 0; count < nSims; count++) {

      final DoubleArray z = DoubleArray.of(NUM_YIELD_CURVE_POINTS, i -> NORMAL.nextRandom());
      final DoubleArray w = (DoubleArray) MA.multiply(YC_COVAR_SQR, z);
      final DoubleArray peturbedRates = (DoubleArray) MA.add(base, w);

      try {
        @SuppressWarnings("unused")
        final IsdaCompliantCurve yieldCurve = ycBuilder.build(peturbedRates.toArray());
      } catch (final MathException e) {
        failed++;
      }
    }
    final double totalTime = (System.nanoTime() - startTime) / 1e9;
    System.out.println("total time for " + nSims + " yield Curves: " + totalTime + "s. Failed to build " + failed + " curves (" + ((100. * failed) / nSims) + "%)");
  }

  @Test(enabled = false)
  public void creditCurvePeturbTest() {
    System.out.println("CalibrationTimingTest - set enabled=false before push");
    final IsdaCompliantYieldCurve yieldCurve = IsdaCompliantYieldCurveBuild.build(SPOTDATE, YC_INST_TYPES, YC_INST_TENOR, YC_MARKET_RATES, ACT360, D30360, SWAP_INTERVAL, MOD_FOLLOWING);
    final Period tenor = Period.ofMonths(3);
    final StubConvention stubType = StubConvention.SHORT_INITIAL;
    final boolean payAccOndefault = true;
    final boolean protectionStart = true;
    final double recovery = 0.4;

    final double[] coupons = new double[NUM_CREDIT_CURVE_POINTS];
    final CdsAnalytic[] cds = new CdsAnalytic[NUM_CREDIT_CURVE_POINTS];
    for (int i = 0; i < NUM_CREDIT_CURVE_POINTS; i++) {
      cds[i] = new CdsAnalytic(TODAY, STEPIN_DATE, VALUEDATE, STARTDATE, CC_DATES[i], payAccOndefault, tenor, stubType, protectionStart, recovery);
      coupons[i] = MARKET_CREDIT_SPREADS[i] / 10000.0;
    }

    final DoubleArray base = DoubleArray.copyOf(coupons);
    final int nSims = 10000;

    final long startTime = System.nanoTime();
    int failed = 0;
    for (int count = 0; count < nSims; count++) {

      final DoubleArray z = DoubleArray.of(NUM_CREDIT_CURVE_POINTS, i -> NORMAL.nextRandom());
      final DoubleArray w = (DoubleArray) MA.multiply(CC_COVAR_SQR, z);
      final DoubleArray peturbedSpreads = (DoubleArray) MA.add(base, w);

      try {
        @SuppressWarnings("unused")
        final IsdaCompliantCreditCurve creditCurve =
            CREDIT_CURVE_BUILDER.calibrateCreditCurve(cds, peturbedSpreads.toArray(), yieldCurve);
      } catch (final MathException e) {
        failed++;
      }
    }
    final double totalTime = (System.nanoTime() - startTime) / 1e9;
    System.out.println("total time for " + nSims + " credit Curves: " + totalTime + "s. Failed to build " + failed + " curves (" + ((100. * failed) / nSims) + "%)");
  }
}
