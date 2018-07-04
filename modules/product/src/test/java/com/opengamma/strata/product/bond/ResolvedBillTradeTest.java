/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedBillTrade}.
 */
@Test
public class ResolvedBillTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedBillTrade test = sut();
    assertEquals(test.getSettlement().isPresent(), true);
  }

  public void test_builder_quantitySettlement() {
    assertThrows(() -> ResolvedBillTrade.builder()
        .info(TradeInfo.of(date(2015, 3, 25)))
        .product(ResolvedBillTest.sut())
        .quantity(123)
        .settlement(Payment.of(Currency.USD, 120, date(2015, 3, 27)))
        .build());
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
  static ResolvedBillTrade sut() {
    return BillTradeTest.sut_yield().resolve(REF_DATA);
  }

  static ResolvedBillTrade sut2() {
    return BillTradeTest.sut2().resolve(REF_DATA);
  }

}
