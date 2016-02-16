/**
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

/**
 * Test {@link CashSettlement}.
 */
@Test
public class CashSettlementTest {

  //-------------------------------------------------------------------------
  public void test_builder() {
    CashSettlement test = CashSettlement.builder()
        .cashSettlementMethod(CashSettlementMethod.CASH_PRICE)
        .settlementDate(date(2015, 6, 30))
        .build();
    assertEquals(test.getCashSettlementMethod(), CashSettlementMethod.CASH_PRICE);
    assertEquals(test.getSettlementDate(), date(2015, 6, 30));
    assertEquals(test.getSettlementType(), SettlementType.CASH);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CashSettlement test = CashSettlement.builder()
        .cashSettlementMethod(CashSettlementMethod.CASH_PRICE)
        .settlementDate(date(2015, 6, 30))
        .build();
    coverImmutableBean(test);
    CashSettlement test2 = CashSettlement.builder()
        .cashSettlementMethod(CashSettlementMethod.PAR_YIELD)
        .settlementDate(date(2015, 7, 30))
        .build();
    coverBeanEquals(test, test2);
    coverEnum(CashSettlementMethod.class);
    coverEnum(SettlementType.class);
  }

  public void test_serialization() {
    CashSettlement test = CashSettlement.builder()
        .cashSettlementMethod(CashSettlementMethod.CASH_PRICE)
        .settlementDate(date(2015, 6, 30))
        .build();
    assertSerialization(test);
  }

}
