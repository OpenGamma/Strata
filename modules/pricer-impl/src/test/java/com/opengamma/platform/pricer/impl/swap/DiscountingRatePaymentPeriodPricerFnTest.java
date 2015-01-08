/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.index.FxIndices.WM_GBP_USD;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.basics.currency.CurrencyPair;
import com.opengamma.platform.finance.observation.FixedRateObservation;
import com.opengamma.platform.finance.swap.FxReset;
import com.opengamma.platform.finance.swap.NegativeRateMethod;
import com.opengamma.platform.finance.swap.RateAccrualPeriod;
import com.opengamma.platform.finance.swap.RatePaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Test.
 */
@Test
public class DiscountingRatePaymentPeriodPricerFnTest {

  private static final LocalDate VALUATION_DATE = LocalDate.of(2014, 1, 22);
  private static final LocalDate FX_DATE_1 = LocalDate.of(2014, 1, 22);
  private static final LocalDate CPN_DATE_1 = LocalDate.of(2014, 1, 24);
  private static final LocalDate CPN_DATE_2 = LocalDate.of(2014, 4, 24);
  private static final LocalDate CPN_DATE_3 = LocalDate.of(2014, 7, 24);
  private static final LocalDate CPN_DATE_4 = LocalDate.of(2014, 10, 24);
  private static final double ACCRUAL_FACTOR_1 = 0.245;
  private static final double ACCRUAL_FACTOR_2 = 0.255;
  private static final double ACCRUAL_FACTOR_3 = 0.25;
  private static final double GEARING = 2.0;
  private static final double SPREAD = -0.0025;
  private static final double NOTIONAL_100 = 1.0E8;
  private static final LocalDate PAYMENT_DATE_1 = LocalDate.of(2014, 4, 26);
  private static final LocalDate PAYMENT_DATE_3 = LocalDate.of(2014, 10, 26);
  private static final double RATE_1 = 0.0123d;
  private static final double RATE_2 = 0.0127d;
  private static final double RATE_3 = 0.0135d;
  private static final double RATE_FX = 1.6d;
  private static final double DISCOUNT_FACTOR = 0.976d;
  private static final double TOLERANCE_PV = 1E-10;

  private static final RateAccrualPeriod ACCRUAL_PERIOD_1 = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_1)
      .endDate(CPN_DATE_2)
      .yearFraction(ACCRUAL_FACTOR_1)
      .rateObservation(FixedRateObservation.of(RATE_1))
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_1_GS = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_1)
      .endDate(CPN_DATE_2)
      .yearFraction(ACCRUAL_FACTOR_1)
      .rateObservation(FixedRateObservation.of(RATE_1))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_1_NEG = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_1)
      .endDate(CPN_DATE_2)
      .yearFraction(ACCRUAL_FACTOR_1)
      .rateObservation(FixedRateObservation.of(RATE_1))
      .gearing(-1d)
      .negativeRateMethod(NegativeRateMethod.NOT_NEGATIVE)
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_2_GS = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_2)
      .endDate(CPN_DATE_3)
      .yearFraction(ACCRUAL_FACTOR_2)
      .rateObservation(FixedRateObservation.of(RATE_2))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();
  private static final RateAccrualPeriod ACCRUAL_PERIOD_3_GS = RateAccrualPeriod.builder()
      .startDate(CPN_DATE_3)
      .endDate(CPN_DATE_4)
      .yearFraction(ACCRUAL_FACTOR_3)
      .rateObservation(FixedRateObservation.of(RATE_3))
      .gearing(GEARING)
      .spread(SPREAD)
      .build();

  private static final RatePaymentPeriod PAYMENT_PERIOD_1 = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_1)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1))
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_1_FX = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_1)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1))
      .currency(USD)
      .notional(NOTIONAL_100)
      .fxReset(FxReset.of(WM_GBP_USD, GBP, FX_DATE_1))
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_1_GS = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_1)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_GS))
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_1_NEG = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_1)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_NEG))
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();

  private static final RatePaymentPeriod PAYMENT_PERIOD_FULL_GS = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_3)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_GS, ACCRUAL_PERIOD_2_GS, ACCRUAL_PERIOD_3_GS))
      .currency(USD)
      .notional(NOTIONAL_100)
      .build();
  private static final RatePaymentPeriod PAYMENT_PERIOD_FULL_GS_FX = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT_DATE_3)
      .accrualPeriods(ImmutableList.of(ACCRUAL_PERIOD_1_GS, ACCRUAL_PERIOD_2_GS, ACCRUAL_PERIOD_3_GS))
      .currency(USD)
      .notional(NOTIONAL_100)
      .fxReset(FxReset.of(WM_GBP_USD, GBP, FX_DATE_1))
      .build();

  // all tests use a fixed rate to avoid excessive use of mocks
  // rate observation is separated from this class, so nothing is missed in unit test terms
  // most testing on futureValue as methods only differ in discountFactor
  //-------------------------------------------------------------------------
  public void test_presentValue_single_paymentBeforeToday() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(PAYMENT_DATE_1.plusDays(1));
    when(env.discountFactor(USD, PAYMENT_DATE_1)).thenReturn(DISCOUNT_FACTOR);
    double fvComputed = DiscountingRatePaymentPeriodPricerFn.DEFAULT.presentValue(env, PAYMENT_PERIOD_1);    
    assertEquals(fvComputed, 0d, TOLERANCE_PV);
  }

  public void test_presentValue_single() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    when(env.discountFactor(USD, PAYMENT_DATE_1)).thenReturn(DISCOUNT_FACTOR);
    double pvExpected = RATE_1 * ACCRUAL_FACTOR_1 * NOTIONAL_100 * DISCOUNT_FACTOR;
    double pvComputed = DiscountingRatePaymentPeriodPricerFn.DEFAULT.presentValue(env, PAYMENT_PERIOD_1);    
    assertEquals(pvComputed, pvExpected, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_single_paymentBeforeToday() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(PAYMENT_DATE_1.plusDays(1));
    double fvComputed = DiscountingRatePaymentPeriodPricerFn.DEFAULT.futureValue(env, PAYMENT_PERIOD_1);    
    assertEquals(fvComputed, 0d, TOLERANCE_PV);
  }

  public void test_futureValue_single() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    double fvExpected = RATE_1 * ACCRUAL_FACTOR_1 * NOTIONAL_100;
    double fvComputed = DiscountingRatePaymentPeriodPricerFn.DEFAULT.futureValue(env, PAYMENT_PERIOD_1);    
    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  public void test_futureValue_single_fx() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    when(env.fxIndexRate(WM_GBP_USD, CurrencyPair.of(GBP, USD), FX_DATE_1)).thenReturn(RATE_FX);
    double fvExpected = RATE_1 * ACCRUAL_FACTOR_1 * NOTIONAL_100 * RATE_FX;
    double fvComputed = DiscountingRatePaymentPeriodPricerFn.DEFAULT.futureValue(env, PAYMENT_PERIOD_1_FX);    
    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  public void test_futureValue_single_gearingSpread() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    double fvExpected = (RATE_1 * GEARING + SPREAD) * ACCRUAL_FACTOR_1 * NOTIONAL_100;
    double fvComputed = DiscountingRatePaymentPeriodPricerFn.DEFAULT.futureValue(env, PAYMENT_PERIOD_1_GS);    
    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  public void test_futureValue_single_gearingNoNegative() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    double fvComputed = DiscountingRatePaymentPeriodPricerFn.DEFAULT.futureValue(env, PAYMENT_PERIOD_1_NEG);    
    assertEquals(fvComputed, 0d, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_compoundNone() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    double fvExpected =
        ((RATE_1 * GEARING + SPREAD) * ACCRUAL_FACTOR_1 * NOTIONAL_100) +
        ((RATE_2 * GEARING + SPREAD) * ACCRUAL_FACTOR_2 * NOTIONAL_100) +
        ((RATE_3 * GEARING + SPREAD) * ACCRUAL_FACTOR_3 * NOTIONAL_100);
    double fvComputed = DiscountingRatePaymentPeriodPricerFn.DEFAULT.futureValue(env, PAYMENT_PERIOD_FULL_GS);    
    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  public void test_futureValue_compoundNone_fx() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
    when(env.fxIndexRate(WM_GBP_USD, CurrencyPair.of(GBP, USD), FX_DATE_1)).thenReturn(RATE_FX);
    double fvExpected =
        ((RATE_1 * GEARING + SPREAD) * ACCRUAL_FACTOR_1 * NOTIONAL_100 * RATE_FX) +
        ((RATE_2 * GEARING + SPREAD) * ACCRUAL_FACTOR_2 * NOTIONAL_100 * RATE_FX) +
        ((RATE_3 * GEARING + SPREAD) * ACCRUAL_FACTOR_3 * NOTIONAL_100 * RATE_FX);
    double fvComputed = DiscountingRatePaymentPeriodPricerFn.DEFAULT.futureValue(env, PAYMENT_PERIOD_FULL_GS_FX);    
    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_compoundStraight() {
    // TODO: why is this wrong
//    RatePaymentPeriod period = PAYMENT_PERIOD_FULL_GS.toBuilder().compoundingMethod(CompoundingMethod.STRAIGHT).build();
//    PricingEnvironment env = mock(PricingEnvironment.class);
//    when(env.getValuationDate()).thenReturn(VALUATION_DATE);
//    double accural1 = ((RATE_1 * GEARING + SPREAD) * ACCRUAL_FACTOR_1 * NOTIONAL_100);
//    double accural2 = ((RATE_2 * GEARING + SPREAD) * ACCRUAL_FACTOR_2 * (NOTIONAL_100 + accural1));
//    double accural3  = ((RATE_3 * GEARING + SPREAD) * ACCRUAL_FACTOR_3 * (NOTIONAL_100 + accural2));
//    double fvExpected = accural1 + accural2 + accural3;
//    double fvComputed = DiscountingRatePaymentPeriodPricerFn.DEFAULT.futureValue(env, period);    
//    assertEquals(fvComputed, fvExpected, TOLERANCE_PV);
  }

  public void test_futureValue_compoundFlat() {
    // TODO
  }

  public void test_futureValue_compoundSpreadExclusive() {
    // TODO
  }

}
