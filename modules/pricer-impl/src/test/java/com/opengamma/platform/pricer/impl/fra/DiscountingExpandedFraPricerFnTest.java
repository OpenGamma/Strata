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

import org.testng.annotations.Test;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.fra.ExpandedFra;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.observation.DispatchingRateObservationFn;

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
   * Test present value for ISDA FRA Discounting method. 
   */
  public void test_presentValue_ISDA() {
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
   * Test present value for NONE FRA Discounting method. 
   */
  public void test_presentValue_NONE() {
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

}
