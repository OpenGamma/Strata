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

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurveBuilder.ArbitrageHandling;

/**
 * Test.
 */
@Test
public class ExtendedCreditCurveCalibrationTest extends com.opengamma.strata.pricer.impl.credit.isda.CreditCurveCalibrationTest {
  private static final CdsAnalyticFactory CDS_FACTORY = new CdsAnalyticFactory();
  private static final Period[] PILLARS = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(2),
      Period.ofYears(3), Period.ofYears(4), Period.ofYears(5), Period.ofYears(7),
      Period.ofYears(10)};

  private static final SuperFastCreditCurveBuilder BUILDER_ISDA = new SuperFastCreditCurveBuilder(ORIGINAL_ISDA);

  @Test
  public void test() {
    testCalibrationAgainstISDA(BUILDER_ISDA, 1e-14);
    //TODO adjust the logic to match the incorrect Markit `fix'
  }

  @SuppressWarnings("unused")
  @Test
  public void speedTest() {
    final LocalDate tradeDate = LocalDate.of(2013, Month.SEPTEMBER, 5);
    final LocalDate spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 1);
    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y",
        "10Y", "12Y", "15Y", "20Y", "25Y", "30Y"};
    final String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S",
        "S", "S", "S", "S", "S"};
    final double[] rates = new double[] {0.004919, 0.005006, 0.00515, 0.005906, 0.008813, 0.0088, 0.01195, 0.01534, 0.01836,
        0.02096, 0.02322, 0.02514, 0.02673, 0.02802, 0.02997, 0.0318, 0.03331,
        0.03383, 0.034};
    final IsdaCompliantYieldCurve yieldCurve = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments,
        rates, ACT_ACT_ISDA, ACT_ACT_ISDA, Period.ofMonths(6));

    final CdsAnalytic[] cds = CDS_FACTORY.makeImmCds(tradeDate, PILLARS);
    final int n = PILLARS.length;
    final double[] spreads = new double[] {0.01, 0.012, 0.015, 0.02, 0.023, 0.021, 0.02, 0.019};
    ArgChecker.isTrue(n == spreads.length, "spreads wrong length");

    final CreditCurveCalibrator calibrator1 = new CreditCurveCalibrator(cds, yieldCurve);
    final IsdaCompliantCreditCurve cc1 = calibrator1.calibrate(spreads);
    final FastCreditCurveBuilder calibrator2 = new FastCreditCurveBuilder();
    final IsdaCompliantCreditCurve cc2 = calibrator2.calibrateCreditCurve(cds, spreads, yieldCurve);
    for (int i = 0; i < n; i++) {
      assertEquals(cc2.getZeroRateAtIndex(i), cc1.getZeroRateAtIndex(i), 1e-15);
    }

    final MultiCdsAnalytic multiCDS = CDS_FACTORY.makeMultiImmCds(tradeDate, PILLARS);
    final CreditCurveCalibrator calibrator3 = new CreditCurveCalibrator(multiCDS, yieldCurve);
    final IsdaCompliantCreditCurve cc3 = calibrator3.calibrate(spreads);
    for (int i = 0; i < n; i++) {
      assertEquals(cc1.getZeroRateAtIndex(i), cc3.getZeroRateAtIndex(i), 1e-15);
    }

    final int warmups = 200;
    final int hotspots = 1000;

    for (int i = 0; i < warmups; i++) {
      final IsdaCompliantCreditCurve cc1a = calibrator1.calibrate(spreads);
      final IsdaCompliantCreditCurve cc2a = calibrator2.calibrateCreditCurve(cds, spreads, yieldCurve);
    }

    if (hotspots > 0) {
      long t0 = System.nanoTime();
      for (int i = 0; i < hotspots; i++) {
        final IsdaCompliantCreditCurve cc1a = calibrator1.calibrate(spreads);
      }
      long t1 = System.nanoTime();

      t0 = System.nanoTime();
      for (int i = 0; i < hotspots; i++) {
        final IsdaCompliantCreditCurve cc2a = calibrator2.calibrateCreditCurve(cds, spreads, yieldCurve);
      }
      t1 = System.nanoTime();
    }

  }

  @SuppressWarnings("unused")
  @Test
  public void test2() {
    final LocalDate tradeDate = LocalDate.of(2013, Month.SEPTEMBER, 5);
    final LocalDate spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 1);
    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y",
        "10Y", "12Y", "15Y", "20Y", "25Y", "30Y"};
    final String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S",
        "S", "S", "S", "S", "S"};
    final double[] rates = new double[] {0.004919, 0.005006, 0.00515, 0.005906, 0.008813, 0.0088, 0.01195, 0.01534, 0.01836,
        0.02096, 0.02322, 0.02514, 0.02673, 0.02802, 0.02997, 0.0318, 0.03331,
        0.03383, 0.034};
    final IsdaCompliantYieldCurve yieldCurve = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments,
        rates, ACT_ACT_ISDA, ACT_ACT_ISDA, Period.ofMonths(6));

    final LocalDate[] maturities = new LocalDate[] {LocalDate.of(2013, Month.OCTOBER, 30),
        LocalDate.of(2014, Month.SEPTEMBER, 20), LocalDate.of(2015, Month.JUNE, 10),
        LocalDate.of(2016, Month.SEPTEMBER, 5), LocalDate.of(2017, Month.OCTOBER, 1), LocalDate.of(2018, Month.DECEMBER, 30),
        LocalDate.of(2020, Month.JANUARY, 12),
        LocalDate.of(2023, Month.OCTOBER, 30)};

    final LocalDate effective = LocalDate.of(2013, Month.AUGUST, 1);
    final CdsAnalytic[] cds = CDS_FACTORY.makeCds(tradeDate, effective, maturities);

    final int n = cds.length;
    final double[] spreads = new double[] {0.01, 0.012, 0.015, 0.02, 0.023, 0.021, 0.02, 0.019};
    ArgChecker.isTrue(n == spreads.length, "spreads wrong length");

    final CreditCurveCalibrator calibrator1 = new CreditCurveCalibrator(cds, yieldCurve, MARKIT_FIX, ArbitrageHandling.Ignore);
    final IsdaCompliantCreditCurve cc1 = calibrator1.calibrate(spreads);
    final FastCreditCurveBuilder calibrator2 = new FastCreditCurveBuilder(MARKIT_FIX);
    final IsdaCompliantCreditCurve cc2 = calibrator2.calibrateCreditCurve(cds, spreads, yieldCurve);
    for (int i = 0; i < n; i++) {
      assertEquals(cc2.getZeroRateAtIndex(i), cc1.getZeroRateAtIndex(i), 1e-13);
    }

    final int warmups = 200;
    final int hotspots = 1000;

    for (int i = 0; i < warmups; i++) {
      final IsdaCompliantCreditCurve cc1a = calibrator1.calibrate(spreads);
      final IsdaCompliantCreditCurve cc2a = calibrator2.calibrateCreditCurve(cds, spreads, yieldCurve);
    }

    if (hotspots > 0) {
      long t0 = System.nanoTime();
      for (int i = 0; i < hotspots; i++) {

        final IsdaCompliantCreditCurve cc1a = calibrator1.calibrate(spreads);
      }
      long t1 = System.nanoTime();

      t0 = System.nanoTime();
      for (int i = 0; i < hotspots; i++) {
        final IsdaCompliantCreditCurve cc2a = calibrator2.calibrateCreditCurve(cds, spreads, yieldCurve);
      }
      t1 = System.nanoTime();
    }
  }

}
