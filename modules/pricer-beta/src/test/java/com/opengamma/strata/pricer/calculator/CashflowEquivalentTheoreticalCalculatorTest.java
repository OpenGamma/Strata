/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calculator;

import static org.testng.Assert.assertEquals;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.finance.fx.FxPayment;
import com.opengamma.strata.finance.rate.FixedRateObservation;
import com.opengamma.strata.finance.rate.IborRateObservation;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.RateAccrualPeriod;
import com.opengamma.strata.finance.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.pricer.calculator.CashflowEquivalentTheoreticalCalculator;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Tests {@link CashflowEquivalentTheoreticalCalculator}.
 */
public class CashflowEquivalentTheoreticalCalculatorTest {

  private static final LocalDate VAlUATION_DATE_1 = LocalDate.of(2015, 6, 5);
  private static final LocalDate VAlUATION_DATE_2 = LocalDate.of(2015, 9, 5);
  private static final double NOTIONAL = 10_000_000;
  private static final LocalDate START_DATE = LocalDate.of(2015, 8, 18);
  private static final LocalDate END_DATE = LocalDate.of(2015, 11, 18);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 12, 18);
  private static final double AF = 0.54;
  /* Fixed rate observation */
  private static final double FIXED_RATE = 0.01;
  private static final FixedRateObservation FIXED_OBS = FixedRateObservation.of(FIXED_RATE);
  private static final RateAccrualPeriod FIXED_ACCRUAL =
      RateAccrualPeriod.builder()
          .startDate(START_DATE)
          .endDate(END_DATE)
          .rateObservation(FIXED_OBS)
          .yearFraction(AF)
          .build();
  private static final PaymentPeriod FIXED_PAY =
      RatePaymentPeriod.builder()
          .accrualPeriods(FIXED_ACCRUAL)
          .notional(NOTIONAL)
          .paymentDate(PAYMENT_DATE)
          .currency(EUR)
          .build();
  /* Ibor rate observation */
  private static final LocalDate IBOR_FIXING_DATE = LocalDate.of(2015, 8, 10);
  private static final IborRateObservation IBOR_OBS = IborRateObservation.of(EUR_EURIBOR_6M, IBOR_FIXING_DATE);
  private static final RateAccrualPeriod IBOR_ACCRUAL =
      RateAccrualPeriod.builder()
          .startDate(START_DATE)
          .endDate(END_DATE)
          .rateObservation(IBOR_OBS)
          .yearFraction(AF)
          .build();
  private static final PaymentPeriod IBOR_PAY =
      RatePaymentPeriod.builder()
          .accrualPeriods(IBOR_ACCRUAL)
          .notional(NOTIONAL)
          .paymentDate(PAYMENT_DATE)
          .currency(EUR)
          .build();
  private static final double IBOR_SPREAD = 0.001;
  private static final double IBOR_GEARING = 2.0;
  private static final RateAccrualPeriod IBOR_ACCRUAL_S =
      RateAccrualPeriod.builder()
          .startDate(START_DATE)
          .endDate(END_DATE)
          .rateObservation(IBOR_OBS)
          .yearFraction(AF)
          .spread(IBOR_SPREAD)
          .gearing(IBOR_GEARING)
          .build();
  private static final PaymentPeriod IBOR_PAY_S =
      RatePaymentPeriod.builder()
          .accrualPeriods(IBOR_ACCRUAL_S)
          .notional(NOTIONAL)
          .paymentDate(PAYMENT_DATE)
          .currency(EUR)
          .build();
  /* Pricer */
  private static final CashflowEquivalentTheoreticalCalculator CFEC = CashflowEquivalentTheoreticalCalculator.DEFAULT;
  /* Rate provider */
  private static final ImmutableRatesProvider RATES_1 = 
      ImmutableRatesProvider.builder()
      .valuationDate(VAlUATION_DATE_1)
      .dayCount(ACT_360)
      .build();
  private static final ImmutableRatesProvider RATES_2 = 
      ImmutableRatesProvider.builder()
      .valuationDate(IBOR_FIXING_DATE)
      .dayCount(ACT_360)
      .build();
  private static final double IBOR_FIXING_VALUE = 0.02;
  private static final LocalDateDoubleTimeSeries TS_EURIBOR6M = 
      LocalDateDoubleTimeSeries.builder().put(IBOR_FIXING_DATE, IBOR_FIXING_VALUE).build();
  private static final Map<Index, LocalDateDoubleTimeSeries> MAP_TS = new HashMap<>();
  static {
    MAP_TS.put(EUR_EURIBOR_6M, TS_EURIBOR6M);
  }
  private static final Map<Index, Curve> MAP_IND_CURVE = new HashMap<>();
  private static final Curve DUMMY_CURVE = ConstantNodalCurve.of("EUR-EURIBOR6M", 0.0);
  static {
    MAP_IND_CURVE.put(EUR_EURIBOR_6M, DUMMY_CURVE);
  }  
  private static final ImmutableRatesProvider RATES_3 = 
      ImmutableRatesProvider.builder()
      .valuationDate(VAlUATION_DATE_2)
      .timeSeries(MAP_TS)
      .dayCount(ACT_360)
      .indexCurves(MAP_IND_CURVE)
      .build();
  /* Tolerance */
  private static final double TOLERANCE_CF = 1.0E-2;
  
  @Test
  public void test_FixedRateObservation() {
    List<FxPayment> cfeComputed = CFEC.cashFlowEquivalent(FIXED_PAY, RATES_1);
    assertEquals(cfeComputed.size(), 1, "CFE - Fixed");
    FxPayment cfePayment = cfeComputed.get(0);
    assertEquals(cfePayment.getDate(), PAYMENT_DATE);
    assertEquals(cfePayment.getCurrency(), EUR);
    assertEquals(cfePayment.getAmount(), NOTIONAL * FIXED_RATE * AF, TOLERANCE_CF);
  }
  
  @Test
  public void test_IborRateObservation_beforeFixing() {
    List<FxPayment> cfeComputed = CFEC.cashFlowEquivalent(IBOR_PAY, RATES_1);
    assertEquals(cfeComputed.size(), 2, "CFE - Ibor before fixing");
    FxPayment cfePayment1 = cfeComputed.get(0);
    assertEquals(cfePayment1.getDate(), START_DATE);
    assertEquals(cfePayment1.getCurrency(), EUR);
    assertEquals(cfePayment1.getAmount(), NOTIONAL, TOLERANCE_CF);
    FxPayment cfePayment2 = cfeComputed.get(1);
    assertEquals(cfePayment2.getDate(), END_DATE);
    assertEquals(cfePayment2.getCurrency(), EUR);
    assertEquals(cfePayment2.getAmount(), -NOTIONAL, TOLERANCE_CF);
  }
  
  @Test
  public void test_IborRateObservation_beforeFixing_gearingSpread() {
    List<FxPayment> cfeComputed = CFEC.cashFlowEquivalent(IBOR_PAY_S, RATES_1);
    assertEquals(cfeComputed.size(), 2, "CFE - Ibor before fixing");
    FxPayment cfePayment1 = cfeComputed.get(0);
    assertEquals(cfePayment1.getDate(), START_DATE);
    assertEquals(cfePayment1.getCurrency(), EUR);
    assertEquals(cfePayment1.getAmount(), IBOR_GEARING * NOTIONAL, TOLERANCE_CF);
    FxPayment cfePayment2 = cfeComputed.get(1);
    assertEquals(cfePayment2.getDate(), END_DATE);
    assertEquals(cfePayment2.getCurrency(), EUR);
    assertEquals(cfePayment2.getAmount(), - IBOR_GEARING * NOTIONAL + NOTIONAL * IBOR_SPREAD * AF, TOLERANCE_CF);
  }
  
  @Test
  public void test_IborRateObservation_onFixing() {
    List<FxPayment> cfeComputed = CFEC.cashFlowEquivalent(IBOR_PAY, RATES_2);
    assertEquals(cfeComputed.size(), 2, "CFE - Ibor on fixing");
    FxPayment cfePayment1 = cfeComputed.get(0);
    assertEquals(cfePayment1.getDate(), START_DATE);
    assertEquals(cfePayment1.getCurrency(), EUR);
    assertEquals(cfePayment1.getAmount(), NOTIONAL, TOLERANCE_CF);
    FxPayment cfePayment2 = cfeComputed.get(1);
    assertEquals(cfePayment2.getDate(), END_DATE);
    assertEquals(cfePayment2.getCurrency(), EUR);
    assertEquals(cfePayment2.getAmount(), -NOTIONAL, TOLERANCE_CF);
  }

  @Test
  public void test_IborRateObservation_onFixing_gearingSpread() {
    List<FxPayment> cfeComputed = CFEC.cashFlowEquivalent(IBOR_PAY_S, RATES_2);
    assertEquals(cfeComputed.size(), 2, "CFE - Ibor on fixing");
    FxPayment cfePayment1 = cfeComputed.get(0);
    assertEquals(cfePayment1.getDate(), START_DATE);
    assertEquals(cfePayment1.getCurrency(), EUR);
    assertEquals(cfePayment1.getAmount(), IBOR_GEARING * NOTIONAL, TOLERANCE_CF);
    FxPayment cfePayment2 = cfeComputed.get(1);
    assertEquals(cfePayment2.getDate(), END_DATE);
    assertEquals(cfePayment2.getCurrency(), EUR);
    assertEquals(cfePayment2.getAmount(), -IBOR_GEARING * NOTIONAL + NOTIONAL * IBOR_SPREAD * AF, TOLERANCE_CF);
  }
  
  @Test
  public void test_IborRateObservation_afterFixing() {
    List<FxPayment> cfeComputed = CFEC.cashFlowEquivalent(IBOR_PAY, RATES_3);
    assertEquals(cfeComputed.size(), 1, "CFE - Ibor after fixing");
    FxPayment cfePayment = cfeComputed.get(0);
    assertEquals(cfePayment.getDate(), PAYMENT_DATE);
    assertEquals(cfePayment.getCurrency(), EUR);
    assertEquals(cfePayment.getAmount(), NOTIONAL * IBOR_FIXING_VALUE * AF, TOLERANCE_CF);
  }
  
  @Test
  public void test_IborRateObservation_afterFixing_gearingSpread() {
    List<FxPayment> cfeComputed = CFEC.cashFlowEquivalent(IBOR_PAY_S, RATES_3);
    assertEquals(cfeComputed.size(), 1, "CFE - Ibor after fixing");
    FxPayment cfePayment = cfeComputed.get(0);
    assertEquals(cfePayment.getDate(), PAYMENT_DATE);
    assertEquals(cfePayment.getCurrency(), EUR);
    assertEquals(cfePayment.getAmount(), NOTIONAL * (IBOR_GEARING * IBOR_FIXING_VALUE + IBOR_SPREAD) * AF, TOLERANCE_CF);
  }
  
  // TODO: OIS
  
}
