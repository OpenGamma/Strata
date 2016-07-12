/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
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
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapLegPricer;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.swap.NotionalExchange;
import com.opengamma.strata.product.swap.SwapPaymentEvent;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;

/**
 * Test {@link CashFlowEquivalentCalculator}.
 */
@Test
public class CashFlowEquivalentCalculatorTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

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
  private static final IborRateComputation GBP_LIBOR_3M_COMP1 = IborRateComputation.of(GBP_LIBOR_3M, FIXING1, REF_DATA);
  private static final IborRateComputation GBP_LIBOR_3M_COMP2 = IborRateComputation.of(GBP_LIBOR_3M, FIXING2, REF_DATA);

  // accrual periods
  private static final  RateAccrualPeriod IBOR1 = RateAccrualPeriod.builder()
      .startDate(START1)
      .endDate(END1)
      .rateComputation(GBP_LIBOR_3M_COMP1)
      .yearFraction(PAY_YC1)
      .build();
  private static final  RateAccrualPeriod IBOR2 = RateAccrualPeriod.builder()
      .startDate(START2)
      .endDate(END2)
      .rateComputation(GBP_LIBOR_3M_COMP2)
      .yearFraction(PAY_YC2)
      .build();
  private static final  RateAccrualPeriod FIXED1 = RateAccrualPeriod.builder()
      .startDate(START1)
      .endDate(END1)
      .rateComputation(FixedRateComputation.of(RATE))
      .yearFraction(PAY_YC1)
      .build();
  private static final RateAccrualPeriod FIXED2 = RateAccrualPeriod.builder()
      .startDate(START2)
      .endDate(END2)
      .rateComputation(FixedRateComputation.of(RATE))
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
  private static final ResolvedSwapLeg IBOR_LEG = ResolvedSwapLeg.builder()
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
  private static final ResolvedSwapLeg FIXED_LEG = ResolvedSwapLeg.builder()
      .type(FIXED)
      .payReceive(RECEIVE)
      .paymentPeriods(FIXED_RATE_PAYMENT1, FIXED_RATE_PAYMENT2)
      .build();
  
  private static final ImmutableRatesProvider PROVIDER = RatesProviderDataSets.MULTI_GBP;
  private static final double TOLERANCE_PV = 1.0E-2;

  public void test_cashFlowEquivalent() {
    ResolvedSwap swap = ResolvedSwap.of(IBOR_LEG, FIXED_LEG);
    ResolvedSwapLeg computed = CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap, PROVIDER);
    ResolvedSwapLeg computedIborLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentIborLeg(IBOR_LEG, PROVIDER);
    ResolvedSwapLeg computedFixedLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentFixedLeg(FIXED_LEG, PROVIDER);
    assertEquals(computedFixedLeg.getPaymentEvents(), computed.getPaymentEvents().subList(0, 2));
    assertEquals(computedIborLeg.getPaymentEvents(), computed.getPaymentEvents().subList(2, 6));

    // expected payments from fixed leg
    NotionalExchange fixedPayment1 = NotionalExchange.of(CurrencyAmount.of(GBP, NOTIONAL * RATE * PAY_YC1), PAYMENT1);
    NotionalExchange fixedPayment2 = NotionalExchange.of(CurrencyAmount.of(GBP, NOTIONAL * RATE * PAY_YC2), PAYMENT2);
    // expected payments from ibor leg
    LocalDate fixingSTART1 = GBP_LIBOR_3M.calculateEffectiveFromFixing(FIXING1, REF_DATA);
    double fixedYearFraction1 = GBP_LIBOR_3M.getDayCount().relativeYearFraction(fixingSTART1,
        GBP_LIBOR_3M.calculateMaturityFromEffective(fixingSTART1, REF_DATA));
    double beta1 = (1d + fixedYearFraction1 * PROVIDER.iborIndexRates(GBP_LIBOR_3M).rate(GBP_LIBOR_3M_COMP1.getObservation()))
        * PROVIDER.discountFactor(GBP, PAYMENT1) / PROVIDER.discountFactor(GBP, fixingSTART1);
    NotionalExchange iborPayment11 =
        NotionalExchange.of(CurrencyAmount.of(GBP, -NOTIONAL * beta1 * PAY_YC1 / fixedYearFraction1), fixingSTART1);
    NotionalExchange iborPayment12 =
        NotionalExchange.of(CurrencyAmount.of(GBP, NOTIONAL * PAY_YC1 / fixedYearFraction1), PAYMENT1);
    LocalDate fixingSTART2 = GBP_LIBOR_3M.calculateEffectiveFromFixing(FIXING2, REF_DATA);
    double fixedYearFraction2 = GBP_LIBOR_3M.getDayCount().relativeYearFraction(fixingSTART2,
        GBP_LIBOR_3M.calculateMaturityFromEffective(fixingSTART2, REF_DATA));
    double beta2 = (1d + fixedYearFraction2 * PROVIDER.iborIndexRates(GBP_LIBOR_3M).rate(GBP_LIBOR_3M_COMP2.getObservation()))
        * PROVIDER.discountFactor(GBP, PAYMENT2) / PROVIDER.discountFactor(GBP, fixingSTART2);
    NotionalExchange iborPayment21 =
        NotionalExchange.of(CurrencyAmount.of(GBP, -NOTIONAL * beta2 * PAY_YC2 / fixedYearFraction2), fixingSTART2);
    NotionalExchange iborPayment22 =
        NotionalExchange.of(CurrencyAmount.of(GBP, NOTIONAL * PAY_YC2 / fixedYearFraction2), PAYMENT2);

    ResolvedSwapLeg expected = ResolvedSwapLeg
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
    ResolvedSwap swap = ResolvedSwap.of(IBOR_LEG, FIXED_LEG);
    ResolvedSwapLeg cfe = CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap, PROVIDER);
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
    ResolvedSwapLeg iborLegCmp = ResolvedSwapLeg.builder()
        .type(IBOR)
        .payReceive(PAY)
        .paymentPeriods(iborCmp)
        .build();
    ResolvedSwap swap1 = ResolvedSwap.of(iborLegCmp, FIXED_LEG);
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap1, PROVIDER));
    RatePaymentPeriod fixedCmp = RatePaymentPeriod.builder()
        .paymentDate(PAYMENT2)
        .accrualPeriods(FIXED1, FIXED2)
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(NOTIONAL)
        .build();
    ResolvedSwapLeg fixedLegCmp = ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(RECEIVE)
        .paymentPeriods(fixedCmp)
        .build();
    ResolvedSwap swap2 = ResolvedSwap.of(IBOR_LEG, fixedLegCmp);
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap2, PROVIDER));
  }

  public void test_cashFlowEquivalent_wrongSwap() {
    ResolvedSwap swap1 = ResolvedSwap.of(IBOR_LEG, FIXED_LEG, IBOR_LEG);
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap1, PROVIDER));
    ResolvedSwap swap2 = ResolvedSwap.of(FIXED_LEG, FIXED_LEG);
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap2, PROVIDER));
    ResolvedSwap swap3 = ResolvedSwap.of(
        FIXED_LEG,
        CashFlowEquivalentCalculator.cashFlowEquivalentIborLeg(IBOR_LEG, PROVIDER));
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap3, PROVIDER));
  }

  //-------------------------------------------------------------------------
  public void test_cashFlowEquivalentAndSensitivity() {
    ResolvedSwap swap = ResolvedSwap.of(IBOR_LEG, FIXED_LEG);
    ImmutableMap<Payment, PointSensitivityBuilder> computedFull =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap, PROVIDER);
    ImmutableList<Payment> keyComputedFull = computedFull.keySet().asList();
    ImmutableList<PointSensitivityBuilder> valueComputedFull = computedFull.values().asList();
    ImmutableMap<Payment, PointSensitivityBuilder> computedIborLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivityIborLeg(IBOR_LEG, PROVIDER);
    ImmutableMap<Payment, PointSensitivityBuilder> computedFixedLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivityFixedLeg(FIXED_LEG, PROVIDER);
    assertEquals(computedFixedLeg.keySet().asList(), keyComputedFull.subList(0, 2));
    assertEquals(computedIborLeg.keySet().asList(), keyComputedFull.subList(2, 6));
    assertEquals(computedFixedLeg.values().asList(), valueComputedFull.subList(0, 2));
    assertEquals(computedIborLeg.values().asList(), valueComputedFull.subList(2, 6));

    double eps = 1.0e-7;
    RatesFiniteDifferenceSensitivityCalculator calc = new RatesFiniteDifferenceSensitivityCalculator(eps);
    int size = keyComputedFull.size();
    for (int i = 0; i < size; ++i) {
      final int index = i;
      CurrencyParameterSensitivities expected = calc.sensitivity(PROVIDER,
          p -> ((NotionalExchange) CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap, p)
              .getPaymentEvents().get(index)).getPaymentAmount());
      SwapPaymentEvent event = CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap, PROVIDER).getPaymentEvents().get(index);
      PointSensitivityBuilder point = computedFull.get(((NotionalExchange) event).getPayment());
      CurrencyParameterSensitivities computed = PROVIDER.parameterSensitivity(point.build());
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
    ResolvedSwapLeg iborLegCmp = ResolvedSwapLeg.builder()
        .type(IBOR)
        .payReceive(PAY)
        .paymentPeriods(iborCmp)
        .build();
    ResolvedSwap swap1 = ResolvedSwap.of(iborLegCmp, FIXED_LEG);
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap1, PROVIDER));
    RatePaymentPeriod fixedCmp = RatePaymentPeriod.builder()
        .paymentDate(PAYMENT2)
        .accrualPeriods(FIXED1, FIXED2)
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(NOTIONAL)
        .build();
    ResolvedSwapLeg fixedLegCmp = ResolvedSwapLeg.builder()
        .type(FIXED)
        .payReceive(RECEIVE)
        .paymentPeriods(fixedCmp)
        .build();
    ResolvedSwap swap2 = ResolvedSwap.of(IBOR_LEG, fixedLegCmp);
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap2, PROVIDER));
  }

  public void test_cashFlowEquivalentAndSensitivity_wrongSwap() {
    ResolvedSwap swap1 = ResolvedSwap.of(IBOR_LEG, FIXED_LEG, IBOR_LEG);
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap1, PROVIDER));
    ResolvedSwap swap2 = ResolvedSwap.of(FIXED_LEG, FIXED_LEG);
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap2, PROVIDER));
    ResolvedSwap swap3 = ResolvedSwap.of(FIXED_LEG, CashFlowEquivalentCalculator.cashFlowEquivalentIborLeg(IBOR_LEG, PROVIDER));
    assertThrowsIllegalArg(() -> CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap3, PROVIDER));
  }

}
