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
 * Test {@link EtdStyle}.
 */
@Test
public class EtdStyleTest {

  public void test_monthly() {
    EtdStyle test = EtdStyle.ofMonthly();
    assertEquals(test.getType(), EtdStyleType.MONTHLY);
    assertEquals(test.getCode(), "");
    assertEquals(test.getDateCode().isPresent(), false);
    assertEquals(test.getSettlementType().isPresent(), false);
    assertEquals(test.getOptionType().isPresent(), false);
  }

  public void test_weekly() {
    EtdStyle test = EtdStyle.ofWeekly(2);
    assertEquals(test.getType(), EtdStyleType.WEEKLY);
    assertEquals(test.getCode(), "W2");
    assertEquals(test.getDateCode().getAsInt(), 2);
    assertEquals(test.getSettlementType().isPresent(), false);
    assertEquals(test.getOptionType().isPresent(), false);
  }

  public void test_daily() {
    EtdStyle test = EtdStyle.ofDaily(24);
    assertEquals(test.getType(), EtdStyleType.DAILY);
    assertEquals(test.getCode(), "24");
    assertEquals(test.getDateCode().getAsInt(), 24);
    assertEquals(test.getSettlementType().isPresent(), false);
    assertEquals(test.getOptionType().isPresent(), false);
  }

  public void test_flexFuture() {
    EtdStyle test = EtdStyle.ofFlexFuture(2, EtdSettlementType.CASH);
    assertEquals(test.getType(), EtdStyleType.FLEX);
    assertEquals(test.getCode(), "02C");
    assertEquals(test.getDateCode().getAsInt(), 2);
    assertEquals(test.getSettlementType().get(), EtdSettlementType.CASH);
    assertEquals(test.getOptionType().isPresent(), false);
  }

  public void test_flexOption() {
    EtdStyle test = EtdStyle.ofFlexOption(24, EtdSettlementType.CASH, EtdOptionType.AMERICAN);
    assertEquals(test.getType(), EtdStyleType.FLEX);
    assertEquals(test.getCode(), "24CA");
    assertEquals(test.getDateCode().getAsInt(), 24);
    assertEquals(test.getSettlementType().get(), EtdSettlementType.CASH);
    assertEquals(test.getOptionType().get(), EtdOptionType.AMERICAN);
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
  static EtdStyle sut() {
    return EtdStyle.MONTHLY;
  }

  static EtdStyle sut2() {
    return EtdStyle.ofFlexOption(6, EtdSettlementType.CASH, EtdOptionType.EUROPEAN);
  }

}
