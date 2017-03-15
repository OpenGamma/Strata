/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.product.common.SettlementType;

/**
 * Test {@link CashSwaptionSettlement}.
 */
@Test
public class CashSwaptionSettlementTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    CashSwaptionSettlement test = CashSwaptionSettlement.of(date(2015, 6, 30), CashSwaptionSettlementMethod.CASH_PRICE);
    assertEquals(test.getMethod(), CashSwaptionSettlementMethod.CASH_PRICE);
    assertEquals(test.getSettlementDate(), date(2015, 6, 30));
    assertEquals(test.getSettlementType(), SettlementType.CASH);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CashSwaptionSettlement test = CashSwaptionSettlement.of(date(2015, 6, 30), CashSwaptionSettlementMethod.CASH_PRICE);
    coverImmutableBean(test);
    CashSwaptionSettlement test2 = CashSwaptionSettlement.of(date(2015, 7, 30), CashSwaptionSettlementMethod.PAR_YIELD);
    coverBeanEquals(test, test2);
    coverEnum(CashSwaptionSettlementMethod.class);
    coverEnum(SettlementType.class);
  }

  public void test_serialization() {
    CashSwaptionSettlement test = CashSwaptionSettlement.of(date(2015, 6, 30), CashSwaptionSettlementMethod.CASH_PRICE);
    assertSerialization(test);
  }

}
