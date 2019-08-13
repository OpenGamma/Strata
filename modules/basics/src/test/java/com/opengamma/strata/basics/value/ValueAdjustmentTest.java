/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.value;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

/**
 * Test {@link ValueAdjustment}.
 */
public class ValueAdjustmentTest {

  private static Offset<Double> TOLERANCE = within(1e-8d);

  @Test
  public void test_NONE() {
    ValueAdjustment test = ValueAdjustment.NONE;
    assertThat(test.getModifyingValue()).isEqualTo(0);
    assertThat(test.getType()).isEqualTo(ValueAdjustmentType.DELTA_AMOUNT);
    assertThat(test.adjust(100)).isEqualTo(100);
    assertThat(test.toString()).isEqualTo("ValueAdjustment[result = input]");
  }

  @Test
  public void test_ofReplace() {
    ValueAdjustment test = ValueAdjustment.ofReplace(200);
    assertThat(test.getModifyingValue()).isEqualTo(200);
    assertThat(test.getType()).isEqualTo(ValueAdjustmentType.REPLACE);
    assertThat(test.adjust(100)).isEqualTo(200);
    assertThat(test.toString()).isEqualTo("ValueAdjustment[result = 200.0]");
  }

  @Test
  public void test_ofDeltaAmount() {
    ValueAdjustment test = ValueAdjustment.ofDeltaAmount(20);
    assertThat(test.getModifyingValue()).isEqualTo(20);
    assertThat(test.getType()).isEqualTo(ValueAdjustmentType.DELTA_AMOUNT);
    assertThat(test.adjust(100)).isEqualTo(120);
    assertThat(test.toString()).isEqualTo("ValueAdjustment[result = input + 20.0]");
  }

  @Test
  public void test_ofDeltaMultiplier() {
    ValueAdjustment test = ValueAdjustment.ofDeltaMultiplier(0.1);
    assertThat(test.getModifyingValue()).isEqualTo(0.1);
    assertThat(test.getType()).isEqualTo(ValueAdjustmentType.DELTA_MULTIPLIER);
    assertThat(test.adjust(100)).isEqualTo(110, TOLERANCE);
    assertThat(test.toString()).isEqualTo("ValueAdjustment[result = input + input * 0.1]");
  }

  @Test
  public void test_ofMultiplier() {
    ValueAdjustment test = ValueAdjustment.ofMultiplier(1.1);
    assertThat(test.getModifyingValue()).isEqualTo(1.1);
    assertThat(test.getType()).isEqualTo(ValueAdjustmentType.MULTIPLIER);
    assertThat(test.adjust(100)).isEqualTo(110, TOLERANCE);
    assertThat(test.toString()).isEqualTo("ValueAdjustment[result = input * 1.1]");
  }

  //-------------------------------------------------------------------------
  @Test
  public void equals() {
    ValueAdjustment a1 = ValueAdjustment.ofReplace(200);
    ValueAdjustment a2 = ValueAdjustment.ofReplace(200);
    ValueAdjustment b = ValueAdjustment.ofDeltaMultiplier(200);
    ValueAdjustment c = ValueAdjustment.ofDeltaMultiplier(0.1);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(c)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(ValueAdjustment.ofReplace(200));
  }

  @Test
  public void test_serialization() {
    assertSerialization(ValueAdjustment.ofReplace(200));
  }

}
