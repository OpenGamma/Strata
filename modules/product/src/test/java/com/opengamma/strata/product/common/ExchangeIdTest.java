/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ExchangeId}.
 */
public class ExchangeIdTest {

  @Test
  public void test_of() {
    ExchangeId test = ExchangeId.of("GB");
    assertThat(test.getName()).isEqualTo("GB");
    assertThat(test).hasToString("GB");
    assertThatIllegalArgumentException().isThrownBy(() -> CcpId.of(""));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    ExchangeId a = ExchangeId.of("ECAG");
    ExchangeId a2 = ExchangeIds.ECAG;
    ExchangeId b = ExchangeId.of("XLON");
    assertThat(a)
        .isEqualTo(a)
        .isEqualTo(a2)
        .isNotEqualTo(b)
        .isNotEqualTo("")
        .isNotEqualTo(null)
        .hasSameHashCodeAs(a2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(ExchangeIds.class);
  }

  @Test
  public void test_serialization() {
    ExchangeId test = ExchangeId.of("ECAG");
    assertSerialization(test);
  }

}
