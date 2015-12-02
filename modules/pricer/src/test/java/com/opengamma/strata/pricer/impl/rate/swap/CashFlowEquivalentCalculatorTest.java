/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.swap.SwapLegType.FIXED;
import static com.opengamma.strata.product.swap.SwapLegType.IBOR;
import static com.opengamma.strata.product.swap.SwapLegType.OTHER;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.product.rate.FixedRateObservation;
import com.opengamma.strata.product.rate.IborRateObservation;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.KnownAmountPaymentPeriod;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.Swap;

/**
 * Test {@link CashFlowEquivalentCalculator}.
 */
@Test
public class CashFlowEquivalentCalculatorTest {
  // setup
  private static final LocalDate PAYMENT1 = date(2014, 10, 6);
  private static final LocalDate START1 = date(2014, 7, 2);
  private static final LocalDate END1 = date(2014, 10, 2);
  private static final LocalDate FIXING1 = date(2014, 6, 30);
  private static final double PAY_YC1 = 0.251;
  private static final LocalDate PAYMENT2 = date(2015, 1, 4);
  private static final LocalDate START2 = date(2014, 10, 2);
  private static final LocalDate END2 = date(2015, 1, 2);
  private static final LocalDate FIXING2 = date(2014, 9, 30);
  private static final double PAY_YC2 = 0.249;
  private static final double RATE = 0.0123d;
  private static final double NOTIONAL = 100_000_000;
  // accrual periods
  private static final  RateAccrualPeriod IBOR1 = RateAccrualPeriod.builder()
      .startDate(START1)
      .endDate(END1)
      .rateObservation(IborRateObservation.of(GBP_LIBOR_3M, FIXING1))
      .yearFraction(PAY_YC1)
      .build();
  private static final  RateAccrualPeriod IBOR2 = RateAccrualPeriod.builder()
      .startDate(START2)
      .endDate(END2)
      .rateObservation(IborRateObservation.of(GBP_LIBOR_3M, FIXING2))
      .yearFraction(PAY_YC2)
      .build();
  private static final  RateAccrualPeriod FIXED1 = RateAccrualPeriod.builder()
      .startDate(START1)
      .endDate(END1)
      .rateObservation(FixedRateObservation.of(RATE))
      .yearFraction(PAY_YC1)
      .build();
  private static final RateAccrualPeriod FIXED2 = RateAccrualPeriod.builder()
      .startDate(START2)
      .endDate(END2)
      .rateObservation(FixedRateObservation.of(RATE))
      .yearFraction(PAY_YC2)
      .build();
  //Ibor leg
  private static final RatePaymentPeriod IBOR_RATE_PAYMENT1 = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT1)
      .accrualPeriods(IBOR1)
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(-NOTIONAL)
      .build();
  private static final RatePaymentPeriod IBOR_RATE_PAYMENT2 = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT2)
      .accrualPeriods(IBOR2)
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(-NOTIONAL)
      .build();
  private static final ExpandedSwapLeg IBOR_LEG = ExpandedSwapLeg.builder()
      .type(IBOR)
      .payReceive(PAY)
      .paymentPeriods(IBOR_RATE_PAYMENT1, IBOR_RATE_PAYMENT2)
      .build();
  // fixed leg
  private static final RatePaymentPeriod FIXED_RATE_PAYMENT1 = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT1)
      .accrualPeriods(FIXED1)
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(NOTIONAL)
      .build();
  private static final RatePaymentPeriod FIXED_RATE_PAYMENT2 = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT2)
      .accrualPeriods(FIXED2)
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(NOTIONAL)
      .build();
  private static final ExpandedSwapLeg FIXED_LEG = ExpandedSwapLeg.builder()
      .type(FIXED)
      .payReceive(RECEIVE)
      .paymentPeriods(FIXED_RATE_PAYMENT1, FIXED_RATE_PAYMENT2)
      .build();
  

  public void test_cashFlowEquivalent() {
    // expected payments from fixed leg
    KnownAmountPaymentPeriod fixedKnownPayment1 = KnownAmountPaymentPeriod.of(
        Payment.of(GBP, NOTIONAL * RATE * PAY_YC1, PAYMENT1), SchedulePeriod.of(START1, END1));
    KnownAmountPaymentPeriod fixedKnownPayment2 = KnownAmountPaymentPeriod.of(
        Payment.of(GBP, NOTIONAL * RATE * PAY_YC2, PAYMENT2),
        SchedulePeriod.of(START2, END2));
    // expected payments from ibor leg
    LocalDate fixingSTART1 = GBP_LIBOR_3M.calculateEffectiveFromFixing(FIXING1);
    double fixedYearFraction1 = GBP_LIBOR_3M.getDayCount().relativeYearFraction(fixingSTART1,
        GBP_LIBOR_3M.calculateMaturityFromEffective(fixingSTART1));
    KnownAmountPaymentPeriod iborKnownPayment1 = KnownAmountPaymentPeriod.of(
        Payment.of(GBP, NOTIONAL * PAY_YC1 / fixedYearFraction1, PAYMENT1),
        SchedulePeriod.of(START1, END1));
    LocalDate fixingSTART2 = GBP_LIBOR_3M.calculateEffectiveFromFixing(FIXING2);
    double fixedYearFraction2 = GBP_LIBOR_3M.getDayCount().relativeYearFraction(fixingSTART2,
        GBP_LIBOR_3M.calculateMaturityFromEffective(fixingSTART2));
    KnownAmountPaymentPeriod iborKnownPayment2 = KnownAmountPaymentPeriod.of(
        Payment.of(GBP, NOTIONAL * PAY_YC2 / fixedYearFraction2, PAYMENT2),
        SchedulePeriod.of(START2, END2));
    RatePaymentPeriod ratePayment1 = RatePaymentPeriod.builder()
        .paymentDate(PAYMENT1)
        .accrualPeriods(IBOR1)
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-NOTIONAL * PAY_YC1 / fixedYearFraction1)
        .build();
    RatePaymentPeriod ratePayment2 = RatePaymentPeriod.builder()
        .paymentDate(PAYMENT2)
        .accrualPeriods(IBOR2)
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-NOTIONAL * PAY_YC2 / fixedYearFraction2)
        .build();

    ExpandedSwapLeg expected = ExpandedSwapLeg
        .builder()
        .type(OTHER)
        .payReceive(RECEIVE)
        .paymentPeriods(ratePayment1, iborKnownPayment1, ratePayment2, iborKnownPayment2,
            fixedKnownPayment1, fixedKnownPayment2)
        .build();
    Swap swap = Swap.builder()
        .legs(IBOR_LEG, FIXED_LEG)
        .build();
    ExpandedSwapLeg computed = CashFlowEquivalentCalculator.cashFlowEquivalent(swap);
    assertEquals(computed.getPaymentPeriods(), expected.getPaymentPeriods());
  }

  public void test_cashFlowEquivalent_compounding() {
    RatePaymentPeriod iborCmp = RatePaymentPeriod.builder()
        .paymentDate(PAYMENT2)
        .accrualPeriods(IBOR1, IBOR2)
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(-NOTIONAL)
        .build();
    ExpandedSwapLeg iborLegCmp = ExpandedSwapLeg.builder()
        .type(IBOR)
        .payReceive(PAY)
        .paymentPeriods(iborCmp)
        .build();
    Swap swap1 = Swap.builder()
        .legs(iborLegCmp, FIXED_LEG)
        .build();
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalent(swap1));
    RatePaymentPeriod fixedCmp = RatePaymentPeriod.builder()
        .paymentDate(PAYMENT2)
        .accrualPeriods(FIXED1, FIXED2)
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(NOTIONAL)
        .build();
    ExpandedSwapLeg fixedLegCmp = ExpandedSwapLeg.builder()
        .type(FIXED)
        .payReceive(RECEIVE)
        .paymentPeriods(fixedCmp)
        .build();
    Swap swap2 = Swap.builder()
        .legs(IBOR_LEG, fixedLegCmp)
        .build();
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalent(swap2));
  }
}
