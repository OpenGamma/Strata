/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getIMMDateSet;
import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getNextIMMDate;
import static org.testng.AssertJUnit.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurveBuilder.ArbitrageHandling;

/**
 * Test.
 */
@Test
public class IsdaFixTest extends IsdaBaseTest {

  private static final LocalDate TODAY = LocalDate.of(2011, Month.JUNE, 13);
  private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2011, Month.JUNE, 14); // AKA stepin date
  private static final LocalDate CASH_SETTLE_DATE = LocalDate.of(2011, Month.JUNE, 16); // AKA valuation date
  private static final LocalDate STARTDATE = LocalDate.of(2011, Month.MARCH, 20);

  private static final Period[] TENORS = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3), Period.ofYears(5), Period.ofYears(7), Period.ofYears(10) };
  private static final LocalDate NEXT_IMM = getNextIMMDate(TODAY);
  private static final LocalDate[] PILLAR_DATES = getIMMDateSet(NEXT_IMM, TENORS);
  private static final LocalDate[] MATURITIES = getIMMDateSet(NEXT_IMM, 41);

  //yield curve
  private static final LocalDate SPOT_DATE = LocalDate.of(2011, Month.JUNE, 15);
  private static final String[] YIELD_CURVE_POINTS = new String[] {"1M", "2M", "3M", "6M", "9M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "11Y", "12Y", "15Y", "20Y", "25Y", "30Y" };
  private static final String[] YIELD_CURVE_INSTRUMENTS = new String[] {"M", "M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
  private static final double[] YIELD_CURVE_RATES = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033, 0.02525, 0.02696, 0.02825, 0.02931, 0.03017,
    0.03092, 0.0316, 0.03231, 0.03367, 0.03419, 0.03411, 0.03412 };
  private static final IsdaCompliantYieldCurve YIELD_CURVE = makeYieldCurve(TODAY, SPOT_DATE, YIELD_CURVE_POINTS, YIELD_CURVE_INSTRUMENTS, YIELD_CURVE_RATES, ACT360, D30360, Period.ofYears(1));

  private static final double COUPON = 0.01;
  private static final double[] SPREADS = new double[] {0.008863157, 0.008863157, 0.013304469, 0.017149007, 0.018390364, 0.019472189 };

  //expected - from ISDA c with fix
  private static final double[] EXPECTED_UPFRONT_CHARGE = new double[] {-0.00241075106526235, -0.00269899615991187, -0.00298238427878267, -0.00326282361599637, -0.00354331987012609,
    -0.00214761317825186, -0.000780370416529379, 0.000559049539387898, 0.00191612369893203, 0.00326113246893948, 0.00457374473904669, 0.00585461493530264, 0.0071473732885757, 0.0102187194344682,
    0.0132019892156819, 0.016100015995287, 0.0190120462912064, 0.0218700573302236, 0.0246447451707703, 0.0273685378129013, 0.0300724366917735, 0.0324352888163721, 0.0347298107220322,
    0.0369581573599116, 0.0391958505369267, 0.0413914714774095, 0.0435234698837631, 0.0455936907497847, 0.0476723136393884, 0.0498368066543075, 0.0519373036905721, 0.0539758894867396,
    0.0560213423936182, 0.0580273438957959, 0.0599740156744084, 0.0618839928096648, 0.0637794893777406, 0.0656377709642745, 0.0674405858573872, 0.0691898860799818, 0.0709445474586503 };

  public void test() {
    final IsdaCompliantCreditCurveBuilder curveBuilder = new FastCreditCurveBuilder(MARKIT_FIX, ArbitrageHandling.ZeroHazardRate);
    final AnalyticCdsPricer pricer = new AnalyticCdsPricer(MARKIT_FIX);

    final int nPillars = PILLAR_DATES.length;
    final CdsAnalytic[] pillarCDSS = new CdsAnalytic[nPillars];
    for (int i = 0; i < nPillars; i++) {
      pillarCDSS[i] = new CdsAnalytic(TODAY, EFFECTIVE_DATE, CASH_SETTLE_DATE, STARTDATE, PILLAR_DATES[i], PAY_ACC_ON_DEFAULT, PAYMENT_INTERVAL, STUB, PROCTECTION_START, RECOVERY_RATE);
    }

    final IsdaCompliantCreditCurve creditCurve = curveBuilder.calibrateCreditCurve(pillarCDSS, SPREADS, YIELD_CURVE);

    final int nMat = MATURITIES.length;
    for (int i = 0; i < nMat; i++) {
      final CdsAnalytic cds = new CdsAnalytic(TODAY, EFFECTIVE_DATE, CASH_SETTLE_DATE, STARTDATE, MATURITIES[i], PAY_ACC_ON_DEFAULT, PAYMENT_INTERVAL, STUB, PROCTECTION_START, RECOVERY_RATE);
      final double dPV = pricer.pv(cds, YIELD_CURVE, creditCurve, COUPON, CdsPriceType.DIRTY);
      assertEquals(MATURITIES[i].toString(), EXPECTED_UPFRONT_CHARGE[i], dPV, 1e-15);
    }

  }

}
