/*
 * Copyright (C) 2026 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link SimpleMoneynessStrike}.
 */
public class SimpleMoneynessStrikeTest {

  @Test
  public void test_of() {
    SimpleMoneynessStrike test = SimpleMoneynessStrike.of(0.6d);
    assertThat(test.getType()).isEqualTo(StrikeType.SIMPLE_MONEYNESS);
    assertThat(test.getValue()).isCloseTo(0.6d, offset(0d));
    assertThat(test.getLabel()).isEqualTo("SimpleMoneyness=0.6");
    assertThat(test.withValue(0.2d)).isEqualTo(SimpleMoneynessStrike.of(0.2d));
  }

  @Test
  public void test_ofStrikeAndForward() {
    SimpleMoneynessStrike test = SimpleMoneynessStrike.ofStrikeAndForward(0.6d, 1.2d);
    assertThat(test.getType()).isEqualTo(StrikeType.SIMPLE_MONEYNESS);
    assertThat(test.getValue()).isCloseTo(-0.6d, offset(0d));
    assertThat(test.getLabel()).isEqualTo("SimpleMoneyness=-0.6");
    assertThat(test.withValue(0.2d)).isEqualTo(SimpleMoneynessStrike.of(0.2d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SimpleMoneynessStrike test = SimpleMoneynessStrike.of(0.6d);
    coverImmutableBean(test);
    SimpleMoneynessStrike test2 = SimpleMoneynessStrike.of(0.2d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    SimpleMoneynessStrike test = SimpleMoneynessStrike.of(0.6d);
    assertSerialization(test);
  }
}
