/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertTrue;

import java.time.LocalDate;
import java.time.Month;

import org.testng.annotations.Test;

import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurveBuilder.ArbitrageHandling;

/**
 * Test.
 */
@Test
public class CreditBootstrapFailTest extends IsdaBaseTest {

  protected static final double NOTIONAL = 1e6;
  private static final LocalDate TRADE_DATE = LocalDate.of(2013, Month.MARCH, 13); //Today
  private static final LocalDate EFFECTIVE_DATE = TRADE_DATE.plusDays(1); // AKA stepin date
  private static final LocalDate CASH_SETTLE_DATE = DEFAULT_CALENDAR.shift(TRADE_DATE, 3); // AKA valuation date
  private static final LocalDate STARTDATE = LocalDate.of(2012, Month.DECEMBER, 20);//last IMM date before TRADE_DATE;

  //yield curve
  private static final LocalDate SPOT_DATE = LocalDate.of(2013, Month.APRIL, 12);
  private static final String[] YIELD_CURVE_POINTS = new String[] {"1M", "2M", "3M", "6M", "9M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "12Y", "15Y", "20Y", "30Y" };
  private static final String[] YIELD_CURVE_INSTRUMENTS = new String[] {"M", "M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
  private static final double[] YIELD_CURVE_RATES = new double[] {0.00117, 0.00163, 0.00201, 0.00326, 0.00436, 0.00542, 0.00453, 0.00593, 0.00762, 0.00952, 0.01141, 0.0132, 0.01486, 0.01635, 0.01768,
    0.01992, 0.02221, 0.02376, 0.02443 };
  private static final IsdaCompliantYieldCurve YIELD_CURVE = makeYieldCurve(TRADE_DATE, SPOT_DATE, YIELD_CURVE_POINTS, YIELD_CURVE_INSTRUMENTS, YIELD_CURVE_RATES);

  private static final LocalDate[] PILLAR_DATES = new LocalDate[] {LocalDate.of(2013, Month.DECEMBER, 20), LocalDate.of(2014, Month.JUNE, 20), LocalDate.of(2016, Month.JUNE, 20),
    LocalDate.of(2018, Month.JUNE, 20), LocalDate.of(2020, Month.JUNE, 20), LocalDate.of(2023, Month.JUNE, 20) };

  private static final double[] PAR_SPREADS = new double[] {0.0181398, 0.0181398, 0.027096, 0.0279819, 0.0357239, 0.0273206 };

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void failTest() {
    final IsdaCompliantCreditCurveBuilder creditCurveBuilder = new FastCreditCurveBuilder(MARKIT_FIX, ArbitrageHandling.Fail);
    final int m = PILLAR_DATES.length;
    final CdsAnalytic[] curveCDSs = new CdsAnalytic[m];
    for (int i = 0; i < m; i++) {
      curveCDSs[i] = new CdsAnalytic(TRADE_DATE, EFFECTIVE_DATE, CASH_SETTLE_DATE, STARTDATE, PILLAR_DATES[i], PAY_ACC_ON_DEFAULT, PAYMENT_INTERVAL, STUB, PROCTECTION_START, RECOVERY_RATE);
    }
    @SuppressWarnings("unused")
    final IsdaCompliantCreditCurve creditCurve = creditCurveBuilder.calibrateCreditCurve(curveCDSs, PAR_SPREADS, YIELD_CURVE);
  }

  @Test
  //(enabled = false)
  public void test() {
    final IsdaCompliantCreditCurveBuilder creditCurveBuilder = new FastCreditCurveBuilder(MARKIT_FIX, ArbitrageHandling.ZeroHazardRate);

    final int m = PILLAR_DATES.length;

    final CdsAnalytic[] curveCDSs = new CdsAnalytic[m];
    for (int i = 0; i < m; i++) {
      curveCDSs[i] = new CdsAnalytic(TRADE_DATE, EFFECTIVE_DATE, CASH_SETTLE_DATE, STARTDATE, PILLAR_DATES[i], PAY_ACC_ON_DEFAULT, PAYMENT_INTERVAL, STUB, PROCTECTION_START, RECOVERY_RATE);
    }
    final IsdaCompliantCreditCurve creditCurve = creditCurveBuilder.calibrateCreditCurve(curveCDSs, PAR_SPREADS, YIELD_CURVE);

    for (int i = 0; i < 200; i++) {
      final double t = 12.0 * i / 200.;
      final double lambda = creditCurve.getForwardRate(t);
      assertTrue(lambda >= 0);
    }
  }
}
