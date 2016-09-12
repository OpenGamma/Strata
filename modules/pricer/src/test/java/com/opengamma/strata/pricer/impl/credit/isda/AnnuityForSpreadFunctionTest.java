/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * 
 */
@Test
public class AnnuityForSpreadFunctionTest {

  private static final LocalDate TRADE_DATE = LocalDate.of(2011, Month.JUNE, 13);
  private static final double[] YIELD_CURVE_RATES = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033, 0.02525, 0.02696, 0.02825, 0.02931, 0.03017,
    0.03092, 0.0316, 0.03231, 0.03367, 0.03419, 0.03411, 0.03412 };

  private static final Period TENOR = Period.ofYears(10);
  private static final HolidayCalendar CALENDAR = HolidayCalendars.SAT_SUN;

  private static final IsdaCompliantYieldCurve YIELD_CURVE;

  static {
    int num;
    final int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12 };
    final int[] swapYears = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20, 25, 30 };
    final int nMoneyMarket = mmMonths.length;
    final int nSwaps = swapYears.length;
    num = nMoneyMarket + nSwaps;

    final IsdaInstrumentTypes[] instrumentTypes = new IsdaInstrumentTypes[num];
    final Period[] tenors = new Period[num];
    for (int i = 0; i < nMoneyMarket; i++) {
      instrumentTypes[i] = IsdaInstrumentTypes.MONEY_MARKET;
      tenors[i] = Period.ofMonths(mmMonths[i]);
    }
    for (int i = nMoneyMarket; i < num; i++) {
      instrumentTypes[i] = IsdaInstrumentTypes.SWAP;
      tenors[i] = Period.ofYears(swapYears[i - nMoneyMarket]);
    }

    final IsdaCompliantYieldCurveBuild builder = new IsdaCompliantYieldCurveBuild(TRADE_DATE, TRADE_DATE.plusDays(2), instrumentTypes, tenors, DayCounts.ACT_360, DayCounts.THIRTY_U_360,
        Period.ofYears(1), DayCounts.ACT_365F, BusinessDayConventions.MODIFIED_FOLLOWING, CALENDAR);
    YIELD_CURVE = builder.build(YIELD_CURVE_RATES);
  }

  /**
   * 
   */
  @Test
  public void shortAccPeriodTest() {
    final Period interval = Period.ofDays(4);

    final CdsAnalytic cds = new CdsAnalytic(
        TRADE_DATE,
        TRADE_DATE.plusDays(1),
        CALENDAR.shift(TRADE_DATE, 3),
        ImmDateLogic.getPrevIMMDate(TRADE_DATE),
        ImmDateLogic.getNextIMMDate(TRADE_DATE).plus(TENOR),
        true,
        interval,
        StubConvention.SHORT_INITIAL,
        true,
        0.4);
    final AnnuityForSpreadContPemiumApproxFunction contPrem = new AnnuityForSpreadContPemiumApproxFunction(cds, YIELD_CURVE);
    final AnnuityForSpreadIsdaFunction isda = new AnnuityForSpreadIsdaFunction(cds, YIELD_CURVE);
    final AnnuityForSpreadApproxFunction approx = new AnnuityForSpreadApproxFunction(cds, YIELD_CURVE);

    final double spread = 100 * 1.e-4;

    final double integralWithTriangle = contPrem.apply(spread);
    final double sumWithCalibrate = isda.apply(spread);
    final double sumWithTriangle = approx.apply(spread);
    final double ref = sumWithCalibrate * 1.e-3;
    assertEquals(sumWithCalibrate, integralWithTriangle, ref);
    assertEquals(sumWithCalibrate, sumWithTriangle, ref);
  }

  /**
   * 
   */
  @Test
  public void shortTenorTest() {
    final Period interval = Period.ofMonths(3);
    final Period tenor = Period.ofMonths(3);

    final CdsAnalytic cds = new CdsAnalytic(
        TRADE_DATE,
        TRADE_DATE.plusDays(1),
        CALENDAR.shift(TRADE_DATE, 3),
        ImmDateLogic.getPrevIMMDate(TRADE_DATE),
        ImmDateLogic.getNextIMMDate(TRADE_DATE).plus(tenor),
        true,
        interval,
        StubConvention.SHORT_INITIAL,
        true,
        0.4);
    final AnnuityForSpreadContPemiumApproxFunction contPrem = new AnnuityForSpreadContPemiumApproxFunction(cds, YIELD_CURVE);
    final AnnuityForSpreadIsdaFunction isda = new AnnuityForSpreadIsdaFunction(cds, YIELD_CURVE);
    final AnnuityForSpreadApproxFunction approx = new AnnuityForSpreadApproxFunction(cds, YIELD_CURVE);

    final double spread = 100 * 1.e-4;

    final double integralWithTriangle = contPrem.apply(spread);
    final double sumWithCalibrate = isda.apply(spread);
    final double sumWithTriangle = approx.apply(spread);
    final double ref = sumWithCalibrate * 1.e-2;
    assertEquals(sumWithCalibrate, integralWithTriangle, ref);
    assertEquals(sumWithCalibrate, sumWithTriangle, ref);
  }
}
