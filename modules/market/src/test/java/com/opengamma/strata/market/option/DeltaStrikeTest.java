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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

/**
 * Test {@link DeltaStrike}.
 */
public class DeltaStrikeTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    DeltaStrike test = DeltaStrike.of(0.6d);
    assertThat(test.getType()).isEqualTo(StrikeType.DELTA);
    assertThat(test.getValue()).isCloseTo(0.6d, offset(0d));
    assertThat(test.getLabel()).isEqualTo("Delta=0.6");
    assertThat(test.withValue(0.2d)).isEqualTo(DeltaStrike.of(0.2d));
  }

  @Test
  public void test_of_invalid() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DeltaStrike.of(-0.001d));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DeltaStrike.of(1.0001d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    DeltaStrike test = DeltaStrike.of(0.6d);
    coverImmutableBean(test);
    DeltaStrike test2 = DeltaStrike.of(0.2d);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    DeltaStrike test = DeltaStrike.of(0.6d);
    assertSerialization(test);
  }

}
