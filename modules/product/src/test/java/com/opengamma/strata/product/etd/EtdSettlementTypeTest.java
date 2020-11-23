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
        {EtdSettlementType.CASH, "Cash", "C"},
        {EtdSettlementType.PHYSICAL, "Physical", "E"},
        {EtdSettlementType.DERIVATIVE, "Derivative", "D"},
        {EtdSettlementType.PAYMENT_VS_PAYMENT, "PaymentVsPayment", "P"},
        {EtdSettlementType.NOTIONAL, "Notional", "N"},
        {EtdSettlementType.STOCK, "Stock", "S"},
        {EtdSettlementType.CASCADE, "Cascade", "T"},
        {EtdSettlementType.ALTERNATE, "Alternate", "A"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(EtdSettlementType convention, String name, String code) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(EtdSettlementType convention, String name, String code) {
    assertThat(EtdSettlementType.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_getCode(EtdSettlementType convention, String name, String code) {
    assertThat(convention.getCode()).isEqualTo(code);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_parseCode(EtdSettlementType convention, String name, String code) {
    assertThat(EtdSettlementType.parseCode(code)).isEqualTo(convention);
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
  public void test_parseCode_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdSettlementType.parseCode("Rubbish"));
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
