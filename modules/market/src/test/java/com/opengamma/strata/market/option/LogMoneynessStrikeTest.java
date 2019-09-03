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
 * Test {@link LogMoneynessStrike}.
 */
public class LogMoneynessStrikeTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    LogMoneynessStrike test = LogMoneynessStrike.of(0.6d);
    assertThat(test.getType()).isEqualTo(StrikeType.LOG_MONEYNESS);
    assertThat(test.getValue()).isCloseTo(0.6d, offset(0d));
    assertThat(test.getLabel()).isEqualTo("LogMoneyness=0.6");
    assertThat(test.withValue(0.2d)).isEqualTo(LogMoneynessStrike.of(0.2d));
  }

  @Test
  public void test_ofStrikeAndForward() {
    LogMoneynessStrike test = LogMoneynessStrike.ofStrikeAndForward(0.6d, 1.2d);
    assertThat(test.getType()).isEqualTo(StrikeType.LOG_MONEYNESS);
    assertThat(test.getValue()).isCloseTo(Math.log(0.5d), offset(0d));
    assertThat(test.getLabel()).isEqualTo("LogMoneyness=" + Math.log(0.5d));
    assertThat(test.withValue(0.2d)).isEqualTo(LogMoneynessStrike.of(0.2d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    LogMoneynessStrike test = LogMoneynessStrike.of(0.6d);
    coverImmutableBean(test);
    LogMoneynessStrike test2 = LogMoneynessStrike.of(0.2d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    LogMoneynessStrike test = LogMoneynessStrike.of(0.6d);
    assertSerialization(test);
  }

}
