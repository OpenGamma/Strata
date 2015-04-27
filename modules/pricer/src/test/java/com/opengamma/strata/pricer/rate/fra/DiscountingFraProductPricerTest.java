/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.fra;

import static com.opengamma.strata.pricer.rate.fra.FraDummyData.FRA;
import static com.opengamma.strata.pricer.rate.fra.FraDummyData.FRA_AFMA;
import static com.opengamma.strata.pricer.rate.fra.FraDummyData.FRA_NONE;
import static com.opengamma.strata.pricer.rate.fra.FraDummyData.FRA_TRADE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.RateObservation;
import com.opengamma.strata.finance.rate.fra.ExpandedFra;
import com.opengamma.strata.finance.rate.fra.Fra;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.sensitivity.ZeroRateSensitivity;

/**
 * Test.
 */
@Test
public class DiscountingFraProductPricerTest {

  private static final LocalDate VALUATION_DATE = LocalDate.of(2014, 1, 22);
  private static final double TOLERANCE = 1E-12;

  /**
   * Test future value for ISDA FRA Discounting method. 
   */
  public void test_futureValue_ISDA() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    double forwardRate = 0.02;
    ExpandedFra fraExp = FRA.expand();
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    double fixedRate = FRA.getFixedRate();
    double yearFraction = fraExp.getYearFraction();
    double notional = fraExp.getNotional();
    when(mockObs.rate(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), mockProv))
        .thenReturn(forwardRate);
    CurrencyAmount computed = test.futureValue(fraExp, mockProv);
    double expected = notional * yearFraction * (forwardRate - fixedRate) / (1.0 + yearFraction * forwardRate);
    assertEquals(computed.getAmount(), expected, TOLERANCE);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.futureValue(FRA_TRADE, mockProv), test.futureValue(fraExp, mockProv));
  }

  /**
   * Test future value for NONE FRA Discounting method. 
   */
  public void test_futureValue_NONE() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    double forwardRate = 0.02;
    ExpandedFra fraExp = FRA_NONE.expand();
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    double fixedRate = FRA_NONE.getFixedRate();
    double yearFraction = fraExp.getYearFraction();
    double notional = fraExp.getNotional();
    when(mockObs.rate(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), mockProv))
        .thenReturn(forwardRate);
    CurrencyAmount computed = test.futureValue(fraExp, mockProv);
    double expected = notional * yearFraction * (forwardRate - fixedRate);
    assertEquals(computed.getAmount(), expected, TOLERANCE);
  }

  /**
   * Test future value for AFMA FRA Discounting method. 
   */
  public void test_futureValue_AFMA() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    double forwardRate = 0.02;
    ExpandedFra fraExp = FRA_AFMA.expand();
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    double fixedRate = FRA_AFMA.getFixedRate();
    double yearFraction = fraExp.getYearFraction();
    double notional = fraExp.getNotional();
    when(mockObs.rate(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), mockProv))
        .thenReturn(forwardRate);
    CurrencyAmount computed = test.futureValue(fraExp, mockProv);
    double expected = -notional * (1.0 / (1.0 + yearFraction * forwardRate) - 1.0 / (1.0 + yearFraction * fixedRate));
    assertEquals(computed.getAmount(), expected, TOLERANCE);
  }

  /**
   * Test FRA paying in the past.
   */
  public void test_futureValue_inPast() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    ExpandedFra fraExp = FRA.expand().toBuilder().paymentDate(VALUATION_DATE.minusDays(1)).build();
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    CurrencyAmount computed = test.futureValue(fraExp, mockProv);
    assertEquals(computed.getAmount(), 0d, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  /**
   * Test present value for ISDA FRA Discounting method. 
   */
  public void test_presentValue_ISDA() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    double forwardRate = 0.02;
    double discountFactor = 0.98d;
    ExpandedFra fraExp = FRA.expand();
    Currency currency = FRA.getCurrency();
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    when(mockObs.rate(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), mockProv))
        .thenReturn(forwardRate);
    when(mockProv.discountFactor(currency, fraExp.getPaymentDate())).thenReturn(discountFactor);
    CurrencyAmount pvComputed = test.presentValue(fraExp, mockProv);
    CurrencyAmount pvExpected = test.futureValue(fraExp, mockProv).multipliedBy(discountFactor);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.presentValue(FRA_TRADE, mockProv), test.presentValue(fraExp, mockProv));
  }

  /**
   * Test present value for NONE FRA Discounting method. 
   */
  public void test_presentValue_NONE() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    double forwardRate = 0.02;
    double discountFactor = 0.98d;
    ExpandedFra fraExp = FRA_NONE.expand();
    Currency currency = FRA_NONE.getCurrency();
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    when(mockObs.rate(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), mockProv))
        .thenReturn(forwardRate);
    when(mockProv.discountFactor(currency, fraExp.getPaymentDate())).thenReturn(discountFactor);
    CurrencyAmount pvComputed = test.presentValue(fraExp, mockProv);
    CurrencyAmount pvExpected = test.futureValue(fraExp, mockProv).multipliedBy(discountFactor);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE);
  }

  /**
   * Test present value for ISDA FRA Discounting method. 
   */
  public void test_presentValue_AFMA() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    double forwardRate = 0.02;
    double discountFactor = 0.98d;
    ExpandedFra fraExp = FRA_AFMA.expand();
    Currency currency = FRA_AFMA.getCurrency();
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    when(mockObs.rate(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), mockProv))
        .thenReturn(forwardRate);
    when(mockProv.discountFactor(currency, fraExp.getPaymentDate()))
        .thenReturn(discountFactor);
    CurrencyAmount pvComputed = test.presentValue(fraExp, mockProv);
    CurrencyAmount pvExpected = test.futureValue(fraExp, mockProv).multipliedBy(discountFactor);
    assertEquals(pvComputed.getAmount(), pvExpected.getAmount(), TOLERANCE);
  }

  /**
   * Test FRA paying in the past.
   */
  public void test_presentValue_inPast() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    ExpandedFra fraExp = FRA.expand().toBuilder().paymentDate(VALUATION_DATE.minusDays(1)).build();
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    CurrencyAmount computed = test.presentValue(fraExp, mockProv);
    assertEquals(computed.getAmount(), 0d, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  /**
   * Test future value sensitivity for ISDA FRA discounting method. 
   */
  public void test_futureValueSensitivity_ISDA() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    ExpandedFra fraExp = FRA.expand();
    double forwardRate = 0.05;
    LocalDate fixingDate = FRA.getStartDate();
    PointSensitivityBuilder sens = IborRateSensitivity.of(FRA.getIndex(), fixingDate, 1d);
    when(mockObs.rateSensitivity(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), mockProv))
        .thenReturn(sens);
    when(mockObs.rate(fraExp.getFloatingRate(), FRA.getStartDate(), FRA.getEndDate(), mockProv))
        .thenReturn(forwardRate);
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    PointSensitivities sensitivity = test.futureValueSensitivity(fraExp, mockProv);
    double eps = 1.e-7;
    double fdSense = futureValueFwdSensitivity(FRA, forwardRate, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 1);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA.getIndex());
    assertEquals(sensitivity0.getFixingDate(), fixingDate);
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA.getNotional() * eps);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.futureValueSensitivity(FRA_TRADE, mockProv), test.futureValueSensitivity(fraExp, mockProv));
  }

  /**
   * Test future value sensitivity for NONE FRA discounting method.
   */
  public void test_futureValueSensitivity_NONE() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    ExpandedFra fraExp = FRA_NONE.expand();
    double forwardRate = 0.035;
    LocalDate fixingDate = FRA_NONE.getStartDate();
    PointSensitivityBuilder sens = IborRateSensitivity.of(FRA_NONE.getIndex(), fixingDate, 1d);
    when(mockObs.rateSensitivity(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), mockProv))
        .thenReturn(sens);
    when(mockObs.rate(fraExp.getFloatingRate(), FRA_NONE.getStartDate(), FRA_NONE.getEndDate(), mockProv))
        .thenReturn(forwardRate);
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    PointSensitivities sensitivity = test.futureValueSensitivity(fraExp, mockProv);
    double eps = 1.e-7;
    double fdSense = futureValueFwdSensitivity(FRA_NONE, forwardRate, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 1);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA_NONE.getIndex());
    assertEquals(sensitivity0.getFixingDate(), fixingDate);
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA_NONE.getNotional() * eps);
  }

  /**
   * Test future value sensitivity for AFMA FRA discounting method. 
   */
  public void test_futureValueSensitivity_AFMA() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    ExpandedFra fraExp = FRA_AFMA.expand();
    double forwardRate = 0.04;
    LocalDate fixingDate = FRA_AFMA.getStartDate();
    PointSensitivityBuilder sens = IborRateSensitivity.of(FRA_AFMA.getIndex(), fixingDate, 1d);
    when(mockObs.rateSensitivity(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), mockProv))
        .thenReturn(sens);
    when(mockObs.rate(fraExp.getFloatingRate(), FRA_AFMA.getStartDate(), FRA_AFMA.getEndDate(), mockProv))
        .thenReturn(forwardRate);
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    PointSensitivities sensitivity = test.futureValueSensitivity(fraExp, mockProv);
    double eps = 1.e-7;
    double fdSense = futureValueFwdSensitivity(FRA_AFMA, forwardRate, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 1);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA_AFMA.getIndex());
    assertEquals(sensitivity0.getFixingDate(), fixingDate);
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA_AFMA.getNotional() * eps);
  }

  //-------------------------------------------------------------------------
  /**
   * Test present value sensitivity for ISDA  
   */
  public void test_presentValueSensitivity_ISDA() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    ExpandedFra fraExp = FRA.expand();
    double forwardRate = 0.05;
    double discountRate = 0.015;
    double paymentTime = 0.3;
    double discountFactor = Math.exp(-discountRate * paymentTime);
    LocalDate fixingDate = FRA.getStartDate();
    PointSensitivityBuilder sens = IborRateSensitivity.of(FRA.getIndex(), fixingDate, 1d);
    when(mockProv.discountFactor(FRA.getCurrency(), fraExp.getPaymentDate()))
        .thenReturn(discountFactor);
    when(mockProv.discountFactorZeroRateSensitivity(FRA.getCurrency(), fraExp.getPaymentDate()))
        .thenReturn(ZeroRateSensitivity.of(
            fraExp.getCurrency(), fraExp.getPaymentDate(), -discountFactor * paymentTime));
    when(mockObs.rateSensitivity(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), mockProv))
        .thenReturn(sens);
    when(mockObs.rate(fraExp.getFloatingRate(), FRA.getStartDate(), FRA.getEndDate(), mockProv))
        .thenReturn(forwardRate);
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    PointSensitivities sensitivity = test.presentValueSensitivity(fraExp, mockProv);
    double eps = 1.e-7;
    double fdDscSense = dscSensitivity(FRA, forwardRate, discountFactor, paymentTime, eps);
    double fdSense = presentValueFwdSensitivity(FRA, forwardRate, discountFactor, paymentTime, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 2);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA.getIndex());
    assertEquals(sensitivity0.getFixingDate(), fixingDate);
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA.getNotional() * eps);
    ZeroRateSensitivity sensitivity1 = (ZeroRateSensitivity) sensitivities.get(1);
    assertEquals(sensitivity1.getCurrency(), FRA.getCurrency());
    assertEquals(sensitivity1.getDate(), fraExp.getPaymentDate());
    assertEquals(sensitivity1.getSensitivity(), fdDscSense, FRA.getNotional() * eps);

    // test via FraTrade
    DiscountingFraTradePricer testTrade = new DiscountingFraTradePricer(test);
    assertEquals(testTrade.presentValueSensitivity(FRA_TRADE, mockProv), test.presentValueSensitivity(fraExp, mockProv));
  }

  /**
   * Test present value sensitivity for NONE FRA discounting method. 
   */
  public void test_presentValueSensitivity_NONE() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    ExpandedFra fraExp = FRA_NONE.expand();
    double forwardRate = 0.025;
    double discountRate = 0.01;
    double paymentTime = 0.3;
    double discountFactor = Math.exp(-discountRate * paymentTime);
    LocalDate fixingDate = FRA_NONE.getStartDate();
    PointSensitivityBuilder sens = IborRateSensitivity.of(FRA.getIndex(), fixingDate, 1d);
    when(mockProv.discountFactor(FRA_NONE.getCurrency(), fraExp.getPaymentDate()))
        .thenReturn(discountFactor);
    when(mockProv.discountFactorZeroRateSensitivity(FRA.getCurrency(), fraExp.getPaymentDate()))
        .thenReturn(ZeroRateSensitivity.of(
            fraExp.getCurrency(), fraExp.getPaymentDate(), -discountFactor * paymentTime));
    when(mockObs.rateSensitivity(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), mockProv))
        .thenReturn(sens);
    when(mockObs.rate(fraExp.getFloatingRate(), FRA_NONE.getStartDate(), FRA_NONE.getEndDate(), mockProv))
        .thenReturn(forwardRate);
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    PointSensitivities sensitivity = test.presentValueSensitivity(fraExp, mockProv);
    double eps = 1.e-7;
    double fdDscSense = dscSensitivity(FRA_NONE, forwardRate, discountFactor, paymentTime, eps);
    double fdSense = presentValueFwdSensitivity(FRA_NONE, forwardRate, discountFactor, paymentTime, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 2);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA_NONE.getIndex());
    assertEquals(sensitivity0.getFixingDate(), fixingDate);
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA_NONE.getNotional() * eps);
    ZeroRateSensitivity sensitivity1 = (ZeroRateSensitivity) sensitivities.get(1);
    assertEquals(sensitivity1.getCurrency(), FRA_NONE.getCurrency());
    assertEquals(sensitivity1.getDate(), fraExp.getPaymentDate());
    assertEquals(sensitivity1.getSensitivity(), fdDscSense, FRA_NONE.getNotional() * eps);
  }

  /**
   * Test present value sensitivity for AFMA FRA discounting method. 
   */
  public void test_presentValueSensitivity_AFMA() {
    RateObservationFn<RateObservation> mockObs = mock(RateObservationFn.class);
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.getValuationDate()).thenReturn(VALUATION_DATE);
    ExpandedFra fraExp = FRA_AFMA.expand();
    double forwardRate = 0.05;
    double discountRate = 0.025;
    double paymentTime = 0.3;
    double discountFactor = Math.exp(-discountRate * paymentTime);
    LocalDate fixingDate = FRA_AFMA.getStartDate();
    PointSensitivityBuilder sens = IborRateSensitivity.of(FRA.getIndex(), fixingDate, 1d);
    when(mockProv.discountFactor(FRA_AFMA.getCurrency(), fraExp.getPaymentDate()))
        .thenReturn(discountFactor);
    when(mockProv.discountFactorZeroRateSensitivity(FRA.getCurrency(), fraExp.getPaymentDate()))
        .thenReturn(ZeroRateSensitivity.of(
            fraExp.getCurrency(), fraExp.getPaymentDate(), -discountFactor * paymentTime));
    when(mockObs.rateSensitivity(fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate(), mockProv))
        .thenReturn(sens);
    when(mockObs.rate(fraExp.getFloatingRate(), FRA_AFMA.getStartDate(), FRA_AFMA.getEndDate(), mockProv))
        .thenReturn(forwardRate);
    DiscountingFraProductPricer test = new DiscountingFraProductPricer(mockObs);
    PointSensitivities sensitivity = test.presentValueSensitivity(fraExp, mockProv);
    double eps = 1.e-7;
    double fdDscSense = dscSensitivity(FRA_AFMA, forwardRate, discountFactor, paymentTime, eps);
    double fdSense = presentValueFwdSensitivity(FRA_AFMA, forwardRate, discountFactor, paymentTime, eps);

    ImmutableList<PointSensitivity> sensitivities = sensitivity.getSensitivities();
    assertEquals(sensitivities.size(), 2);
    IborRateSensitivity sensitivity0 = (IborRateSensitivity) sensitivities.get(0);
    assertEquals(sensitivity0.getIndex(), FRA_AFMA.getIndex());
    assertEquals(sensitivity0.getFixingDate(), fixingDate);
    assertEquals(sensitivity0.getSensitivity(), fdSense, FRA_AFMA.getNotional() * eps);
    ZeroRateSensitivity sensitivity1 = (ZeroRateSensitivity) sensitivities.get(1);
    assertEquals(sensitivity1.getCurrency(), FRA_AFMA.getCurrency());
    assertEquals(sensitivity1.getDate(), fraExp.getPaymentDate());
    assertEquals(sensitivity1.getSensitivity(), fdDscSense, FRA_AFMA.getNotional() * eps);
  }

  //-------------------------------------------------------------------------
  private double futureValueFwdSensitivity(Fra fra, double forwardRate, double eps) {

    RateObservationFn<RateObservation> obsFuncNew = mock(RateObservationFn.class);
    RatesProvider provNew = mock(RatesProvider.class);
    when(provNew.getValuationDate()).thenReturn(VALUATION_DATE);
    ExpandedFra fraExp = fra.expand();
    when(obsFuncNew.rate(fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate + eps);
    CurrencyAmount upValue = new DiscountingFraProductPricer(obsFuncNew).futureValue(fraExp, provNew);
    when(obsFuncNew.rate(fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate - eps);
    CurrencyAmount downValue = new DiscountingFraProductPricer(obsFuncNew).futureValue(fraExp, provNew);
    return upValue.minus(downValue).multipliedBy(0.5 / eps).getAmount();
  }

  private double presentValueFwdSensitivity(
      Fra fra, double forwardRate, double discountFactor, double paymentTime, double eps) {

    RateObservationFn<RateObservation> obsFuncNew = mock(RateObservationFn.class);
    RatesProvider provNew = mock(RatesProvider.class);
    when(provNew.getValuationDate()).thenReturn(VALUATION_DATE);
    ExpandedFra fraExp = fra.expand();
    when(provNew.discountFactor(fra.getCurrency(), fraExp.getPaymentDate()))
        .thenReturn(discountFactor);
    when(provNew.relativeTime(fraExp.getPaymentDate()))
        .thenReturn(paymentTime);
    when(obsFuncNew.rate(fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate + eps);
    CurrencyAmount upValue = new DiscountingFraProductPricer(obsFuncNew).presentValue(fraExp, provNew);
    when(obsFuncNew.rate(fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate - eps);
    CurrencyAmount downValue = new DiscountingFraProductPricer(obsFuncNew).presentValue(fraExp, provNew);
    return upValue.minus(downValue).multipliedBy(0.5 / eps).getAmount();
  }

  private double dscSensitivity(
      Fra fra, double forwardRate, double discountFactor, double paymentTime, double eps) {

    RatesProvider provNew = mock(RatesProvider.class);
    when(provNew.getValuationDate()).thenReturn(VALUATION_DATE);
    RateObservationFn<RateObservation> obsFuncNew = mock(RateObservationFn.class);
    ExpandedFra fraExp = fra.expand();
    when(obsFuncNew.rate(fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate(), provNew))
        .thenReturn(forwardRate);
    when(provNew.discountFactor(fra.getCurrency(), fraExp.getPaymentDate()))
        .thenReturn(discountFactor * Math.exp(-eps * paymentTime));
    CurrencyAmount upDscValue = new DiscountingFraProductPricer(obsFuncNew).presentValue(fraExp, provNew);
    when(provNew.discountFactor(fra.getCurrency(), fraExp.getPaymentDate()))
        .thenReturn(discountFactor * Math.exp(eps * paymentTime));
    CurrencyAmount downDscValue = new DiscountingFraProductPricer(obsFuncNew).presentValue(fraExp, provNew);
    return upDscValue.minus(downDscValue).multipliedBy(0.5 / eps).getAmount();
  }

}
