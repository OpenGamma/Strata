/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ValueStep}.
 */
public class ValueStepTest {

  private static ValueAdjustment DELTA_MINUS_2000 = ValueAdjustment.ofDeltaAmount(-2000);
  private static ValueAdjustment ABSOLUTE_100 = ValueAdjustment.ofReplace(100);

  @Test
  public void test_of_intAdjustment() {
    ValueStep test = ValueStep.of(2, DELTA_MINUS_2000);
    assertThat(test.getDate()).isEqualTo(Optional.empty());
    assertThat(test.getPeriodIndex()).isEqualTo(OptionalInt.of(2));
    assertThat(test.getValue()).isEqualTo(DELTA_MINUS_2000);
  }

  @Test
  public void test_of_dateAdjustment() {
    ValueStep test = ValueStep.of(date(2014, 6, 30), DELTA_MINUS_2000);
    assertThat(test.getDate()).hasValue(date(2014, 6, 30));
    assertThat(test.getPeriodIndex()).isEqualTo(OptionalInt.empty());
    assertThat(test.getValue()).isEqualTo(DELTA_MINUS_2000);
  }

  @Test
  public void test_builder_invalid() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ValueStep.builder().value(DELTA_MINUS_2000).build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ValueStep.builder().date(date(2014, 6, 30)).periodIndex(1).value(DELTA_MINUS_2000).build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ValueStep.builder().periodIndex(0).value(DELTA_MINUS_2000).build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ValueStep.builder().periodIndex(-1).value(DELTA_MINUS_2000).build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void equals() {
    ValueStep a1 = ValueStep.of(2, DELTA_MINUS_2000);
    ValueStep a2 = ValueStep.of(2, DELTA_MINUS_2000);
    ValueStep b = ValueStep.of(1, DELTA_MINUS_2000);
    ValueStep c = ValueStep.of(2, ABSOLUTE_100);
    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(c)).isEqualTo(false);

    ValueStep d1 = ValueStep.of(date(2014, 6, 30), DELTA_MINUS_2000);
    ValueStep d2 = ValueStep.of(date(2014, 6, 30), DELTA_MINUS_2000);
    ValueStep e = ValueStep.of(date(2014, 7, 30), DELTA_MINUS_2000);
    ValueStep f = ValueStep.of(date(2014, 7, 30), ABSOLUTE_100);
    assertThat(d1.equals(d1)).isEqualTo(true);
    assertThat(d1.equals(d2)).isEqualTo(true);
    assertThat(d1.equals(e)).isEqualTo(false);
    assertThat(d1.equals(f)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(ValueStep.of(2, DELTA_MINUS_2000));
  }

  @Test
  public void test_serialization() {
    assertSerialization(ValueStep.of(2, DELTA_MINUS_2000));
  }

}
