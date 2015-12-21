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
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapLegPricer;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.rate.FixedRateObservation;
import com.opengamma.strata.product.rate.IborRateObservation;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.NotionalExchange;
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
  
  private static final ImmutableRatesProvider PROVIDER = RatesProviderDataSets.MULTI_GBP;
  private static final double TOLERANCE_PV = 1.0E-2;

  public void test_cashFlowEquivalent() {
    Swap swap = Swap.builder()
        .legs(IBOR_LEG, FIXED_LEG)
        .build();
    ExpandedSwapLeg computed = CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap.expand(), PROVIDER);
    ExpandedSwapLeg computedIborLeg = CashFlowEquivalentCalculator.cashFlowEquivalentIborLeg(IBOR_LEG.expand(), PROVIDER);
    ExpandedSwapLeg computedFixedLeg = CashFlowEquivalentCalculator.cashFlowEquivalentFixedLeg(FIXED_LEG.expand(), PROVIDER);
    assertEquals(computedFixedLeg.getPaymentEvents(), computed.getPaymentEvents().subList(0, 2));
    assertEquals(computedIborLeg.getPaymentEvents(), computed.getPaymentEvents().subList(2, 6));

    // expected payments from fixed leg
    NotionalExchange fixedPayment1 = NotionalExchange.of(PAYMENT1, CurrencyAmount.of(GBP, NOTIONAL * RATE * PAY_YC1));
    NotionalExchange fixedPayment2 = NotionalExchange.of(PAYMENT2, CurrencyAmount.of(GBP, NOTIONAL * RATE * PAY_YC2));
    // expected payments from ibor leg
    LocalDate fixingSTART1 = GBP_LIBOR_3M.calculateEffectiveFromFixing(FIXING1);
    double fixedYearFraction1 = GBP_LIBOR_3M.getDayCount().relativeYearFraction(fixingSTART1,
        GBP_LIBOR_3M.calculateMaturityFromEffective(fixingSTART1));
    double beta1 = (1d + fixedYearFraction1 * PROVIDER.iborIndexRates(GBP_LIBOR_3M).rate(FIXING1))
        * PROVIDER.discountFactor(GBP, PAYMENT1) / PROVIDER.discountFactor(GBP, fixingSTART1);
    NotionalExchange iborPayment11 =
        NotionalExchange.of(fixingSTART1, CurrencyAmount.of(GBP, -NOTIONAL * beta1 * PAY_YC1 / fixedYearFraction1));
    NotionalExchange iborPayment12 =
        NotionalExchange.of(PAYMENT1, CurrencyAmount.of(GBP, NOTIONAL * PAY_YC1 / fixedYearFraction1));
    LocalDate fixingSTART2 = GBP_LIBOR_3M.calculateEffectiveFromFixing(FIXING2);
    double fixedYearFraction2 = GBP_LIBOR_3M.getDayCount().relativeYearFraction(fixingSTART2,
        GBP_LIBOR_3M.calculateMaturityFromEffective(fixingSTART2));
    double beta2 = (1d + fixedYearFraction2 * PROVIDER.iborIndexRates(GBP_LIBOR_3M).rate(FIXING2))
        * PROVIDER.discountFactor(GBP, PAYMENT2) / PROVIDER.discountFactor(GBP, fixingSTART2);
    NotionalExchange iborPayment21 =
        NotionalExchange.of(fixingSTART2, CurrencyAmount.of(GBP, -NOTIONAL * beta2 * PAY_YC2 / fixedYearFraction2));
    NotionalExchange iborPayment22 =
        NotionalExchange.of(PAYMENT2, CurrencyAmount.of(GBP, NOTIONAL * PAY_YC2 / fixedYearFraction2));

    ExpandedSwapLeg expected = ExpandedSwapLeg
        .builder()
        .type(OTHER)
        .payReceive(RECEIVE)
        .paymentEvents(fixedPayment1, fixedPayment2, iborPayment11, iborPayment12, iborPayment21, iborPayment22)
        .build();

    double eps = 1.0e-12;
    assertEquals(computed.getPaymentEvents().size(), expected.getPaymentEvents().size());
    for (int i = 0; i < 6; ++i) {
      NotionalExchange payCmp = (NotionalExchange) computed.getPaymentEvents().get(i);
      NotionalExchange payExp = (NotionalExchange) expected.getPaymentEvents().get(i);
      assertEquals(payCmp.getCurrency(), payExp.getCurrency());
      assertEquals(payCmp.getPaymentDate(), payExp.getPaymentDate());
      assertTrue(DoubleMath.fuzzyEquals(payCmp.getPaymentAmount().getAmount(),
          payExp.getPaymentAmount().getAmount(), NOTIONAL * eps));
    }
  }
  
  public void test_cashFlowEquivalent_pv() {
    Swap swap = Swap.builder()
        .legs(IBOR_LEG, FIXED_LEG)
        .build();
    ExpandedSwapLeg cfe = CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap.expand(), PROVIDER);
    DiscountingSwapLegPricer pricerLeg = DiscountingSwapLegPricer.DEFAULT;
    DiscountingSwapProductPricer pricerSwap = DiscountingSwapProductPricer.DEFAULT;
    CurrencyAmount pvCfe = pricerLeg.presentValue(cfe, PROVIDER);
    MultiCurrencyAmount pvSwap = pricerSwap.presentValue(swap, PROVIDER);
    assertEquals(pvCfe.getAmount(), pvSwap.getAmount(GBP).getAmount(), TOLERANCE_PV);    
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
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap1.expand(), PROVIDER));
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
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap2.expand(), PROVIDER));
  }

  public void test_cashFlowEquivalent_wrongSwap() {
    Swap swap1 = Swap.builder()
        .legs(IBOR_LEG, FIXED_LEG, IBOR_LEG)
        .build();
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap1.expand(), PROVIDER));
    Swap swap2 = Swap.builder()
        .legs(FIXED_LEG, FIXED_LEG)
        .build();
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap2.expand(), PROVIDER));
    Swap swap3 = Swap.builder()
        .legs(FIXED_LEG, CashFlowEquivalentCalculator.cashFlowEquivalentIborLeg(IBOR_LEG, PROVIDER))
        .build();
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap3.expand(), PROVIDER));
  }

  //-------------------------------------------------------------------------
  public void test_cashFlowEquivalentAndSensitivity() {
    Swap swap = Swap.builder()
        .legs(IBOR_LEG, FIXED_LEG)
        .build();
    ImmutableMap<NotionalExchange, PointSensitivityBuilder> computedFull =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap.expand(), PROVIDER);
    ImmutableList<NotionalExchange> keyComputedFull = computedFull.keySet().asList();
    ImmutableList<PointSensitivityBuilder> valueComputedFull = computedFull.values().asList();
    ImmutableMap<NotionalExchange, PointSensitivityBuilder> computedIborLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivityIborLeg(IBOR_LEG.expand(), PROVIDER);
    ImmutableMap<NotionalExchange, PointSensitivityBuilder> computedFixedLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivityFixedLeg(FIXED_LEG.expand(), PROVIDER);
    assertEquals(computedFixedLeg.keySet().asList(), keyComputedFull.subList(0, 2));
    assertEquals(computedIborLeg.keySet().asList(), keyComputedFull.subList(2, 6));
    assertEquals(computedFixedLeg.values().asList(), valueComputedFull.subList(0, 2));
    assertEquals(computedIborLeg.values().asList(), valueComputedFull.subList(2, 6));

    double eps = 1.0e-7;
    RatesFiniteDifferenceSensitivityCalculator calc = new RatesFiniteDifferenceSensitivityCalculator(eps);
    int size = keyComputedFull.size();
    for (int i = 0; i < size; ++i) {
      final int index = i;
      CurveCurrencyParameterSensitivities expected = calc.sensitivity(PROVIDER,
          p -> ((NotionalExchange) CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap.expand(), p)
              .getPaymentEvents().get(index)).getPaymentAmount());
      PointSensitivityBuilder point = computedFull.get(
          CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap.expand(), PROVIDER).getPaymentEvents().get(index));
      CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point.build());
      assertTrue(computed.equalWithTolerance(expected, eps * NOTIONAL));
    }
  }

  public void test_cashFlowEquivalentAndSensitivity_compounding() {
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
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap1.expand(), PROVIDER));
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
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap2.expand(), PROVIDER));
  }

  public void test_cashFlowEquivalentAndSensitivity_wrongSwap() {
    Swap swap1 = Swap.builder()
        .legs(IBOR_LEG, FIXED_LEG, IBOR_LEG)
        .build();
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap1.expand(), PROVIDER));
    Swap swap2 = Swap.builder()
        .legs(FIXED_LEG, FIXED_LEG)
        .build();
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap2.expand(), PROVIDER));
    Swap swap3 = Swap.builder()
        .legs(FIXED_LEG, CashFlowEquivalentCalculator.cashFlowEquivalentIborLeg(IBOR_LEG, PROVIDER))
        .build();
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap3.expand(), PROVIDER));
  }
}
