/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link EtdSettlementType}.
 */
public class EtdSettlementTypeTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {EtdSettlementType.CASH, "Cash"},
        {EtdSettlementType.PHYSICAL, "Physical"},
        {EtdSettlementType.DERIVATIVE, "Derivative"},
        {EtdSettlementType.PAYMENT_VS_PAYMENT, "PaymentVsPayment"},
        {EtdSettlementType.NOTIONAL, "Notional"},
        {EtdSettlementType.STOCK, "Stock"},
        {EtdSettlementType.CASCADE, "Cascade"},
        {EtdSettlementType.ALTERNATE, "Alternate"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(EtdSettlementType convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(EtdSettlementType convention, String name) {
    assertThat(EtdSettlementType.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdSettlementType.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdSettlementType.of(null));
  }

  @Test
  public void test_getCode() {
    assertThat(EtdSettlementType.CASH.getCode()).isEqualTo("C");
    assertThat(EtdSettlementType.PHYSICAL.getCode()).isEqualTo("E");
    assertThat(EtdSettlementType.DERIVATIVE.getCode()).isEqualTo("D");
    assertThat(EtdSettlementType.NOTIONAL.getCode()).isEqualTo("N");
    assertThat(EtdSettlementType.PAYMENT_VS_PAYMENT.getCode()).isEqualTo("P");
    assertThat(EtdSettlementType.STOCK.getCode()).isEqualTo("S");
    assertThat(EtdSettlementType.CASCADE.getCode()).isEqualTo("T");
    assertThat(EtdSettlementType.ALTERNATE.getCode()).isEqualTo("A");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(EtdSettlementType.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(EtdSettlementType.DERIVATIVE);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(EtdSettlementType.class, EtdSettlementType.STOCK);
  }

}
