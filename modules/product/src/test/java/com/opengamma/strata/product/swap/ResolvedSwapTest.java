/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.swap.SwapLegType.FIXED;
import static com.opengamma.strata.product.swap.SwapLegType.IBOR;
import static com.opengamma.strata.product.swap.SwapLegType.OTHER;
import static com.opengamma.strata.product.swap.SwapLegType.OVERNIGHT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test.
 */
public class ResolvedSwapTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);
  private static final LocalDate DATE_2014_09_30 = date(2014, 9, 30);
  private static final LocalDate DATE_2014_10_01 = date(2014, 10, 1);
  private static final IborRateComputation GBP_LIBOR_3M_2014_06_28 =
      IborRateComputation.of(GBP_LIBOR_3M, date(2014, 6, 28), REF_DATA);
  private static final NotionalExchange NOTIONAL_EXCHANGE =
      NotionalExchange.of(CurrencyAmount.of(GBP, 2000d), DATE_2014_10_01);
  private static final RateAccrualPeriod RAP = RateAccrualPeriod.builder()
      .startDate(DATE_2014_06_30)
      .endDate(DATE_2014_09_30)
      .yearFraction(0.25d)
      .rateComputation(GBP_LIBOR_3M_2014_06_28)
      .build();
  private static final RatePaymentPeriod RPP1 = RatePaymentPeriod.builder()
      .paymentDate(DATE_2014_10_01)
      .accrualPeriods(RAP)
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(5000d)
      .build();
  private static final RatePaymentPeriod RPP2 = RatePaymentPeriod.builder()
      .paymentDate(DATE_2014_10_01)
      .accrualPeriods(RAP)
      .dayCount(ACT_365F)
      .currency(USD)
      .notional(6000d)
      .build();
  static final ResolvedSwapLeg LEG1 = ResolvedSwapLeg.builder()
      .type(FIXED)
      .payReceive(PAY)
      .paymentPeriods(RPP1)
      .paymentEvents(NOTIONAL_EXCHANGE)
      .build();
  static final ResolvedSwapLeg LEG2 = ResolvedSwapLeg.builder()
      .type(IBOR)
      .payReceive(RECEIVE)
      .paymentPeriods(RPP2)
      .build();

  @Test
  public void test_of() {
    ResolvedSwap test = ResolvedSwap.of(LEG1, LEG2);
    assertThat(test.getLegs()).containsOnly(LEG1, LEG2);
    assertThat(test.getLegs(SwapLegType.FIXED)).containsExactly(LEG1);
    assertThat(test.getLegs(SwapLegType.IBOR)).containsExactly(LEG2);
    assertThat(test.getLeg(PayReceive.PAY)).isEqualTo(Optional.of(LEG1));
    assertThat(test.getLeg(PayReceive.RECEIVE)).isEqualTo(Optional.of(LEG2));
    assertThat(test.getPayLeg()).isEqualTo(Optional.of(LEG1));
    assertThat(test.getReceiveLeg()).isEqualTo(Optional.of(LEG2));
    assertThat(test.getStartDate()).isEqualTo(LEG1.getStartDate());
    assertThat(test.getEndDate()).isEqualTo(LEG1.getEndDate());
    assertThat(test.isCrossCurrency()).isTrue();
    assertThat(test.allPaymentCurrencies()).containsOnly(GBP, USD);
    assertThat(test.allIndices()).containsOnly(GBP_LIBOR_3M);
  }

  @Test
  public void test_of_singleCurrency() {
    ResolvedSwap test = ResolvedSwap.of(LEG1);
    assertThat(test.getLegs()).containsOnly(LEG1);
    assertThat(test.isCrossCurrency()).isFalse();
    assertThat(test.allPaymentCurrencies()).containsOnly(GBP);
    assertThat(test.allIndices()).containsOnly(GBP_LIBOR_3M);
  }

  @Test
  public void test_builder() {
    ResolvedSwap test = ResolvedSwap.builder()
        .legs(LEG1)
        .build();
    assertThat(test.getLegs()).containsOnly(LEG1);
    assertThat(test.isCrossCurrency()).isFalse();
    assertThat(test.allPaymentCurrencies()).containsOnly(GBP);
    assertThat(test.allIndices()).containsOnly(GBP_LIBOR_3M);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_getLegs_SwapLegType() {
    assertThat(ResolvedSwap.of(LEG1, LEG2).getLegs(FIXED)).containsExactly(LEG1);
    assertThat(ResolvedSwap.of(LEG1, LEG2).getLegs(IBOR)).containsExactly(LEG2);
    assertThat(ResolvedSwap.of(LEG1, LEG2).getLegs(OVERNIGHT)).isEmpty();
    assertThat(ResolvedSwap.of(LEG1, LEG2).getLegs(OTHER)).isEmpty();
  }

  @Test
  public void test_getLeg_PayReceive() {
    assertThat(ResolvedSwap.of(LEG1, LEG2).getLeg(PAY)).isEqualTo(Optional.of(LEG1));
    assertThat(ResolvedSwap.of(LEG1, LEG2).getLeg(RECEIVE)).isEqualTo(Optional.of(LEG2));
    assertThat(ResolvedSwap.of(LEG1).getLeg(PAY)).isEqualTo(Optional.of(LEG1));
    assertThat(ResolvedSwap.of(LEG2).getLeg(PAY)).isEqualTo(Optional.empty());
    assertThat(ResolvedSwap.of(LEG1).getLeg(RECEIVE)).isEqualTo(Optional.empty());
    assertThat(ResolvedSwap.of(LEG2).getLeg(RECEIVE)).isEqualTo(Optional.of(LEG2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ResolvedSwap test = ResolvedSwap.builder()
        .legs(LEG1)
        .build();
    coverImmutableBean(test);
    ResolvedSwap test2 = ResolvedSwap.builder()
        .legs(LEG2)
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    ResolvedSwap test = ResolvedSwap.builder()
        .legs(LEG1)
        .build();
    assertSerialization(test);
  }

}
