/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertEquals;

import java.time.LocalDate;
import java.time.Month;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class PufCreditCurveCalibrationTest extends IsdaBaseTest {
  private static final MarketQuoteConverter PUF_CONVERTER = new MarketQuoteConverter();

  protected static final double NOTIONAL = 1e7;
  private static final LocalDate TRADE_DATE = LocalDate.of(2013, Month.APRIL, 10); //Today
  private static final LocalDate EFFECTIVE_DATE = TRADE_DATE.plusDays(1); // AKA stepin date
  private static final LocalDate CASH_SETTLE_DATE = DEFAULT_CALENDAR.shift(TRADE_DATE, 3); // AKA valuation date
  private static final LocalDate STARTDATE = LocalDate.of(2013, Month.MARCH, 20);//last IMM date before TRADE_DATE;

  private static final double COUPON = 500;
  private static final double[] PUF = new double[] {0.32, 0.69, 1.32, 1.79, 2.36, 3.01, 3.7, 4.39, 5.02, 5.93, 6.85, 7.76, 8.67, 9.6, 10.53, 11.45, 12.33, 13.29, 14.26, 15.2, 16.11, 16.62, 17.12,
    17.62, 18.09, 18.55, 19, 19.44, 19.87, 20.33, 20.79, 21.24, 21.67, 22.04, 22.41, 22.77, 23.12, 23.46, 23.8, 24.14, 24.46 };
  private static final double[] PUF_FRAC;
  private static final double[] PAR_SPREADS;

  private static final CdsAnalytic[] CURVE_CDS;
  private static final String[] MATURITY_STRINGS = new String[] {"20/06/2013", "20/09/2013", "20/12/2013", "20/03/2014", "20/06/2014", "20/09/2014", "20/12/2014", "20/03/2015", "20/06/2015",
    "20/09/2015", "20/12/2015", "20/03/2016", "20/06/2016", "20/09/2016", "20/12/2016", "20/03/2017", "20/06/2017", "20/09/2017", "20/12/2017", "20/03/2018", "20/06/2018", "20/09/2018", "20/12/2018",
    "20/03/2019", "20/06/2019", "20/09/2019", "20/12/2019", "20/03/2020", "20/06/2020", "20/09/2020", "20/12/2020", "20/03/2021", "20/06/2021", "20/09/2021", "20/12/2021", "20/03/2022", "20/06/2022",
    "20/09/2022", "20/12/2022", "20/03/2023", "20/06/2023" };
  private static final LocalDate[] MATURITIES = parseDateStrings(MATURITY_STRINGS);

  //yield curve
  private static final LocalDate SPOT_DATE = LocalDate.of(2013, Month.APRIL, 12);
  private static final String[] YIELD_CURVE_POINTS = new String[] {"1M", "2M", "3M", "6M", "9M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "12Y", "15Y", "20Y", "25Y", "30Y" };
  private static final String[] YIELD_CURVE_INSTRUMENTS = new String[] {"M", "M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
  private static final double[] YIELD_CURVE_RATES = new double[] {0.001993, 0.002403, 0.002781, 0.004419, 0.005782, 0.00721, 0.003795, 0.00483, 0.00658, 0.008815, 0.01127, 0.01362, 0.01573, 0.017605,
    0.019215, 0.02195, 0.02468, 0.026975, 0.0281, 0.02874 };
  private static final IsdaCompliantYieldCurve YIELD_CURVE = makeYieldCurve(TRADE_DATE, SPOT_DATE, YIELD_CURVE_POINTS, YIELD_CURVE_INSTRUMENTS, YIELD_CURVE_RATES);

  static {
    final int n = PUF.length;
    PUF_FRAC = new double[n];
    CURVE_CDS = new CdsAnalytic[n];
    for (int i = 0; i < n; i++) {
      PUF_FRAC[i] = PUF[i] * ONE_PC;
      CURVE_CDS[i] = new CdsAnalytic(TRADE_DATE, EFFECTIVE_DATE, CASH_SETTLE_DATE, STARTDATE, MATURITIES[i], PROCTECTION_START, PAYMENT_INTERVAL, STUB, PAY_ACC_ON_DEFAULT, RECOVERY_RATE);
    }

    PAR_SPREADS = PUF_CONVERTER.pufToParSpreads(CURVE_CDS, COUPON * ONE_BP, YIELD_CURVE, PUF_FRAC);
  }

  @Test
  public void test() {
    final double coupon = COUPON * ONE_BP;
    final int n = PUF.length;
    final CdsQuoteConvention[] puf = new CdsQuoteConvention[n];
    final CdsQuoteConvention[] parSpread = new CdsQuoteConvention[n];
    for (int i = 0; i < n; i++) {
      puf[i] = new PointsUpFront(coupon, PUF_FRAC[i]);
      parSpread[i] = new CdsParSpread(PAR_SPREADS[i]);
    }

    final IsdaCompliantCreditCurve pufCurve = CREDIT_CURVE_BUILDER.calibrateCreditCurve(CURVE_CDS, puf, YIELD_CURVE);
    final IsdaCompliantCreditCurve psCurve = CREDIT_CURVE_BUILDER.calibrateCreditCurve(CURVE_CDS, parSpread, YIELD_CURVE);

    for (int i = 0; i < n; i++) {
      assertEquals(pufCurve.getZeroRateAtIndex(i), psCurve.getZeroRateAtIndex(i), 1e-15);
    }
  }

}
