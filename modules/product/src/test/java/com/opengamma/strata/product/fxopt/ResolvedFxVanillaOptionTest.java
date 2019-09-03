/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static com.opengamma.strata.product.common.PutCall.CALL;
import static com.opengamma.strata.product.common.PutCall.PUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.fx.ResolvedFxSingle;

/**
 * Test {@link ResolvedFxVanillaOption}.
 */
public class ResolvedFxVanillaOptionTest {

  private static final ZonedDateTime EXPIRY_DATE_TIME = ZonedDateTime.of(2015, 2, 14, 12, 15, 0, 0, ZoneOffset.UTC);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 2, 16);
  private static final double NOTIONAL = 1.0e6;
  private static final double STRIKE = 1.35;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * STRIKE);
  private static final ResolvedFxSingle FX = ResolvedFxSingle.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);
  private static final double STRIKE_RE = 0.9;
  private static final ResolvedFxSingle FX_RE = ResolvedFxSingle.of(
      CurrencyAmount.of(EUR, -NOTIONAL), CurrencyAmount.of(GBP, NOTIONAL * STRIKE_RE), PAYMENT_DATE);

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    ResolvedFxVanillaOption test = sut();
    assertThat(test.getExpiry()).isEqualTo(EXPIRY_DATE_TIME);
    assertThat(test.getExpiryDate()).isEqualTo(EXPIRY_DATE_TIME.toLocalDate());
    assertThat(test.getLongShort()).isEqualTo(LONG);
    assertThat(test.getCounterCurrency()).isEqualTo(USD);
    assertThat(test.getPutCall()).isEqualTo(CALL);
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getUnderlying()).isEqualTo(FX);
    assertThat(test.getCurrencyPair()).isEqualTo(FX.getCurrencyPair());
  }

  @Test
  public void test_builder_inverseFx() {
    ResolvedFxVanillaOption test = sut2();
    assertThat(test.getExpiry()).isEqualTo(EXPIRY_DATE_TIME.plusSeconds(1));
    assertThat(test.getExpiryDate()).isEqualTo(EXPIRY_DATE_TIME.toLocalDate());
    assertThat(test.getLongShort()).isEqualTo(SHORT);
    assertThat(test.getCounterCurrency()).isEqualTo(GBP);
    assertThat(test.getPutCall()).isEqualTo(PUT);
    assertThat(test.getStrike()).isEqualTo(STRIKE_RE);
    assertThat(test.getUnderlying()).isEqualTo(FX_RE);
  }

  @Test
  public void test_builder_earlyPaymentDate() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFxVanillaOption.builder()
            .longShort(LONG)
            .expiry(LocalDate.of(2015, 2, 21).atStartOfDay(ZoneOffset.UTC))
            .underlying(FX)
            .build());
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
  static ResolvedFxVanillaOption sut() {
    return ResolvedFxVanillaOption.builder()
        .longShort(LONG)
        .expiry(EXPIRY_DATE_TIME)
        .underlying(FX)
        .build();
  }

  static ResolvedFxVanillaOption sut2() {
    ;
    return ResolvedFxVanillaOption.builder()
        .longShort(SHORT)
        .expiry(EXPIRY_DATE_TIME.plusSeconds(1))
        .underlying(FX_RE)
        .build();
  }

}
