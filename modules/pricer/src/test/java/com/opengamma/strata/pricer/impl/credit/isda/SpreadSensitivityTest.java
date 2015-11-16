/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;

/**
 * Test.
 */
@Test
public class SpreadSensitivityTest {

  private static final FiniteDifferenceSpreadSensitivityCalculator CDV01_CAL = new FiniteDifferenceSpreadSensitivityCalculator();
  private static final HolidayCalendar DEFAULT_CALENDAR = HolidayCalendars.SAT_SUN;

  // common data
  private static final LocalDate TODAY = LocalDate.of(2013, 4, 21);
  private static final LocalDate EFFECTIVE_DATE = TODAY.plusDays(1); // AKA stepin date
  private static final LocalDate CASH_SETTLE_DATE = DEFAULT_CALENDAR.shift(TODAY, 3); // AKA valuation date
  private static final double RECOVERY_RATE = 0.4;
  private static final double NOTIONAL = 1e7;

  // valuation CDS
  private static final LocalDate PROTECTION_STATE_DATE = LocalDate.of(2013, 2, 3); // Seasoned CDS
  private static final LocalDate PROTECTION_END_DATE = LocalDate.of(2018, 3, 20);
  private static final double DEAL_SPREAD = 101;
  private static final CdsAnalytic CDS;

  // market CDSs
  private static final LocalDate[] PAR_SPD_DATES = new LocalDate[] {LocalDate.of(2013, 6, 20), LocalDate.of(2013, 9, 20), LocalDate.of(2014, 3, 20), LocalDate.of(2015, 3, 20),
    LocalDate.of(2016, 3, 20), LocalDate.of(2018, 3, 20), LocalDate.of(2023, 3, 20) };
  private static final double[] PAR_SPREADS = new double[] {50, 70, 80, 95, 100, 95, 80 };
  private static final int NUM_MARKET_CDS = PAR_SPD_DATES.length;
  private static final CdsAnalytic[] MARKET_CDS = new CdsAnalytic[NUM_MARKET_CDS];

  // yield curve
  private static IsdaCompliantYieldCurve YIELD_CURVE;

  static {
    final double flatrate = 0.05;
    final double t = 20.0;
    YIELD_CURVE = new IsdaCompliantYieldCurve(new double[] {t }, new double[] {flatrate });

    final boolean payAccOndefault = true;
    final Period tenor = Period.ofMonths(3);
    final StubConvention stubType = StubConvention.SHORT_INITIAL;
    final boolean protectionStart = true;

    CDS = new CdsAnalytic(TODAY, EFFECTIVE_DATE, CASH_SETTLE_DATE, PROTECTION_STATE_DATE, PROTECTION_END_DATE, payAccOndefault, tenor, stubType, protectionStart, RECOVERY_RATE);

    for (int i = 0; i < NUM_MARKET_CDS; i++) {
      MARKET_CDS[i] = new CdsAnalytic(TODAY, EFFECTIVE_DATE, CASH_SETTLE_DATE, TODAY, PAR_SPD_DATES[i], payAccOndefault, tenor, stubType, protectionStart, RECOVERY_RATE);
    }
  }

  public void parellelCreditDV01Test() {
    final double fromExcel = 4238.557409;

    final double dealSpread = DEAL_SPREAD / 10000;
    final double[] mrkSpreads = new double[NUM_MARKET_CDS];
    for (int i = 0; i < NUM_MARKET_CDS; i++) {
      mrkSpreads[i] = PAR_SPREADS[i] / 10000;
    }

    final double cdv01 = NOTIONAL / 10000 * CDV01_CAL.parallelCS01FromParSpreads(
        CDS, dealSpread, YIELD_CURVE, MARKET_CDS, mrkSpreads, 1e-4, ShiftType.ABSOLUTE);
    assertEquals("", fromExcel, cdv01, 1e-13 * NOTIONAL);

    /*
     * Errors checked
     */

    try {
      CDV01_CAL.parallelCS01FromParSpreads(CDS, dealSpread, YIELD_CURVE, MARKET_CDS, mrkSpreads, 1e-12, ShiftType.ABSOLUTE);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      final double[] mktSpShort = Arrays.copyOf(mrkSpreads, NUM_MARKET_CDS - 2);
      CDV01_CAL.parallelCS01FromParSpreads(CDS, dealSpread, YIELD_CURVE, MARKET_CDS, mktSpShort, 1e-4, ShiftType.ABSOLUTE);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
  }

  /**
   * 
   */
  public void crossPrallelCS01test() {
    /*
     * Tol is not needed if exactly the same steps are taken
     */
    final double tol = 1.e-13;

    final AccrualOnDefaultFormulae form = AccrualOnDefaultFormulae.MARKIT_FIX;
    final FiniteDifferenceSpreadSensitivityCalculator localCal = new FiniteDifferenceSpreadSensitivityCalculator(form);
    final MarketQuoteConverter conv = new MarketQuoteConverter(form);

    final IsdaCompliantCreditCurveBuilder cvBuild = new FastCreditCurveBuilder(form);
    final AnalyticCdsPricer pricer = new AnalyticCdsPricer(form);

    final double bump = 1.e-4;
    final double spread = 115. * bump;
    final double fixedCoupon = 100. * bump;
    final double puf = 0.1;

    final CdsQuoteConvention quotePS = new CdsParSpread(spread);
    final CdsQuoteConvention quoteQS = new CdsQuotedSpread(fixedCoupon, spread);
    final CdsQuoteConvention quotePU = new PointsUpFront(fixedCoupon, puf);
    final double pCS01PS = localCal.parallelCS01(CDS, quotePS, YIELD_CURVE, bump);
    final double pCS01QS = localCal.parallelCS01(CDS, quoteQS, YIELD_CURVE, bump);
    final double pCS01PU = localCal.parallelCS01(CDS, quotePU, YIELD_CURVE, bump);

    final double pCS01PSd = localCal.parallelCS01FromSpread(CDS, spread, YIELD_CURVE, spread, bump, ShiftType.ABSOLUTE);
    final double pCS01QSd = localCal.parallelCS01FromSpread(CDS, fixedCoupon, YIELD_CURVE, spread, bump, ShiftType.ABSOLUTE);
    final double pCS01PUd = localCal.parallelCS01FromPUF(CDS, fixedCoupon, YIELD_CURVE, puf, bump);

    assertEquals(pCS01PS, pCS01PSd, tol);
    assertEquals(pCS01QS, pCS01QSd, tol);
    assertEquals(pCS01PU, pCS01PUd, tol);

    final IsdaCompliantCreditCurve curvePSUp = cvBuild.calibrateCreditCurve(CDS, spread + bump, YIELD_CURVE);
    final IsdaCompliantCreditCurve curvePS = cvBuild.calibrateCreditCurve(CDS, spread, YIELD_CURVE);
    final double pufFromBumpedPSpread = pricer.pv(CDS, YIELD_CURVE, curvePSUp, spread, CdsPriceType.DIRTY);
    final double pufFromPSpread = pricer.pv(CDS, YIELD_CURVE, curvePS, spread, CdsPriceType.DIRTY);

    final double pCS01PSExp = (pufFromBumpedPSpread - pufFromPSpread) / bump;
    assertEquals(pCS01PSExp, pCS01PS, tol);

    final IsdaCompliantCreditCurve curveUp = cvBuild.calibrateCreditCurve(CDS, spread + bump, YIELD_CURVE);
    final IsdaCompliantCreditCurve curve = cvBuild.calibrateCreditCurve(CDS, spread, YIELD_CURVE);
    final double up = pricer.pv(CDS, YIELD_CURVE, curveUp, fixedCoupon, CdsPriceType.DIRTY);
    final double price = pricer.pv(CDS, YIELD_CURVE, curve, fixedCoupon, CdsPriceType.DIRTY);

    final double pCS01QSExp = (up - price) / bump;
    assertEquals(pCS01QSExp, pCS01QS, tol);

    final double bumpedQSpreadFromPUF = conv.pufToQuotedSpread(CDS, fixedCoupon, YIELD_CURVE, puf) + bump;
    final double pufFromBumpedSpread = conv.quotedSpreadToPUF(CDS, fixedCoupon, YIELD_CURVE, bumpedQSpreadFromPUF);
    final double pCS01PUExp = (pufFromBumpedSpread - puf) / bump;
    assertEquals(pCS01PUExp, pCS01PU, tol);

    final double pCS01Diff = localCal.parallelCS01FromQuotedSpread(
        CDS, fixedCoupon, YIELD_CURVE, MARKET_CDS[1], spread, bump, ShiftType.ABSOLUTE);
    final IsdaCompliantCreditCurve curveAnUp = cvBuild.calibrateCreditCurve(MARKET_CDS[1], spread + bump, YIELD_CURVE);
    final IsdaCompliantCreditCurve curveAn = cvBuild.calibrateCreditCurve(MARKET_CDS[1], spread, YIELD_CURVE);
    final double upAn = pricer.pv(CDS, YIELD_CURVE, curveAnUp, fixedCoupon, CdsPriceType.DIRTY);
    final double priceAn = pricer.pv(CDS, YIELD_CURVE, curveAn, fixedCoupon, CdsPriceType.DIRTY);
    final double pCS01DiffExp = (upAn - priceAn) / bump;
    assertEquals(pCS01DiffExp, pCS01Diff, tol);

    /*
     * Errors checked
     */

    try {
      localCal.parallelCS01FromQuotedSpread(
          CDS, fixedCoupon, YIELD_CURVE, MARKET_CDS[1], spread, bump * bump * bump, ShiftType.ABSOLUTE);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }

  }

  /**
   * 
   */
  public void CS01PillarAndCreditTest() {
    /*
     * Tol is not needed if exactly the same steps are taken
     */
    final double tol = 1.e-13;

    final AccrualOnDefaultFormulae form = AccrualOnDefaultFormulae.ORIGINAL_ISDA;
    final FiniteDifferenceSpreadSensitivityCalculator localCal = new FiniteDifferenceSpreadSensitivityCalculator(form);
    final IsdaCompliantCreditCurveBuilder cvBuild = new FastCreditCurveBuilder(form);
    final AnalyticCdsPricer pricer = new AnalyticCdsPricer(form);
    final MarketQuoteConverter conv = new MarketQuoteConverter(form);

    final double basisPt = 1.e-4;
    final double coupon = 125. * basisPt;
    final LocalDate startDate = ImmDateLogic.getPrevIMMDate(TODAY);
    final LocalDate nextIMM = ImmDateLogic.getNextIMMDate(TODAY);

    final double[] pillarSpreads = new double[] {107.81, 112.99, 115.26, 117.63, 120.8, 124.09, 127.81, 130.38, 136.82, 138.77, 141.3 };
    final Period[] tenors = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5), Period.ofYears(6),
      Period.ofYears(7), Period.ofYears(8), Period.ofYears(9), Period.ofYears(10) };
    final LocalDate[] pillarDates = ImmDateLogic.getIMMDateSet(nextIMM, tenors);
    pillarDates[2] = pillarDates[2].minusMonths(2);
    pillarDates[3] = pillarDates[3].plusWeeks(3);

    final int nPillars = pillarDates.length;
    final CdsAnalytic[] pillarCDSs = new CdsAnalytic[nPillars];
    final CdsQuoteConvention[] pillar_quotes = new CdsQuoteConvention[nPillars];
    final CdsQuoteConvention[] pillar_quotes_bumped = new CdsQuoteConvention[nPillars];
    final double[] pillar_qSpreads = new double[nPillars];

    pillarCDSs[0] = new CdsAnalytic(TODAY, EFFECTIVE_DATE, CASH_SETTLE_DATE, startDate, pillarDates[0], true, Period.ofMonths(3), StubConvention.SHORT_INITIAL, true, 0.4);
    pillar_qSpreads[0] = pillarSpreads[0] * basisPt;
    final double puf = conv.quotedSpreadToPUF(pillarCDSs[0], coupon, YIELD_CURVE, pillar_qSpreads[0]);
    final double pufBumped = conv.quotedSpreadToPUF(pillarCDSs[0], coupon, YIELD_CURVE, pillar_qSpreads[0] + basisPt);
    pillar_quotes[0] = new PointsUpFront(coupon, puf);
    pillar_quotes_bumped[0] = new PointsUpFront(coupon, pufBumped);

    for (int i = 1; i < nPillars; i++) {
      pillarCDSs[i] = new CdsAnalytic(TODAY, EFFECTIVE_DATE, CASH_SETTLE_DATE, startDate, pillarDates[i], true, Period.ofMonths(3), StubConvention.SHORT_INITIAL, true, 0.4);
      pillar_qSpreads[i] = pillarSpreads[i] * basisPt;
      if (ImmDateLogic.isIMMDate(pillarDates[i])) {
        pillar_quotes[i] = new CdsQuotedSpread(coupon, pillar_qSpreads[i]);
        pillar_quotes_bumped[i] = new CdsQuotedSpread(coupon, pillar_qSpreads[i] + basisPt);
      } else {
        pillar_quotes[i] = new CdsParSpread(pillar_qSpreads[i]);
        pillar_quotes_bumped[i] = new CdsParSpread(pillar_qSpreads[i] + basisPt);
      }
    }

    /*
     * Test parallelCS01FromPillarQuotes
     */
    final double res1 = localCal.parallelCS01FromPillarQuotes(CDS, coupon, YIELD_CURVE, pillarCDSs, pillar_quotes, basisPt);
    final IsdaCompliantCreditCurve curve = cvBuild.calibrateCreditCurve(pillarCDSs, pillar_quotes, YIELD_CURVE);
    final double pv = pricer.pv(CDS, YIELD_CURVE, curve, coupon);
    final IsdaCompliantCreditCurve curveBumped = cvBuild.calibrateCreditCurve(pillarCDSs, pillar_quotes_bumped, YIELD_CURVE);
    final double pvBumped = pricer.pv(CDS, YIELD_CURVE, curveBumped, coupon);
    assertEquals((pvBumped - pv) / basisPt, res1, tol);

    /*
     * Test parallelCS01FromCreditCurve
     */
    final double res2 = localCal.parallelCS01FromCreditCurve(CDS, coupon, pillarCDSs, YIELD_CURVE, curve, basisPt);
    final double[] impSpreads = new double[nPillars];
    final double[] impSpreadsBumped = new double[nPillars];
    for (int i = 0; i < nPillars; ++i) {
      impSpreads[i] = pricer.parSpread(pillarCDSs[i], YIELD_CURVE, curve);
      impSpreadsBumped[i] = impSpreads[i] + basisPt;
    }
    final IsdaCompliantCreditCurve curveFromImpliedSpreads = cvBuild.calibrateCreditCurve(pillarCDSs, impSpreads, YIELD_CURVE);
    final double pvBase = pricer.pv(CDS, YIELD_CURVE, curveFromImpliedSpreads, coupon);
    final IsdaCompliantCreditCurve curveFromImpliedSpreadsBumped = cvBuild.calibrateCreditCurve(pillarCDSs, impSpreadsBumped, YIELD_CURVE);
    final double pvBaseBumped = pricer.pv(CDS, YIELD_CURVE, curveFromImpliedSpreadsBumped, coupon);
    assertEquals((pvBaseBumped - pvBase) / basisPt, res2, tol);

    /*
     * Test bucketedCS01FromPillarQuotes
     */
    final double[] bucketed1 = localCal.bucketedCS01FromPillarQuotes(CDS, coupon, YIELD_CURVE, pillarCDSs, pillar_quotes, basisPt);
    for (int i = 0; i < nPillars; ++i) {
      final double[] spreadsWithOneBump = Arrays.copyOf(pillar_qSpreads, nPillars);
      spreadsWithOneBump[i] += basisPt;
      final CdsQuoteConvention[] pillarQuotesLocal = new CdsQuoteConvention[nPillars];
      for (int j = 0; j < nPillars; ++j) {
        if (ImmDateLogic.isIMMDate(pillarDates[j])) {
          pillarQuotesLocal[j] = new CdsQuotedSpread(coupon, spreadsWithOneBump[j]);
        } else {
          pillarQuotesLocal[j] = new CdsParSpread(spreadsWithOneBump[j]);
        }
      }
      final IsdaCompliantCreditCurve curveWithOneBump = cvBuild.calibrateCreditCurve(pillarCDSs, pillarQuotesLocal, YIELD_CURVE);
      final double pvWithOneBump = pricer.pv(CDS, YIELD_CURVE, curveWithOneBump, coupon);
      assertEquals((pvWithOneBump - pv) / basisPt, bucketed1[i], tol);
    }

    /*
     * Test bucketedCS01FromPillarQuotes
     */
    final LocalDate[] pSpDates = new LocalDate[] {LocalDate.of(2013, 9, 20), LocalDate.of(2014, 6, 20), LocalDate.of(2016, 9, 20), LocalDate.of(2018, 6, 20), LocalDate.of(2023, 9, 20) };
    final CdsAnalytic[] bucketCDSs = new CdsAnalytic[pSpDates.length];
    for (int i = 0; i < pSpDates.length; i++) {
      bucketCDSs[i] = new CdsAnalytic(TODAY, EFFECTIVE_DATE, CASH_SETTLE_DATE, TODAY, pSpDates[i], true, Period.ofMonths(3), StubConvention.SHORT_INITIAL, true, RECOVERY_RATE);
    }
    final double[] bucketed2 = localCal.bucketedCS01FromParSpreads(CDS, coupon, bucketCDSs, YIELD_CURVE, pillarCDSs, pillar_qSpreads, basisPt);
    final double[] impSps = new double[pSpDates.length];
    final IsdaCompliantCreditCurve curveFromSpreads = cvBuild.calibrateCreditCurve(pillarCDSs, pillar_qSpreads, YIELD_CURVE);
    for (int i = 0; i < pSpDates.length; ++i) {
      impSps[i] = pricer.parSpread(bucketCDSs[i], YIELD_CURVE, curveFromSpreads);
    }
    final IsdaCompliantCreditCurve curveBucket = cvBuild.calibrateCreditCurve(bucketCDSs, impSps, YIELD_CURVE);
    final double bvBase = pricer.pv(CDS, YIELD_CURVE, curveBucket, coupon);
    for (int i = 0; i < pSpDates.length; ++i) {
      final double[] impSpsBump = Arrays.copyOf(impSps, pSpDates.length);
      impSpsBump[i] += basisPt;
      final IsdaCompliantCreditCurve curveBucketBump = cvBuild.calibrateCreditCurve(bucketCDSs, impSpsBump, YIELD_CURVE);
      final double bvBaseBump = pricer.pv(CDS, YIELD_CURVE, curveBucketBump, coupon);
      assertEquals((bvBaseBump - bvBase) / basisPt, bucketed2[i], tol);
    }

    /*
     * Errors checked
     */
    final CdsAnalytic[] shortCDSs = Arrays.copyOf(pillarCDSs, nPillars - 1);
    final double[] shortSpreads = Arrays.copyOf(pillarSpreads, pillarSpreads.length - 2);
    try {
      localCal.parallelCS01FromPillarQuotes(CDS, coupon, YIELD_CURVE, pillarCDSs, pillar_quotes, basisPt * 1.e-9);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      localCal.parallelCS01FromPillarQuotes(CDS, coupon, YIELD_CURVE, shortCDSs, pillar_quotes, basisPt);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      localCal.parallelCS01FromCreditCurve(CDS, coupon, pillarCDSs, YIELD_CURVE, curve, basisPt * 1.e-9);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      final CdsAnalytic[] unsortedCDSs = Arrays.copyOf(pillarCDSs, nPillars);
      final CdsAnalytic tmp = unsortedCDSs[2];
      unsortedCDSs[2] = unsortedCDSs[1];
      unsortedCDSs[1] = tmp;
      localCal.parallelCS01FromCreditCurve(CDS, coupon, unsortedCDSs, YIELD_CURVE, curve, basisPt);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      localCal.bucketedCS01FromPillarQuotes(CDS, coupon, YIELD_CURVE, pillarCDSs, pillar_quotes, basisPt * 1.e-9);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      localCal.bucketedCS01FromPillarQuotes(CDS, coupon, YIELD_CURVE, shortCDSs, pillar_quotes, basisPt);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }

    try {
      localCal.bucketedCS01FromCreditCurve(CDS, coupon, pillarCDSs, YIELD_CURVE, curve, basisPt * 1.e-7);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      pillarCDSs[2] = new CdsAnalytic(TODAY, EFFECTIVE_DATE, CASH_SETTLE_DATE, startDate, pillarDates[2].plusYears(10), true, Period.ofMonths(3), StubConvention.SHORT_INITIAL, true, 0.4);
      localCal.bucketedCS01FromCreditCurve(CDS, coupon, pillarCDSs, YIELD_CURVE, curve, basisPt);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      localCal.bucketedCS01FromQuotedSpreads(
          new CdsAnalytic[] {CDS}, coupon, YIELD_CURVE, pillarCDSs, pillarSpreads, basisPt * 1.e-7, ShiftType.ABSOLUTE);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      localCal.bucketedCS01FromQuotedSpreads(
          new CdsAnalytic[] {CDS}, coupon, YIELD_CURVE, pillarCDSs, shortSpreads, basisPt, ShiftType.ABSOLUTE);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      localCal.bucketedCS01FromParSpreads(
          CDS, coupon, YIELD_CURVE, pillarCDSs, pillarSpreads, basisPt * 1.e-7, ShiftType.ABSOLUTE);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      localCal.bucketedCS01FromParSpreads(
          CDS, coupon, YIELD_CURVE, pillarCDSs, shortSpreads, basisPt, ShiftType.ABSOLUTE);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      localCal.bucketedCS01FromQuotedSpreads(
          CDS, coupon, YIELD_CURVE, pillarCDSs, pillarSpreads, basisPt * 1.e-7, ShiftType.ABSOLUTE);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      localCal.bucketedCS01FromQuotedSpreads(
          CDS, coupon, YIELD_CURVE, pillarCDSs, shortSpreads, basisPt, ShiftType.ABSOLUTE);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
  }

  /**
   * 
   */
  public void finiteDifferenceSpreadSensitivityTest() {
    /*
     * Tol is not needed if exactly the same steps are taken
     */
    final double tol = 1.e-13;

    final AccrualOnDefaultFormulae form = AccrualOnDefaultFormulae.CORRECT;
    final FiniteDifferenceSpreadSensitivityCalculator localCal = new FiniteDifferenceSpreadSensitivityCalculator(form);
    final IsdaCompliantCreditCurveBuilder cvBuild = new FastCreditCurveBuilder(form);
    final AnalyticCdsPricer pricer = new AnalyticCdsPricer(form);

    final CdsPriceType pType = CdsPriceType.DIRTY;
    final double basisPt = 1.e-4;
    final double coupon = 125. * basisPt;

    final double[] spreads = new double[NUM_MARKET_CDS];
    final double[] spreadBumps = new double[NUM_MARKET_CDS];
    final double[] spreadBumpUp = new double[NUM_MARKET_CDS];
    final double[] spreadBumpDw = new double[NUM_MARKET_CDS];

    for (int i = 0; i < NUM_MARKET_CDS; ++i) {
      spreads[i] = PAR_SPREADS[i] * basisPt;
      spreadBumps[i] = spreads[i] * 1.e-2;
      spreadBumpUp[i] = spreads[i] + spreadBumps[i];
      spreadBumpDw[i] = spreads[i] - spreadBumps[i];
    }

    final double resCnt = localCal.finiteDifferenceSpreadSensitivity(CDS, coupon, pType, YIELD_CURVE, MARKET_CDS, spreads, spreadBumps, FiniteDifferenceType.CENTRAL);
    final double resFwd = localCal.finiteDifferenceSpreadSensitivity(CDS, coupon, pType, YIELD_CURVE, MARKET_CDS, spreads, spreadBumps, FiniteDifferenceType.FORWARD);
    final double resBck = localCal.finiteDifferenceSpreadSensitivity(CDS, coupon, pType, YIELD_CURVE, MARKET_CDS, spreads, spreadBumps, FiniteDifferenceType.BACKWARD);

    final IsdaCompliantCreditCurve curve = cvBuild.calibrateCreditCurve(MARKET_CDS, spreads, YIELD_CURVE);
    final IsdaCompliantCreditCurve curveUp = cvBuild.calibrateCreditCurve(MARKET_CDS, spreadBumpUp, YIELD_CURVE);
    final IsdaCompliantCreditCurve curveDw = cvBuild.calibrateCreditCurve(MARKET_CDS, spreadBumpDw, YIELD_CURVE);

    final double pv = pricer.pv(CDS, YIELD_CURVE, curve, coupon, pType);
    final double pvUp = pricer.pv(CDS, YIELD_CURVE, curveUp, coupon, pType);
    final double pvDw = pricer.pv(CDS, YIELD_CURVE, curveDw, coupon, pType);

    assertEquals(pvUp - pvDw, resCnt, tol);
    assertEquals(pvUp - pv, resFwd, tol);
    assertEquals(pv - pvDw, resBck, tol);

    /*
     * Errors checked
     */
    try {
      final double[] shortSpreads = Arrays.copyOf(spreads, NUM_MARKET_CDS - 2);
      localCal.finiteDifferenceSpreadSensitivity(CDS, coupon, pType, YIELD_CURVE, MARKET_CDS, shortSpreads, spreadBumps, FiniteDifferenceType.CENTRAL);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      final double[] shortSpreadBumps = Arrays.copyOf(spreadBumps, NUM_MARKET_CDS - 3);
      localCal.finiteDifferenceSpreadSensitivity(CDS, coupon, pType, YIELD_CURVE, MARKET_CDS, spreads, shortSpreadBumps, FiniteDifferenceType.CENTRAL);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      final double[] negativeSpreads = Arrays.copyOf(spreads, NUM_MARKET_CDS);
      negativeSpreads[1] *= -1.;
      localCal.finiteDifferenceSpreadSensitivity(CDS, coupon, pType, YIELD_CURVE, MARKET_CDS, negativeSpreads, spreadBumps, FiniteDifferenceType.CENTRAL);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      final double[] negativeSpreadBumps = Arrays.copyOf(spreadBumps, NUM_MARKET_CDS);
      negativeSpreadBumps[3] *= -1.;
      localCal.finiteDifferenceSpreadSensitivity(CDS, coupon, pType, YIELD_CURVE, MARKET_CDS, spreads, negativeSpreadBumps, FiniteDifferenceType.CENTRAL);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      final double[] largeSpreadBumps = Arrays.copyOf(spreadBumps, NUM_MARKET_CDS);
      largeSpreadBumps[1] += 1.e2;
      localCal.finiteDifferenceSpreadSensitivity(CDS, coupon, pType, YIELD_CURVE, MARKET_CDS, spreads, largeSpreadBumps, FiniteDifferenceType.CENTRAL);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }

  }

}
