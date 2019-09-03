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
 * Test {@link MoneynessStrike}.
 */
public class MoneynessStrikeTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    MoneynessStrike test = MoneynessStrike.of(0.6d);
    assertThat(test.getType()).isEqualTo(StrikeType.MONEYNESS);
    assertThat(test.getValue()).isCloseTo(0.6d, offset(0d));
    assertThat(test.getLabel()).isEqualTo("Moneyness=0.6");
    assertThat(test.withValue(0.2d)).isEqualTo(MoneynessStrike.of(0.2d));
  }

  @Test
  public void test_ofStrikeAndForward() {
    MoneynessStrike test = MoneynessStrike.ofStrikeAndForward(0.6d, 1.2d);
    assertThat(test.getType()).isEqualTo(StrikeType.MONEYNESS);
    assertThat(test.getValue()).isCloseTo(0.5d, offset(0d));
    assertThat(test.getLabel()).isEqualTo("Moneyness=0.5");
    assertThat(test.withValue(0.2d)).isEqualTo(MoneynessStrike.of(0.2d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    MoneynessStrike test = MoneynessStrike.of(0.6d);
    coverImmutableBean(test);
    MoneynessStrike test2 = MoneynessStrike.of(0.2d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    MoneynessStrike test = MoneynessStrike.of(0.6d);
    assertSerialization(test);
  }

}
