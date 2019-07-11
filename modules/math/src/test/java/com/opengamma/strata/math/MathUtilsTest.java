/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Test {@link MathUtils}.
 */
public class MathUtilsTest {

  @Test
  public void test_pow2() {
    assertThat(MathUtils.pow2(3)).isEqualTo(9);
    assertThat(MathUtils.pow2(2)).isEqualTo(4);
    assertThat(MathUtils.pow2(1)).isEqualTo(1);
    assertThat(MathUtils.pow2(0)).isEqualTo(0);
    assertThat(MathUtils.pow2(-1)).isEqualTo(1);
    assertThat(MathUtils.pow2(-2)).isEqualTo(4);
    assertThat(MathUtils.pow2(-3)).isEqualTo(9);
    assertThat(MathUtils.pow2(Double.POSITIVE_INFINITY)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(MathUtils.pow2(Double.NEGATIVE_INFINITY)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(MathUtils.pow2(Double.NaN)).isNaN();
    assertThat(MathUtils.pow2(Double.POSITIVE_INFINITY)).isEqualTo(Math.pow(Double.POSITIVE_INFINITY, 2));
    assertThat(MathUtils.pow2(Double.NEGATIVE_INFINITY)).isEqualTo(Math.pow(Double.NEGATIVE_INFINITY, 2));
    assertThat(MathUtils.pow2(Double.NaN)).isEqualTo(Math.pow(Double.NaN, 2));
  }

  @Test
  public void test_pow3() {
    assertThat(MathUtils.pow3(3)).isEqualTo(27);
    assertThat(MathUtils.pow3(2)).isEqualTo(8);
    assertThat(MathUtils.pow3(1)).isEqualTo(1);
    assertThat(MathUtils.pow3(0)).isEqualTo(0);
    assertThat(MathUtils.pow3(-1)).isEqualTo(-1);
    assertThat(MathUtils.pow3(-2)).isEqualTo(-8);
    assertThat(MathUtils.pow3(-3)).isEqualTo(-27);
    assertThat(MathUtils.pow3(Double.POSITIVE_INFINITY)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(MathUtils.pow3(Double.NEGATIVE_INFINITY)).isEqualTo(Double.NEGATIVE_INFINITY);
    assertThat(MathUtils.pow3(Double.NaN)).isNaN();
    assertThat(MathUtils.pow3(Double.POSITIVE_INFINITY)).isEqualTo(Math.pow(Double.POSITIVE_INFINITY, 3));
    assertThat(MathUtils.pow3(Double.NEGATIVE_INFINITY)).isEqualTo(Math.pow(Double.NEGATIVE_INFINITY, 3));
    assertThat(MathUtils.pow3(Double.NaN)).isEqualTo(Math.pow(Double.NaN, 3));
  }

  @Test
  public void test_pow4() {
    assertThat(MathUtils.pow4(3)).isEqualTo(81);
    assertThat(MathUtils.pow4(2)).isEqualTo(16);
    assertThat(MathUtils.pow4(1)).isEqualTo(1);
    assertThat(MathUtils.pow4(0)).isEqualTo(0);
    assertThat(MathUtils.pow4(-1)).isEqualTo(1);
    assertThat(MathUtils.pow4(-2)).isEqualTo(16);
    assertThat(MathUtils.pow4(-3)).isEqualTo(81);
    assertThat(MathUtils.pow4(Double.POSITIVE_INFINITY)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(MathUtils.pow4(Double.NEGATIVE_INFINITY)).isEqualTo(Double.POSITIVE_INFINITY);
    assertThat(MathUtils.pow4(Double.NaN)).isNaN();
    assertThat(MathUtils.pow4(Double.POSITIVE_INFINITY)).isEqualTo(Math.pow(Double.POSITIVE_INFINITY, 4));
    assertThat(MathUtils.pow4(Double.NEGATIVE_INFINITY)).isEqualTo(Math.pow(Double.NEGATIVE_INFINITY, 4));
    assertThat(MathUtils.pow4(Double.NaN)).isEqualTo(Math.pow(Double.NaN, 4));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_nearZero() {
    assertThat(MathUtils.nearZero(0, 0)).isTrue();
    assertThat(MathUtils.nearZero(-0, 0)).isTrue();

    assertThat(MathUtils.nearZero(0, 0.5)).isTrue();
    assertThat(MathUtils.nearZero(-0, 0.5)).isTrue();

    assertThat(MathUtils.nearZero(0.5, 0.5)).isTrue();
    assertThat(MathUtils.nearZero(Math.nextAfter(0.5, -1000d), 0.5)).isTrue();
    assertThat(MathUtils.nearZero(Math.nextAfter(0.5, 1000d), 0.5)).isFalse();

    assertThat(MathUtils.nearZero(-0.5, 0.5)).isTrue();
    assertThat(MathUtils.nearZero(Math.nextAfter(-0.5, -1000d), 0.5)).isFalse();
    assertThat(MathUtils.nearZero(Math.nextAfter(-0.5, 1000d), 0.5)).isTrue();

    assertThat(MathUtils.nearZero(Double.POSITIVE_INFINITY, 0.5)).isFalse();
    assertThat(MathUtils.nearZero(Double.NEGATIVE_INFINITY, 0.5)).isFalse();
    assertThat(MathUtils.nearZero(Double.NaN, 0.5)).isFalse();
  }

  @Test
  public void test_nearOne() {
    assertThat(MathUtils.nearOne(1, 0)).isTrue();
    assertThat(MathUtils.nearOne(1, 0.5)).isTrue();

    assertThat(MathUtils.nearOne(1.5, 0.5)).isTrue();
    assertThat(MathUtils.nearOne(Math.nextAfter(1.5, -1000d), 0.5)).isTrue();
    assertThat(MathUtils.nearOne(Math.nextAfter(1.5, 1000d), 0.5)).isFalse();

    assertThat(MathUtils.nearOne(0.5, 0.5)).isTrue();
    assertThat(MathUtils.nearOne(Math.nextAfter(0.5, -1000d), 0.5)).isFalse();
    assertThat(MathUtils.nearOne(Math.nextAfter(0.5, 1000d), 0.5)).isTrue();

    assertThat(MathUtils.nearOne(Double.POSITIVE_INFINITY, 0.5)).isFalse();
    assertThat(MathUtils.nearOne(Double.NEGATIVE_INFINITY, 0.5)).isFalse();
    assertThat(MathUtils.nearOne(Double.NaN, 0.5)).isFalse();
  }

}
