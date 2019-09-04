/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
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
 * Test {@link IborFuture}.
 */
public class IborFutureTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL = 1_000d;
  private static final double NOTIONAL2 = 2_000d;
  private static final double ACCRUAL_FACTOR = TENOR_3M.getPeriod().toTotalMonths() / 12.0;
  private static final double ACCRUAL_FACTOR2 = TENOR_2M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE = date(2015, 6, 15);
  private static final LocalDate LAST_TRADE_DATE2 = date(2015, 6, 18);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(6);
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "IborFuture");
  private static final SecurityId SECURITY_ID2 = SecurityId.of("OG-Test", "IborFuture2");

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    IborFuture test = sut();
    assertThat(test.getSecurityId()).isEqualTo(SECURITY_ID);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getAccrualFactor()).isEqualTo(ACCRUAL_FACTOR);
    assertThat(test.getLastTradeDate()).isEqualTo(LAST_TRADE_DATE);
    assertThat(test.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(test.getRounding()).isEqualTo(ROUNDING);
    assertThat(test.getFixingDate()).isEqualTo(LAST_TRADE_DATE);
  }

  @Test
  public void test_builder_defaults() {
    IborFuture test = IborFuture.builder()
        .securityId(SECURITY_ID)
        .currency(GBP)
        .notional(NOTIONAL)
        .lastTradeDate(LAST_TRADE_DATE)
        .index(GBP_LIBOR_2M)
        .build();
    assertThat(test.getSecurityId()).isEqualTo(SECURITY_ID);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getAccrualFactor()).isEqualTo(ACCRUAL_FACTOR2);
    assertThat(test.getLastTradeDate()).isEqualTo(LAST_TRADE_DATE);
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_2M);
    assertThat(test.getRounding()).isEqualTo(Rounding.none());
    assertThat(test.getFixingDate()).isEqualTo(LAST_TRADE_DATE);
  }

  @Test
  public void test_builder_noIndex() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborFuture.builder()
            .securityId(SECURITY_ID)
            .notional(NOTIONAL)
            .currency(GBP)
            .lastTradeDate(LAST_TRADE_DATE)
            .rounding(ROUNDING)
            .build());
  }

  @Test
  public void test_builder_noCurrency() {
    IborFuture test = IborFuture.builder()
        .securityId(SECURITY_ID)
        .notional(NOTIONAL)
        .index(GBP_LIBOR_2M)
        .lastTradeDate(LAST_TRADE_DATE)
        .rounding(ROUNDING)
        .build();
    assertThat(GBP).isEqualTo(test.getCurrency());
  }

  @Test
  public void test_builder_noLastTradeDate() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborFuture.builder()
            .securityId(SECURITY_ID)
            .notional(NOTIONAL)
            .currency(GBP)
            .index(GBP_LIBOR_2M)
            .rounding(ROUNDING)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    IborFuture test = sut();
    ResolvedIborFuture expected = ResolvedIborFuture.builder()
        .securityId(SECURITY_ID)
        .currency(USD)
        .notional(NOTIONAL)
        .accrualFactor(ACCRUAL_FACTOR)
        .iborRate(IborRateComputation.of(USD_LIBOR_3M, LAST_TRADE_DATE, REF_DATA))
        .rounding(ROUNDING)
        .build();
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
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
  static IborFuture sut() {
    return IborFuture.builder()
        .securityId(SECURITY_ID)
        .currency(USD)
        .notional(NOTIONAL)
        .accrualFactor(ACCRUAL_FACTOR)
        .lastTradeDate(LAST_TRADE_DATE)
        .index(USD_LIBOR_3M)
        .rounding(ROUNDING)
        .build();
  }

  static IborFuture sut2() {
    return IborFuture.builder()
        .securityId(SECURITY_ID2)
        .currency(GBP)
        .notional(NOTIONAL2)
        .accrualFactor(ACCRUAL_FACTOR2)
        .lastTradeDate(LAST_TRADE_DATE2)
        .index(GBP_LIBOR_2M)
        .build();
  }

}
