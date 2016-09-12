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

import org.testng.annotations.Test;

import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurveBuilder.ArbitrageHandling;

/**
 * Test.
 */
@SuppressWarnings("deprecation")
@Test
public class FastCreditCurveBuilderTest extends CreditCurveCalibrationTest {

  private static final FastCreditCurveBuilder BUILDER_ISDA = new FastCreditCurveBuilder();
  private static final FastCreditCurveBuilder BUILDER_MARKIT = new FastCreditCurveBuilder(MARKIT_FIX);

  @Test
  public void test() {
    testCalibrationAgainstISDA(BUILDER_ISDA, 1e-14);
    testCalibrationAgainstISDA(BUILDER_MARKIT, 1e-14);
  }

  /**
   * 
   */
  @SuppressWarnings("deprecation")
  @Test
  public void noAccOnDefaultTest() {
    final FastCreditCurveBuilder fastOg = new FastCreditCurveBuilder(OG_FIX, ArbitrageHandling.Ignore);

    final SimpleCreditCurveBuilder simpleISDA = new SimpleCreditCurveBuilder(ORIGINAL_ISDA);
    final SimpleCreditCurveBuilder simpleFix = new SimpleCreditCurveBuilder(MARKIT_FIX);
    final SimpleCreditCurveBuilder simpleOg = new SimpleCreditCurveBuilder(OG_FIX);

    final LocalDate tradeDate = LocalDate.of(2013, Month.APRIL, 25);

    final CdsAnalyticFactory baseFactory = new CdsAnalyticFactory();
    final CdsAnalyticFactory noAccFactory = baseFactory.withPayAccOnDefault(false);
    final Period[] tenors = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3), Period.ofYears(5), Period.ofYears(7), Period.ofYears(10) };
    final CdsAnalytic[] pillar = noAccFactory.makeImmCds(tradeDate, tenors);
    final double[] spreads = new double[] {0.027, 0.017, 0.012, 0.009, 0.008, 0.005 };

    final LocalDate spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 3);
    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "9M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "11Y", "12Y", "15Y", "20Y", "25Y", "30Y" };
    final String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
    final double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033, 0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231,
      0.03367, 0.03419, 0.03411, 0.03412 };
    final IsdaCompliantYieldCurve yc = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofYears(1));

    final IsdaCompliantCreditCurve curveFastISDA = BUILDER_ISDA.calibrateCreditCurve(pillar, spreads, yc);
    final IsdaCompliantCreditCurve curveFastFix = BUILDER_MARKIT.calibrateCreditCurve(pillar, spreads, yc);
    final IsdaCompliantCreditCurve curveFastOriginal = fastOg.calibrateCreditCurve(pillar, spreads, yc);
    final IsdaCompliantCreditCurve curveSimpleISDA = simpleISDA.calibrateCreditCurve(pillar, spreads, yc);
    final IsdaCompliantCreditCurve curveSimpleFix = simpleFix.calibrateCreditCurve(pillar, spreads, yc);
    final IsdaCompliantCreditCurve curveSimpleOriginal = simpleOg.calibrateCreditCurve(pillar, spreads, yc);

    final double[] sampleTime = new double[] {30 / 365., 90 / 365., 180. / 365., 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
    final int num = sampleTime.length;
    for (int i = 0; i < num; ++i) {
      assertEquals(curveSimpleISDA.getHazardRate(sampleTime[i]), curveFastISDA.getHazardRate(sampleTime[i]), 1.e-6);
      assertEquals(curveSimpleFix.getHazardRate(sampleTime[i]), curveFastFix.getHazardRate(sampleTime[i]), 1.e-6);
      assertEquals(curveSimpleOriginal.getHazardRate(sampleTime[i]), curveFastOriginal.getHazardRate(sampleTime[i]), 1.e-6);
    }

    /*
     * Flat hazard rate case
     */
    final double coupon = 0.025;
    final MarketQuoteConverter conv = new MarketQuoteConverter();
    final double[] pufs = conv.parSpreadsToPUF(new CdsAnalytic[] {pillar[3] }, coupon, yc, new double[] {spreads[3] });
    final double[] qsps = conv.quotedSpreadToParSpreads(new CdsAnalytic[] {pillar[3] }, coupon, yc, new double[] {spreads[3] });

    final PointsUpFront puf = new PointsUpFront(coupon, pufs[0]);
    final CdsQuotedSpread qsp = new CdsQuotedSpread(coupon, qsps[0]);
    final CdsParSpread psp = new CdsParSpread(spreads[3]);

    final IsdaCompliantCreditCurve curveFastPuf = BUILDER_ISDA.calibrateCreditCurve(pillar[3], puf, yc);
    final IsdaCompliantCreditCurve curveFastQsp = BUILDER_ISDA.calibrateCreditCurve(pillar[3], qsp, yc);
    final IsdaCompliantCreditCurve curveFastPsp = BUILDER_ISDA.calibrateCreditCurve(pillar[3], psp, yc);
    final IsdaCompliantCreditCurve curveSimplePuf = simpleISDA.calibrateCreditCurve(pillar[3], puf, yc);

    final LocalDate stepinDate = tradeDate.plusDays(1);
    final LocalDate valueDate = DEFAULT_CALENDAR.shift(tradeDate, 3);
    final LocalDate startDate = ImmDateLogic.getPrevIMMDate(tradeDate);
    final LocalDate endDate = ImmDateLogic.getNextIMMDate(tradeDate.plus(tenors[3]));
    final IsdaCompliantCreditCurve curveFastElem = BUILDER_ISDA.calibrateCreditCurve(tradeDate, stepinDate, valueDate, startDate, endDate, spreads[3], false, Period.ofMonths(3), StubConvention.SHORT_INITIAL,
        true, yc, 0.4);

    assertEquals(1, curveFastPuf.getNumberOfKnots());
    assertEquals(1, curveFastQsp.getNumberOfKnots());
    assertEquals(1, curveFastPsp.getNumberOfKnots());

    for (int i = 0; i < num; ++i) {
      assertEquals(curveFastPuf.getForwardRate(sampleTime[i]), curveFastQsp.getForwardRate(sampleTime[i]), 1.e-12);
      assertEquals(curveFastPuf.getForwardRate(sampleTime[i]), curveFastPsp.getForwardRate(sampleTime[i]), 1.e-12);
      assertEquals(curveFastPuf.getForwardRate(sampleTime[i]), curveFastElem.getForwardRate(sampleTime[i]), 1.e-12);
      assertEquals(curveSimplePuf.getForwardRate(sampleTime[i]), curveFastPuf.getForwardRate(sampleTime[i]), 1.e-6);
    }

    /*
     * Consistency
     */

    final FastCreditCurveBuilder fastOriginalFail = new FastCreditCurveBuilder(AccrualOnDefaultFormulae.ORIGINAL_ISDA, ArbitrageHandling.Fail);
    /*
     * Fail with zero pufs
     */
    try {
      fastOriginalFail.calibrateCreditCurve(pillar, spreads, yc);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }

    /*
     * Fail with nonzero pufs
     */
    final int nSpreads = spreads.length;
    final PointsUpFront[] pufsFail = new PointsUpFront[nSpreads];
    final double[] pufValues = conv.parSpreadsToPUF(pillar, coupon, yc, spreads);
    for (int i = 0; i < nSpreads; ++i) {
      pufsFail[i] = new PointsUpFront(coupon, pufValues[i]);
    }
    try {
      fastOriginalFail.calibrateCreditCurve(pillar, pufsFail, yc);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }

    final double[] prems = new double[nSpreads];
    Arrays.fill(prems, coupon);
    final double[] shortPufs = Arrays.copyOf(pufValues, nSpreads - 1);
    try {
      fastOriginalFail.calibrateCreditCurve(pillar, prems, yc, shortPufs);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    final double[] shortPrems = Arrays.copyOf(prems, nSpreads - 1);
    try {
      fastOriginalFail.calibrateCreditCurve(pillar, shortPrems, yc, pufValues);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }

    final CdsAnalytic[] pillarCopy = Arrays.copyOf(pillar, nSpreads);
    pillarCopy[2] = pillarCopy[2].withOffset(0.5);
    try {
      fastOriginalFail.calibrateCreditCurve(pillarCopy, prems, yc, pufValues);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    pillarCopy[2] = pillar[3];
    pillarCopy[3] = pillar[2];
    try {
      fastOriginalFail.calibrateCreditCurve(pillarCopy, prems, yc, pufValues);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }

    try {
      BUILDER_ISDA.calibrateCreditCurve(tradeDate, stepinDate, valueDate, startDate, new LocalDate[] {endDate }, spreads, false, Period.ofMonths(3), StubConvention.SHORT_INITIAL,
          true, yc, 0.4);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }

  }

  /**
   * 
   */
  public void viaConverterTest() {
    LocalDate tradeDate = LocalDate.of(2014, 5, 22);
    double recovery = 0.25;
    CdsAnalyticFactory immCDSFact = new CdsAnalyticFactory(recovery);

    LocalDate spotDate = LocalDate.of(2014, 5, 27);
    String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "12Y", "15Y", "20Y", "25Y", "30Y" };
    String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
    double[] rates = new double[] {0.001, 0.002, 0.0025, 0.003, 0.0052, 0.0053, 0.00851, 0.0125, 0.016, 0.02, 0.02, 0.022, 0.024, 0.025, 0.02, 0.031,
      0.030, 0.031, 0.0323 };
    IsdaCompliantYieldCurve yieldCurve = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofMonths(6));
    LocalDate end = LocalDate.of(2014, 6, 20);
    double spF = 6.726 * 1.e-4;
    double spS = 6.727 * 1.e-4;

    CdsAnalytic cds = immCDSFact.makeCds(tradeDate, getPrevIMMDate(tradeDate), end);
    double coupon = 500. * 1.e-4;
    MarketQuoteConverter conv = new MarketQuoteConverter();
    double[] resS = conv.parSpreadsToQuotedSpreads(new CdsAnalytic[] {cds }, coupon, yieldCurve, new double[] {spS });
    double[] resF = conv.parSpreadsToQuotedSpreads(new CdsAnalytic[] {cds }, coupon, yieldCurve, new double[] {spF });
    assertEquals(resS[0], resF[0], 1.e-5);
  }

  /**
   * 
   */
  public void viaImpliedSpreadTest() {

    LocalDate tradeDate = LocalDate.of(2014, 5, 16);
    double recoveryF = 0.248;
    double recoveryS = 0.247;
    CdsAnalyticFactory nonImmCDSFactS = new CdsAnalyticFactory(recoveryS, Period.ofMonths(6));
    CdsAnalyticFactory nonImmCDSFactF = new CdsAnalyticFactory(recoveryF, Period.ofMonths(6));

    LocalDate spotDate = LocalDate.of(2014, Month.MAY, 20);
    String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "12Y", "15Y", "20Y", "25Y", "30Y" };
    String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
    double[] rates = new double[] {0.00151, 0.0018, 0.0026, 0.0031, 0.0052, 0.0053, 0.00851, 0.0125, 0.016, 0.02, 0.02, 0.022, 0.024, 0.025, 0.02, 0.031,
      0.030, 0.031, 0.0323 };
    IsdaCompliantYieldCurve yieldCurve = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofMonths(6));

    LocalDate end = LocalDate.of(2014, 5, 20);
    double puf = 1.101e-2;
    double coupon = 500.0 * 1.0e-4;

    Period[] buckets = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5), Period.ofYears(6),
      Period.ofYears(7), Period.ofYears(8), Period.ofYears(9), Period.ofYears(10), Period.ofYears(12), Period.ofYears(15), Period.ofYears(20), Period.ofYears(25), Period.ofYears(30) };
    CdsAnalytic cdsS = nonImmCDSFactS.makeCds(tradeDate, getPrevIMMDate(tradeDate), end);
    CdsAnalytic[] bucketCDSS = nonImmCDSFactS.makeImmCds(tradeDate, buckets);
    CdsAnalytic cdsF = nonImmCDSFactF.makeCds(tradeDate, getPrevIMMDate(tradeDate), end);
    CdsAnalytic[] bucketCDSF = nonImmCDSFactF.makeImmCds(tradeDate, buckets);
    double bump = 1.e-4;
    FiniteDifferenceSpreadSensitivityCalculator cs01Cal = new FiniteDifferenceSpreadSensitivityCalculator();

    PointsUpFront pufC = new PointsUpFront(coupon, puf);
    double[] resS = cs01Cal.bucketedCS01FromPUF(cdsS, pufC, yieldCurve, bucketCDSS, bump);
    double[] resF = cs01Cal.bucketedCS01FromPUF(cdsF, pufC, yieldCurve, bucketCDSF, bump);

    for (int i = 0; i < resS.length; ++i) {
      assertEquals(resS[i], resF[i], 1.e-5);
    }
  }
}
