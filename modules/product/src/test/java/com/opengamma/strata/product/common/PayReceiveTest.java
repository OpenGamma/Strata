/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link PayReceive}.
 */
public class PayReceiveTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_ofPay() {
    assertThat(PayReceive.ofPay(true)).isEqualTo(PayReceive.PAY);
    assertThat(PayReceive.ofPay(false)).isEqualTo(PayReceive.RECEIVE);
  }

  @Test
  public void test_ofSignedAmount() {
    assertThat(PayReceive.ofSignedAmount(-1d)).isEqualTo(PayReceive.PAY);
    assertThat(PayReceive.ofSignedAmount(-0d)).isEqualTo(PayReceive.PAY);
    assertThat(PayReceive.ofSignedAmount(0d)).isEqualTo(PayReceive.RECEIVE);
    assertThat(PayReceive.ofSignedAmount(+0d)).isEqualTo(PayReceive.RECEIVE);
    assertThat(PayReceive.ofSignedAmount(1d)).isEqualTo(PayReceive.RECEIVE);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize_pay_double() {
    assertThat(PayReceive.PAY.normalize(1d)).isCloseTo(-1d, offset(0d));
    assertThat(PayReceive.PAY.normalize(0d)).isCloseTo(0d, offset(0d));
    assertThat(PayReceive.PAY.normalize(-0d)).isCloseTo(0d, offset(0d));
    assertThat(PayReceive.PAY.normalize(-1d)).isCloseTo(-1d, offset(0d));
  }

  @Test
  public void test_normalize_pay_amount() {
    assertThat(PayReceive.PAY.normalize(CurrencyAmount.of(GBP, 1d))).isEqualTo(CurrencyAmount.of(GBP, -1d));
    assertThat(PayReceive.PAY.normalize(CurrencyAmount.of(GBP, 0d))).isEqualTo(CurrencyAmount.of(GBP, 0d));
    assertThat(PayReceive.PAY.normalize(CurrencyAmount.of(GBP, -1d))).isEqualTo(CurrencyAmount.of(GBP, -1d));
  }

  @Test
  public void test_normalize_receive_double() {
    assertThat(PayReceive.RECEIVE.normalize(1d)).isCloseTo(1d, offset(0d));
    assertThat(PayReceive.RECEIVE.normalize(0d)).isCloseTo(0d, offset(0d));
    assertThat(PayReceive.RECEIVE.normalize(-0d)).isCloseTo(0d, offset(0d));
    assertThat(PayReceive.RECEIVE.normalize(-1d)).isCloseTo(1d, offset(0d));
  }

  @Test
  public void test_normalize_receive_amount() {
    assertThat(PayReceive.RECEIVE.normalize(CurrencyAmount.of(GBP, 1d))).isEqualTo(CurrencyAmount.of(GBP, 1d));
    assertThat(PayReceive.RECEIVE.normalize(CurrencyAmount.of(GBP, 0d))).isEqualTo(CurrencyAmount.of(GBP, 0d));
    assertThat(PayReceive.RECEIVE.normalize(CurrencyAmount.of(GBP, -1d))).isEqualTo(CurrencyAmount.of(GBP, 1d));
  }

  @Test
  public void test_isPay() {
    assertThat(PayReceive.PAY.isPay()).isTrue();
    assertThat(PayReceive.RECEIVE.isPay()).isFalse();
  }

  @Test
  public void test_isReceive() {
    assertThat(PayReceive.PAY.isReceive()).isFalse();
    assertThat(PayReceive.RECEIVE.isReceive()).isTrue();
  }

  @Test
  public void test_opposite() {
    assertThat(PayReceive.PAY.opposite()).isEqualTo(PayReceive.RECEIVE);
    assertThat(PayReceive.RECEIVE.opposite()).isEqualTo(PayReceive.PAY);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {PayReceive.PAY, "Pay"},
        {PayReceive.RECEIVE, "Receive"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(PayReceive convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(PayReceive convention, String name) {
    assertThat(PayReceive.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupUpperCase(PayReceive convention, String name) {
    assertThat(PayReceive.of(name.toUpperCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupLowerCase(PayReceive convention, String name) {
    assertThat(PayReceive.of(name.toLowerCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PayReceive.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PayReceive.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(PayReceive.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(PayReceive.PAY);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(PayReceive.class, PayReceive.PAY);
  }

}
