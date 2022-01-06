/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.swap.SwapLegType.FIXED;
import static com.opengamma.strata.product.swap.SwapLegType.IBOR;
import static com.opengamma.strata.product.swap.SwapLegType.OTHER;
import static com.opengamma.strata.product.swap.SwapLegType.OVERNIGHT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapLegPricer;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.rate.OvernightRateComputation;
import com.opengamma.strata.product.swap.NotionalExchange;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.SwapPaymentEvent;

/**
 * Test {@link CashFlowEquivalentCalculator}.
 */
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
  private static final OvernightRateComputation GBP_SONIA_COMP1 =
      OvernightRateComputation.of(GBP_SONIA, START1, END1, 0, OvernightAccrualMethod.COMPOUNDED, REF_DATA);
  private static final OvernightRateComputation GBP_SONIA_COMP2 =
      OvernightRateComputation.of(GBP_SONIA, START2, END2, 0, OvernightAccrualMethod.COMPOUNDED, REF_DATA);

  // accrual periods
  private static final RateAccrualPeriod IBOR1 = RateAccrualPeriod.builder()
      .startDate(START1)
      .endDate(END1)
      .rateComputation(GBP_LIBOR_3M_COMP1)
      .yearFraction(PAY_YC1)
      .build();
  private static final RateAccrualPeriod IBOR2 = RateAccrualPeriod.builder()
      .startDate(START2)
      .endDate(END2)
      .rateComputation(GBP_LIBOR_3M_COMP2)
      .yearFraction(PAY_YC2)
      .build();
  private static final RateAccrualPeriod FIXED1 = RateAccrualPeriod.builder()
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
  private static final RateAccrualPeriod ON1 = RateAccrualPeriod.builder()
      .startDate(START1)
      .endDate(END1)
      .rateComputation(GBP_SONIA_COMP1)
      .yearFraction(PAY_YC1)
      .build();
  private static final RateAccrualPeriod ON2 = RateAccrualPeriod.builder()
      .startDate(START2)
      .endDate(END2)
      .rateComputation(GBP_SONIA_COMP2)
      .yearFraction(PAY_YC2)
      .spread(RATE)
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
  //Overnight leg
  private static final RatePaymentPeriod ON_RATE_PAYMENT1 = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT1)
      .accrualPeriods(ON1)
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(NOTIONAL)
      .build();
  private static final RatePaymentPeriod ON_RATE_PAYMENT2 = RatePaymentPeriod.builder()
      .paymentDate(PAYMENT2)
      .accrualPeriods(ON2)
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(NOTIONAL)
      .build();
  private static final ResolvedSwapLeg ON_LEG = ResolvedSwapLeg.builder()
      .type(OVERNIGHT)
      .payReceive(RECEIVE)
      .paymentPeriods(ON_RATE_PAYMENT1, ON_RATE_PAYMENT2)
      .build();

  private static final ImmutableRatesProvider PROVIDER = RatesProviderDataSets.MULTI_GBP;
  private static final double TOLERANCE_PV = 1.0E-2;

  @Test
  public void test_cashFlowEquivalent_onleg() {
    ResolvedSwapLeg computedOnLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentOnLeg(ON_LEG, PROVIDER);
    int nbEquiv = 4; // 2 coupons x 2 dates
    assertThat(computedOnLeg.getPaymentEvents()).hasSize(nbEquiv);
    List<NotionalExchange> onEquivalent = new ArrayList<>();
    double ratio1 = PROVIDER.discountFactor(GBP, PAYMENT1) / PROVIDER.discountFactor(GBP, END1);
    double computationAccrual1 = ACT_365F.yearFraction(START1, END1);
    double ratioAccrual1 = PAY_YC1 / computationAccrual1;
    onEquivalent.add(NotionalExchange.of(CurrencyAmount.of(GBP, NOTIONAL * ratio1 * ratioAccrual1), START1));
    onEquivalent.add(NotionalExchange.of(CurrencyAmount.of(GBP, -NOTIONAL * ratioAccrual1), PAYMENT1));
    double ratio2 = PROVIDER.discountFactor(GBP, PAYMENT2) / PROVIDER.discountFactor(GBP, END2);
    double computationAccrual2 = ACT_365F.yearFraction(START2, END2);
    double ratioAccrual2 = PAY_YC2 / computationAccrual2;
    onEquivalent.add(NotionalExchange.of(CurrencyAmount.of(GBP, NOTIONAL * ratio2 * ratioAccrual2), START2));
    onEquivalent.add(NotionalExchange.of(
        CurrencyAmount.of(GBP, -NOTIONAL * (ratioAccrual2 - RATE * PAY_YC2)), PAYMENT2));
    for (int i = 0; i < nbEquiv; ++i) {
      NotionalExchange payCmp = (NotionalExchange) computedOnLeg.getPaymentEvents().get(i);
      assertThat(payCmp.getCurrency()).isEqualTo(GBP);
      assertThat(payCmp.getPaymentDate()).isEqualTo(onEquivalent.get(i).getPaymentDate());
      assertThat(DoubleMath.fuzzyEquals(payCmp.getPaymentAmount().getAmount(),
          onEquivalent.get(i).getPaymentAmount().getAmount(), TOLERANCE_PV)).isTrue();
      assertThat(payCmp).isEqualTo(onEquivalent.get(i));
    }
  }

  @Test
  public void test_cashFlowEquivalent_swap_on() {
    ResolvedSwap swap = ResolvedSwap.of(ON_LEG, FIXED_LEG);
    ResolvedSwapLeg computed = CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap, PROVIDER);
    ResolvedSwapLeg computedOnLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentOnLeg(ON_LEG, PROVIDER);
    ResolvedSwapLeg computedFixedLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentFixedLeg(FIXED_LEG, PROVIDER);
    assertThat(computedOnLeg.getPaymentEvents()).isEqualTo(computed.getPaymentEvents().subList(0, 4));
    assertThat(computedFixedLeg.getPaymentEvents()).isEqualTo(computed.getPaymentEvents().subList(4, 6));
  }

  @Test
  public void test_cashFlowEquivalent() {
    ResolvedSwap swap = ResolvedSwap.of(IBOR_LEG, FIXED_LEG);
    ResolvedSwapLeg computed = CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap, PROVIDER);
    ResolvedSwapLeg computedIborLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentIborLeg(IBOR_LEG, PROVIDER);
    ResolvedSwapLeg computedFixedLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentFixedLeg(FIXED_LEG, PROVIDER);
    assertThat(computedIborLeg.getPaymentEvents()).isEqualTo(computed.getPaymentEvents().subList(0, 4));
    assertThat(computedFixedLeg.getPaymentEvents()).isEqualTo(computed.getPaymentEvents().subList(4, 6));

    // expected payments from fixed leg
    NotionalExchange fixedPayment1 = NotionalExchange.of(CurrencyAmount.of(GBP, NOTIONAL * RATE * PAY_YC1), PAYMENT1);
    NotionalExchange fixedPayment2 = NotionalExchange.of(CurrencyAmount.of(GBP, NOTIONAL * RATE * PAY_YC2), PAYMENT2);
    // expected payments from ibor leg
    LocalDate fixingSTART1 = GBP_LIBOR_3M.calculateEffectiveFromFixing(FIXING1, REF_DATA);
    double fixedYearFraction1 = GBP_LIBOR_3M.getDayCount().relativeYearFraction(fixingSTART1,
        GBP_LIBOR_3M.calculateMaturityFromEffective(fixingSTART1, REF_DATA));
    double beta1 =
        (1d + fixedYearFraction1 * PROVIDER.iborIndexRates(GBP_LIBOR_3M).rate(GBP_LIBOR_3M_COMP1.getObservation())) *
            PROVIDER.discountFactor(GBP, PAYMENT1) / PROVIDER.discountFactor(GBP, fixingSTART1);
    NotionalExchange iborPayment11 =
        NotionalExchange.of(CurrencyAmount.of(GBP, -NOTIONAL * beta1 * PAY_YC1 / fixedYearFraction1), fixingSTART1);
    NotionalExchange iborPayment12 =
        NotionalExchange.of(CurrencyAmount.of(GBP, NOTIONAL * PAY_YC1 / fixedYearFraction1), PAYMENT1);
    LocalDate fixingSTART2 = GBP_LIBOR_3M.calculateEffectiveFromFixing(FIXING2, REF_DATA);
    double fixedYearFraction2 = GBP_LIBOR_3M.getDayCount().relativeYearFraction(fixingSTART2,
        GBP_LIBOR_3M.calculateMaturityFromEffective(fixingSTART2, REF_DATA));
    double beta2 =
        (1d + fixedYearFraction2 * PROVIDER.iborIndexRates(GBP_LIBOR_3M).rate(GBP_LIBOR_3M_COMP2.getObservation())) *
            PROVIDER.discountFactor(GBP, PAYMENT2) / PROVIDER.discountFactor(GBP, fixingSTART2);
    NotionalExchange iborPayment21 =
        NotionalExchange.of(CurrencyAmount.of(GBP, -NOTIONAL * beta2 * PAY_YC2 / fixedYearFraction2), fixingSTART2);
    NotionalExchange iborPayment22 =
        NotionalExchange.of(CurrencyAmount.of(GBP, NOTIONAL * PAY_YC2 / fixedYearFraction2), PAYMENT2);

    ResolvedSwapLeg expected = ResolvedSwapLeg
        .builder()
        .type(OTHER)
        .payReceive(RECEIVE)
        .paymentEvents(iborPayment11, iborPayment12, iborPayment21, iborPayment22, fixedPayment1, fixedPayment2)
        .build();

    double eps = 1.0e-12;
    assertThat(computed.getPaymentEvents()).hasSize(expected.getPaymentEvents().size());
    for (int i = 0; i < 6; ++i) {
      NotionalExchange payCmp = (NotionalExchange) computed.getPaymentEvents().get(i);
      NotionalExchange payExp = (NotionalExchange) expected.getPaymentEvents().get(i);
      assertThat(payCmp.getCurrency()).isEqualTo(payExp.getCurrency());
      assertThat(payCmp.getPaymentDate()).isEqualTo(payExp.getPaymentDate());
      assertThat(DoubleMath.fuzzyEquals(payCmp.getPaymentAmount().getAmount(),
          payExp.getPaymentAmount().getAmount(), NOTIONAL * eps)).isTrue();
    }
  }

  @Test
  public void test_cashFlowEquivalent_swap_3legs() {
    ResolvedSwap swap = ResolvedSwap.of(ON_LEG, FIXED_LEG, IBOR_LEG);
    ResolvedSwapLeg computed = CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap, PROVIDER);
    ResolvedSwapLeg computedOnLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentOnLeg(ON_LEG, PROVIDER);
    ResolvedSwapLeg computedFixedLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentFixedLeg(FIXED_LEG, PROVIDER);
    ResolvedSwapLeg computedIborLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentIborLeg(IBOR_LEG, PROVIDER);
    assertThat(computedOnLeg.getPaymentEvents()).isEqualTo(computed.getPaymentEvents().subList(0, 4));
    assertThat(computedFixedLeg.getPaymentEvents()).isEqualTo(computed.getPaymentEvents().subList(4, 6));
    assertThat(computedIborLeg.getPaymentEvents()).isEqualTo(computed.getPaymentEvents().subList(6, 10));
  }

  @Test
  public void test_cashFlowEquivalent_pv() {
    ResolvedSwap swap = ResolvedSwap.of(IBOR_LEG, FIXED_LEG);
    ResolvedSwapLeg cfe = CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap, PROVIDER);
    DiscountingSwapLegPricer pricerLeg = DiscountingSwapLegPricer.DEFAULT;
    DiscountingSwapProductPricer pricerSwap = DiscountingSwapProductPricer.DEFAULT;
    CurrencyAmount pvCfe = pricerLeg.presentValue(cfe, PROVIDER);
    MultiCurrencyAmount pvSwap = pricerSwap.presentValue(swap, PROVIDER);
    assertThat(pvCfe.getAmount()).isCloseTo(pvSwap.getAmount(GBP).getAmount(), offset(TOLERANCE_PV));
  }

  @Test
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
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap1, PROVIDER));
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
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap2, PROVIDER));
  }

  @Test
  public void test_cashFlowEquivalent_wrongSwap() {
    ResolvedSwap swap3 = ResolvedSwap.of(
        FIXED_LEG,
        CashFlowEquivalentCalculator.cashFlowEquivalentIborLeg(IBOR_LEG, PROVIDER));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap3, PROVIDER));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cashFlowEquivalentAndSensitivity_onleg() {
    ResolvedSwapLeg computedCfe =
        CashFlowEquivalentCalculator.cashFlowEquivalentOnLeg(ON_LEG, PROVIDER);
    ImmutableMap<Payment, PointSensitivityBuilder> computedCfeSensi =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivityOnLeg(ON_LEG, PROVIDER);
    int nbEquiv = 4; // 2 coupons x 2 dates
    assertThat(computedCfe.getPaymentEvents()).hasSize(nbEquiv);
    assertThat(computedCfeSensi).hasSize(nbEquiv);
    // expected payments from fixed leg
    for (int i = 0; i < nbEquiv; ++i) {
      Payment payment = Payment.of(computedCfe.getPaymentEvents().get(i).getCurrency(),
          ((NotionalExchange) computedCfe.getPaymentEvents().get(i)).getPaymentAmount().getAmount(),
          computedCfe.getPaymentEvents().get(i).getPaymentDate());
      assertThat(computedCfeSensi.containsKey(payment)).isTrue();
    }
    ImmutableList<Payment> keyComputedFull = computedCfeSensi.keySet().asList();
    double eps = 1.0e-7;
    RatesFiniteDifferenceSensitivityCalculator calc = new RatesFiniteDifferenceSensitivityCalculator(eps);
    int size = keyComputedFull.size();
    for (int i = 0; i < size; ++i) {
      final int index = i;
      CurrencyParameterSensitivities expected = calc.sensitivity(PROVIDER,
          p -> ((NotionalExchange) CashFlowEquivalentCalculator.cashFlowEquivalentOnLeg(ON_LEG, p)
              .getPaymentEvents().get(index)).getPaymentAmount());
      SwapPaymentEvent event = CashFlowEquivalentCalculator.cashFlowEquivalentOnLeg(ON_LEG, PROVIDER)
          .getPaymentEvents().get(index);
      PointSensitivityBuilder point = computedCfeSensi.get(((NotionalExchange) event).getPayment());
      CurrencyParameterSensitivities computed = PROVIDER.parameterSensitivity(point.build());
      assertThat(computed.equalWithTolerance(expected, eps * NOTIONAL)).isTrue();
    }
  }

  @Test
  public void test_cashFlowEquivalentAndSensitivity_3legs() {
    ResolvedSwap swap = ResolvedSwap.of(ON_LEG, FIXED_LEG, IBOR_LEG);
    ImmutableMap<Payment, PointSensitivityBuilder> computed =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap, PROVIDER);
    ImmutableList<Payment> keyComputedFull = computed.keySet().asList();
    ImmutableList<PointSensitivityBuilder> valueComputedFull = computed.values().asList();
    ImmutableMap<Payment, PointSensitivityBuilder> computedOnLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivityOnLeg(ON_LEG, PROVIDER);
    ImmutableMap<Payment, PointSensitivityBuilder> computedFixedLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivityFixedLeg(FIXED_LEG, PROVIDER);
    ImmutableMap<Payment, PointSensitivityBuilder> computedIborLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivityIborLeg(IBOR_LEG, PROVIDER);
    assertThat(computedOnLeg.keySet().asList()).isEqualTo(keyComputedFull.subList(0, 4));
    assertThat(computedFixedLeg.keySet().asList()).isEqualTo(keyComputedFull.subList(4, 6));
    assertThat(computedIborLeg.keySet().asList()).isEqualTo(keyComputedFull.subList(6, 10));
    assertThat(computedOnLeg.values().asList()).isEqualTo(valueComputedFull.subList(0, 4));
    assertThat(computedFixedLeg.values().asList()).isEqualTo(valueComputedFull.subList(4, 6));
    assertThat(computedIborLeg.values().asList()).isEqualTo(valueComputedFull.subList(6, 10));
  }

  @Test
  public void test_cashFlowEquivalentAndSensitivity() {
    ResolvedSwap swap = ResolvedSwap.of(FIXED_LEG, IBOR_LEG);
    ImmutableMap<Payment, PointSensitivityBuilder> computedFull =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap, PROVIDER);
    ImmutableList<Payment> keyComputedFull = computedFull.keySet().asList();
    ImmutableList<PointSensitivityBuilder> valueComputedFull = computedFull.values().asList();
    ImmutableMap<Payment, PointSensitivityBuilder> computedIborLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivityIborLeg(IBOR_LEG, PROVIDER);
    ImmutableMap<Payment, PointSensitivityBuilder> computedFixedLeg =
        CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivityFixedLeg(FIXED_LEG, PROVIDER);
    assertThat(computedFixedLeg.keySet().asList()).isEqualTo(keyComputedFull.subList(0, 2));
    assertThat(computedIborLeg.keySet().asList()).isEqualTo(keyComputedFull.subList(2, 6));
    assertThat(computedFixedLeg.values().asList()).isEqualTo(valueComputedFull.subList(0, 2));
    assertThat(computedIborLeg.values().asList()).isEqualTo(valueComputedFull.subList(2, 6));

    double eps = 1.0e-7;
    RatesFiniteDifferenceSensitivityCalculator calc = new RatesFiniteDifferenceSensitivityCalculator(eps);
    int size = keyComputedFull.size();
    for (int i = 0; i < size; ++i) {
      final int index = i;
      CurrencyParameterSensitivities expected = calc.sensitivity(PROVIDER,
          p -> ((NotionalExchange) CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap, p)
              .getPaymentEvents().get(index)).getPaymentAmount());
      SwapPaymentEvent event =
          CashFlowEquivalentCalculator.cashFlowEquivalentSwap(swap, PROVIDER).getPaymentEvents().get(index);
      PointSensitivityBuilder point = computedFull.get(((NotionalExchange) event).getPayment());
      CurrencyParameterSensitivities computed = PROVIDER.parameterSensitivity(point.build());
      assertThat(computed.equalWithTolerance(expected, eps * NOTIONAL)).isTrue();
    }
  }

  @Test
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
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap1, PROVIDER));
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
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap2, PROVIDER));
  }

  @Test
  public void test_cashFlowEquivalent_normalize() {
    List<Payment> cfeInput = new ArrayList<>();
    cfeInput.add(Payment.of(CurrencyAmount.of(GBP, 1.0), LocalDate.of(2020, 1, 3)));
    cfeInput.add(Payment.of(CurrencyAmount.of(GBP, 2.0), LocalDate.of(2020, 1, 5)));
    cfeInput.add(Payment.of(CurrencyAmount.of(GBP, 3.0), LocalDate.of(2020, 1, 2)));
    cfeInput.add(Payment.of(CurrencyAmount.of(GBP, 4.0), LocalDate.of(2020, 1, 1)));
    cfeInput.add(Payment.of(CurrencyAmount.of(GBP, 5.0), LocalDate.of(2020, 1, 5)));
    cfeInput.add(Payment.of(CurrencyAmount.of(GBP, -6.0), LocalDate.of(2020, 1, 2)));
    List<Payment> cfeComputed = CashFlowEquivalentCalculator.normalize(cfeInput);
    assertThat(cfeComputed.size()).isEqualTo(4);
    List<Payment> cfeExpected = new ArrayList<>();
    cfeExpected.add(Payment.of(CurrencyAmount.of(GBP, 4.0), LocalDate.of(2020, 1, 1)));
    cfeExpected.add(Payment.of(CurrencyAmount.of(GBP, -3.0), LocalDate.of(2020, 1, 2)));
    cfeExpected.add(Payment.of(CurrencyAmount.of(GBP, 1.0), LocalDate.of(2020, 1, 3)));
    cfeExpected.add(Payment.of(CurrencyAmount.of(GBP, 7.0), LocalDate.of(2020, 1, 5)));
    assertThat(cfeExpected).isEqualTo(cfeComputed);
  }

  @Test
  public void test_cashFlowEquivalent_normalize_sensi() {
    Map<Payment, PointSensitivityBuilder> cfeInput = new HashMap<>();
    cfeInput.put(Payment.of(CurrencyAmount.of(GBP, 1.0), LocalDate.of(2020, 1, 3)), PointSensitivityBuilder.none());
    cfeInput.put(Payment.of(CurrencyAmount.of(GBP, 2.0), LocalDate.of(2020, 1, 5)), ZeroRateSensitivity.of(GBP, 0.01d, 10.0d));
    cfeInput.put(Payment.of(CurrencyAmount.of(GBP, 3.0), LocalDate.of(2020, 1, 2)), ZeroRateSensitivity.of(GBP, 1.00d, 25.0d));
    cfeInput.put(Payment.of(CurrencyAmount.of(GBP, 4.0), LocalDate.of(2020, 1, 1)), ZeroRateSensitivity.of(GBP, 1.00d, 25.0d));
    cfeInput.put(Payment.of(CurrencyAmount.of(GBP, 5.0), LocalDate.of(2020, 1, 5)), ZeroRateSensitivity.of(GBP, 2.50d, -10.0d));
    cfeInput.put(Payment.of(CurrencyAmount.of(GBP, -6.0), LocalDate.of(2020, 1, 2)),
        ZeroRateSensitivity.of(GBP, 2.00d, 25.0d).combinedWith(ZeroRateSensitivity.of(GBP, 3.00d, 45.0d)));
    cfeInput.put(Payment.of(CurrencyAmount.of(GBP, -7.0), LocalDate.of(2020, 1, 2)),
        ZeroRateSensitivity.of(GBP, 5.00d, 25.0d));
    Map<Payment, PointSensitivityBuilder> cfeComputed = CashFlowEquivalentCalculator.normalize(cfeInput);
    assertThat(cfeComputed.size()).isEqualTo(4);
    Map<Payment, PointSensitivityBuilder> cfeExpected = new HashMap<>();
    cfeExpected.put(Payment.of(CurrencyAmount.of(GBP, 4.0), LocalDate.of(2020, 1, 1)),
        ZeroRateSensitivity.of(GBP, 1.00d, 25.0d));
    cfeExpected.put(Payment.of(CurrencyAmount.of(GBP, -10.0), LocalDate.of(2020, 1, 2)),
        ZeroRateSensitivity.of(GBP, 1.00d, 25.0d).combinedWith(ZeroRateSensitivity.of(GBP, 2.00d, 25.0d)
            .combinedWith(ZeroRateSensitivity.of(GBP, 3.00d, 45.0d))
            .combinedWith(ZeroRateSensitivity.of(GBP, 5.00d, 25.0d))));
    cfeExpected.put(Payment.of(CurrencyAmount.of(GBP, 1.0), LocalDate.of(2020, 1, 3)), PointSensitivityBuilder.none());
    cfeExpected.put(Payment.of(CurrencyAmount.of(GBP, 7.0), LocalDate.of(2020, 1, 5)),
        ZeroRateSensitivity.of(GBP, 0.01d, 10.0d).combinedWith(ZeroRateSensitivity.of(GBP, 2.50d, -10.0d)));
    for (Entry<Payment, PointSensitivityBuilder> cf : cfeComputed.entrySet()) {
      assertThat(cfeExpected.containsKey(cf.getKey())).isTrue();
      if (cf.getValue() instanceof MutablePointSensitivities) {
        ImmutableList<PointSensitivity> listComputed = ((MutablePointSensitivities) cf.getValue()).getSensitivities();
        ImmutableList<PointSensitivity> listExpected =
            ((MutablePointSensitivities) cfeExpected.get(cf.getKey())).getSensitivities();
        assertThat(listComputed.size()).isEqualTo(listExpected.size());
        for (PointSensitivity e : listComputed) {
          assertThat(listExpected.contains(e)).isTrue();
        }
      } else {
        assertThat(cf.getValue()).isEqualTo(cfeExpected.get(cf.getKey()));
      }
    }
  }

  @Test
  public void test_cashFlowEquivalentAndSensitivity_wrongSwap() {
    ResolvedSwap swap3 =
        ResolvedSwap.of(FIXED_LEG, CashFlowEquivalentCalculator.cashFlowEquivalentIborLeg(IBOR_LEG, PROVIDER));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CashFlowEquivalentCalculator.cashFlowEquivalentAndSensitivitySwap(swap3, PROVIDER));
  }

}
