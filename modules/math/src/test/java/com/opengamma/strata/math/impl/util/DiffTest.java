/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link Diff}.
 */
public class DiffTest {

  /* double data */
  private final double[] _dataDouble = {-7, -3, -6, 0, 1, 14, 2, 4};
  private final double[] _dataDoubleAnswerDiff0times = {-7, -3, -6, 0, 1, 14, 2, 4};
  private final double[] _dataDoubleAnswerDiff1times = {4, -3, 6, 1, 13, -12, 2};
  private final double[] _dataDoubleAnswerDiff2times = {-7, 9, -5, 12, -25, 14};
  private final double[] _dataDoubleAnswerDiff3times = {16, -14, 17, -37, 39};
  private final double[] _dataDoubleAnswerDiff4times = {-30, 31, -54, 76};
  private final double[] _dataDoubleAnswerDiff5times = {61, -85, 130};
  private final double[] _dataDoubleAnswerDiff6times = {-146, 215};
  private final double[] _dataDoubleAnswerDiff7times = {361};
  private final double[] _dataNullDouble = null;

  private final float[] _dataFloat = {-7, -3, -6, 0, 1, 14, 2, 4};
  private final float[] _dataFloatAnswerDiff0times = {-7, -3, -6, 0, 1, 14, 2, 4};
  private final float[] _dataFloatAnswerDiff1times = {4, -3, 6, 1, 13, -12, 2};
  private final float[] _dataFloatAnswerDiff2times = {-7, 9, -5, 12, -25, 14};
  private final float[] _dataFloatAnswerDiff3times = {16, -14, 17, -37, 39};
  private final float[] _dataFloatAnswerDiff4times = {-30, 31, -54, 76};
  private final float[] _dataFloatAnswerDiff5times = {61, -85, 130};
  private final float[] _dataFloatAnswerDiff6times = {-146, 215};
  private final float[] _dataFloatAnswerDiff7times = {361};
  private final float[] _dataNullFloat = null;

  /* long data */
  private final long[] _dataLong = {-7, -3, -6, 0, 1, 14, 2, 4};
  private final long[] _dataLongAnswerDiff0times = {-7, -3, -6, 0, 1, 14, 2, 4};
  private final long[] _dataLongAnswerDiff1times = {4, -3, 6, 1, 13, -12, 2};
  private final long[] _dataLongAnswerDiff2times = {-7, 9, -5, 12, -25, 14};
  private final long[] _dataLongAnswerDiff3times = {16, -14, 17, -37, 39};
  private final long[] _dataLongAnswerDiff4times = {-30, 31, -54, 76};
  private final long[] _dataLongAnswerDiff5times = {61, -85, 130};
  private final long[] _dataLongAnswerDiff6times = {-146, 215};
  private final long[] _dataLongAnswerDiff7times = {361};
  private final long[] _dataNullLong = null;

  /* int data */
  private final int[] _dataInteger = {-7, -3, -6, 0, 1, 14, 2, 4};
  private final int[] _dataIntegerAnswerDiff0times = {-7, -3, -6, 0, 1, 14, 2, 4};
  private final int[] _dataIntegerAnswerDiff1times = {4, -3, 6, 1, 13, -12, 2};
  private final int[] _dataIntegerAnswerDiff2times = {-7, 9, -5, 12, -25, 14};
  private final int[] _dataIntegerAnswerDiff3times = {16, -14, 17, -37, 39};
  private final int[] _dataIntegerAnswerDiff4times = {-30, 31, -54, 76};
  private final int[] _dataIntegerAnswerDiff5times = {61, -85, 130};
  private final int[] _dataIntegerAnswerDiff6times = {-146, 215};
  private final int[] _dataIntegerAnswerDiff7times = {361};
  private final int[] _dataNullInteger = null;

  /* test doubles */
  @Test
  public void testDiffDoubleNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataNullDouble));
  }

  @Test
  public void testDiffDouble() {
    assertThat(Arrays.equals(_dataDoubleAnswerDiff1times, Diff.values(_dataDouble))).isTrue();
  }

  @Test
  public void testDiffDoubleNtimesDoubleNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataNullDouble, 1));
  }

  @Test
  public void testDiffDoubleNtimesTtooLarge() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataDouble, 8));
  }

  @Test
  public void testDiffDoubleNtimesTtooSmall() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataDouble, -1));
  }

  @Test
  public void testDiffDoubleNtimes() {
    assertThat(Arrays.equals(_dataDoubleAnswerDiff0times, Diff.values(_dataDouble, 0))).isTrue();
    assertThat(Arrays.equals(_dataDoubleAnswerDiff1times, Diff.values(_dataDouble, 1))).isTrue();
    assertThat(Arrays.equals(_dataDoubleAnswerDiff2times, Diff.values(_dataDouble, 2))).isTrue();
    assertThat(Arrays.equals(_dataDoubleAnswerDiff3times, Diff.values(_dataDouble, 3))).isTrue();
    assertThat(Arrays.equals(_dataDoubleAnswerDiff4times, Diff.values(_dataDouble, 4))).isTrue();
    assertThat(Arrays.equals(_dataDoubleAnswerDiff5times, Diff.values(_dataDouble, 5))).isTrue();
    assertThat(Arrays.equals(_dataDoubleAnswerDiff6times, Diff.values(_dataDouble, 6))).isTrue();
    assertThat(Arrays.equals(_dataDoubleAnswerDiff7times, Diff.values(_dataDouble, 7))).isTrue();
  }

  /* test floats */
  @Test
  public void testDiffFloatNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataNullFloat));
  }

  @Test
  public void testDiffFloat() {
    assertThat(Arrays.equals(_dataFloatAnswerDiff1times, Diff.values(_dataFloat))).isTrue();
  }

  @Test
  public void testDiffFloatNtimesFloatNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataNullFloat, 1));
  }

  @Test
  public void testDiffFloatNtimesTtooLarge() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataFloat, 8));
  }

  @Test
  public void testDiffFloatNtimesTtooSmall() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataFloat, -1));
  }

  @Test
  public void testDiffFloatNtimes() {
    assertThat(Arrays.equals(_dataFloatAnswerDiff0times, Diff.values(_dataFloat, 0))).isTrue();
    assertThat(Arrays.equals(_dataFloatAnswerDiff1times, Diff.values(_dataFloat, 1))).isTrue();
    assertThat(Arrays.equals(_dataFloatAnswerDiff2times, Diff.values(_dataFloat, 2))).isTrue();
    assertThat(Arrays.equals(_dataFloatAnswerDiff3times, Diff.values(_dataFloat, 3))).isTrue();
    assertThat(Arrays.equals(_dataFloatAnswerDiff4times, Diff.values(_dataFloat, 4))).isTrue();
    assertThat(Arrays.equals(_dataFloatAnswerDiff5times, Diff.values(_dataFloat, 5))).isTrue();
    assertThat(Arrays.equals(_dataFloatAnswerDiff6times, Diff.values(_dataFloat, 6))).isTrue();
    assertThat(Arrays.equals(_dataFloatAnswerDiff7times, Diff.values(_dataFloat, 7))).isTrue();
  }

  /* test integers */
  @Test
  public void testDiffIntegerNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataNullInteger));
  }

  @Test
  public void testDiffInteger() {
    assertThat(Arrays.equals(_dataIntegerAnswerDiff1times, Diff.values(_dataInteger))).isTrue();
  }

  @Test
  public void testDiffIntegerNtimesIntegerNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataNullInteger, 1));
  }

  @Test
  public void testDiffIntegerNtimesTtooLarge() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataInteger, 8));
  }

  @Test
  public void testDiffIntegerNtimesTtooSmall() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataInteger, -1));
  }

  @Test
  public void testDiffIntegerNtimes() {
    assertThat(Arrays.equals(_dataIntegerAnswerDiff0times, Diff.values(_dataInteger, 0))).isTrue();
    assertThat(Arrays.equals(_dataIntegerAnswerDiff1times, Diff.values(_dataInteger, 1))).isTrue();
    assertThat(Arrays.equals(_dataIntegerAnswerDiff2times, Diff.values(_dataInteger, 2))).isTrue();
    assertThat(Arrays.equals(_dataIntegerAnswerDiff3times, Diff.values(_dataInteger, 3))).isTrue();
    assertThat(Arrays.equals(_dataIntegerAnswerDiff4times, Diff.values(_dataInteger, 4))).isTrue();
    assertThat(Arrays.equals(_dataIntegerAnswerDiff5times, Diff.values(_dataInteger, 5))).isTrue();
    assertThat(Arrays.equals(_dataIntegerAnswerDiff6times, Diff.values(_dataInteger, 6))).isTrue();
    assertThat(Arrays.equals(_dataIntegerAnswerDiff7times, Diff.values(_dataInteger, 7))).isTrue();
  }

  /* test longs */
  @Test
  public void testDiffLongNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataNullLong));
  }

  @Test
  public void testDiffLong() {
    assertThat(Arrays.equals(_dataLongAnswerDiff1times, Diff.values(_dataLong))).isTrue();
  }

  @Test
  public void testDiffLongNtimesLongNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataNullLong, 1));
  }

  @Test
  public void testDiffLongNtimesTtooLarge() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataLong, 8));
  }

  @Test
  public void testDiffLongNtimesTtooSmall() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Diff.values(_dataLong, -1));
  }

  @Test
  public void testDiffLongNtimes() {
    assertThat(Arrays.equals(_dataLongAnswerDiff0times, Diff.values(_dataLong, 0))).isTrue();
    assertThat(Arrays.equals(_dataLongAnswerDiff1times, Diff.values(_dataLong, 1))).isTrue();
    assertThat(Arrays.equals(_dataLongAnswerDiff2times, Diff.values(_dataLong, 2))).isTrue();
    assertThat(Arrays.equals(_dataLongAnswerDiff3times, Diff.values(_dataLong, 3))).isTrue();
    assertThat(Arrays.equals(_dataLongAnswerDiff4times, Diff.values(_dataLong, 4))).isTrue();
    assertThat(Arrays.equals(_dataLongAnswerDiff5times, Diff.values(_dataLong, 5))).isTrue();
    assertThat(Arrays.equals(_dataLongAnswerDiff6times, Diff.values(_dataLong, 6))).isTrue();
    assertThat(Arrays.equals(_dataLongAnswerDiff7times, Diff.values(_dataLong, 7))).isTrue();
  }

}
