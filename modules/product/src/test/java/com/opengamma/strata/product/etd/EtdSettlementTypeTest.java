/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link EtdSettlementType}.
 */
@Test
public class EtdSettlementTypeTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
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

  @Test(dataProvider = "name")
  public void test_toString(EtdSettlementType convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(EtdSettlementType convention, String name) {
    assertEquals(EtdSettlementType.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdSettlementType.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdSettlementType.of(null));
  }

  public void test_getCode() {
    assertEquals(EtdSettlementType.CASH.getCode(), "C");
    assertEquals(EtdSettlementType.PHYSICAL.getCode(), "E");
    assertEquals(EtdSettlementType.DERIVATIVE.getCode(), "D");
    assertEquals(EtdSettlementType.NOTIONAL.getCode(), "N");
    assertEquals(EtdSettlementType.PAYMENT_VS_PAYMENT.getCode(), "P");
    assertEquals(EtdSettlementType.STOCK.getCode(), "S");
    assertEquals(EtdSettlementType.CASCADE.getCode(), "T");
    assertEquals(EtdSettlementType.ALTERNATE.getCode(), "A");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(EtdSettlementType.class);
  }

  public void test_serialization() {
    assertSerialization(EtdSettlementType.DERIVATIVE);
  }

  public void test_jodaConvert() {
    assertJodaConvert(EtdSettlementType.class, EtdSettlementType.STOCK);
  }

}
