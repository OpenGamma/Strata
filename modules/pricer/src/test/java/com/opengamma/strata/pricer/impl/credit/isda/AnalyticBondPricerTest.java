/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.math.impl.function.Function1D;

/**
 * 
 */
@Test
public class AnalyticBondPricerTest extends IsdaBaseTest {

  public void bondPriceTest() {

    final double recoveryRate = 0.4;
    final CdsAnalyticFactory factory = new CdsAnalyticFactory(recoveryRate);
    final LocalDate tradeDate = LocalDate.of(2014, 3, 14);

    final double bondCoupon = 0.05;
    //for now use the CDS mechanics to generate bond payment schedule 
    final CdsAnalytic dummyCDS = factory.with(Period.ofMonths(6)).withProtectionStart(false).makeImmCds(tradeDate, Period.ofYears(5));
    final CdsCoupon[] cdsCoupons = dummyCDS.getCoupons();
    final int n = dummyCDS.getNumPayments();
    final double[] paymentTimes = new double[n];
    final double[] paymentAmounts = new double[n];

    for (int i = 0; i < n; i++) {
      paymentTimes[i] = cdsCoupons[i].getPaymentTime();
      paymentAmounts[i] = cdsCoupons[i].getYearFrac() * bondCoupon;
    }
    paymentAmounts[n - 1] += 1.0;
    final BondAnalytic bond = new BondAnalytic(paymentTimes, paymentAmounts, recoveryRate, dummyCDS.getAccruedPremium(bondCoupon));

    final IsdaCompliantYieldCurve yieldCurve = new IsdaCompliantYieldCurve(1.0, 0.05);
    final double cleanPrice = defaultFreeBondCleanPrice(bond, yieldCurve);

    final AnalyticBondPricer bondSpreadCal = new AnalyticBondPricer();
    final Function1D<Double, Double> bondPriceFunc = bondSpreadCal.getBondPriceForHazardRateFunction(bond, yieldCurve, CdsPriceType.CLEAN);
    //now price will zero hazard rate - should get same number
    final double price = bondPriceFunc.evaluate(0.0);
    assertEquals(cleanPrice, price, 1e-15);

    assertEquals("Hazard rate limit", recoveryRate, bondSpreadCal.bondPriceForHazardRate(bond, yieldCurve, 1000.0, CdsPriceType.DIRTY), 2e-5);

  }

  /**
   * Check our bond price is consistent with the CDS price. To do this we must price the protection leg of a CDS with protection from start true, but the annuity with
   * protection from start false (the annuity must also not have accrual-on-default)
   */
  public void bondPriceTest2() {

    final IsdaCompliantYieldCurve yieldCurve = YieldCurveProvider.ISDA_USD_20140205;

    final double recoveryRate = 0.27;

    final Period couponPeriod = Period.ofMonths(6);
    final CdsStubType stub = CdsStubType.FRONTSHORT;
    final BusinessDayConvention bd = FOLLOWING;
    final HolidayCalendar cal = DEFAULT_CALENDAR;
    final CdsAnalyticFactory factory = new CdsAnalyticFactory(0.0).with(couponPeriod).withPayAccOnDefault(false);

    final LocalDate startDate = LocalDate.of(2013, 9, 20);
    final LocalDate endDate = LocalDate.of(2019, 3, 20);
    final LocalDate tradeDate = LocalDate.of(2014, 2, 5);
    final double exp = ACT365F.yearFraction(tradeDate, endDate);

    final CdsAnalytic protectionLegCDS = factory.makeCds(tradeDate, startDate, endDate);
    final CdsAnalytic annuityCDS = factory.withProtectionStart(false).makeCds(tradeDate, startDate, endDate);

    final double bondCoupon = 0.07;
    final IsdaPremiumLegSchedule sch = new IsdaPremiumLegSchedule(startDate, endDate, couponPeriod, stub, bd, cal, false);
    final BondAnalytic bond = new BondAnalytic(tradeDate, bondCoupon, sch, recoveryRate, ACT360);

    final AnalyticCdsPricer cdsPricer = new AnalyticCdsPricer();
    final AnalyticBondPricer bondPricer = new AnalyticBondPricer();

    for (int i = 0; i < 10; i++) {
      final double lambda = 0.0 + 0.3 * i / 9.;
      final IsdaCompliantCreditCurve cc = new IsdaCompliantCreditCurve(10.0, lambda);
      final double bondPrice = bondPricer.bondPriceForHazardRate(bond, yieldCurve, lambda, CdsPriceType.DIRTY);
      final double cdsProtLeg = cdsPricer.protectionLeg(protectionLegCDS, yieldCurve, cc, 0.0);
      final double cdsAnnuity = cdsPricer.annuity(annuityCDS, yieldCurve, cc, CdsPriceType.DIRTY, 0.0);
      final double q = cc.getSurvivalProbability(exp);
      final double p = yieldCurve.getDiscountFactor(exp);
      final double bondPriceAsCDS = cdsAnnuity * bondCoupon + q * p + recoveryRate * cdsProtLeg;

      assertEquals(bondPriceAsCDS, bondPrice, 1e-15);
    }
  }

  /**
   * Bond and CDS coincide for certain setup
   */
  public void limitedCaseTest() {
    final double tol = 1.e-12;
    final AnalyticBondPricer bondPricer = new AnalyticBondPricer();
    final AnalyticCdsPricer cdsPricer = new AnalyticCdsPricer();

    final LocalDate tradeDate = LocalDate.of(2014, 2, 13);
    final LocalDate startDate = LocalDate.of(2013, 12, 20);
    final LocalDate endDate = LocalDate.of(2018, 12, 20);

    final Period couponPrd = Period.ofMonths(6);
    final CdsStubType stubTp = CdsStubType.FRONTSHORT;
    final BusinessDayConvention bdConv = MOD_FOLLOWING;
    final HolidayCalendar cal = DEFAULT_CALENDAR;
    boolean ProtStart = false;
    double rr = 0.;
    final CdsPriceType priceTp = CdsPriceType.DIRTY;

    final IsdaCompliantYieldCurve yc = YieldCurveProvider.ISDA_USD_20140213;
    final double hr = 0.2;
    final IsdaCompliantCreditCurve cc = new IsdaCompliantCreditCurve(10., hr);
    final double coupon = 0.1;
    final IsdaPremiumLegSchedule schedule = new IsdaPremiumLegSchedule(startDate, endDate, couponPrd, stubTp, bdConv, cal, ProtStart);
    final BondAnalytic bond = new BondAnalytic(tradeDate, coupon, schedule, rr, ACT360);
    final CdsAnalytic cds = new CdsAnalytic(tradeDate, tradeDate.plusDays(1), tradeDate, startDate, endDate, false, couponPrd, stubTp,
        ProtStart, 1. - rr, bdConv, cal, ACT360, DayCounts.ACT_365F);

    final double resBond1 = bondPricer.bondPriceForHazardRate(bond, yc, hr, priceTp);
    final double resCDS1 = -cdsPricer.pv(cds, yc, cc, coupon, priceTp);
    final double mat = bond.getPaymentTime(bond.getnPayments() - 1);
    assertEquals((resCDS1 + 1. * yc.getDiscountFactor(mat) * cc.getDiscountFactor(mat)), resBond1, tol);
    final double eqSp1 = bondPricer.getEquivalentCdsSpread(bond, yc, resBond1, priceTp, cds);
    assertEquals(0., eqSp1, tol);

    rr = 0.3;
    ProtStart = true;
    final CdsAnalytic cds2 = new CdsAnalytic(tradeDate, tradeDate.plusDays(1), tradeDate, startDate, endDate, false, couponPrd, stubTp,
        ProtStart, 1. - rr, bdConv, cal, ACT360, DayCounts.ACT_365F);
    final BondAnalytic bond2 = new BondAnalytic(tradeDate, 0., schedule, rr, ACT360);
    final double resBond2 = bondPricer.bondPriceForHazardRate(bond2, yc, hr, priceTp);
    final double resCDS2 = cdsPricer.pv(cds2, yc, cc, 0., priceTp);
    assertEquals((resCDS2 + 1. * yc.getDiscountFactor(mat) * cc.getDiscountFactor(mat)), resBond2, tol);
    final double eqSp2 = bondPricer.getEquivalentCdsSpread(bond2, yc, resBond2, priceTp, cds2);
    final double sp2 = cdsPricer.parSpread(cds2, yc, cc);
    assertEquals(sp2, eqSp2, tol);
  }

  /**
   * 
   */
  public void hazardRateTest() {
    final double tol = 1.e-12;
    final AnalyticBondPricer bondPricer = new AnalyticBondPricer();

    final LocalDate tradeDate = LocalDate.of(2014, 2, 13);
    final LocalDate startDate = LocalDate.of(2013, 12, 20);
    final LocalDate endDate = LocalDate.of(2018, 12, 20);
    final IsdaCompliantYieldCurve yc = YieldCurveProvider.ISDA_USD_20140213;

    final double coupon = 0.11;
    double rr = 0.3;
    final Period couponPrd = Period.ofMonths(6);
    final CdsStubType stubTp = CdsStubType.FRONTSHORT;
    final BusinessDayConvention bdConv = MOD_FOLLOWING;
    final HolidayCalendar cal = DEFAULT_CALENDAR;
    final boolean ProtStart = true;
    final IsdaPremiumLegSchedule schedule = new IsdaPremiumLegSchedule(startDate, endDate, couponPrd, stubTp, bdConv, cal, ProtStart);
    final BondAnalytic bond = new BondAnalytic(tradeDate, coupon, schedule, rr, ACT360);

    final double hr = 0.15;
    final double cleanPrice = bondPricer.bondPriceForHazardRate(bond, yc, hr, CdsPriceType.CLEAN);
    final double dirtyPrice = bondPricer.bondPriceForHazardRate(bond, yc, hr, CdsPriceType.DIRTY);
    final double hrClean = bondPricer.getHazardRate(bond, yc, cleanPrice, CdsPriceType.CLEAN);
    final double hrDirty = bondPricer.getHazardRate(bond, yc, dirtyPrice, CdsPriceType.DIRTY);
    assertEquals(hr, hrClean, tol);
    assertEquals(hr, hrDirty, tol);

    final double priceZero = bondPricer.bondPriceForHazardRate(bond, yc, 0., CdsPriceType.DIRTY);
    final double hrZero = bondPricer.getHazardRate(bond, yc, priceZero, CdsPriceType.DIRTY);
    assertEquals(0., hrZero, tol);

    /*
     * Exception thrown
     */
    try {
      bondPricer.getHazardRate(bond, yc, priceZero * 2., CdsPriceType.CLEAN);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      bondPricer.getHazardRate(bond, yc, bond.getRecoveryRate() * 0.5, CdsPriceType.DIRTY);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("The dirty price of " + bond.getRecoveryRate() * 0.5 + " give, is less than the bond's recovery rate of " + bond.getRecoveryRate() + ". Please check inputs", e.getMessage());
    }
    try {
      bondPricer.getHazardRate(bond, yc, -cleanPrice, CdsPriceType.CLEAN);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Bond price must be positive", e.getMessage());
    }
  }

  /**
   * 
   */
  public void exceptionalBranchTest() {
    final double tol = 1.e-12;
    final AnalyticBondPricer bondPricer = new AnalyticBondPricer();

    final LocalDate tradeDate = LocalDate.of(2014, 2, 13);
    final LocalDate startDate = LocalDate.of(2013, 12, 20);
    final LocalDate endDate = LocalDate.of(2014, 12, 20);
    final double coupon = 0.11;
    double rr = 0.3;
    final Period couponPrd = Period.ofMonths(6);
    final CdsStubType stubTp = CdsStubType.FRONTSHORT;
    final BusinessDayConvention bdConv = MOD_FOLLOWING;
    final HolidayCalendar cal = DEFAULT_CALENDAR;
    final boolean ProtStart = true;
    final IsdaPremiumLegSchedule schedule = new IsdaPremiumLegSchedule(startDate, endDate, couponPrd, stubTp, bdConv, cal, ProtStart);
    final BondAnalytic bond = new BondAnalytic(tradeDate, coupon, schedule, rr, ACT360);
    final double bondPaymentTimeLast = bond.getPaymentTime(bond.getnPayments() - 1);

    final double hr = 0.11;
    final IsdaCompliantYieldCurve yc1 = new IsdaCompliantYieldCurve(new double[] {0.3 * bondPaymentTimeLast, 0.7 * bondPaymentTimeLast,
      bondPaymentTimeLast, 1.2 * bondPaymentTimeLast, 2. * bondPaymentTimeLast }, new double[] {-hr, 0.11, 0.044, 0.1, 0.12 });
    final IsdaCompliantYieldCurve yc2 = new IsdaCompliantYieldCurve(new double[] {1.1 * bondPaymentTimeLast, 1.2 * bondPaymentTimeLast,
      1.3 * bondPaymentTimeLast, 1.5 * bondPaymentTimeLast, 2.1 * bondPaymentTimeLast }, new double[] {0.1, 0.11, 0.08, 0.12, 0.12 });
    final IsdaCompliantYieldCurve yc3 = new IsdaCompliantYieldCurve(new double[] {bondPaymentTimeLast, 1.2 * bondPaymentTimeLast,
      1.3 * bondPaymentTimeLast, 1.5 * bondPaymentTimeLast, 2.1 * bondPaymentTimeLast }, new double[] {0.12, 0.11, 0.08, 0.12, 0.12 });
    final IsdaCompliantYieldCurve[] ycArr = new IsdaCompliantYieldCurve[] {yc1, yc2, yc3 };
    final int nyc = ycArr.length;

    for (int i = 0; i < nyc; ++i) {
      final double bondPrice = bondPricer.bondPriceForHazardRate(bond, ycArr[i], hr, CdsPriceType.CLEAN);
      final double impliedHr = bondPricer.getHazardRate(bond, ycArr[i], bondPrice, CdsPriceType.CLEAN);
      assertEquals(hr, impliedHr, tol);
    }
  }

  private double defaultFreeBondCleanPrice(final BondAnalytic bond, final IsdaCompliantYieldCurve yieldCurve) {
    double pv = -bond.getAccruedInterest();
    final int n = bond.getnPayments();
    for (int i = 0; i < n; i++) {
      pv += bond.getPaymentAmount(i) * yieldCurve.getDiscountFactor(bond.getPaymentTime(i));
    }
    return pv;
  }

}
