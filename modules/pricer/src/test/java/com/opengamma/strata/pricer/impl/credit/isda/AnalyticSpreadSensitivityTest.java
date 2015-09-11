/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.market.curve.perturb.ShiftType;

/**
 * Test.
 */
@Test
public class AnalyticSpreadSensitivityTest extends IsdaBaseTest {

  private static final AnalyticSpreadSensitivityCalculator ANAL_CS01_CAL = new AnalyticSpreadSensitivityCalculator();

  // common data
  private static final LocalDate TODAY = LocalDate.of(2013, 4, 21);
  private static final LocalDate EFFECTIVE_DATE = TODAY.plusDays(1); // AKA stepin date
  private static final LocalDate CASH_SETTLE_DATE = DEFAULT_CALENDAR.shift(TODAY, 3); // AKA valuation date

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
    final CdsStubType stubType = CdsStubType.FRONTSHORT;
    final boolean protectionStart = true;

    CDS = new CdsAnalytic(TODAY, EFFECTIVE_DATE, CASH_SETTLE_DATE, PROTECTION_STATE_DATE, PROTECTION_END_DATE, payAccOndefault, tenor, stubType, protectionStart, RECOVERY_RATE);

    for (int i = 0; i < NUM_MARKET_CDS; i++) {
      MARKET_CDS[i] = new CdsAnalytic(TODAY, EFFECTIVE_DATE, CASH_SETTLE_DATE, TODAY, PAR_SPD_DATES[i], payAccOndefault, tenor, stubType, protectionStart, RECOVERY_RATE);
    }
  }

  @Test
  public void Test() {

    final double dealSpread = DEAL_SPREAD * ONE_BP;
    final double[] mrkSpreads = new double[NUM_MARKET_CDS];
    for (int i = 0; i < NUM_MARKET_CDS; i++) {
      mrkSpreads[i] = PAR_SPREADS[i] * ONE_BP;
    }

    // compare with bump and reprice
    final double[] an_CS01 = ANAL_CS01_CAL.bucketedCS01FromParSpreads(CDS, dealSpread, YIELD_CURVE, MARKET_CDS, mrkSpreads);
    final double[] fd_CS01 = CS01_CAL.bucketedCS01FromParSpreads(
        CDS, dealSpread, YIELD_CURVE, MARKET_CDS, mrkSpreads, 1e-7, ShiftType.ABSOLUTE);

    final int n = fd_CS01.length;
    for (int i = 0; i < n; i++) {
      assertEquals(fd_CS01[i], an_CS01[i], 1e-6); // the fd is only forward difference - so accuracy is not great
    }
  }

  /**
   * 
   */
  @Test
  public void ParallelCS01FiniteDifferenceComparisonTest() {
    final AccrualOnDefaultFormulae form = AccrualOnDefaultFormulae.CORRECT;
    final AnalyticSpreadSensitivityCalculator aCal = new AnalyticSpreadSensitivityCalculator(form);
    final FiniteDifferenceSpreadSensitivityCalculator fCal = new FiniteDifferenceSpreadSensitivityCalculator(form);
    final double eps = 1.e-6;

    final double coupon = 100. * 1.e-4;
    final double quotedSpread = 104. * 1.e-4;
    final CdsQuoteConvention qSp = new CdsQuotedSpread(coupon, quotedSpread);
    final double puf = 0.3;

    final double pCS01 = aCal.parallelCS01(CDS, qSp, YIELD_CURVE);
    final double pCS01Fin = fCal.parallelCS01(CDS, qSp, YIELD_CURVE, eps);
    assertEquals(pCS01Fin, pCS01, Math.abs(pCS01Fin) * eps * 10.);

    final double pCS01FromPuf = aCal.parallelCS01FromPUF(CDS, coupon, YIELD_CURVE, puf);
    final double pCS01FromPufFin = fCal.parallelCS01FromPUF(CDS, coupon, YIELD_CURVE, puf, eps);
    assertEquals(pCS01FromPufFin, pCS01FromPuf, Math.abs(pCS01FromPufFin) * eps * 10.);

    final double pCS01FromSpread = aCal.parallelCS01FromSpread(CDS, coupon, YIELD_CURVE, quotedSpread);
    final double pCS01FromSpreadFin = fCal.parallelCS01FromSpread(
        CDS, coupon, YIELD_CURVE, quotedSpread, eps, ShiftType.ABSOLUTE);
    assertEquals(pCS01FromSpreadFin, pCS01FromSpread, Math.abs(pCS01FromSpreadFin) * eps * 10.);

    final double pCS01FromEqSpread = aCal.parallelCS01FromSpread(CDS, coupon, YIELD_CURVE, coupon);
    final double pCS01FromEqSpreadFin = fCal.parallelCS01FromSpread(
        CDS, coupon, YIELD_CURVE, coupon, eps, ShiftType.ABSOLUTE);
    assertEquals(pCS01FromEqSpreadFin, pCS01FromEqSpread, Math.abs(pCS01FromEqSpreadFin) * eps * 10.);
  }

  /**
   * 
   */
  @Test(enabled = false)
  public void BucketedCS01FiniteDifferenceComparisonTest() {
    //TODO Tests should be added (PLAT-5931) once PLAT-5971 is fixed
    final AccrualOnDefaultFormulae form = AccrualOnDefaultFormulae.CORRECT;
    final AnalyticSpreadSensitivityCalculator aCal = new AnalyticSpreadSensitivityCalculator(form);
    final FiniteDifferenceSpreadSensitivityCalculator fCal = new FiniteDifferenceSpreadSensitivityCalculator(form);
    final IsdaCompliantCreditCurveBuilder builder = new FastCreditCurveBuilder(form);
    final double eps = 1.e-6;

    final double coupon = 100. * 1.e-4;
    final double quotedSpread = 104. * 1.e-4;

    final double[] bCS01FromSpread = aCal.bucketedCS01FromSpread(CDS, coupon, YIELD_CURVE, quotedSpread, MARKET_CDS);
    final IsdaCompliantCreditCurve curve = builder.calibrateCreditCurve(CDS, quotedSpread, YIELD_CURVE);
    final double[] bCS01FromSpreadFin = fCal.bucketedCS01FromCreditCurve(CDS, coupon, MARKET_CDS, YIELD_CURVE, curve, eps);
    for (int i = 0; i < NUM_MARKET_CDS; ++i) {
      assertEquals(bCS01FromSpreadFin[i], bCS01FromSpread[i], Math.abs(bCS01FromSpreadFin[i]) * eps * 10.);
    }
  }
}
