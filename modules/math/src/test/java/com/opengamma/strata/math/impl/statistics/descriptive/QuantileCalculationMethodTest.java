/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.integration.RungeKuttaIntegrator1D;

/**
 * Test {@link QuantileCalculationMethod} and its implementations.
 */
@Test
public class QuantileCalculationMethodTest {

  private static final IndexAboveQuantileMethod QUANTILE_INDEX_ABOVE = IndexAboveQuantileMethod.DEFAULT;
  private static final NearestIndexQuantileMethod QUANTILE_NEAREST_INDEX = NearestIndexQuantileMethod.DEFAULT;
  private static final SamplePlusOneNearestIndexQuantileMethod QUANTILE_SAMPLE1_NEAREST_INDEX =
      SamplePlusOneNearestIndexQuantileMethod.DEFAULT;
  private static final SampleInterpolationQuantileMethod QUANTILE_SAMPLE_INTERPOLATION =
      SampleInterpolationQuantileMethod.DEFAULT;
  private static final SamplePlusOneInterpolationQuantileMethod QUANTILE_SAMPLE1_INTERPOLATION =
      SamplePlusOneInterpolationQuantileMethod.DEFAULT;
  private static final MidwayInterpolationQuantileMethod QUANTILE_MIDWAY_INTERPOLATION =
      MidwayInterpolationQuantileMethod.DEFAULT;
  private static final RungeKuttaIntegrator1D INTEG = new RungeKuttaIntegrator1D();
  private static final double TOL_INTEGRAL = 1.0e-8;

  private static final DoubleArray UNSORTED_100 = DoubleArray.copyOf(new double[] {
      0.0237, 0.1937, 0.8809, 0.1733, 0.3506, 0.5376, 0.9825, 0.4312, 0.3867, 0.1333, 0.5955, 0.9544, 0.6803, 0.0455,
      0.0856, 0.4352, 0.7584, 0.5514, 0.6541, 0.3978, 0.3788, 0.8850, 0.7649, 0.0405, 0.8908, 0.6704, 0.1587, 0.7711,
      0.3952, 0.6093, 0.7686, 0.9327, 0.5229, 0.5449, 0.8742, 0.2808, 0.8203, 0.8464, 0.3886, 0.6139, 0.9245, 0.1733,
      0.2583, 0.5908, 0.9387, 0.0666, 0.5747, 0.1704, 0.6772, 0.4914, 0.1453, 0.3012, 0.7400, 0.4040, 0.9535, 0.4559,
      0.6973, 0.6567, 0.1478, 0.3592, 0.0174, 0.0679, 0.9434, 0.7864, 0.7549, 0.2831, 0.0955, 0.6263, 0.4323, 0.1303,
      0.4073, 0.4964, 0.9917, 0.5385, 0.0745, 0.0724, 0.1745, 0.7220, 0.5342, 0.9532, 0.1927, 0.2631, 0.8871, 0.3213,
      0.0967, 0.9255, 0.8922, 0.8758, 0.8159, 0.5188, 0.9948, 0.5192, 0.4513, 0.8976, 0.8418, 0.0589, 0.0317, 0.2319,
      0.2633, 0.7495});
  private static final DoubleArray SORTED_100 = DoubleArray.copyOf(new double[] {
      0.0174, 0.0237, 0.0317, 0.0405, 0.0455, 0.0589, 0.0666, 0.0679, 0.0724, 0.0745, 0.0856, 0.0955, 0.0967, 0.1303,
      0.1333, 0.1453, 0.1478, 0.1587, 0.1704, 0.1733, 0.1733, 0.1745, 0.1927, 0.1937, 0.2319, 0.2583, 0.2631, 0.2633,
      0.2808, 0.2831, 0.3012, 0.3213, 0.3506, 0.3592, 0.3788, 0.3867, 0.3886, 0.3952, 0.3978, 0.4040, 0.4073, 0.4312,
      0.4323, 0.4352, 0.4513, 0.4559, 0.4914, 0.4964, 0.5188, 0.5192, 0.5229, 0.5342, 0.5376, 0.5385, 0.5449, 0.5514,
      0.5747, 0.5908, 0.5955, 0.6093, 0.6139, 0.6263, 0.6541, 0.6567, 0.6704, 0.6772, 0.6803, 0.6973, 0.7220, 0.7400,
      0.7495, 0.7549, 0.7584, 0.7649, 0.7686, 0.7711, 0.7864, 0.8159, 0.8203, 0.8418, 0.8464, 0.8742, 0.8758, 0.8809,
      0.8850, 0.8871, 0.8908, 0.8922, 0.8976, 0.9245, 0.9255, 0.9327, 0.9387, 0.9434, 0.9532, 0.9535, 0.9544, 0.9825,
      0.9917, 0.9948});
  private static final int SAMPLE_SIZE_100 = UNSORTED_100.size();
  private static final DoubleArray SORTED_123 = DoubleArray.copyOf(new double[] {
      0.0058, 0.0128, 0.0222, 0.0352, 0.0393, 0.0478, 0.0490, 0.0594, 0.0625, 0.0701, 0.1110, 0.1147, 0.1290, 0.1353,
      0.1359, 0.1405, 0.1484, 0.1614, 0.1740, 0.1859, 0.2031, 0.2185, 0.2190, 0.2224, 0.2311, 0.2394, 0.2456, 0.2467,
      0.2531, 0.2633, 0.2735, 0.2782, 0.2817, 0.2855, 0.2861, 0.2910, 0.2961, 0.3075, 0.3077, 0.3218, 0.3266, 0.3504,
      0.3505, 0.3641, 0.3689, 0.3821, 0.3906, 0.3952, 0.4151, 0.4167, 0.4189, 0.4237, 0.4467, 0.4582, 0.4584, 0.4613,
      0.4694, 0.4814, 0.4999, 0.5014, 0.5050, 0.5085, 0.5154, 0.5326, 0.5328, 0.5491, 0.5678, 0.5718, 0.5743, 0.5755,
      0.5781, 0.5935, 0.5960, 0.6044, 0.6174, 0.6238, 0.6272, 0.6291, 0.6520, 0.6731, 0.6763, 0.6790, 0.6967, 0.7156,
      0.7228, 0.7348, 0.7451, 0.7500, 0.7676, 0.7680, 0.7710, 0.7822, 0.7868, 0.7950, 0.7987, 0.8013, 0.8120, 0.8156,
      0.8293, 0.8325, 0.8423, 0.8483, 0.8486, 0.8507, 0.8595, 0.8661, 0.8682, 0.8722, 0.8748, 0.8785, 0.9071, 0.9188,
      0.9230, 0.9322, 0.9329, 0.9403, 0.9411, 0.9426, 0.9569, 0.9635, 0.9688, 0.9752, 0.9841});
  private static final DoubleArray UNSORTED_123 = DoubleArray.copyOf(new double[] {
      0.3266, 0.5718, 0.9841, 0.3505, 0.9071, 0.8293, 0.9411, 0.5935, 0.5491, 0.4613, 0.8722, 0.1614, 0.0594, 0.4151,
      0.4237, 0.1353, 0.8507, 0.5781, 0.6967, 0.9752, 0.1405, 0.3689, 0.7676, 0.2782, 0.6763, 0.2910, 0.9426, 0.3218,
      0.1359, 0.8423, 0.2185, 0.9230, 0.5328, 0.0478, 0.3075, 0.8486, 0.8013, 0.6731, 0.6272, 0.1290, 0.2311, 0.8661,
      0.8595, 0.1484, 0.5743, 0.5755, 0.0352, 0.3077, 0.4584, 0.5960, 0.6174, 0.3952, 0.2031, 0.5050, 0.7228, 0.5326,
      0.2633, 0.0701, 0.4467, 0.7156, 0.2961, 0.9329, 0.2394, 0.5154, 0.2861, 0.5014, 0.4167, 0.4694, 0.2531, 0.0222,
      0.1859, 0.0490, 0.8156, 0.7451, 0.0128, 0.8682, 0.2817, 0.9403, 0.7680, 0.9688, 0.0058, 0.2855, 0.8483, 0.6044,
      0.4582, 0.7987, 0.3906, 0.9322, 0.8325, 0.0625, 0.7710, 0.4189, 0.0393, 0.9569, 0.8120, 0.5678, 0.6238, 0.4999,
      0.5085, 0.7822, 0.4814, 0.6790, 0.6291, 0.7950, 0.2456, 0.3504, 0.2467, 0.1147, 0.8748, 0.3821, 0.1110, 0.3641,
      0.2735, 0.2190, 0.7868, 0.2224, 0.7500, 0.9635, 0.6520, 0.9188, 0.7348, 0.8785, 0.1740});
  private static final int SAMPLE_SIZE_123 = UNSORTED_123.size();

  private static final double TOL = 1.0E-10;
  private static final double LEVEL1 = 0.935;
  private static final double LEVEL2 = 0.764;
  private static final double LEVEL3 = 0.95;
  private static final double LEVEL4 = 0.0001;
  private static final double LEVEL5 = 0.9999;

  //-------------------------------------------------------------------------
  public void discrete_wrong_quantile_large() {
    assertThrowsIllegalArg(() -> QUANTILE_INDEX_ABOVE.quantileFromUnsorted(1.01, SORTED_100));
  }

  public void discrete_wrong_quantile_0() {
    assertThrowsIllegalArg(() -> QUANTILE_INDEX_ABOVE.quantileFromUnsorted(0.0, SORTED_100));
  }

  public void interpolation_wrong_quantile_1() {
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(1.01, SORTED_100));
  }

  public void interpolation_wrong_quantile_0() {
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(0.0, SORTED_100));
  }

  public void interpolation_wrong_quantile_small() {
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL4, SORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL4, SORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_MIDWAY_INTERPOLATION.quantileFromUnsorted(LEVEL4, SORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL4, SORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL4, SORTED_100));
  }

  public void interpolation_wrong_quantile_large() {
    assertThrowsIllegalArg(() -> QUANTILE_MIDWAY_INTERPOLATION.quantileFromUnsorted(LEVEL5, SORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL5, SORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL5, SORTED_100));
  }

  //-------------------------------------------------------------------------
  public void discrete_wrong_expectedShortfall_large() {
    assertThrowsIllegalArg(() -> QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(1.01, SORTED_100));
  }

  public void discrete_wrong_expectedShortfall_0() {
    assertThrowsIllegalArg(() -> QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(0.0, SORTED_100));
  }

  public void interpolation_wrong_expectedShortfall_1() {
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE_INTERPOLATION.expectedShortfallFromUnsorted(1.01, SORTED_100));
  }

  public void interpolation_wrong_expectedShortfall_0() {
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE_INTERPOLATION.expectedShortfallFromUnsorted(0.0, SORTED_100));
  }

  //-------------------------------------------------------------------------
  public void index_above_095_100() {
    double indexDouble = LEVEL3 * SAMPLE_SIZE_100;
    int indexCeil = (int) Math.ceil(indexDouble);
    double quantileExpected = SORTED_100.get(indexCeil - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_INDEX_ABOVE.quantileFromUnsorted(LEVEL3, SORTED_100).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
    double quantileExtrapComputed = QUANTILE_INDEX_ABOVE.quantileWithExtrapolationFromUnsorted(
        LEVEL3,
        SORTED_100).getValue();
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  public void index_above_095_123() {
    double indexDouble = LEVEL3 * SAMPLE_SIZE_123;
    int indexCeil = (int) Math.ceil(indexDouble);
    double quantileExpected = SORTED_123.get(indexCeil - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_INDEX_ABOVE.quantileFromUnsorted(LEVEL3, SORTED_123).getValue();
    assertEquals(quantileComputed, quantileExpected);
    double quantileExtrapComputed = QUANTILE_INDEX_ABOVE.quantileWithExtrapolationFromUnsorted(
        LEVEL3,
        SORTED_123).getValue();
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  /* On sample points, different methods match. */
  public void index_nearest_095_100() {
    double quantileExpected = QUANTILE_INDEX_ABOVE.quantileFromUnsorted(LEVEL3, SORTED_100).getValue();
    double quantileComputed = QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL3, SORTED_100).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void index_nearest_0951_100() {
    double indexDouble = (LEVEL3 + 0.001) * SAMPLE_SIZE_100;
    int indexRound = (int) Math.round(indexDouble);
    double quantileExpected = SORTED_100.get(indexRound - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL3, SORTED_100).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
    double quantileExtrapComputed = QUANTILE_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(
        LEVEL3,
        SORTED_100).getValue();
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  public void index_nearest_0001_100() {
    double quantileExpected = SORTED_100.get(0); // Java index start at 0.
    double quantileComputed = QUANTILE_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(
        LEVEL4,
        SORTED_100).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void index_nearest_9999_100() {
    double quantileExpected = SORTED_100.get(SAMPLE_SIZE_100 - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(
        LEVEL5,
        SORTED_100).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void index_nearest_one_0951_100() {
    double indexDouble = (LEVEL3 + 0.001) * (SAMPLE_SIZE_100 + 1d);
    int indexRound = (int) Math.round(indexDouble);
    double quantileExpected = SORTED_100.get(indexRound - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL3, SORTED_100).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
    double quantileExtrapComputed = QUANTILE_SAMPLE1_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(
        LEVEL3,
        SORTED_100).getValue();
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  public void index_nearest_one_0001_100() {
    double quantileExpected = SORTED_100.get(0); // Java index start at 0.
    double quantileComputed = QUANTILE_SAMPLE1_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(
        LEVEL4,
        SORTED_100).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void index_nearest_one_9999_100() {
    double quantileExpected = SORTED_100.get(SAMPLE_SIZE_100 - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_SAMPLE1_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(
        LEVEL5,
        SORTED_100).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  /* On sample points, different methods match. */
  @Test
  public void interpolation_sample_095_100() {
    double quantileExpected = QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL3, SORTED_100).getValue();
    double quantileComputed = QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL3, SORTED_100).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void interpolation_sample_0001_100() {
    double quantileExpected = SORTED_100.get(0); // Java index start at 0.
    double quantileComputed = QUANTILE_SAMPLE_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL4,
        SORTED_100).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void interpolation_sample_095_123() {
    double indexDouble = LEVEL3 * SAMPLE_SIZE_123;
    int indexCeil = (int) Math.ceil(indexDouble);
    int indexFloor = (int) Math.floor(indexDouble);
    double quantileCeil = SORTED_123.get(indexCeil - 1); // Java index start at 0.
    double quantileFloor = SORTED_123.get(indexFloor - 1);
    double pi = (double) indexFloor / (double) SAMPLE_SIZE_123;
    double pi1 = (double) indexCeil / (double) SAMPLE_SIZE_123;
    double quantileExpected = quantileFloor + (LEVEL3 - pi) / (pi1 - pi) * (quantileCeil - quantileFloor);
    double quantileComputed = QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL3, SORTED_123).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
    double quantileExtrapComputed = QUANTILE_SAMPLE_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL3,
        SORTED_123).getValue();
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  public void interpolation_samplePlusOne_095_123() {
    double indexDouble = LEVEL3 * (SAMPLE_SIZE_123 + 1);
    int indexCeil = (int) Math.ceil(indexDouble);
    int indexFloor = (int) Math.floor(indexDouble);
    double quantileCeil = SORTED_123.get(indexCeil - 1); // Java index start at 0.
    double quantileFloor = SORTED_123.get(indexFloor - 1);
    double pi = (double) indexFloor / (double) (SAMPLE_SIZE_123 + 1);
    double pi1 = (double) indexCeil / (double) (SAMPLE_SIZE_123 + 1);
    double quantileExpected = quantileFloor + (LEVEL3 - pi) / (pi1 - pi) * (quantileCeil - quantileFloor);
    double quantileComputed = QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL3, SORTED_123).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
    double quantileExtrapComputed = QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL3,
        SORTED_123).getValue();
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  public void interpolation_samplePlusOne_0001_100() {
    double quantileExpected = SORTED_100.get(0); // Java index start at 0.
    double quantileComputed = QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL4,
        SORTED_100).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void interpolation_samplePlusOne_9999_100() {
    double quantileExpected = SORTED_100.get(SAMPLE_SIZE_100 - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL5,
        SORTED_100).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void interpolation_midway_095_123() {
    double correction = 0.5;
    double indexDouble = LEVEL3 * SAMPLE_SIZE_123 + correction;
    int indexCeil = (int) Math.ceil(indexDouble);
    int indexFloor = (int) Math.floor(indexDouble);
    double quantileCeil = SORTED_123.get(indexCeil - 1); // Java index start at 0.
    double quantileFloor = SORTED_123.get(indexFloor - 1);
    double pi = (indexFloor - correction) / (double) SAMPLE_SIZE_123;
    double pi1 = (indexCeil - correction) / (double) SAMPLE_SIZE_123;
    double quantileExpected = quantileFloor + (LEVEL3 - pi) / (pi1 - pi) * (quantileCeil - quantileFloor);
    double quantileComputed = QUANTILE_MIDWAY_INTERPOLATION.quantileFromUnsorted(LEVEL3, SORTED_123).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
    double quantileExtrapComputed = QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL3,
        SORTED_123).getValue();
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  public void interpolation_midway_0001_100() {
    double quantileExpected = SORTED_100.get(0); // Java index start at 0.
    double quantileComputed = QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL4,
        SORTED_100).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void interpolation_midway_9999_100() {
    double quantileExpected = SORTED_100.get(SAMPLE_SIZE_100 - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL5,
        SORTED_100).getValue();
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void excel() {
    DoubleArray data = DoubleArray.of(1.0, 3.0, 2.0, 4.0);
    double level = 0.3;
    double quantileComputed = ExcelInterpolationQuantileMethod.DEFAULT.quantileFromUnsorted(level, data).getValue();
    double quantileExpected = 1.9; // From Excel doc
    assertEquals(quantileComputed, quantileExpected, TOL);
    double quantileExtrapComputed = ExcelInterpolationQuantileMethod.DEFAULT
        .quantileWithExtrapolationFromUnsorted(level, data).getValue();
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  //-------------------------------------------------------------------------
  public void index_above_095_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_INDEX_ABOVE.quantileWithExtrapolationFromUnsorted(level, SORTED_100).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(
        LEVEL3,
        SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }



  public void index_above_095_123_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_INDEX_ABOVE.quantileWithExtrapolationFromUnsorted(level, SORTED_123).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(
        LEVEL3,
        SORTED_123).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void index_above_0001_100_expected_shortfall() {
    double expectedShortfallComputed = QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(
        LEVEL4,
        SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, SORTED_100.get(0), TOL_INTEGRAL);
  }

  public void index_above_9999_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_INDEX_ABOVE.quantileWithExtrapolationFromUnsorted(level, SORTED_100).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL5) / LEVEL5;
    double expectedShortfallComputed = QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(
        LEVEL5,
        SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void index_nearest_095_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(level, SORTED_100).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_NEAREST_INDEX.expectedShortfallFromUnsorted(
        LEVEL3,
        SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void index_nearest_095_123_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(level, SORTED_123).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_NEAREST_INDEX.expectedShortfallFromUnsorted(
        LEVEL3,
        SORTED_123).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void index_nearest_0001_100_expected_shortfall() {
    double expectedShortfallComputed =
        QUANTILE_NEAREST_INDEX.expectedShortfallFromUnsorted(LEVEL4, SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, SORTED_100.get(0), TOL_INTEGRAL);
  }

  public void index_nearest_9999_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(level, SORTED_100).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL5) / LEVEL5;
    double expectedShortfallComputed = QUANTILE_NEAREST_INDEX.expectedShortfallFromUnsorted(
        LEVEL5,
        SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void index_nearest_one_095_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_SAMPLE1_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(level, SORTED_100).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_SAMPLE1_NEAREST_INDEX.expectedShortfallFromUnsorted(
        LEVEL3,
        SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void index_nearest_one_0001_100_expected_shortfall() {
    double expectedShortfallExpected = SORTED_100.get(0);
    double expectedShortfallComputed =
        QUANTILE_SAMPLE1_NEAREST_INDEX.expectedShortfallFromUnsorted(LEVEL4, SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL);
  }

  public void index_nearest_one_9999_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_SAMPLE1_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(level, SORTED_100).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL5) / LEVEL5;
    double expectedShortfallComputed =
        QUANTILE_SAMPLE1_NEAREST_INDEX.expectedShortfallFromUnsorted(LEVEL5, SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_sample_095_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_SAMPLE_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, SORTED_100).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_SAMPLE_INTERPOLATION.expectedShortfallFromUnsorted(
        LEVEL3,
        SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_sample_095_123_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_SAMPLE_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, SORTED_123).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_SAMPLE_INTERPOLATION.expectedShortfallFromUnsorted(
        LEVEL3,
        SORTED_123).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_sample_0001_100_expected_shortfall() {
    double expectedShortfallExpected = SORTED_100.get(0);
    double expectedShortfallComputed =
        QUANTILE_SAMPLE_INTERPOLATION.expectedShortfallFromUnsorted(LEVEL4, SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL);
  }

  public void interpolation_sample_9999_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_SAMPLE_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, SORTED_100).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL5) / LEVEL5;
    double expectedShortfallComputed =
        QUANTILE_SAMPLE_INTERPOLATION.expectedShortfallFromUnsorted(LEVEL5, SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_samplePlusOne_095_123_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, SORTED_123).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_SAMPLE1_INTERPOLATION.expectedShortfallFromUnsorted(
        LEVEL3,
        SORTED_123).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_samplePlusOne_0001_100_expected_shortfall() {
    double expectedShortfallExpected = SORTED_100.get(0);
    double expectedShortfallComputed =
        QUANTILE_SAMPLE1_INTERPOLATION.expectedShortfallFromUnsorted(LEVEL4, SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL);
  }

  public void interpolation_samplePlusOne_9999_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, SORTED_100).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL5) / LEVEL5;
    double expectedShortfallComputed =
        QUANTILE_SAMPLE1_INTERPOLATION.expectedShortfallFromUnsorted(LEVEL5, SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_midway_095_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, SORTED_100).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_MIDWAY_INTERPOLATION.expectedShortfallFromUnsorted(
        LEVEL3,
        SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_midway_095_123_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, SORTED_123).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_MIDWAY_INTERPOLATION.expectedShortfallFromUnsorted(
        LEVEL3,
        SORTED_123).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_midway_0001_100_expected_shortfall() {
    double expectedShortfallExpected = SORTED_100.get(0);
    double expectedShortfallComputed =
        QUANTILE_MIDWAY_INTERPOLATION.expectedShortfallFromUnsorted(LEVEL4, SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL);
  }

  public void interpolation_midway_9999_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, SORTED_100).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL5) / LEVEL5;
    double expectedShortfallComputed =
        QUANTILE_MIDWAY_INTERPOLATION.expectedShortfallFromUnsorted(LEVEL5, SORTED_100).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void excel_expected_shortfall() {
    DoubleArray data = DoubleArray.of(1.0, 3.0, 2.0, 4.0);
    double level = 0.3;
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return ExcelInterpolationQuantileMethod.DEFAULT.quantileWithExtrapolationFromUnsorted(level, data).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL / 1000d, level) / level;
    double expectedShortfallComputed =
        ExcelInterpolationQuantileMethod.DEFAULT.expectedShortfallFromUnsorted(level, data).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void excel_expected_shortfall_0001() {
    DoubleArray data = DoubleArray.of(1.0, 3.0, 2.0, 4.0);
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return ExcelInterpolationQuantileMethod.DEFAULT.quantileWithExtrapolationFromUnsorted(level, data).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL / 1000d, LEVEL4) / LEVEL4;
    double expectedShortfallComputed =
        ExcelInterpolationQuantileMethod.DEFAULT.expectedShortfallFromUnsorted(LEVEL4, data).getValue();
    assertEquals(
        expectedShortfallComputed,
        expectedShortfallExpected,
        TOL_INTEGRAL * 10d); // high sensitivity to lower bound
  }

  public void excel_expected_shortfall_9999() {
    DoubleArray data = DoubleArray.of(1.0, 3.0, 2.0, 4.0);
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return ExcelInterpolationQuantileMethod.DEFAULT.quantileWithExtrapolationFromUnsorted(level, data).getValue();
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL / 1000d, LEVEL5) / LEVEL5;
    double expectedShortfallComputed =
        ExcelInterpolationQuantileMethod.DEFAULT.expectedShortfallFromUnsorted(LEVEL5, data).getValue();
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  //-------------------------------------------------------------------------
  public void regression_test1() {
    assertEquals(QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL1, SORTED_100).getValue(), 0.92365, TOL);
    assertEquals(QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL1, SORTED_100).getValue(), 0.9275, TOL);
    assertEquals(
        QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(LEVEL1, SORTED_100).getValue(),
        0.5114133689839573,
        TOL);
    assertEquals(QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL2, SORTED_100).getValue(), 0.80356, TOL);
    assertEquals(QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL2, SORTED_100).getValue(), 0.8024, TOL);
    assertEquals(
        QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(LEVEL2, SORTED_100).getValue(),
        0.4333301047120419,
        TOL);
    assertEquals(QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL3, SORTED_100).getValue(), 0.9524, TOL);
    assertEquals(QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL3, SORTED_100).getValue(), 0.9524, TOL);
    assertEquals(
        QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(LEVEL3, SORTED_100).getValue(),
        0.5182452631578948,
        TOL);
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL4, SORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL4, SORTED_100));
    assertEquals(QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL5, SORTED_100).getValue(), 0.981218, TOL);
    assertEquals(QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL5, SORTED_100).getValue(), 0.9813, TOL);
    assertEquals(
        QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(LEVEL5, SORTED_100).getValue(),
        0.5407389438943896,
        TOL);
  }

  public void regression_test2() {
    assertEquals(QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL1, SORTED_100).getValue(), 0.9383315, TOL);
    assertEquals(QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL1, SORTED_100).getValue(), 0.9275, TOL);
    assertEquals(QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL2, SORTED_100).getValue(), 0.8056608, TOL);
    assertEquals(QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL2, SORTED_100).getValue(), 0.8053, TOL);
    assertEquals(QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL3, SORTED_100).getValue(), 0.95677, TOL);
    assertEquals(QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL3, SORTED_100).getValue(), 0.957, TOL);
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL4, SORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL4, SORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL5, SORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL5, SORTED_100));
  }

  public void regression_test3() {
    assertEquals(
        QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL1, SORTED_100).getValue(),
        0.9275,
        TOL);
    assertEquals(
        QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL2, SORTED_100).getValue(),
        0.80501,
        TOL);
    assertEquals(
        QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL3, SORTED_100).getValue(),
        0.9547,
        TOL);
    assertEquals(
        QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL4, SORTED_100).getValue(),
        0.0286,
        TOL);
    assertEquals(
        QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL5, SORTED_100).getValue(),
        0.9813,
        TOL);
  }

  public void regression_test4() {
    assertEquals(
        QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL1, SORTED_100).getValue(),
        0.9383315,
        TOL);
    assertEquals(
        QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL2, SORTED_100).getValue(),
        0.8056608,
        TOL);
    assertEquals(
        QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL3, SORTED_100).getValue(),
        0.95677,
        TOL);
    assertEquals(
        QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL4, SORTED_100).getValue(),
        0.0286,
        TOL);
    assertEquals(
        QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL5, SORTED_100).getValue(),
        0.9813,
        TOL);
  }

}
