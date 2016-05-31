/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendarIds;

/**
 * Test.
 */
@Test
public class CdsJapanTest extends IsdaBaseTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final HolidayCalendar TYO_CAL = HolidayCalendarIds.JPTO.resolve(REF_DATA);
  private static final MarketQuoteConverter CONVERTER = new MarketQuoteConverter();
  private static final CdsAnalyticFactory FACTORY = new CdsAnalyticFactory(0.35).with(TYO_CAL);
  private static final FiniteDifferenceSpreadSensitivityCalculator CS01_CAL = new FiniteDifferenceSpreadSensitivityCalculator();

  public void test() {
    final double coupon = 0.01;
    final double tradeLevel = 0.012;
    final CdsQuotedSpread qs = new CdsQuotedSpread(coupon, tradeLevel);
    final double notional = 1e13;

    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "12Y", "15Y", "20Y", "30Y" };
    final String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
    final double[] rates = new double[] {0.001107, 0.001279, 0.001429, 0.002111, 0.003943, 0.002163, 0.002525, 0.003075, 0.003763, 0.004575, 0.00545, 0.006375, 0.007288, 0.008213, 0.010088, 0.012763,
      0.01585, 0.017925 };

    final LocalDate tradeDate = LocalDate.of(2013, Month.OCTOBER, 16);
    final LocalDate spotDate = TYO_CAL.shift(tradeDate.minusDays(1), 3);

    final IsdaCompliantYieldCurve yieldCurve = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, ACT_ACT_ISDA, Period.ofMonths(6), TYO_CAL);
    final CdsAnalytic cds = FACTORY.makeImmCds(tradeDate, Period.ofYears(5));

    final PointsUpFront puf = CONVERTER.convert(cds, qs, yieldCurve);
    final double accAmt = notional * cds.getAccruedPremium(coupon);
    final double cashSettle = notional * puf.getPointsUpFront() - accAmt;
    final double cs01 = notional * ONE_BP * CS01_CAL.parallelCS01(cds, qs, yieldCurve, ONE_BP);

    assertEquals(27, cds.getAccuredDays());
    assertEquals(7.5e9, accAmt);
    assertEquals("cashSettle", 91814571779.0, cashSettle, 1);
    assertEquals("CS01", 4924458158.0, cs01, 1);
  }

}
