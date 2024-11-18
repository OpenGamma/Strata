/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market;

import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ShiftType}.
 */
public class ShiftTypeTest {

  @Test
  public void test_applyShift() {
    assertThat(ShiftType.ABSOLUTE.applyShift(2, 0.1)).isEqualTo(2.1);
    assertThat(ShiftType.RELATIVE.applyShift(2, 0.1)).isEqualTo(2.2);
    assertThat(ShiftType.SCALED.applyShift(2, 1.1)).isEqualTo(2.2);
    assertThat(ShiftType.FIXED.applyShift(2, 3)).isEqualTo(3);
  }

  @Test
  public void test_toValueAdjustment() {
    assertThat(ShiftType.ABSOLUTE.toValueAdjustment(0.1).adjust(2)).isEqualTo(2.1);
    assertThat(ShiftType.RELATIVE.toValueAdjustment(0.1).adjust(2)).isEqualTo(2.2);
    assertThat(ShiftType.SCALED.toValueAdjustment(1.1).adjust(2)).isEqualTo(2.2);
    assertThat(ShiftType.FIXED.toValueAdjustment(3).adjust(2)).isEqualTo(3);
  }

  @Test
  public void test_computeShift() {
    double tol = 1.0e-15;
    double base = 2.0;
    double shifted = 2.1;

    assertThat(ShiftType.ABSOLUTE.computeShift(base, shifted)).isCloseTo(0.1, offset(tol));
    assertThat(ShiftType.RELATIVE.computeShift(base, shifted)).isCloseTo(0.05, offset(tol));
    assertThat(ShiftType.SCALED.computeShift(base, shifted)).isCloseTo(1.05, offset(tol));
    assertThat(ShiftType.FIXED.computeShift(base, shifted)).isCloseTo(shifted, offset(tol));

    assertThat(ShiftType.ABSOLUTE.applyShift(base, ShiftType.ABSOLUTE.computeShift(base, shifted)))
        .isCloseTo(shifted, offset(tol));
    assertThat(ShiftType.RELATIVE.applyShift(base, ShiftType.RELATIVE.computeShift(base, shifted)))
        .isCloseTo(shifted, offset(tol));
    assertThat(ShiftType.SCALED.applyShift(base, ShiftType.SCALED.computeShift(base, shifted)))
        .isCloseTo(shifted, offset(tol));
    assertThat(ShiftType.FIXED.applyShift(base, ShiftType.FIXED.computeShift(base, shifted)))
        .isCloseTo(shifted, offset(tol));
  }

  @Test
  public void test_name() {
    assertThat(ShiftType.ABSOLUTE.name()).isEqualTo("ABSOLUTE");
    assertThat(ShiftType.RELATIVE.name()).isEqualTo("RELATIVE");
    assertThat(ShiftType.SCALED.name()).isEqualTo("SCALED");
    assertThat(ShiftType.FIXED.name()).isEqualTo("FIXED");
  }

  @Test
  public void test_toString() {
    assertThat(ShiftType.ABSOLUTE.toString()).isEqualTo("Absolute");
    assertThat(ShiftType.RELATIVE.toString()).isEqualTo("Relative");
    assertThat(ShiftType.SCALED.toString()).isEqualTo("Scaled");
    assertThat(ShiftType.FIXED.toString()).isEqualTo("Fixed");
  }

  @Test
  public void coverage() {
    coverEnum(ShiftType.class);
  }

}
