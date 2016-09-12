/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Test.
 */
@Test
public class MarketQuoteConverterTest {

  private static final MarketQuoteConverter PUF = new MarketQuoteConverter();
  private static final HolidayCalendar DEFAULT_CALENDAR = HolidayCalendars.SAT_SUN;

  private static final DayCount ACT365 = DayCounts.ACT_365F;
  private static final DayCount ACT360 = DayCounts.ACT_360;

  private static final BusinessDayConvention FOLLOWING = BusinessDayConventions.FOLLOWING;

  private static final LocalDate TODAY = LocalDate.of(2008, Month.SEPTEMBER, 19);
  private static final LocalDate STEPIN_DATE = TODAY.plusDays(1);
  private static final LocalDate CASH_SETTLE_DATE = DEFAULT_CALENDAR.shift(TODAY, 3); // AKA valuation date
  private static final LocalDate START_DATE = LocalDate.of(2007, Month.MARCH, 20);
  private static final LocalDate END_DATE = LocalDate.of(2015, Month.DECEMBER, 20);

  private static final LocalDate[] MATURITIES = new LocalDate[] {LocalDate.of(2008, Month.DECEMBER, 20), LocalDate.of(2009, Month.JUNE, 20), LocalDate.of(2010, Month.JUNE, 20),
    LocalDate.of(2011, Month.JUNE, 20), LocalDate.of(2012, Month.JUNE, 20), LocalDate.of(2014, Month.JUNE, 20), LocalDate.of(2017, Month.JUNE, 20) };

  // yield curve
  private static final LocalDate SPOT_DATE = DEFAULT_CALENDAR.shift(TODAY, 2);
  private static final IsdaCompliantYieldCurve YIELD_CURVE;

  static {
    final int nMoneyMarket = 6;
    final int nSwaps = 15;
    final int nInstruments = nMoneyMarket + nSwaps;

    final IsdaInstrumentTypes[] types = new IsdaInstrumentTypes[nInstruments];
    final Period[] tenors = new Period[nInstruments];
    final int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12 };
    final int[] swapYears = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20, 25, 30 };
    // check
    ArgChecker.isTrue(mmMonths.length == nMoneyMarket, "mmMonths");
    ArgChecker.isTrue(swapYears.length == nSwaps, "swapYears");

    for (int i = 0; i < nMoneyMarket; i++) {
      types[i] = IsdaInstrumentTypes.MONEY_MARKET;
      tenors[i] = Period.ofMonths(mmMonths[i]);
    }
    for (int i = nMoneyMarket; i < nInstruments; i++) {
      types[i] = IsdaInstrumentTypes.SWAP;
      tenors[i] = Period.ofYears(swapYears[i - nMoneyMarket]);
    }

    final double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033, 0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231,
      0.03367, 0.03419, 0.03411, 0.03412 };

    final DayCount moneyMarketDCC = ACT360;
    final DayCount swapDCC = ACT360;
    final DayCount curveDCC = ACT365;
    final Period swapInterval = Period.ofMonths(6);

    YIELD_CURVE = IsdaCompliantYieldCurveBuild.build(TODAY, SPOT_DATE, types, tenors, rates, moneyMarketDCC, swapDCC, swapInterval, curveDCC, FOLLOWING);
  }

  public void SingleCDSTest() {
    final Period tenor = Period.ofMonths(3);
    final boolean payAccOnDefault = true;
    final StubConvention stubType = StubConvention.SHORT_INITIAL;
    final boolean protectionStart = true;

    final double pointsUpFront = 0.007;
    final double permium = 100. / 10000;
    final double recovery = 0.4;
    final CdsAnalytic cds = new CdsAnalytic(TODAY, STEPIN_DATE, CASH_SETTLE_DATE, START_DATE, END_DATE, payAccOnDefault, tenor, stubType, protectionStart, recovery);
    final double parSpread = PUF.pufToQuotedSpread(cds, permium, YIELD_CURVE, pointsUpFront);
    final double expectedParSpread = 0.011112592882846; // taken from Excel-ISDA 1.8.2
    assertEquals("Par Spread", expectedParSpread, parSpread, 1e-14);

    final double derivedPUF = PUF.quotedSpreadToPUF(cds, permium, YIELD_CURVE, parSpread);
    assertEquals("PUF", pointsUpFront, derivedPUF, 1e-15);

  }

  public void SingleCDSTest2() {
    final Period tenor = Period.ofMonths(6);
    final boolean payAccOnDefault = true;
    final StubConvention stubType = StubConvention.LONG_INITIAL;
    final boolean protectionStart = true;

    final double parSpread = 143.4 / 10000;
    final double permium = 500. / 10000;
    final double recovery = 0.4;
    final CdsAnalytic cds = new CdsAnalytic(TODAY, STEPIN_DATE, CASH_SETTLE_DATE, START_DATE, END_DATE, payAccOnDefault, tenor, stubType, protectionStart, recovery);
    final double puf = PUF.quotedSpreadToPUF(cds, permium, YIELD_CURVE, parSpread);
    final double expectedPUF = -0.2195134271137960; // taken from Excel-ISDA 1.8.2
    assertEquals("PUF", expectedPUF, puf, 5e-13);

    final double derivedParSpread = PUF.pufToQuotedSpread(cds, permium, YIELD_CURVE, puf);
    assertEquals("Par Spread", parSpread, derivedParSpread, 1e-15);

  }

  public void MultiCDSTest() {
    final Period tenor = Period.ofMonths(3);
    final boolean payAccOnDefault = true;
    final StubConvention stubType = StubConvention.SHORT_INITIAL;
    final boolean protectionStart = true;
    final double permium = 100. / 10000;
    final double recovery = 0.4;

    final int n = 7;
    final double[] pointsUpFront = {0.007, 0.008, 0.001, 0.0011, 0.0012, 0.0015, 0.0014 };

    // make CDS
    final CdsAnalytic[] cds = new CdsAnalytic[n];
    for (int i = 0; i < n; i++) {
      cds[i] = new CdsAnalytic(TODAY, STEPIN_DATE, CASH_SETTLE_DATE, START_DATE, MATURITIES[i], payAccOnDefault, tenor, stubType, protectionStart, recovery);
    }

    final double[] quotedSpreads = PUF.pufToQuotedSpreads(cds, permium, YIELD_CURVE, pointsUpFront);
    final double[] parSpreads = PUF.pufToParSpreads(cds, permium, YIELD_CURVE, pointsUpFront);

    final double[] derivedPUF = PUF.quotedSpreadsToPUF(cds, permium, YIELD_CURVE, quotedSpreads);
    final double[] derivedPUF2 = PUF.parSpreadsToPUF(cds, permium, YIELD_CURVE, parSpreads);
    for (int i = 0; i < n; i++) {
      assertEquals("PUF1", pointsUpFront[i], derivedPUF[i], 1e-15);
      assertEquals("PUF1", pointsUpFront[i], derivedPUF2[i], 1e-15);
    }

  }

  /**
   * 
   */
  public void consistencyTest() {
    final double tol = 1.e-13;

    final AccrualOnDefaultFormulae form = AccrualOnDefaultFormulae.MARKIT_FIX;
    final MarketQuoteConverter localConv = new MarketQuoteConverter(form);
    final IsdaCompliantCreditCurveBuilder builder = new FastCreditCurveBuilder(form);
    final AnalyticCdsPricer pricer = new AnalyticCdsPricer(form);

    final LocalDate today = LocalDate.of(2011, 4, 21);
    final LocalDate stepIn = today.plusDays(1);
    final LocalDate val = DEFAULT_CALENDAR.shift(today, 3);
    final LocalDate start = ImmDateLogic.getPrevIMMDate(today);
    final LocalDate end = ImmDateLogic.getPrevIMMDate(today.plusYears(5));
    final LocalDate end1 = ImmDateLogic.getPrevIMMDate(today.plusYears(10));

    final Period tenor = Period.ofMonths(3);
    final boolean payAccOnDefault = true;
    final StubConvention stubType = StubConvention.LONG_INITIAL;
    final boolean protectionStart = true;
    final double recovery = 0.4;
    final double recovery1 = 0.25;

    final CdsAnalytic cds = new CdsAnalytic(today, stepIn, val, start, end, payAccOnDefault, tenor, stubType, protectionStart, recovery);
    final CdsAnalytic cds1 = new CdsAnalytic(today, stepIn, val, start, end1, payAccOnDefault, tenor, stubType, protectionStart, recovery1);

    final double spread = 212. * 1.e-4;
    final double coupon = 250. * 1.e-4;
    final double spread1 = 102. * 1.e-4;
    final double coupon1 = 125. * 1.e-4;

    final CdsQuotedSpread sp = new CdsQuotedSpread(coupon, spread);
    final CdsQuotedSpread sp1 = new CdsQuotedSpread(coupon1, spread1);

    final IsdaCompliantCreditCurve cCurve = builder.calibrateCreditCurve(cds, spread, YIELD_CURVE);

    /*
     * cleanPrice and principal
     */
    final double clean = localConv.cleanPrice(cds, YIELD_CURVE, cCurve, coupon);
    final double cleanExp = localConv.cleanPrice(pricer.pv(cds, YIELD_CURVE, cCurve, coupon, CdsPriceType.CLEAN));
    assertEquals(cleanExp, clean, tol);
    final double notional = 25000.;
    final double principal = localConv.principal(notional, cds, YIELD_CURVE, cCurve, coupon);
    final double principalExp = notional * pricer.pv(cds, YIELD_CURVE, cCurve, coupon, CdsPriceType.CLEAN);
    assertEquals(principalExp, principal, tol);

    /*
     * A quoted spread -> A points up-front
     */
    final PointsUpFront puf1 = localConv.convert(cds, sp, YIELD_CURVE);
    final double puf2 = localConv.pointsUpFront(cds, coupon, YIELD_CURVE, cCurve);
    assertEquals(puf2, puf1.getPointsUpFront(), tol);
    assertEquals(coupon, puf1.getCoupon());
    final CdsQuotedSpread spRe = localConv.convert(cds, puf1, YIELD_CURVE);
    assertEquals(spread, spRe.getQuotedSpread(), tol);
    assertEquals(coupon, spRe.getCoupon());

    /*
     * par spreads -> points up-fronts
     */
    final IsdaCompliantCreditCurve cCurveCol = builder.calibrateCreditCurve(new CdsAnalytic[] {cds, cds1 }, new double[] {spread, spread1 }, YIELD_CURVE);
    final double[] pufs1 = localConv.pointsUpFront(new CdsAnalytic[] {cds, cds1 }, new double[] {coupon, coupon1 }, YIELD_CURVE, cCurveCol);
    final double[] pufs1Col = localConv.parSpreadsToPUF(new CdsAnalytic[] {cds, cds1 }, new double[] {coupon, coupon1 }, YIELD_CURVE, new double[] {spread, spread1 });
    assertEquals(pufs1[0], pufs1Col[0], tol);
    assertEquals(pufs1[1], pufs1Col[1], tol);

    /*
     * quoted spreads <-> points up-fronts
     */
    final IsdaCompliantCreditCurve cCurve1 = builder.calibrateCreditCurve(cds1, spread1, YIELD_CURVE);
    final double pufCol = localConv.pointsUpFront(cds, coupon, YIELD_CURVE, cCurve);
    final double pufCol1 = localConv.pointsUpFront(cds1, coupon1, YIELD_CURVE, cCurve1);
    final double[] pufs2 = localConv.quotedSpreadsToPUF(new CdsAnalytic[] {cds, cds1 }, new double[] {coupon, coupon1 }, YIELD_CURVE, new double[] {spread, spread1 });
    final PointsUpFront[] pufs3 = localConv.convert(new CdsAnalytic[] {cds, cds1 }, new CdsQuotedSpread[] {sp, sp1 }, YIELD_CURVE);
    assertEquals(pufCol, pufs2[0], tol);
    assertEquals(pufCol1, pufs2[1], tol);
    assertEquals(pufs2[0], pufs3[0].getPointsUpFront(), tol);
    assertEquals(pufs2[1], pufs3[1].getPointsUpFront(), tol);

    final double[] spsRe = localConv.pufToQuotedSpreads(new CdsAnalytic[] {cds, cds1 }, new double[] {coupon, coupon1 }, YIELD_CURVE, pufs2);
    assertEquals(spread, spsRe[0], tol);
    assertEquals(spread1, spsRe[1], tol);

    final CdsQuotedSpread[] qSpInd = localConv.convert(new CdsAnalytic[] {cds, cds1 }, pufs3, YIELD_CURVE);
    assertEquals(sp.getQuotedSpread(), qSpInd[0].getQuotedSpread(), tol);
    assertEquals(sp1.getQuotedSpread(), qSpInd[1].getQuotedSpread(), tol);

    /*
     * par spreads <-> quoted spreads, via points up-fronts
     */
    final double[] sps = new double[] {spread, spread * 0.9 };
    final double[] qSpFromPSp1 = localConv.parSpreadsToQuotedSpreads(new CdsAnalytic[] {cds, cds1 }, coupon, YIELD_CURVE, sps);
    final IsdaCompliantCreditCurve cCurveCol1 = builder.calibrateCreditCurve(new CdsAnalytic[] {cds, cds1 }, sps, YIELD_CURVE);
    final double[] tmpPufs = new double[] {pricer.pv(cds, YIELD_CURVE, cCurveCol1, coupon, CdsPriceType.CLEAN), pricer.pv(cds1, YIELD_CURVE, cCurveCol1, coupon, CdsPriceType.CLEAN) };
    final double[] qSpFromPSp1Exp = new double[] {pricer.parSpread(cds, YIELD_CURVE, builder.calibrateCreditCurve(cds, coupon, YIELD_CURVE, tmpPufs[0])),
      pricer.parSpread(cds1, YIELD_CURVE, builder.calibrateCreditCurve(cds1, coupon, YIELD_CURVE, tmpPufs[1])) };
    assertEquals(qSpFromPSp1Exp[0], qSpFromPSp1[0], tol);
    assertEquals(qSpFromPSp1Exp[1], qSpFromPSp1[1], tol);
    final double[] qSpFromPSp2 = localConv.parSpreadsToQuotedSpreads(new CdsAnalytic[] {cds, cds1 }, new double[] {coupon, coupon1 }, YIELD_CURVE, new double[] {spread, spread1 });
    final double[] qSpFromPSp2Exp = new double[] {pricer.parSpread(cds, YIELD_CURVE, builder.calibrateCreditCurve(cds, coupon, YIELD_CURVE, pufs1[0])),
      pricer.parSpread(cds1, YIELD_CURVE, builder.calibrateCreditCurve(cds1, coupon1, YIELD_CURVE, pufs1[1])) };
    assertEquals(qSpFromPSp2Exp[0], qSpFromPSp2[0], tol);
    assertEquals(qSpFromPSp2Exp[1], qSpFromPSp2[1], tol);

    final double[] pspsBack1 = localConv.quotedSpreadToParSpreads(new CdsAnalytic[] {cds, cds1 }, coupon, YIELD_CURVE, qSpFromPSp1);
    assertEquals(sps[0], pspsBack1[0], tol);
    assertEquals(sps[1], pspsBack1[1], tol);
    final double[] pspsBack2 = localConv.quotedSpreadToParSpreads(new CdsAnalytic[] {cds, cds1 }, new double[] {coupon, coupon1 }, YIELD_CURVE, qSpFromPSp2);
    assertEquals(spread, pspsBack2[0], tol);
    assertEquals(spread1, pspsBack2[1], tol);

    /*
     * Error test
     */
    try {
      localConv.convert(new CdsAnalytic[] {cds }, qSpInd, YIELD_CURVE);
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      localConv.quotedSpreadsToPUF(new CdsAnalytic[] {cds }, coupon, YIELD_CURVE, sps);
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      localConv.quotedSpreadsToPUF(new CdsAnalytic[] {cds, cds1 }, new double[] {coupon }, YIELD_CURVE, sps);
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      localConv.quotedSpreadsToPUF(new CdsAnalytic[] {cds, cds1 }, new double[] {coupon, coupon1 }, YIELD_CURVE, new double[] {spread });
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      localConv.convert(new CdsAnalytic[] {cds }, pufs3, YIELD_CURVE);
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
  }
}
