/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link EtdVariant}.
 */
@Test
public class EtdVariantTest {

  public void test_monthly() {
    EtdVariant test = EtdVariant.ofMonthly();
    assertEquals(test.getType(), EtdExpiryType.MONTHLY);
    assertEquals(test.getDateCode().isPresent(), false);
    assertEquals(test.getSettlementType().isPresent(), false);
    assertEquals(test.getOptionType().isPresent(), false);
    assertEquals(test.isFlex(), false);
    assertEquals(test.getCode(), "");
  }

  public void test_weekly() {
    EtdVariant test = EtdVariant.ofWeekly(2);
    assertEquals(test.getType(), EtdExpiryType.WEEKLY);
    assertEquals(test.getDateCode().getAsInt(), 2);
    assertEquals(test.getSettlementType().isPresent(), false);
    assertEquals(test.getOptionType().isPresent(), false);
    assertEquals(test.isFlex(), false);
    assertEquals(test.getCode(), "W2");
  }

  public void test_daily() {
    EtdVariant test = EtdVariant.ofDaily(24);
    assertEquals(test.getType(), EtdExpiryType.DAILY);
    assertEquals(test.getDateCode().getAsInt(), 24);
    assertEquals(test.getSettlementType().isPresent(), false);
    assertEquals(test.getOptionType().isPresent(), false);
    assertEquals(test.isFlex(), false);
    assertEquals(test.getCode(), "24");
  }

  public void test_flexFuture() {
    EtdVariant test = EtdVariant.ofFlexFuture(2, EtdSettlementType.CASH);
    assertEquals(test.getType(), EtdExpiryType.DAILY);
    assertEquals(test.getDateCode().getAsInt(), 2);
    assertEquals(test.getSettlementType().get(), EtdSettlementType.CASH);
    assertEquals(test.getOptionType().isPresent(), false);
    assertEquals(test.isFlex(), true);
    assertEquals(test.getCode(), "02C");
  }

  public void test_flexOption() {
    EtdVariant test = EtdVariant.ofFlexOption(24, EtdSettlementType.CASH, EtdOptionType.AMERICAN);
    assertEquals(test.getType(), EtdExpiryType.DAILY);
    assertEquals(test.getDateCode().getAsInt(), 24);
    assertEquals(test.getSettlementType().get(), EtdSettlementType.CASH);
    assertEquals(test.getOptionType().get(), EtdOptionType.AMERICAN);
    assertEquals(test.isFlex(), true);
    assertEquals(test.getCode(), "24CA");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static EtdVariant sut() {
    return EtdVariant.MONTHLY;
  }

  static EtdVariant sut2() {
    return EtdVariant.ofFlexOption(6, EtdSettlementType.CASH, EtdOptionType.EUROPEAN);
  }

}
