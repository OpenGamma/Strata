/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.fra;

import static com.opengamma.platform.pricer.impl.fra.FraDummyData.FRA;
import static com.opengamma.platform.pricer.impl.fra.FraDummyData.FRA_AFMA;
import static com.opengamma.platform.pricer.impl.fra.FraDummyData.FRA_NONE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.tuple.Pair;
import com.opengamma.platform.finance.fra.ExpandedFra;
import com.opengamma.platform.finance.fra.Fra;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.observation.DispatchingRateObservationFn;
import com.opengamma.platform.pricer.sensitivity.multicurve.ForwardRateSensitivityLD;
import com.opengamma.platform.pricer.sensitivity.multicurve.MulticurveSensitivity3LD;

/**
 * Test.
 */
@Test
public class DiscountingExpandedFraPricerFnTest {

  private final PricingEnvironment mockEnv = mock(PricingEnvironment.class);
  private final DispatchingRateObservationFn mockObs = mock(DispatchingRateObservationFn.class);
  private static final double TOLERANCE = 1E-12;

  /**
   * Test future value for ISDA FRA Discounting method. 
   */
  public void test_futureValue_ISDA() {
    double forwardRate = 0.02;
    ExpandedFra fraExp = FRA.expand();
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(mockObs);
    double fixedRate = FRA.getFixedRate();
    double yearFraction = fraExp.getYearFraction();
    double notional = fraExp.getNotional();
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate()))
        .thenReturn(forwardRate);
    MultiCurrencyAmount computed = test.futureValue(mockEnv, fraExp);
    double expected = notional * yearFraction * (forwardRate - fixedRate) / (1.0 + yearFraction * forwardRate);
    assertEquals(computed.getAmount(FRA.getCurrency()).getAmount(), expected, TOLERANCE);
  }

  /**
   * Test future value for NONE FRA Discounting method. 
   */
  public void test_futureValue_NONE() {
    double forwardRate = 0.02;
    ExpandedFra fraExp = FRA_NONE.expand();
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(mockObs);
    double fixedRate = FRA_NONE.getFixedRate();
    double yearFraction = fraExp.getYearFraction();
    double notional = fraExp.getNotional();
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate()))
        .thenReturn(forwardRate);
    MultiCurrencyAmount computed = test.futureValue(mockEnv, fraExp);
    double expected = notional * yearFraction * (forwardRate - fixedRate);
    assertEquals(computed.getAmount(FRA_NONE.getCurrency()).getAmount(), expected, TOLERANCE);
  }

  /**
   * Test future value for AFMA FRA Discounting method. 
   */
  public void test_futureValue_AFMA() {
    double forwardRate = 0.02;
    ExpandedFra fraExp = FRA_AFMA.expand();
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(mockObs);
    double fixedRate = FRA_AFMA.getFixedRate();
    double yearFraction = fraExp.getYearFraction();
    double notional = fraExp.getNotional();
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate()))
        .thenReturn(forwardRate);
    MultiCurrencyAmount computed = test.futureValue(mockEnv, fraExp);
    double expected = -notional * (1.0 / (1.0 + yearFraction * forwardRate) - 1.0 / (1.0 + yearFraction * fixedRate));
    assertEquals(computed.getAmount(FRA_AFMA.getCurrency()).getAmount(), expected, TOLERANCE);
  }

  /**
   * Test present value for NONE FRA Discounting method. 
   */
  public void test_presentValue_NONE() {
    double forwardRate = 0.02;
    double discountFactor = 0.98d;
    ExpandedFra fraExp = FRA_NONE.expand();
    Currency currency = FRA_NONE.getCurrency();
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(mockObs);
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate()))
        .thenReturn(forwardRate);
    when(mockEnv.discountFactor(currency, fraExp.getPaymentDate())).thenReturn(discountFactor);
    MultiCurrencyAmount pvComputed = test.presentValue(mockEnv, fraExp);
    MultiCurrencyAmount pvExpected = test.futureValue(mockEnv, fraExp).multipliedBy(discountFactor);
    assertEquals(pvComputed.getAmount(FRA_NONE.getCurrency()).getAmount(), 
        pvExpected.getAmount(FRA_NONE.getCurrency()).getAmount(), TOLERANCE);
  }

  /**
   * Test present value for ISDA FRA Discounting method. 
   */
  public void test_presentValue_ISDA() {
    double forwardRate = 0.02;
    double discountFactor = 0.98d;
    ExpandedFra fraExp = FRA.expand();
    Currency currency = FRA.getCurrency();
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(mockObs);
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate()))
        .thenReturn(forwardRate);
    when(mockEnv.discountFactor(currency, fraExp.getPaymentDate())).thenReturn(discountFactor);
    MultiCurrencyAmount pvComputed = test.presentValue(mockEnv, fraExp);
    MultiCurrencyAmount pvExpected = test.futureValue(mockEnv, fraExp).multipliedBy(discountFactor);
    assertEquals(pvComputed.getAmount(FRA.getCurrency()).getAmount(), 
        pvExpected.getAmount(FRA.getCurrency()).getAmount(), TOLERANCE);
  }

  /**
   * Test present value for ISDA FRA Discounting method. 
   */
  public void test_presentValue_AFMA() {
    double forwardRate = 0.02;
    double discountFactor = 0.98d;
    ExpandedFra fraExp = FRA_AFMA.expand();
    Currency currency = FRA_AFMA.getCurrency();
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(mockObs);
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), fraExp.getStartDate(), fraExp.getEndDate()))
        .thenReturn(forwardRate);
    when(mockEnv.discountFactor(currency, fraExp.getPaymentDate())).thenReturn(discountFactor);
    MultiCurrencyAmount pvComputed = test.presentValue(mockEnv, fraExp);
    MultiCurrencyAmount pvExpected = test.futureValue(mockEnv, fraExp).multipliedBy(discountFactor);
    assertEquals(pvComputed.getAmount(FRA_AFMA.getCurrency()).getAmount(), 
        pvExpected.getAmount(FRA_AFMA.getCurrency()).getAmount(), TOLERANCE);
  }

  /**
   * Test future value sensitivity for ISDA FRA discounting method. 
   */
  public void test_futureValueSensitivity_ISDA() {
    ExpandedFra fraExp = FRA.expand();
    double forwardRate = 0.05;
    LocalDate fixingDate = FRA.getStartDate();
    List<ForwardRateSensitivityLD> forwardRateSensi = new ArrayList<>();
    forwardRateSensi.add(new ForwardRateSensitivityLD(FRA.getIndex(), fixingDate, 1.0d, FRA.getCurrency()));
    Pair<Double, MulticurveSensitivity3LD> snese = Pair.of(forwardRate,
        MulticurveSensitivity3LD.ofForwardRate(forwardRateSensi));
    when(mockObs.rateMulticurveSensitivity3LD(mockEnv, fraExp.getFloatingRate(), fraExp.getStartDate(),
        fraExp.getEndDate())).thenReturn(snese);
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), FRA.getStartDate(), FRA.getEndDate())).thenReturn(forwardRate);
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(mockObs);
    MulticurveSensitivity3LD sensitivity = test.futureValueCurveSensitivity3LD(mockEnv, fraExp);
    double eps = 1.e-7;
    double fdSense = futureValueFwdSensitivity(FRA, eps);

    assertTrue(sensitivity.getZeroRateSensitivities().isEmpty());
    assertEquals(sensitivity.getForwardRateSensitivities().size(), 1);
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getFixingDate(), fixingDate);
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getIndex(), FRA.getIndex());
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getValue(), fdSense, FRA.getNotional() * eps);
  }

  /**
   * Test future value sensitivity for NONE FRA discounting method.
   */
  public void test_futureValueSensitivity_NONE() {
    ExpandedFra fraExp = FRA_NONE.expand();
    double forwardRate = 0.035;
    LocalDate fixingDate = FRA_NONE.getStartDate();
    List<ForwardRateSensitivityLD> forwardRateSensi = new ArrayList<>();
    forwardRateSensi.add(new ForwardRateSensitivityLD(FRA_NONE.getIndex(), fixingDate, 1.0d, FRA_NONE.getCurrency()));
    Pair<Double, MulticurveSensitivity3LD> snese = Pair.of(forwardRate,
        MulticurveSensitivity3LD.ofForwardRate(forwardRateSensi));
    when(mockObs.rateMulticurveSensitivity3LD(mockEnv, fraExp.getFloatingRate(), fraExp.getStartDate(),
        fraExp.getEndDate())).thenReturn(snese);
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), FRA_NONE.getStartDate(), FRA_NONE.getEndDate())).thenReturn(
        forwardRate);
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(mockObs);
    MulticurveSensitivity3LD sensitivity = test.futureValueCurveSensitivity3LD(mockEnv, fraExp);
    double eps = 1.e-7;
    double fdSense = futureValueFwdSensitivity(FRA_NONE, eps);

    assertTrue(sensitivity.getZeroRateSensitivities().isEmpty());
    assertEquals(sensitivity.getForwardRateSensitivities().size(), 1);
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getFixingDate(), fixingDate);
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getIndex(), FRA_NONE.getIndex());
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getValue(), fdSense, FRA_NONE.getNotional() * eps);
  }

  /**
   * Test future value sensitivity for AFMA FRA discounting method. 
   */
  public void test_futureValueSensitivity_AFMA() {
    ExpandedFra fraExp = FRA_AFMA.expand();
    double forwardRate = 0.04;
    LocalDate fixingDate = FRA_AFMA.getStartDate();
    List<ForwardRateSensitivityLD> forwardRateSensi = new ArrayList<>();
    forwardRateSensi.add(new ForwardRateSensitivityLD(FRA_AFMA.getIndex(), fixingDate, 1.0d, FRA_AFMA.getCurrency()));
    Pair<Double, MulticurveSensitivity3LD> snese = Pair.of(forwardRate,
        MulticurveSensitivity3LD.ofForwardRate(forwardRateSensi));
    when(mockObs.rateMulticurveSensitivity3LD(mockEnv, fraExp.getFloatingRate(), fraExp.getStartDate(),
        fraExp.getEndDate())).thenReturn(snese);
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), FRA_AFMA.getStartDate(), FRA_AFMA.getEndDate())).thenReturn(
        forwardRate);
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(mockObs);
    MulticurveSensitivity3LD sensitivity = test.futureValueCurveSensitivity3LD(mockEnv, fraExp);
    double eps = 1.e-7;
    double fdSense = futureValueFwdSensitivity(FRA_AFMA, eps);

    assertTrue(sensitivity.getZeroRateSensitivities().isEmpty());
    assertEquals(sensitivity.getForwardRateSensitivities().size(), 1);
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getFixingDate(), fixingDate);
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getIndex(), FRA_AFMA.getIndex());
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getValue(), fdSense, FRA_AFMA.getNotional() * eps);
  }

  /**
   * Test present value sensitivity for ISDA  
   */
  public void test_presentValueSensitivity_ISDA() {
    ExpandedFra fraExp = FRA.expand();
    double forwardRate = 0.05;
    double discountRate = 0.015;
    double paymentTime = 0.3;
    double discountFactor = Math.exp(-discountRate * paymentTime);
    LocalDate fixingDate = FRA.getStartDate();
    List<ForwardRateSensitivityLD> forwardRateSensi = new ArrayList<>();
    forwardRateSensi.add(new ForwardRateSensitivityLD(FRA.getIndex(), fixingDate, 1.0d, FRA.getCurrency()));
    Pair<Double, MulticurveSensitivity3LD> snese = Pair.of(forwardRate,
        MulticurveSensitivity3LD.ofForwardRate(forwardRateSensi));
    when(mockEnv.discountFactor(FRA.getCurrency(), fraExp.getPaymentDate())).thenReturn(discountFactor);
    when(mockEnv.relativeTime(fraExp.getPaymentDate())).thenReturn(paymentTime);
    when(mockObs.rateMulticurveSensitivity3LD(mockEnv, fraExp.getFloatingRate(), fraExp.getStartDate(),
        fraExp.getEndDate())).thenReturn(snese);
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), FRA.getStartDate(), FRA.getEndDate())).thenReturn(forwardRate);
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(mockObs);
    MulticurveSensitivity3LD sensitivity = test.presentValueCurveSensitivity3LD(mockEnv, fraExp);
    double eps = 1.e-7;
    double fdDscSense = dscSensitivity(FRA, eps);
    double fdSense = presentValueFwdSensitivity(FRA, eps);

    assertEquals(sensitivity.getZeroRateSensitivities().size(), 1);
    assertEquals(sensitivity.getZeroRateSensitivities().get(0).getCurrencyDiscount(), FRA.getCurrency());
    assertEquals(sensitivity.getZeroRateSensitivities().get(0).getDate(), fraExp.getPaymentDate());
    assertEquals(sensitivity.getZeroRateSensitivities().get(0).getValue(), fdDscSense, FRA.getNotional() * eps);
    assertEquals(sensitivity.getZeroRateSensitivities().size(), 1);
    assertEquals(sensitivity.getForwardRateSensitivities().size(), 1);
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getFixingDate(), fixingDate);
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getIndex(), FRA.getIndex());
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getValue(), fdSense, FRA.getNotional() * eps);
  }

  /**
   * Test present value sensitivity for NONE FRA discounting method. 
   */
  public void test_presentValueSensitivity_NONE() {
    ExpandedFra fraExp = FRA_NONE.expand();
    double forwardRate = 0.025;
    double discountRate = 0.01;
    double paymentTime = 0.3;
    double discountFactor = Math.exp(-discountRate * paymentTime);
    LocalDate fixingDate = FRA_NONE.getStartDate();
    List<ForwardRateSensitivityLD> forwardRateSensi = new ArrayList<>();
    forwardRateSensi.add(new ForwardRateSensitivityLD(FRA_NONE.getIndex(), fixingDate, 1.0d, FRA_NONE.getCurrency()));
    Pair<Double, MulticurveSensitivity3LD> snese = Pair.of(forwardRate,
        MulticurveSensitivity3LD.ofForwardRate(forwardRateSensi));
    when(mockEnv.discountFactor(FRA_NONE.getCurrency(), fraExp.getPaymentDate())).thenReturn(discountFactor);
    when(mockEnv.relativeTime(fraExp.getPaymentDate())).thenReturn(paymentTime);
    when(mockObs.rateMulticurveSensitivity3LD(mockEnv, fraExp.getFloatingRate(), fraExp.getStartDate(),
        fraExp.getEndDate())).thenReturn(snese);
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), FRA_NONE.getStartDate(), FRA_NONE.getEndDate())).thenReturn(
        forwardRate);
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(mockObs);
    MulticurveSensitivity3LD sensitivity = test.presentValueCurveSensitivity3LD(mockEnv, fraExp);
    double eps = 1.e-7;
    double fdDscSense = dscSensitivity(FRA_NONE, eps);
    double fdSense = presentValueFwdSensitivity(FRA_NONE, eps);

    assertEquals(sensitivity.getZeroRateSensitivities().size(), 1);
    assertEquals(sensitivity.getZeroRateSensitivities().get(0).getCurrencyDiscount(), FRA_NONE.getCurrency());
    assertEquals(sensitivity.getZeroRateSensitivities().get(0).getDate(), fraExp.getPaymentDate());
    assertEquals(sensitivity.getZeroRateSensitivities().get(0).getValue(), fdDscSense, FRA_NONE.getNotional() * eps);
    assertEquals(sensitivity.getZeroRateSensitivities().size(), 1);
    assertEquals(sensitivity.getForwardRateSensitivities().size(), 1);
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getFixingDate(), fixingDate);
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getIndex(), FRA_NONE.getIndex());
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getValue(), fdSense, FRA_NONE.getNotional() * eps);
  }

  /**
   * Test present value sensitivity for AFMA FRA discounting method. 
   */
  public void test_presentValueSensitivity_AFMA() {
    ExpandedFra fraExp = FRA_AFMA.expand();
    double forwardRate = 0.05;
    double discountRate = 0.025;
    double paymentTime = 0.3;
    double discountFactor = Math.exp(-discountRate * paymentTime);
    LocalDate fixingDate = FRA_AFMA.getStartDate();
    List<ForwardRateSensitivityLD> forwardRateSensi = new ArrayList<>();
    forwardRateSensi.add(new ForwardRateSensitivityLD(FRA_AFMA.getIndex(), fixingDate, 1.0d, FRA_AFMA.getCurrency()));
    Pair<Double, MulticurveSensitivity3LD> snese = Pair.of(forwardRate,
        MulticurveSensitivity3LD.ofForwardRate(forwardRateSensi));
    when(mockEnv.discountFactor(FRA_AFMA.getCurrency(), fraExp.getPaymentDate())).thenReturn(discountFactor);
    when(mockEnv.relativeTime(fraExp.getPaymentDate())).thenReturn(paymentTime);
    when(mockObs.rateMulticurveSensitivity3LD(mockEnv, fraExp.getFloatingRate(), fraExp.getStartDate(),
        fraExp.getEndDate())).thenReturn(snese);
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), FRA_AFMA.getStartDate(), FRA_AFMA.getEndDate())).thenReturn(
        forwardRate);
    DiscountingExpandedFraPricerFn test = new DiscountingExpandedFraPricerFn(mockObs);
    MulticurveSensitivity3LD sensitivity = test.presentValueCurveSensitivity3LD(mockEnv, fraExp);
    double eps = 1.e-7;
    double fdDscSense = dscSensitivity(FRA_AFMA, eps);
    double fdSense = presentValueFwdSensitivity(FRA_AFMA, eps);

    assertEquals(sensitivity.getZeroRateSensitivities().size(), 1);
    assertEquals(sensitivity.getZeroRateSensitivities().get(0).getCurrencyDiscount(), FRA_AFMA.getCurrency());
    assertEquals(sensitivity.getZeroRateSensitivities().get(0).getDate(), fraExp.getPaymentDate());
    assertEquals(sensitivity.getZeroRateSensitivities().get(0).getValue(), fdDscSense, FRA_AFMA.getNotional() * eps);
    assertEquals(sensitivity.getZeroRateSensitivities().size(), 1);
    assertEquals(sensitivity.getForwardRateSensitivities().size(), 1);
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getFixingDate(), fixingDate);
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getIndex(), FRA_AFMA.getIndex());
    assertEquals(sensitivity.getForwardRateSensitivities().get(0).getValue(), fdSense, FRA_AFMA.getNotional() * eps);
  }

  private double futureValueFwdSensitivity(Fra fra, double eps) {
    ExpandedFra fraExp = fra.expand();
    double forwardRate = mockObs.rate(mockEnv, fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate());
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate())).thenReturn(
        forwardRate + eps);
    MultiCurrencyAmount upValue = (new DiscountingExpandedFraPricerFn(mockObs)).futureValue(mockEnv, fraExp);
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate())).thenReturn(
        forwardRate - eps);
    MultiCurrencyAmount downValue = (new DiscountingExpandedFraPricerFn(mockObs)).futureValue(mockEnv, fraExp);
    return upValue.minus(downValue).multipliedBy(0.5 / eps).getAmount(fra.getCurrency()).getAmount();
  }

  private double presentValueFwdSensitivity(Fra fra, double eps) {
    ExpandedFra fraExp = fra.expand();
    double forwardRate = mockObs.rate(mockEnv, fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate());
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate())).thenReturn(
        forwardRate + eps);
    MultiCurrencyAmount upValue = (new DiscountingExpandedFraPricerFn(mockObs)).presentValue(mockEnv, fraExp);
    when(mockObs.rate(mockEnv, fraExp.getFloatingRate(), fra.getStartDate(), fra.getEndDate())).thenReturn(
        forwardRate - eps);
    MultiCurrencyAmount downValue = (new DiscountingExpandedFraPricerFn(mockObs)).presentValue(mockEnv, fraExp);
    return upValue.minus(downValue).multipliedBy(0.5 / eps).getAmount(fra.getCurrency()).getAmount();
  }

  private double dscSensitivity(Fra fra, double eps) {
    ExpandedFra fraExp = fra.expand();
    double discountFactor = mockEnv.discountFactor(fra.getCurrency(), fraExp.getPaymentDate());
    double paymentTime = mockEnv.relativeTime(fraExp.getPaymentDate());
    when(mockEnv.discountFactor(fra.getCurrency(), fraExp.getPaymentDate())).thenReturn(
        discountFactor * Math.exp(-eps * paymentTime));
    MultiCurrencyAmount upDscValue = (new DiscountingExpandedFraPricerFn(mockObs)).presentValue(mockEnv, fraExp);
    when(mockEnv.discountFactor(fra.getCurrency(), fraExp.getPaymentDate())).thenReturn(
        discountFactor * Math.exp(eps * paymentTime));
    MultiCurrencyAmount downDscValue = (new DiscountingExpandedFraPricerFn(mockObs)).presentValue(mockEnv, fraExp);
    return upDscValue.minus(downDscValue).multipliedBy(0.5 / eps).getAmount(fra.getCurrency()).getAmount();
  }
}
