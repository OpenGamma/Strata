/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PutCall.CALL;
import static com.opengamma.strata.product.common.PutCall.PUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

/**
 * Test {@link EuropeanVanillaOption}.
 */
public class EuropeanVanillaOptionTest {

  private static final double STRIKE = 100;
  private static final double TIME = 0.5;

  @Test
  public void testNegativeTime() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EuropeanVanillaOption.of(STRIKE, -TIME, CALL));
  }

  @Test
  public void test_of() {
    EuropeanVanillaOption test = EuropeanVanillaOption.of(STRIKE, TIME, CALL);
    assertThat(test.getStrike()).isCloseTo(STRIKE, offset(0d));
    assertThat(test.getTimeToExpiry()).isCloseTo(TIME, offset(0d));
    assertThat(test.getPutCall()).isEqualTo(CALL);
    assertThat(test.isCall()).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    EuropeanVanillaOption test = EuropeanVanillaOption.of(STRIKE, TIME, CALL);
    coverImmutableBean(test);
    EuropeanVanillaOption test2 = EuropeanVanillaOption.of(110, 0.6, PUT);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    EuropeanVanillaOption test = EuropeanVanillaOption.of(STRIKE, TIME, CALL);
    assertSerialization(test);
  }

}
