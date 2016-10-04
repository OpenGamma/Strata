package com.opengamma.strata.pricer.credit.cds;

import org.testng.annotations.Test;

public class FastCreditCurveCalibratorTest extends IsdaCompliantCreditCurveCalibratorBase {

  // calibrators
  private static final FastCreditCurveCalibrator BUILDER_ISDA =
      new FastCreditCurveCalibrator(AccrualOnDefaultFormulae.ORIGINAL_ISDA);
  private static final FastCreditCurveCalibrator BUILDER_MARKIT =
      new FastCreditCurveCalibrator(AccrualOnDefaultFormulae.MARKIT_FIX);

  @Test
  public void regression_consistency_test() {
    testCalibrationAgainstISDA(BUILDER_ISDA, 1e-14);
    testCalibrationAgainstISDA(BUILDER_MARKIT, 1e-14);
  }

//  /** TODO after simple builder is implemented
//   * 
//   */
//  @SuppressWarnings("deprecation")
//  @Test
//  public void noAccOnDefaultTest() {
//    final FastCreditCurveBuilder fastOg = new FastCreditCurveBuilder(OG_FIX, ArbitrageHandling.Ignore);
//
//    final SimpleCreditCurveBuilder simpleISDA = new SimpleCreditCurveBuilder(ORIGINAL_ISDA);
//    final SimpleCreditCurveBuilder simpleFix = new SimpleCreditCurveBuilder(MARKIT_FIX);
//    final SimpleCreditCurveBuilder simpleOg = new SimpleCreditCurveBuilder(OG_FIX);
//
//    final LocalDate tradeDate = LocalDate.of(2013, Month.APRIL, 25);
//
//    final CDSAnalyticFactory baseFactory = new CDSAnalyticFactory();
//    final CDSAnalyticFactory noAccFactory = baseFactory.withPayAccOnDefault(false);
//    final Period[] tenors = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3), Period.ofYears(5),
//        Period.ofYears(7), Period.ofYears(10)};
//    final CDSAnalytic[] pillar = noAccFactory.makeIMMCDS(tradeDate, tenors);
//    final double[] spreads = new double[] {0.027, 0.017, 0.012, 0.009, 0.008, 0.005};
//
//    final LocalDate spotDate = addWorkDays(tradeDate.minusDays(1), 3, DEFAULT_CALENDAR);
//    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "9M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y",
//        "9Y", "10Y", "11Y", "12Y", "15Y", "20Y", "25Y", "30Y"};
//    final String[] yieldCurveInstruments =
//        new String[] {"M", "M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S"};
//    final double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033,
//        0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231,
//        0.03367, 0.03419, 0.03411, 0.03412};
//    final ISDACompliantYieldCurve yc =
//        makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofYears(1));
//
//    final ISDACompliantCreditCurve curveFastISDA = BUILDER_ISDA.calibrateCreditCurve(pillar, spreads, yc);
//    final ISDACompliantCreditCurve curveFastFix = BUILDER_MARKIT.calibrateCreditCurve(pillar, spreads, yc);
//    final ISDACompliantCreditCurve curveFastOriginal = fastOg.calibrateCreditCurve(pillar, spreads, yc);
//    final ISDACompliantCreditCurve curveSimpleISDA = simpleISDA.calibrateCreditCurve(pillar, spreads, yc);
//    final ISDACompliantCreditCurve curveSimpleFix = simpleFix.calibrateCreditCurve(pillar, spreads, yc);
//    final ISDACompliantCreditCurve curveSimpleOriginal = simpleOg.calibrateCreditCurve(pillar, spreads, yc);
//
//    final double[] sampleTime = new double[] {30 / 365., 90 / 365., 180. / 365., 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
//    final int num = sampleTime.length;
//    for (int i = 0; i < num; ++i) {
//      assertEquals(curveSimpleISDA.getHazardRate(sampleTime[i]), curveFastISDA.getHazardRate(sampleTime[i]), 1.e-6);
//      assertEquals(curveSimpleFix.getHazardRate(sampleTime[i]), curveFastFix.getHazardRate(sampleTime[i]), 1.e-6);
//      assertEquals(curveSimpleOriginal.getHazardRate(sampleTime[i]), curveFastOriginal.getHazardRate(sampleTime[i]), 1.e-6);
//    }
//

//  
//    /*
//     * Flat hazard rate case
//     */
//    final double coupon = 0.025;
//    final MarketQuoteConverter conv = new MarketQuoteConverter();
//    final double[] pufs = conv.parSpreadsToPUF(new CDSAnalytic[] {pillar[3]}, coupon, yc, new double[] {spreads[3]});
//    final double[] qsps = conv.quotedSpreadToParSpreads(new CDSAnalytic[] {pillar[3]}, coupon, yc, new double[] {spreads[3]});
//
//    final PointsUpFront puf = new PointsUpFront(coupon, pufs[0]);
//    final QuotedSpread qsp = new QuotedSpread(coupon, qsps[0]);
//    final ParSpread psp = new ParSpread(spreads[3]);
//
//    final ISDACompliantCreditCurve curveFastPuf = BUILDER_ISDA.calibrateCreditCurve(pillar[3], puf, yc);
//    final ISDACompliantCreditCurve curveFastQsp = BUILDER_ISDA.calibrateCreditCurve(pillar[3], qsp, yc);
//    final ISDACompliantCreditCurve curveFastPsp = BUILDER_ISDA.calibrateCreditCurve(pillar[3], psp, yc);
//    final ISDACompliantCreditCurve curveSimplePuf = simpleISDA.calibrateCreditCurve(pillar[3], puf, yc);
//
//    final LocalDate stepinDate = tradeDate.plusDays(1);
//    final LocalDate valueDate = BusinessDayDateUtils.addWorkDays(tradeDate, 3, DEFAULT_CALENDAR);
//    final LocalDate startDate = IMMDateLogic.getPrevIMMDate(tradeDate);
//    final LocalDate endDate = IMMDateLogic.getNextIMMDate(tradeDate.plus(tenors[3]));
//    final ISDACompliantCreditCurve curveFastElem = BUILDER_ISDA.calibrateCreditCurve(tradeDate, stepinDate, valueDate, startDate,
//        endDate, spreads[3], false, Period.ofMonths(3), StubType.FRONTSHORT,
//        true, yc, 0.4);
//
//    assertEquals(1, curveFastPuf.getNumberOfKnots());
//    assertEquals(1, curveFastQsp.getNumberOfKnots());
//    assertEquals(1, curveFastPsp.getNumberOfKnots());
//
//    for (int i = 0; i < num; ++i) {
//      assertEquals(curveFastPuf.getForwardRate(sampleTime[i]), curveFastQsp.getForwardRate(sampleTime[i]), 1.e-12);
//      assertEquals(curveFastPuf.getForwardRate(sampleTime[i]), curveFastPsp.getForwardRate(sampleTime[i]), 1.e-12);
//      assertEquals(curveFastPuf.getForwardRate(sampleTime[i]), curveFastElem.getForwardRate(sampleTime[i]), 1.e-12);
//      assertEquals(curveSimplePuf.getForwardRate(sampleTime[i]), curveFastPuf.getForwardRate(sampleTime[i]), 1.e-6);
//    }
//
//    /*
//     * Consistency
//     */
//
//    final FastCreditCurveBuilder fastOriginalFail =
//        new FastCreditCurveBuilder(AccrualOnDefaultFormulae.OrignalISDA, ArbitrageHandling.Fail);
//    /*
//     * Fail with zero pufs
//     */
//    try {
//      fastOriginalFail.calibrateCreditCurve(pillar, spreads, yc);
//      throw new RuntimeException();
//    } catch (final Exception e) {
//      assertTrue(e instanceof IllegalArgumentException);
//    }
//
//    /*
//     * Fail with nonzero pufs
//     */
//    final int nSpreads = spreads.length;
//    final PointsUpFront[] pufsFail = new PointsUpFront[nSpreads];
//    final double[] pufValues = conv.parSpreadsToPUF(pillar, coupon, yc, spreads);
//    for (int i = 0; i < nSpreads; ++i) {
//      pufsFail[i] = new PointsUpFront(coupon, pufValues[i]);
//    }
//    try {
//      fastOriginalFail.calibrateCreditCurve(pillar, pufsFail, yc);
//      throw new RuntimeException();
//    } catch (final Exception e) {
//      assertTrue(e instanceof IllegalArgumentException);
//    }
//
//    /*
//     * ArgumentChecker hit 
//     */
//    final double[] prems = new double[nSpreads];
//    Arrays.fill(prems, coupon);
//    final double[] shortPufs = Arrays.copyOf(pufValues, nSpreads - 1);
//    try {
//      fastOriginalFail.calibrateCreditCurve(pillar, prems, yc, shortPufs);
//      throw new RuntimeException();
//    } catch (final Exception e) {
//      assertTrue(e instanceof IllegalArgumentException);
//    }
//    final double[] shortPrems = Arrays.copyOf(prems, nSpreads - 1);
//    try {
//      fastOriginalFail.calibrateCreditCurve(pillar, shortPrems, yc, pufValues);
//      throw new RuntimeException();
//    } catch (final Exception e) {
//      assertTrue(e instanceof IllegalArgumentException);
//    }
//
//    final CDSAnalytic[] pillarCopy = Arrays.copyOf(pillar, nSpreads);
//    pillarCopy[2] = pillarCopy[2].withOffset(0.5);
//    try {
//      fastOriginalFail.calibrateCreditCurve(pillarCopy, prems, yc, pufValues);
//      throw new RuntimeException();
//    } catch (final Exception e) {
//      assertTrue(e instanceof IllegalArgumentException);
//    }
//    pillarCopy[2] = pillar[3];
//    pillarCopy[3] = pillar[2];
//    try {
//      fastOriginalFail.calibrateCreditCurve(pillarCopy, prems, yc, pufValues);
//      throw new RuntimeException();
//    } catch (final Exception e) {
//      assertTrue(e instanceof IllegalArgumentException);
//    }
//
//    try {
//      BUILDER_ISDA.calibrateCreditCurve(tradeDate, stepinDate, valueDate, startDate, new LocalDate[] {endDate}, spreads, false,
//          Period.ofMonths(3), StubType.FRONTSHORT,
//          true, yc, 0.4);
//      throw new RuntimeException();
//    } catch (final Exception e) {
//      assertTrue(e instanceof IllegalArgumentException);
//    }
//
//  }

//  /**
//   * 
//   */
//  public void viaConverterTest() {
//    LocalDate tradeDate = LocalDate.of(2014, 5, 22);
//    double recovery = 0.25;
//    CDSAnalyticFactory immCDSFact = new CDSAnalyticFactory(recovery);
//
//    LocalDate spotDate = LocalDate.of(2014, 5, 27);
//    String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y",
//        "12Y", "15Y", "20Y", "25Y", "30Y"};
//    String[] yieldCurveInstruments =
//        new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S"};
//    double[] rates = new double[] {0.001, 0.002, 0.0025, 0.003, 0.0052, 0.0053, 0.00851, 0.0125, 0.016, 0.02, 0.02, 0.022, 0.024,
//        0.025, 0.02, 0.031,
//        0.030, 0.031, 0.0323};
//    ISDACompliantYieldCurve yieldCurve =
//        makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofMonths(6));
//    LocalDate end = LocalDate.of(2014, 6, 20);
//    double spF = 6.726 * 1.e-4;
//    double spS = 6.727 * 1.e-4;
//
//    CDSAnalytic cds = immCDSFact.makeCDS(tradeDate, getPrevIMMDate(tradeDate), end);
//    double coupon = 500. * 1.e-4;
//    MarketQuoteConverter conv = new MarketQuoteConverter();
//    double[] resS = conv.parSpreadsToQuotedSpreads(new CDSAnalytic[] {cds}, coupon, yieldCurve, new double[] {spS});
//    double[] resF = conv.parSpreadsToQuotedSpreads(new CDSAnalytic[] {cds}, coupon, yieldCurve, new double[] {spF});
//    assertEquals(resS[0], resF[0], 1.e-5);
//  }

//  /**
//   * 
//   */
//  public void viaImpliedSpreadTest() {
//
//    LocalDate tradeDate = LocalDate.of(2014, 5, 16);
//    double recoveryF = 0.248;
//    double recoveryS = 0.247;
//    CDSAnalyticFactory nonImmCDSFactS = new CDSAnalyticFactory(recoveryS, Period.ofMonths(6));
//    CDSAnalyticFactory nonImmCDSFactF = new CDSAnalyticFactory(recoveryF, Period.ofMonths(6));
//
//    LocalDate spotDate = LocalDate.of(2014, Month.MAY, 20);
//    String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y",
//        "12Y", "15Y", "20Y", "25Y", "30Y"};
//    String[] yieldCurveInstruments =
//        new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S"};
//    double[] rates = new double[] {0.00151, 0.0018, 0.0026, 0.0031, 0.0052, 0.0053, 0.00851, 0.0125, 0.016, 0.02, 0.02, 0.022,
//        0.024, 0.025, 0.02, 0.031,
//        0.030, 0.031, 0.0323};
//    ISDACompliantYieldCurve yieldCurve =
//        makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofMonths(6));
//
//    LocalDate end = LocalDate.of(2014, 5, 20);
//    double puf = 1.101e-2;
//    double coupon = 500.0 * 1.0e-4;
//
//    Period[] buckets = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(2), Period.ofYears(3),
//        Period.ofYears(4), Period.ofYears(5), Period.ofYears(6),
//        Period.ofYears(7), Period.ofYears(8), Period.ofYears(9), Period.ofYears(10), Period.ofYears(12), Period.ofYears(15),
//        Period.ofYears(20), Period.ofYears(25), Period.ofYears(30)};
//    CDSAnalytic cdsS = nonImmCDSFactS.makeCDS(tradeDate, getPrevIMMDate(tradeDate), end);
//    CDSAnalytic[] bucketCDSS = nonImmCDSFactS.makeIMMCDS(tradeDate, buckets);
//    CDSAnalytic cdsF = nonImmCDSFactF.makeCDS(tradeDate, getPrevIMMDate(tradeDate), end);
//    CDSAnalytic[] bucketCDSF = nonImmCDSFactF.makeIMMCDS(tradeDate, buckets);
//    double bump = 1.e-4;
//    FiniteDifferenceSpreadSensitivityCalculator cs01Cal = new FiniteDifferenceSpreadSensitivityCalculator();
//
//    PointsUpFront pufC = new PointsUpFront(coupon, puf);
//    double[] resS = cs01Cal.bucketedCS01FromPUF(cdsS, pufC, yieldCurve, bucketCDSS, bump);
//    double[] resF = cs01Cal.bucketedCS01FromPUF(cdsF, pufC, yieldCurve, bucketCDSF, bump);
//
//    for (int i = 0; i < resS.length; ++i) {
//      assertEquals(resS[i], resF[i], 1.e-5);
//    }
//  }

}
