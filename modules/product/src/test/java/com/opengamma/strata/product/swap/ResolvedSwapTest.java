/**
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
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test.
 */
@Test
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

  public void test_of() {
    ResolvedSwap test = ResolvedSwap.of(LEG1, LEG2);
    assertEquals(test.getLegs(), ImmutableSet.of(LEG1, LEG2));
    assertEquals(test.getLegs(SwapLegType.FIXED), ImmutableList.of(LEG1));
    assertEquals(test.getLegs(SwapLegType.IBOR), ImmutableList.of(LEG2));
    assertEquals(test.getLeg(PayReceive.PAY), Optional.of(LEG1));
    assertEquals(test.getLeg(PayReceive.RECEIVE), Optional.of(LEG2));
    assertEquals(test.getPayLeg(), Optional.of(LEG1));
    assertEquals(test.getReceiveLeg(), Optional.of(LEG2));
    assertEquals(test.getStartDate(), LEG1.getStartDate());
    assertEquals(test.getEndDate(), LEG1.getEndDate());
    assertEquals(test.isCrossCurrency(), true);
    assertEquals(test.allPaymentCurrencies(), ImmutableSet.of(GBP, USD));
    assertEquals(test.allIndices(), ImmutableSet.of(GBP_LIBOR_3M));
  }

  public void test_of_singleCurrency() {
    ResolvedSwap test = ResolvedSwap.of(LEG1);
    assertEquals(test.getLegs(), ImmutableSet.of(LEG1));
    assertEquals(test.isCrossCurrency(), false);
    assertEquals(test.allPaymentCurrencies(), ImmutableSet.of(GBP));
    assertEquals(test.allIndices(), ImmutableSet.of(GBP_LIBOR_3M));
  }

  public void test_builder() {
    ResolvedSwap test = ResolvedSwap.builder()
        .legs(LEG1)
        .build();
    assertEquals(test.getLegs(), ImmutableSet.of(LEG1));
    assertEquals(test.isCrossCurrency(), false);
    assertEquals(test.allPaymentCurrencies(), ImmutableSet.of(GBP));
    assertEquals(test.allIndices(), ImmutableSet.of(GBP_LIBOR_3M));
  }

  //-------------------------------------------------------------------------
  public void test_getLegs_SwapLegType() {
    assertEquals(ResolvedSwap.of(LEG1, LEG2).getLegs(FIXED), ImmutableList.of(LEG1));
    assertEquals(ResolvedSwap.of(LEG1, LEG2).getLegs(IBOR), ImmutableList.of(LEG2));
    assertEquals(ResolvedSwap.of(LEG1, LEG2).getLegs(OVERNIGHT), ImmutableList.of());
    assertEquals(ResolvedSwap.of(LEG1, LEG2).getLegs(OTHER), ImmutableList.of());
  }

  public void test_getLeg_PayReceive() {
    assertEquals(ResolvedSwap.of(LEG1, LEG2).getLeg(PAY), Optional.of(LEG1));
    assertEquals(ResolvedSwap.of(LEG1, LEG2).getLeg(RECEIVE), Optional.of(LEG2));
    assertEquals(ResolvedSwap.of(LEG1).getLeg(PAY), Optional.of(LEG1));
    assertEquals(ResolvedSwap.of(LEG2).getLeg(PAY), Optional.empty());
    assertEquals(ResolvedSwap.of(LEG1).getLeg(RECEIVE), Optional.empty());
    assertEquals(ResolvedSwap.of(LEG2).getLeg(RECEIVE), Optional.of(LEG2));
  }

  //-------------------------------------------------------------------------
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

  public void test_serialization() {
    ResolvedSwap test = ResolvedSwap.builder()
        .legs(LEG1)
        .build();
    assertSerialization(test);
  }

}
