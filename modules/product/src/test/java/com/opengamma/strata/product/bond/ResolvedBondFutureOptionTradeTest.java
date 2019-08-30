/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.TradedPrice;

/**
 * Test {@link ResolvedBondFutureOptionTrade}.
 */
public class ResolvedBondFutureOptionTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  @Test
  public void test_getters() {
    ResolvedBondFutureOptionTrade test = sut();
    BondFutureOptionTrade base = BondFutureOptionTradeTest.sut();
    assertThat(test.getTradedPrice().get()).isEqualTo(TradedPrice.of(base.getInfo().getTradeDate().get(), base.getPrice()));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
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
