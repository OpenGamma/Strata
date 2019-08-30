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

/**
 * Test {@link ResolvedBondFutureOption}.
 */
public class ResolvedBondFutureOptionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  @Test
  public void test_getters() {
    ResolvedBondFutureOption test = sut();
    assertThat(test.getExpiryDate()).isEqualTo(test.getExpiry().toLocalDate());
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
  static ResolvedBondFutureOption sut() {
    return BondFutureOptionTest.sut().resolve(REF_DATA);
  }

  static ResolvedBondFutureOption sut2() {
    return BondFutureOptionTest.sut2().resolve(REF_DATA);
  }

}
