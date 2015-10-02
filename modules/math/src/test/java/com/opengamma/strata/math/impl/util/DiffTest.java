/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.util;

import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;

import org.testng.annotations.Test;

/**
 * Tests {@link Diff}.
 */
@Test
public class DiffTest {

  /* double data */
  double[] _dataDouble = {-7, -3, -6, 0, 1, 14, 2, 4};
  double[] _dataDoubleAnswerDiff0times = {-7, -3, -6, 0, 1, 14, 2, 4};
  double[] _dataDoubleAnswerDiff1times = {4, -3, 6, 1, 13, -12, 2};
  double[] _dataDoubleAnswerDiff2times = {-7, 9, -5, 12, -25, 14};
  double[] _dataDoubleAnswerDiff3times = {16, -14, 17, -37, 39};
  double[] _dataDoubleAnswerDiff4times = {-30, 31, -54, 76};
  double[] _dataDoubleAnswerDiff5times = {61, -85, 130};
  double[] _dataDoubleAnswerDiff6times = {-146, 215};
  double[] _dataDoubleAnswerDiff7times = {361};
  double[] _dataNullDouble = null;

  float[] _dataFloat = {-7, -3, -6, 0, 1, 14, 2, 4};
  float[] _dataFloatAnswerDiff0times = {-7, -3, -6, 0, 1, 14, 2, 4};
  float[] _dataFloatAnswerDiff1times = {4, -3, 6, 1, 13, -12, 2};
  float[] _dataFloatAnswerDiff2times = {-7, 9, -5, 12, -25, 14};
  float[] _dataFloatAnswerDiff3times = {16, -14, 17, -37, 39};
  float[] _dataFloatAnswerDiff4times = {-30, 31, -54, 76};
  float[] _dataFloatAnswerDiff5times = {61, -85, 130};
  float[] _dataFloatAnswerDiff6times = {-146, 215};
  float[] _dataFloatAnswerDiff7times = {361};
  float[] _dataNullFloat = null;

  /* long data */
  long[] _dataLong = {-7, -3, -6, 0, 1, 14, 2, 4};
  long[] _dataLongAnswerDiff0times = {-7, -3, -6, 0, 1, 14, 2, 4};
  long[] _dataLongAnswerDiff1times = {4, -3, 6, 1, 13, -12, 2};
  long[] _dataLongAnswerDiff2times = {-7, 9, -5, 12, -25, 14};
  long[] _dataLongAnswerDiff3times = {16, -14, 17, -37, 39};
  long[] _dataLongAnswerDiff4times = {-30, 31, -54, 76};
  long[] _dataLongAnswerDiff5times = {61, -85, 130};
  long[] _dataLongAnswerDiff6times = {-146, 215};
  long[] _dataLongAnswerDiff7times = {361};
  long[] _dataNullLong = null;

  /* int data */
  int[] _dataInteger = {-7, -3, -6, 0, 1, 14, 2, 4};
  int[] _dataIntegerAnswerDiff0times = {-7, -3, -6, 0, 1, 14, 2, 4};
  int[] _dataIntegerAnswerDiff1times = {4, -3, 6, 1, 13, -12, 2};
  int[] _dataIntegerAnswerDiff2times = {-7, 9, -5, 12, -25, 14};
  int[] _dataIntegerAnswerDiff3times = {16, -14, 17, -37, 39};
  int[] _dataIntegerAnswerDiff4times = {-30, 31, -54, 76};
  int[] _dataIntegerAnswerDiff5times = {61, -85, 130};
  int[] _dataIntegerAnswerDiff6times = {-146, 215};
  int[] _dataIntegerAnswerDiff7times = {361};
  int[] _dataNullInteger = null;

  /* test doubles */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffDoubleNull() {
    Diff.values(_dataNullDouble);
  }

  public void testDiffDouble() {
    assertTrue(Arrays.equals(_dataDoubleAnswerDiff1times, Diff.values(_dataDouble)));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffDoubleNtimesDoubleNull() {
    Diff.values(_dataNullDouble, 1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffDoubleNtimesTtooLarge() {
    Diff.values(_dataDouble, 8);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffDoubleNtimesTtooSmall() {
    Diff.values(_dataDouble, -1);
  }

  public void testDiffDoubleNtimes() {
    assertTrue(Arrays.equals(_dataDoubleAnswerDiff0times, Diff.values(_dataDouble, 0)));
    assertTrue(Arrays.equals(_dataDoubleAnswerDiff1times, Diff.values(_dataDouble, 1)));
    assertTrue(Arrays.equals(_dataDoubleAnswerDiff2times, Diff.values(_dataDouble, 2)));
    assertTrue(Arrays.equals(_dataDoubleAnswerDiff3times, Diff.values(_dataDouble, 3)));
    assertTrue(Arrays.equals(_dataDoubleAnswerDiff4times, Diff.values(_dataDouble, 4)));
    assertTrue(Arrays.equals(_dataDoubleAnswerDiff5times, Diff.values(_dataDouble, 5)));
    assertTrue(Arrays.equals(_dataDoubleAnswerDiff6times, Diff.values(_dataDouble, 6)));
    assertTrue(Arrays.equals(_dataDoubleAnswerDiff7times, Diff.values(_dataDouble, 7)));
  }

  /* test floats */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffFloatNull() {
    Diff.values(_dataNullFloat);
  }

  public void testDiffFloat() {
    assertTrue(Arrays.equals(_dataFloatAnswerDiff1times, Diff.values(_dataFloat)));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffFloatNtimesFloatNull() {
    Diff.values(_dataNullFloat, 1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffFloatNtimesTtooLarge() {
    Diff.values(_dataFloat, 8);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffFloatNtimesTtooSmall() {
    Diff.values(_dataFloat, -1);
  }

  public void testDiffFloatNtimes() {
    assertTrue(Arrays.equals(_dataFloatAnswerDiff0times, Diff.values(_dataFloat, 0)));
    assertTrue(Arrays.equals(_dataFloatAnswerDiff1times, Diff.values(_dataFloat, 1)));
    assertTrue(Arrays.equals(_dataFloatAnswerDiff2times, Diff.values(_dataFloat, 2)));
    assertTrue(Arrays.equals(_dataFloatAnswerDiff3times, Diff.values(_dataFloat, 3)));
    assertTrue(Arrays.equals(_dataFloatAnswerDiff4times, Diff.values(_dataFloat, 4)));
    assertTrue(Arrays.equals(_dataFloatAnswerDiff5times, Diff.values(_dataFloat, 5)));
    assertTrue(Arrays.equals(_dataFloatAnswerDiff6times, Diff.values(_dataFloat, 6)));
    assertTrue(Arrays.equals(_dataFloatAnswerDiff7times, Diff.values(_dataFloat, 7)));
  }

  /* test integers */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffIntegerNull() {
    Diff.values(_dataNullInteger);
  }

  public void testDiffInteger() {
    assertTrue(Arrays.equals(_dataIntegerAnswerDiff1times, Diff.values(_dataInteger)));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffIntegerNtimesIntegerNull() {
    Diff.values(_dataNullInteger, 1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffIntegerNtimesTtooLarge() {
    Diff.values(_dataInteger, 8);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffIntegerNtimesTtooSmall() {
    Diff.values(_dataInteger, -1);
  }

  public void testDiffIntegerNtimes() {
    assertTrue(Arrays.equals(_dataIntegerAnswerDiff0times, Diff.values(_dataInteger, 0)));
    assertTrue(Arrays.equals(_dataIntegerAnswerDiff1times, Diff.values(_dataInteger, 1)));
    assertTrue(Arrays.equals(_dataIntegerAnswerDiff2times, Diff.values(_dataInteger, 2)));
    assertTrue(Arrays.equals(_dataIntegerAnswerDiff3times, Diff.values(_dataInteger, 3)));
    assertTrue(Arrays.equals(_dataIntegerAnswerDiff4times, Diff.values(_dataInteger, 4)));
    assertTrue(Arrays.equals(_dataIntegerAnswerDiff5times, Diff.values(_dataInteger, 5)));
    assertTrue(Arrays.equals(_dataIntegerAnswerDiff6times, Diff.values(_dataInteger, 6)));
    assertTrue(Arrays.equals(_dataIntegerAnswerDiff7times, Diff.values(_dataInteger, 7)));
  }

  /* test longs */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffLongNull() {
    Diff.values(_dataNullLong);
  }

  public void testDiffLong() {
    assertTrue(Arrays.equals(_dataLongAnswerDiff1times, Diff.values(_dataLong)));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffLongNtimesLongNull() {
    Diff.values(_dataNullLong, 1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffLongNtimesTtooLarge() {
    Diff.values(_dataLong, 8);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDiffLongNtimesTtooSmall() {
    Diff.values(_dataLong, -1);
  }

  public void testDiffLongNtimes() {
    assertTrue(Arrays.equals(_dataLongAnswerDiff0times, Diff.values(_dataLong, 0)));
    assertTrue(Arrays.equals(_dataLongAnswerDiff1times, Diff.values(_dataLong, 1)));
    assertTrue(Arrays.equals(_dataLongAnswerDiff2times, Diff.values(_dataLong, 2)));
    assertTrue(Arrays.equals(_dataLongAnswerDiff3times, Diff.values(_dataLong, 3)));
    assertTrue(Arrays.equals(_dataLongAnswerDiff4times, Diff.values(_dataLong, 4)));
    assertTrue(Arrays.equals(_dataLongAnswerDiff5times, Diff.values(_dataLong, 5)));
    assertTrue(Arrays.equals(_dataLongAnswerDiff6times, Diff.values(_dataLong, 6)));
    assertTrue(Arrays.equals(_dataLongAnswerDiff7times, Diff.values(_dataLong, 7)));
  }

}
