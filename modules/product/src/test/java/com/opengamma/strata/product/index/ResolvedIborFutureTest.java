/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link ResolvedIborFuture}.
 */
public class ResolvedIborFutureTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborFuture PRODUCT = IborFutureTest.sut();
  private static final IborFuture PRODUCT2 = IborFutureTest.sut2();
  private static final double NOTIONAL = 1_000d;
  private static final double ACCRUAL_FACTOR_2M = TENOR_2M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE = date(2015, 6, 15);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(6);
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "IborFuture");

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    ResolvedIborFuture test = sut();
    assertThat(test.getCurrency()).isEqualTo(PRODUCT.getCurrency());
    assertThat(test.getNotional()).isEqualTo(PRODUCT.getNotional());
    assertThat(test.getAccrualFactor()).isEqualTo(PRODUCT.getAccrualFactor());
    assertThat(test.getLastTradeDate()).isEqualTo(PRODUCT.getLastTradeDate());
    assertThat(test.getIndex()).isEqualTo(PRODUCT.getIndex());
    assertThat(test.getRounding()).isEqualTo(PRODUCT.getRounding());
    assertThat(test.getIborRate()).isEqualTo(IborRateComputation.of(PRODUCT.getIndex(), PRODUCT.getLastTradeDate(), REF_DATA));
  }

  @Test
  public void test_builder_defaults() {
    ResolvedIborFuture test = ResolvedIborFuture.builder()
        .securityId(SECURITY_ID)
        .currency(GBP)
        .notional(NOTIONAL)
        .iborRate(IborRateComputation.of(GBP_LIBOR_2M, LAST_TRADE_DATE, REF_DATA))
        .build();
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getAccrualFactor()).isEqualTo(ACCRUAL_FACTOR_2M);
    assertThat(test.getLastTradeDate()).isEqualTo(LAST_TRADE_DATE);
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_2M);
    assertThat(test.getRounding()).isEqualTo(Rounding.none());
    assertThat(test.getIborRate()).isEqualTo(IborRateComputation.of(GBP_LIBOR_2M, LAST_TRADE_DATE, REF_DATA));
  }

  @Test
  public void test_builder_noObservation() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedIborFuture.builder()
            .securityId(SECURITY_ID)
            .notional(NOTIONAL)
            .currency(GBP)
            .rounding(ROUNDING)
            .build());
  }

  @Test
  public void test_builder_noCurrency() {
    ResolvedIborFuture test = ResolvedIborFuture.builder()
        .securityId(SECURITY_ID)
        .notional(NOTIONAL)
        .iborRate(IborRateComputation.of(GBP_LIBOR_2M, LAST_TRADE_DATE, REF_DATA))
        .rounding(ROUNDING)
        .build();
    assertThat(GBP).isEqualTo(test.getCurrency());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedIborFuture sut() {
    return PRODUCT.resolve(REF_DATA);
  }

  static ResolvedIborFuture sut2() {
    return PRODUCT2.resolve(REF_DATA);
  }

}
