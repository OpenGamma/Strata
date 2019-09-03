/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.option;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

/**
 * Test {@link SimpleStrike}.
 */
public class SimpleStrikeTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    SimpleStrike test = SimpleStrike.of(0.6d);
    assertThat(test.getType()).isEqualTo(StrikeType.STRIKE);
    assertThat(test.getValue()).isCloseTo(0.6d, offset(0d));
    assertThat(test.getLabel()).isEqualTo("Strike=0.6");
    assertThat(test.withValue(0.2d)).isEqualTo(SimpleStrike.of(0.2d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SimpleStrike test = SimpleStrike.of(0.6d);
    coverImmutableBean(test);
    SimpleStrike test2 = SimpleStrike.of(0.2d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    SimpleStrike test = SimpleStrike.of(0.6d);
    assertSerialization(test);
  }

}
