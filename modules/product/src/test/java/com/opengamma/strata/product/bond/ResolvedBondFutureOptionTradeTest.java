/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.TradedPrice;

/**
 * Test {@link ResolvedBondFutureOptionTrade}.
 */
@Test
public class ResolvedBondFutureOptionTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_getters() {
    ResolvedBondFutureOptionTrade test = sut();
    BondFutureOptionTrade base = BondFutureOptionTradeTest.sut();
    assertEquals(test.getTradedPrice().get(), TradedPrice.of(base.getInfo().getTradeDate().get(), base.getPrice()));
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
  static ResolvedBondFutureOptionTrade sut() {
    return BondFutureOptionTradeTest.sut().resolve(REF_DATA);
  }

  static ResolvedBondFutureOptionTrade sut2() {
    return BondFutureOptionTradeTest.sut2().resolve(REF_DATA);
  }

}
