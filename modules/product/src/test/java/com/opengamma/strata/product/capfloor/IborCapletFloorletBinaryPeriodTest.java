/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link IborCapletFloorletBinaryPeriod}.
 */
class IborCapletFloorletBinaryPeriodTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate FIXING = LocalDate.of(2011, 1, 4);
  private static final ZonedDateTime FIXING_TIME_ZONE = EUR_EURIBOR_3M.calculateFixingDateTime(FIXING);
  private static final double STRIKE = 0.04;
  private static final LocalDate START_UNADJ = LocalDate.of(2010, 10, 8);
  private static final LocalDate END_UNADJ = LocalDate.of(2011, 1, 8);
  private static final LocalDate START = LocalDate.of(2010, 10, 8);
  private static final LocalDate END = LocalDate.of(2011, 1, 10);
  private static final LocalDate PAYMENT = LocalDate.of(2011, 1, 13);
  private static final double NOTIONAL = 1.e6;
  private static final IborRateComputation RATE_COMP = IborRateComputation.of(EUR_EURIBOR_3M, FIXING, REF_DATA);
  private static final double YEAR_FRACTION = 0.251d;

  @Test
  void test_builder_min() {
    IborCapletFloorletBinaryPeriod test = IborCapletFloorletBinaryPeriod.builder()
        .amount(NOTIONAL)
        .startDate(START)
        .endDate(END)
        .yearFraction(YEAR_FRACTION)
        .caplet(STRIKE)
        .iborRate(RATE_COMP)
        .build();
    assertThat(test.getCaplet().getAsDouble()).isEqualTo(STRIKE);
    assertThat(test.getFloorlet()).isNotPresent();
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getStartDate()).isEqualTo(START);
    assertThat(test.getEndDate()).isEqualTo(END);
    assertThat(test.getPaymentDate()).isEqualTo(test.getEndDate());
    assertThat(test.getCurrency()).isEqualTo(EUR);
    assertThat(test.getAmount()).isEqualTo(NOTIONAL);
    assertThat(test.getIborRate()).isEqualTo(RATE_COMP);
    assertThat(test.getIndex()).isEqualTo(EUR_EURIBOR_3M);
    assertThat(test.getFixingDate()).isEqualTo(FIXING_TIME_ZONE.toLocalDate());
    assertThat(test.getFixingDateTime()).isEqualTo(FIXING_TIME_ZONE);
    assertThat(test.getPutCall()).isEqualTo(PutCall.CALL);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(START);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(END);
    assertThat(test.getYearFraction()).isEqualTo(YEAR_FRACTION);
  }

  @Test
  void test_builder_full() {
    IborCapletFloorletBinaryPeriod test = IborCapletFloorletBinaryPeriod.builder()
        .amount(NOTIONAL)
        .startDate(START)
        .endDate(END)
        .unadjustedStartDate(START_UNADJ)
        .unadjustedEndDate(END_UNADJ)
        .paymentDate(PAYMENT)
        .yearFraction(YEAR_FRACTION)
        .currency(GBP)
        .floorlet(STRIKE)
        .iborRate(RATE_COMP)
        .build();
    assertThat(test.getFloorlet().getAsDouble()).isEqualTo(STRIKE);
    assertThat(test.getCaplet()).isNotPresent();
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getStartDate()).isEqualTo(START);
    assertThat(test.getEndDate()).isEqualTo(END);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(START_UNADJ);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(END_UNADJ);
    assertThat(test.getPaymentDate()).isEqualTo(PAYMENT);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(NOTIONAL);
    assertThat(test.getIborRate()).isEqualTo(RATE_COMP);
    assertThat(test.getIndex()).isEqualTo(EUR_EURIBOR_3M);
    assertThat(test.getFixingDateTime()).isEqualTo(FIXING_TIME_ZONE);
    assertThat(test.getPutCall()).isEqualTo(PutCall.PUT);
    assertThat(test.getYearFraction()).isEqualTo(YEAR_FRACTION);
  }

  @Test
  void test_builder_fail() {
    // rate observation missing
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborCapletFloorletBinaryPeriod.builder()
            .amount(NOTIONAL)
            .caplet(STRIKE)
            .build());
    // cap and floor missing
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborCapletFloorletBinaryPeriod.builder()
            .amount(NOTIONAL)
            .iborRate(RATE_COMP)
            .build());
    // cap and floor present
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborCapletFloorletBinaryPeriod.builder()
            .amount(NOTIONAL)
            .caplet(STRIKE)
            .floorlet(STRIKE)
            .iborRate(RATE_COMP)
            .build());
  }

  @Test
  void test_payoff() {
    // Caplet
    IborCapletFloorletBinaryPeriod cap = sut();
    // Caplet ITM
    double fixingCapItm = STRIKE + 0.01;
    CurrencyAmount payoffCapItm = cap.payoff(fixingCapItm);
    assertThat(payoffCapItm.getCurrency()).isEqualTo(cap.getCurrency());
    assertThat(payoffCapItm.getAmount()).isEqualTo(NOTIONAL * cap.getYearFraction(), Offset.offset(1.0E-2));
    // Caplet OTM
    double fixingCapOtm = STRIKE - 0.01;
    CurrencyAmount payoffCapOtm = cap.payoff(fixingCapOtm);
    assertThat(payoffCapOtm.getCurrency()).isEqualTo(cap.getCurrency());
    assertThat(payoffCapOtm.getAmount()).isEqualTo(0.0);
    // Floorlet
    IborCapletFloorletBinaryPeriod floor = cap.toBuilder().floorlet(STRIKE).caplet(null).build();
    // Floorlet ITM
    double fixingFloorItm = STRIKE - 0.01;
    CurrencyAmount payoffFloorItm = floor.payoff(fixingFloorItm);
    assertThat(payoffFloorItm.getCurrency()).isEqualTo(cap.getCurrency());
    assertThat(payoffFloorItm.getAmount()).isEqualTo(0.0);
    // Floorlet OTM
    double fixingFloorOtm = STRIKE + 0.01;
    CurrencyAmount payoffFloorOtm = floor.payoff(fixingFloorOtm);
    assertThat(payoffFloorOtm.getCurrency()).isEqualTo(cap.getCurrency());
    assertThat(payoffFloorOtm.getAmount()).isEqualTo(NOTIONAL * cap.getYearFraction(), Offset.offset(1.0E-2));
  }

  //-------------------------------------------------------------------------
  @Test
  void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
  void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static IborCapletFloorletBinaryPeriod sut() {
    return IborCapletFloorletBinaryPeriod.builder()
        .amount(NOTIONAL)
        .startDate(START)
        .endDate(END)
        .caplet(STRIKE)
        .iborRate(RATE_COMP)
        .build();
  }

  static IborCapletFloorletBinaryPeriod sut2() {
    return IborCapletFloorletBinaryPeriod.builder()
        .amount(-NOTIONAL)
        .startDate(START.plusDays(1))
        .endDate(END.plusDays(1))
        .floorlet(STRIKE)
        .iborRate(IborRateComputation.of(USD_LIBOR_6M, LocalDate.of(2013, 2, 15), REF_DATA))
        .build();
  }

}
