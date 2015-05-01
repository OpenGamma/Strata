/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.finance.rate.swap.FixedRateCalculation;
import com.opengamma.strata.finance.rate.swap.IborRateCalculation;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.PaymentSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapProductPricer;

public class CurveGammaCalculatorTest {

  /* Data */
  private static final ImmutableRatesProvider SINGLE = RatesProviderDataSets.USD_SINGLE;
  /* Conventions */
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY);
  private static final BusinessDayAdjustment BDA_P = BusinessDayAdjustment.of(PRECEDING, USNY);
  /* Instrument */
  private static final Swap SWAP = swapUsd(LocalDate.of(2016, 6, 30), LocalDate.of(2022, 6, 30), RECEIVE,
      NotionalSchedule.of(USD, 10_000_000), 0.01);
  /* Calculators and pricers */
  private static final DiscountingSwapProductPricer PRICER_SWAP = DiscountingSwapProductPricer.DEFAULT;
  private static final CurveGammaCalculator GAMMA_CAL = CurveGammaCalculator.DEFAULT;
  /* Constants */
  private static final double TOLERANCE_GAMMA = 1.0E+3;
  
  @Test
  public void semiParallelGamma() {
    double shift = 1.0E-5;
    ImmutableMap<Currency, YieldAndDiscountCurve> dsc = SINGLE.getDiscountCurves();
    ImmutableMap<Index, YieldAndDiscountCurve> fwd = SINGLE.getIndexCurves();
    // Check all curves are the same
    YieldAndDiscountCurve single = dsc.entrySet().iterator().next().getValue();
    InterpolatedDoublesCurve curve = GAMMA_CAL.checkInterpolated(single);
    double[] y = curve.getYDataAsPrimitive();
    double[] x = curve.getXDataAsPrimitive();
    int nbNode = y.length;
    double[] gammaExpected = new double[nbNode];
    for (int i = 0; i < nbNode; i++) {
      double[][][] yBumped = new double[2][2][nbNode];
      double[][] pv = new double[2][2];
      for(int pmi = 0; pmi<2; pmi++ ) {
        for(int pmP = 0; pmP<2; pmP++ ) {
          yBumped[pmi][pmP] = y.clone();
          yBumped[pmi][pmP][i] += (pmi == 0?1.0:-1.0)*shift;
          for(int j=0; j<nbNode; j++) {
            yBumped[pmi][pmP][j] += (pmP == 0?1.0:-1.0)*shift;
          }
          YieldAndDiscountCurve curveBumped = new YieldCurve(curve.getName(),
              new InterpolatedDoublesCurve(x, yBumped[pmi][pmP], curve.getInterpolator(), true));
          Map<Currency, YieldAndDiscountCurve> dscBumped = new HashMap<>();
          for (Entry<Currency, YieldAndDiscountCurve> entry : dsc.entrySet()) {
            dscBumped.put(entry.getKey(), curveBumped);
          }
          Map<Index, YieldAndDiscountCurve> fwdBumped = new HashMap<>(fwd);
          for (Entry<Index, YieldAndDiscountCurve> entry : fwd.entrySet()) {
            fwdBumped.put(entry.getKey(), curveBumped);
          }
          ImmutableRatesProvider providerBumped = SINGLE.toBuilder().discountCurves(dsc).indexCurves(fwdBumped).build();
          pv[pmi][pmP] = PRICER_SWAP.presentValue(SWAP, providerBumped).getAmount(USD).getAmount();
        }
      }
      gammaExpected[i] = (pv[1][1] - pv[1][0] - pv[0][1] + pv[0][0]) / (4 * shift * shift);
    }
    double[] gammaComputed = GAMMA_CAL.calculateSemiParallelGamma(SINGLE,
        (p) -> PRICER_SWAP.presentValueSensitivity(SWAP, SINGLE).build());
    for (int i = 0; i < nbNode; i++) {
      assertEquals(gammaComputed[i], gammaExpected[i], TOLERANCE_GAMMA);
    }
  }
  

  
  //-------------------------------------------------------------------------
  // swap USD standard conventions- TODO: replace by a template when available
  private static Swap swapUsd(LocalDate start, LocalDate end, PayReceive payReceive, 
      NotionalSchedule notional, double fixedRate) {
    RateCalculationSwapLeg fixedLeg = 
        fixedLeg(start, end, Frequency.P6M, payReceive, notional, fixedRate, StubConvention.SHORT_INITIAL);
    RateCalculationSwapLeg iborLeg = 
        iborLeg(start, end, USD_LIBOR_3M, (payReceive==PAY)?RECEIVE:PAY, notional, StubConvention.SHORT_INITIAL);
    return Swap.of(fixedLeg, iborLeg);
  }
  
  
  // fixed rate leg
  private static RateCalculationSwapLeg fixedLeg(
      LocalDate start, LocalDate end, Frequency frequency,
      PayReceive payReceive, NotionalSchedule notional, double fixedRate, StubConvention stubConvention) {

    return RateCalculationSwapLeg.builder()
        .payReceive(payReceive)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(start)
            .endDate(end)
            .frequency(frequency)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(stubConvention)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(frequency)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_U_360)
            .rate(ValueSchedule.of(fixedRate))
            .build())
        .build();
  }
  
  // fixed rate leg
  private static RateCalculationSwapLeg iborLeg(
      LocalDate start, LocalDate end, IborIndex index,
      PayReceive payReceive, NotionalSchedule notional, StubConvention stubConvention) {
    Frequency freq = Frequency.of(index.getTenor().getPeriod());
    return RateCalculationSwapLeg.builder()
        .payReceive(payReceive)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(start)
            .endDate(end)
            .frequency(freq)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(stubConvention)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(freq)
            .paymentOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .dayCount(index.getDayCount())
            .index(index)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, index.getFixingCalendar(), BDA_P))
            .build())
        .build();
  }
  
}
