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

  private static final DoubleArray UNSORTED_100 = DoubleArray.copyOf(new double[]{
      0.0237, 0.1937, 0.8809, 0.1733, 0.3506, 0.5376, 0.9825, 0.4312, 0.3867, 0.1333, 0.5955, 0.9544, 0.6803, 0.0455,
      0.0856, 0.4352, 0.7584, 0.5514, 0.6541, 0.3978, 0.3788, 0.8850, 0.7649, 0.0405, 0.8908, 0.6704, 0.1587, 0.7711,
      0.3952, 0.6093, 0.7686, 0.9327, 0.5229, 0.5449, 0.8742, 0.2808, 0.8203, 0.8464, 0.3886, 0.6139, 0.9245, 0.1733,
      0.2583, 0.5908, 0.9387, 0.0666, 0.5747, 0.1704, 0.6772, 0.4914, 0.1453, 0.3012, 0.7400, 0.4040, 0.9535, 0.4559,
      0.6973, 0.6567, 0.1478, 0.3592, 0.0174, 0.0679, 0.9434, 0.7864, 0.7549, 0.2831, 0.0955, 0.6263, 0.4323, 0.1303,
      0.4073, 0.4964, 0.9917, 0.5385, 0.0745, 0.0724, 0.1745, 0.7220, 0.5342, 0.9532, 0.1927, 0.2631, 0.8871, 0.3213,
      0.0967, 0.9255, 0.8922, 0.8758, 0.8159, 0.5188, 0.9948, 0.5192, 0.4513, 0.8976, 0.8418, 0.0589, 0.0317, 0.2319,
      0.2633, 0.7495});
  private static final DoubleArray SORTED_100 = DoubleArray.copyOf(new double[]{
      0.0174, 0.0237, 0.0317, 0.0405, 0.0455, 0.0589, 0.0666, 0.0679, 0.0724, 0.0745, 0.0856, 0.0955, 0.0967, 0.1303,
      0.1333, 0.1453, 0.1478, 0.1587, 0.1704, 0.1733, 0.1733, 0.1745, 0.1927, 0.1937, 0.2319, 0.2583, 0.2631, 0.2633,
      0.2808, 0.2831, 0.3012, 0.3213, 0.3506, 0.3592, 0.3788, 0.3867, 0.3886, 0.3952, 0.3978, 0.4040, 0.4073, 0.4312,
      0.4323, 0.4352, 0.4513, 0.4559, 0.4914, 0.4964, 0.5188, 0.5192, 0.5229, 0.5342, 0.5376, 0.5385, 0.5449, 0.5514,
      0.5747, 0.5908, 0.5955, 0.6093, 0.6139, 0.6263, 0.6541, 0.6567, 0.6704, 0.6772, 0.6803, 0.6973, 0.7220, 0.7400,
      0.7495, 0.7549, 0.7584, 0.7649, 0.7686, 0.7711, 0.7864, 0.8159, 0.8203, 0.8418, 0.8464, 0.8742, 0.8758, 0.8809,
      0.8850, 0.8871, 0.8908, 0.8922, 0.8976, 0.9245, 0.9255, 0.9327, 0.9387, 0.9434, 0.9532, 0.9535, 0.9544, 0.9825,
      0.9917, 0.9948});
  private static final int SAMPLE_SIZE_100 = UNSORTED_100.size();
  private static final DoubleArray UNSORTED_123 = DoubleArray.copyOf(new double[]{
      0.0538, 0.6766, 0.5142, 0.8477, 0.7511, 0.6085, 0.6417, 0.6349, 0.5921, 0.9024, 0.1734, 0.3315, 0.9148, 0.2984,
      0.0592, 0.3017, 0.8261, 0.6726, 0.7391, 0.3419, 0.3201, 0.6232, 0.1063, 0.2776, 0.4875, 0.8716, 0.4922, 0.4168,
      0.9042, 0.3934, 0.1802, 0.5549, 0.6252, 0.0608, 0.4315, 0.8589, 0.0786, 0.2501, 0.5051, 0.8886, 0.5517, 0.2929,
      0.7101, 0.2107, 0.8944, 0.8768, 0.0871, 0.5534, 0.5254, 0.5004, 0.9218, 0.2087, 0.8199, 0.8685, 0.9313, 0.4431,
      0.4837, 0.6797, 0.6221, 0.0044, 0.5711, 0.5997, 0.0221, 0.4212, 0.3942, 0.3433, 0.7092, 0.6855, 0.6872, 0.6918,
      0.8244, 0.0833, 0.0396, 0.9643, 0.5443, 0.3836, 0.7923, 0.9274, 0.3068, 0.3448, 0.3037, 0.3839, 0.9425, 0.5025,
      0.7341, 0.7071, 0.7421, 0.5065, 0.2737, 0.4062, 0.4087, 0.8532, 0.0422, 0.3997, 0.7884, 0.7164, 0.7266, 0.0515,
      0.2518, 0.7296, 0.3953, 0.1802, 0.6052, 0.9827, 0.8149, 0.1921, 0.4538, 0.2133, 0.0431, 0.2061, 0.0076, 0.1505,
      0.0182, 0.5426, 0.4236, 0.2362, 0.3676, 0.2273, 0.9142, 0.5008, 0.6784, 0.2271, 0.0132
  });
  private static final DoubleArray SORTED_123 = DoubleArray.copyOf(new double[]{
      0.0044, 0.0076, 0.0132, 0.0182, 0.0221, 0.0396, 0.0422, 0.0431, 0.0515, 0.0538, 0.0592, 0.0608, 0.0786, 0.0833,
      0.0871, 0.1063, 0.1505, 0.1734, 0.1802, 0.1802, 0.1921, 0.2061, 0.2087, 0.2107, 0.2133, 0.2271, 0.2273, 0.2362,
      0.2501, 0.2518, 0.2737, 0.2776, 0.2929, 0.2984, 0.3017, 0.3037, 0.3068, 0.3201, 0.3315, 0.3419, 0.3433, 0.3448,
      0.3676, 0.3836, 0.3839, 0.3934, 0.3942, 0.3953, 0.3997, 0.4062, 0.4087, 0.4168, 0.4212, 0.4236, 0.4315, 0.4431,
      0.4538, 0.4837, 0.4875, 0.4922, 0.5004, 0.5008, 0.5025, 0.5051, 0.5065, 0.5142, 0.5254, 0.5426, 0.5443, 0.5517,
      0.5534, 0.5549, 0.5711, 0.5921, 0.5997, 0.6052, 0.6085, 0.6221, 0.6232, 0.6252, 0.6349, 0.6417, 0.6726, 0.6766,
      0.6784, 0.6797, 0.6855, 0.6872, 0.6918, 0.7071, 0.7092, 0.7101, 0.7164, 0.7266, 0.7296, 0.7341, 0.7391, 0.7421,
      0.7511, 0.7884, 0.7923, 0.8149, 0.8199, 0.8244, 0.8261, 0.8477, 0.8532, 0.8589, 0.8685, 0.8716, 0.8768, 0.8886,
      0.8944, 0.9024, 0.9042, 0.9142, 0.9148, 0.9218, 0.9274, 0.9313, 0.9425, 0.9643, 0.9827
  });
  private static final int SAMPLE_SIZE_123 = UNSORTED_123.size();

  private static final double TOL = 1.0E-10;
  private static final double LEVEL1 = 0.935;
  private static final double LEVEL2 = 0.764;
  private static final double LEVEL3 = 0.95;
  private static final double LEVEL4 = 0.0001;
  private static final double LEVEL5 = 0.9999;

  //-------------------------------------------------------------------------
  public void discrete_wrong_quantile_large() {
    assertThrowsIllegalArg(() -> QUANTILE_INDEX_ABOVE.quantileFromUnsorted(1.01, UNSORTED_100));
  }

  public void discrete_wrong_quantile_0() {
    assertThrowsIllegalArg(() -> QUANTILE_INDEX_ABOVE.quantileFromUnsorted(0.0, UNSORTED_100));
  }

  public void interpolation_wrong_quantile_1() {
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(1.01, UNSORTED_100));
  }

  public void interpolation_wrong_quantile_0() {
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(0.0, UNSORTED_100));
  }

  public void interpolation_wrong_quantile_small() {
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL4, UNSORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL4, UNSORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_MIDWAY_INTERPOLATION.quantileFromUnsorted(LEVEL4, UNSORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL4, UNSORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL4, UNSORTED_100));
  }

  public void interpolation_wrong_quantile_large() {
    assertThrowsIllegalArg(() -> QUANTILE_MIDWAY_INTERPOLATION.quantileFromUnsorted(LEVEL5, UNSORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL5, UNSORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL5, UNSORTED_100));
  }

  //-------------------------------------------------------------------------
  public void discrete_wrong_expectedShortfall_large() {
    assertThrowsIllegalArg(() -> QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(1.01, UNSORTED_100));
  }

  public void discrete_wrong_expectedShortfall_0() {
    assertThrowsIllegalArg(() -> QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(0.0, UNSORTED_100));
  }

  public void interpolation_wrong_expectedShortfall_1() {
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE_INTERPOLATION.expectedShortfallFromUnsorted(1.01, UNSORTED_100));
  }

  public void interpolation_wrong_expectedShortfall_0() {
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE_INTERPOLATION.expectedShortfallFromUnsorted(0.0, UNSORTED_100));
  }

  //-------------------------------------------------------------------------
  public void index_above_095_100() {
    double indexDouble = LEVEL3 * SAMPLE_SIZE_100;
    int indexCeil = (int) Math.ceil(indexDouble);
    double quantileExpected = SORTED_100.get(indexCeil - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_INDEX_ABOVE.quantileFromUnsorted(LEVEL3, UNSORTED_100);
    assertEquals(quantileComputed, quantileExpected, TOL);
    double quantileExtrapComputed = QUANTILE_INDEX_ABOVE.quantileWithExtrapolationFromUnsorted(
        LEVEL3,
        UNSORTED_100);
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  public void index_above_095_123() {
    double indexDouble = LEVEL3 * SAMPLE_SIZE_123;
    int indexCeil = (int) Math.ceil(indexDouble);
    double quantileExpected = SORTED_123.get(indexCeil - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_INDEX_ABOVE.quantileFromUnsorted(LEVEL3, UNSORTED_123);
    assertEquals(quantileComputed, quantileExpected);
    double quantileExtrapComputed = QUANTILE_INDEX_ABOVE.quantileWithExtrapolationFromUnsorted(
        LEVEL3,
        UNSORTED_123);
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  /* On sample points, different methods match. */
  public void index_nearest_095_100() {
    double quantileExpected = QUANTILE_INDEX_ABOVE.quantileFromUnsorted(LEVEL3, UNSORTED_100);
    double quantileComputed = QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL3, UNSORTED_100);
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void index_nearest_0951_100() {
    double indexDouble = (LEVEL3 + 0.001) * SAMPLE_SIZE_100;
    int indexRound = (int) Math.round(indexDouble);
    double quantileExpected = SORTED_100.get(indexRound - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL3, UNSORTED_100);
    assertEquals(quantileComputed, quantileExpected, TOL);
    double quantileExtrapComputed = QUANTILE_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(
        LEVEL3,
        UNSORTED_100);
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  public void index_nearest_0001_100() {
    double quantileExpected = SORTED_100.get(0); // Java index start at 0.
    double quantileComputed = QUANTILE_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(
        LEVEL4,
        UNSORTED_100);
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void index_nearest_9999_100() {
    double quantileExpected = SORTED_100.get(SAMPLE_SIZE_100 - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(
        LEVEL5,
        UNSORTED_100);
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void index_nearest_one_0951_100() {
    double indexDouble = (LEVEL3 + 0.001) * (SAMPLE_SIZE_100 + 1d);
    int indexRound = (int) Math.round(indexDouble);
    double quantileExpected = SORTED_100.get(indexRound - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL3, UNSORTED_100);
    assertEquals(quantileComputed, quantileExpected, TOL);
    double quantileExtrapComputed = QUANTILE_SAMPLE1_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(
        LEVEL3,
        UNSORTED_100);
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  public void index_nearest_one_0001_100() {
    double quantileExpected = SORTED_100.get(0); // Java index start at 0.
    double quantileComputed = QUANTILE_SAMPLE1_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(
        LEVEL4,
        UNSORTED_100);
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void index_nearest_one_9999_100() {
    double quantileExpected = SORTED_100.get(SAMPLE_SIZE_100 - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_SAMPLE1_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(
        LEVEL5,
        UNSORTED_100);
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  /* On sample points, different methods match. */
  @Test
  public void interpolation_sample_095_100() {
    double quantileExpected = QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL3, UNSORTED_100);
    double quantileComputed = QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL3, UNSORTED_100);
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void interpolation_sample_0001_100() {
    double quantileExpected = SORTED_100.get(0); // Java index start at 0.
    double quantileComputed = QUANTILE_SAMPLE_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL4,
        UNSORTED_100);
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
    double quantileComputed = QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL3, UNSORTED_123);
    assertEquals(quantileComputed, quantileExpected, TOL);
    double quantileExtrapComputed = QUANTILE_SAMPLE_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL3,
        UNSORTED_123);
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
    double quantileComputed = QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL3, UNSORTED_123);
    assertEquals(quantileComputed, quantileExpected, TOL);
    double quantileExtrapComputed = QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL3,
        UNSORTED_123);
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  public void interpolation_samplePlusOne_0001_100() {
    double quantileExpected = SORTED_100.get(0); // Java index start at 0.
    double quantileComputed = QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL4,
        UNSORTED_100);
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void interpolation_samplePlusOne_9999_100() {
    double quantileExpected = SORTED_100.get(SAMPLE_SIZE_100 - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL5,
        UNSORTED_100);
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
    double quantileComputed = QUANTILE_MIDWAY_INTERPOLATION.quantileFromUnsorted(LEVEL3, UNSORTED_123);
    assertEquals(quantileComputed, quantileExpected, TOL);
    double quantileExtrapComputed = QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL3,
        UNSORTED_123);
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  public void interpolation_midway_0001_100() {
    double quantileExpected = SORTED_100.get(0); // Java index start at 0.
    double quantileComputed = QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL4,
        UNSORTED_100);
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void interpolation_midway_9999_100() {
    double quantileExpected = SORTED_100.get(SAMPLE_SIZE_100 - 1); // Java index start at 0.
    double quantileComputed = QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(
        LEVEL5,
        UNSORTED_100);
    assertEquals(quantileComputed, quantileExpected, TOL);
  }

  public void excel() {
    DoubleArray data = DoubleArray.of(1.0, 3.0, 2.0, 4.0);
    double level = 0.3;
    double quantileComputed = ExcelInterpolationQuantileMethod.DEFAULT.quantileFromUnsorted(level, data);
    double quantileExpected = 1.9; // From Excel doc
    assertEquals(quantileComputed, quantileExpected, TOL);
    double quantileExtrapComputed = ExcelInterpolationQuantileMethod.DEFAULT
        .quantileWithExtrapolationFromUnsorted(level, data);
    assertEquals(quantileExtrapComputed, quantileComputed);
  }

  //-------------------------------------------------------------------------
  public void index_above_095_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_INDEX_ABOVE.quantileWithExtrapolationFromUnsorted(level, UNSORTED_100);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(
        LEVEL3,
        UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }


  public void index_above_095_123_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_INDEX_ABOVE.quantileWithExtrapolationFromUnsorted(level, UNSORTED_123);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(
        LEVEL3,
        UNSORTED_123);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void index_above_0001_100_expected_shortfall() {
    double expectedShortfallComputed = QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(
        LEVEL4,
        UNSORTED_100);
    assertEquals(expectedShortfallComputed, SORTED_100.get(0), TOL_INTEGRAL);
  }

  public void index_above_9999_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_INDEX_ABOVE.quantileWithExtrapolationFromUnsorted(level, UNSORTED_100);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL5) / LEVEL5;
    double expectedShortfallComputed = QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(
        LEVEL5,
        UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void index_nearest_095_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(level, UNSORTED_100);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_NEAREST_INDEX.expectedShortfallFromUnsorted(
        LEVEL3,
        UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void index_nearest_095_123_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(level, UNSORTED_123);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_NEAREST_INDEX.expectedShortfallFromUnsorted(
        LEVEL3,
        UNSORTED_123);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void index_nearest_0001_100_expected_shortfall() {
    double expectedShortfallComputed =
        QUANTILE_NEAREST_INDEX.expectedShortfallFromUnsorted(LEVEL4, UNSORTED_100);
    assertEquals(expectedShortfallComputed, SORTED_100.get(0), TOL_INTEGRAL);
  }

  public void index_nearest_9999_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(level, UNSORTED_100);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL5) / LEVEL5;
    double expectedShortfallComputed = QUANTILE_NEAREST_INDEX.expectedShortfallFromUnsorted(
        LEVEL5,
        UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void index_nearest_one_095_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_SAMPLE1_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(level, UNSORTED_100);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_SAMPLE1_NEAREST_INDEX.expectedShortfallFromUnsorted(
        LEVEL3,
        UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void index_nearest_one_0001_100_expected_shortfall() {
    double expectedShortfallExpected = SORTED_100.get(0);
    double expectedShortfallComputed =
        QUANTILE_SAMPLE1_NEAREST_INDEX.expectedShortfallFromUnsorted(LEVEL4, UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL);
  }

  public void index_nearest_one_9999_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_SAMPLE1_NEAREST_INDEX.quantileWithExtrapolationFromUnsorted(level, UNSORTED_100);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL5) / LEVEL5;
    double expectedShortfallComputed =
        QUANTILE_SAMPLE1_NEAREST_INDEX.expectedShortfallFromUnsorted(LEVEL5, UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_sample_095_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_SAMPLE_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, UNSORTED_100);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_SAMPLE_INTERPOLATION.expectedShortfallFromUnsorted(
        LEVEL3,
        UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_sample_095_123_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_SAMPLE_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, UNSORTED_123);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_SAMPLE_INTERPOLATION.expectedShortfallFromUnsorted(
        LEVEL3,
        UNSORTED_123);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_sample_0001_100_expected_shortfall() {
    double expectedShortfallExpected = SORTED_100.get(0);
    double expectedShortfallComputed =
        QUANTILE_SAMPLE_INTERPOLATION.expectedShortfallFromUnsorted(LEVEL4, UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL);
  }

  public void interpolation_sample_9999_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_SAMPLE_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, UNSORTED_100);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL5) / LEVEL5;
    double expectedShortfallComputed =
        QUANTILE_SAMPLE_INTERPOLATION.expectedShortfallFromUnsorted(LEVEL5, UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_samplePlusOne_095_123_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, UNSORTED_123);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_SAMPLE1_INTERPOLATION.expectedShortfallFromUnsorted(
        LEVEL3,
        UNSORTED_123);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_samplePlusOne_0001_100_expected_shortfall() {
    double expectedShortfallExpected = SORTED_100.get(0);
    double expectedShortfallComputed =
        QUANTILE_SAMPLE1_INTERPOLATION.expectedShortfallFromUnsorted(LEVEL4, UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL);
  }

  public void interpolation_samplePlusOne_9999_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, UNSORTED_100);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL5) / LEVEL5;
    double expectedShortfallComputed =
        QUANTILE_SAMPLE1_INTERPOLATION.expectedShortfallFromUnsorted(LEVEL5, UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_midway_095_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, UNSORTED_100);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_MIDWAY_INTERPOLATION.expectedShortfallFromUnsorted(
        LEVEL3,
        UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_midway_095_123_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, UNSORTED_123);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL3) / LEVEL3;
    double expectedShortfallComputed = QUANTILE_MIDWAY_INTERPOLATION.expectedShortfallFromUnsorted(
        LEVEL3,
        UNSORTED_123);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void interpolation_midway_0001_100_expected_shortfall() {
    double expectedShortfallExpected = SORTED_100.get(0);
    double expectedShortfallComputed =
        QUANTILE_MIDWAY_INTERPOLATION.expectedShortfallFromUnsorted(LEVEL4, UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL);
  }

  public void interpolation_midway_9999_100_expected_shortfall() {
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(level, UNSORTED_100);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL, LEVEL5) / LEVEL5;
    double expectedShortfallComputed =
        QUANTILE_MIDWAY_INTERPOLATION.expectedShortfallFromUnsorted(LEVEL5, UNSORTED_100);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void excel_expected_shortfall() {
    DoubleArray data = DoubleArray.of(1.0, 3.0, 2.0, 4.0);
    double level = 0.3;
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return ExcelInterpolationQuantileMethod.DEFAULT.quantileWithExtrapolationFromUnsorted(level, data);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL / 1000d, level) / level;
    double expectedShortfallComputed =
        ExcelInterpolationQuantileMethod.DEFAULT.expectedShortfallFromUnsorted(level, data);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  public void excel_expected_shortfall_0001() {
    DoubleArray data = DoubleArray.of(1.0, 3.0, 2.0, 4.0);
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double level) {
        return ExcelInterpolationQuantileMethod.DEFAULT.quantileWithExtrapolationFromUnsorted(level, data);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL / 1000d, LEVEL4) / LEVEL4;
    double expectedShortfallComputed =
        ExcelInterpolationQuantileMethod.DEFAULT.expectedShortfallFromUnsorted(LEVEL4, data);
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
        return ExcelInterpolationQuantileMethod.DEFAULT.quantileWithExtrapolationFromUnsorted(level, data);
      }
    };
    double expectedShortfallExpected = INTEG.integrate(func, TOL_INTEGRAL / 1000d, LEVEL5) / LEVEL5;
    double expectedShortfallComputed =
        ExcelInterpolationQuantileMethod.DEFAULT.expectedShortfallFromUnsorted(LEVEL5, data);
    assertEquals(expectedShortfallComputed, expectedShortfallExpected, TOL_INTEGRAL);
  }

  //-------------------------------------------------------------------------
  public void regression_test1() {
    assertEquals(QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL1, UNSORTED_100), 0.94105, TOL);
    assertEquals(QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL1, UNSORTED_100), 0.9434, TOL);
    assertEquals(
        QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(LEVEL1, UNSORTED_100),
        0.478780748663101,
        TOL);
    assertEquals(QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL2, UNSORTED_100), 0.77722, TOL);
    assertEquals(QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL2, UNSORTED_100), 0.7711, TOL);
    assertEquals(
        QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(LEVEL2, UNSORTED_100),
        0.388652617801047,
        TOL);
    assertEquals(QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL3, UNSORTED_100), 0.9532, TOL);
    assertEquals(QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL3, UNSORTED_100), 0.9532, TOL);
    assertEquals(
        QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(LEVEL3, UNSORTED_100),
        0.48622,
        TOL);
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL4, UNSORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL4, UNSORTED_100));
    assertEquals(QUANTILE_SAMPLE_INTERPOLATION.quantileFromUnsorted(LEVEL5, UNSORTED_100), 0.994769, TOL);
    assertEquals(QUANTILE_NEAREST_INDEX.quantileFromUnsorted(LEVEL5, UNSORTED_100),  0.9948, TOL);
    assertEquals(
        QUANTILE_INDEX_ABOVE.expectedShortfallFromUnsorted(LEVEL5, UNSORTED_100),
        0.510629582958296,
        TOL);
  }

  public void regression_test2() {
    assertEquals(QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL1, UNSORTED_100), 0.947663, TOL);
    assertEquals(QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL1, UNSORTED_100), 0.9434, TOL);
    assertEquals(QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL2, UNSORTED_100), 0.791238, TOL);
    assertEquals(QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL2, UNSORTED_100), 0.7864, TOL);
    assertEquals(QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL3, UNSORTED_100), 0.953485, TOL);
    assertEquals(QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL3, UNSORTED_100), 0.9535, TOL);
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL4, UNSORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL4, UNSORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_INTERPOLATION.quantileFromUnsorted(LEVEL5, UNSORTED_100));
    assertThrowsIllegalArg(() -> QUANTILE_SAMPLE1_NEAREST_INDEX.quantileFromUnsorted(LEVEL5, UNSORTED_100));
  }

  public void regression_test3() {
    assertEquals(
        QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL1, UNSORTED_100),
        0.9434,
        TOL);
    assertEquals(
        QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL2, UNSORTED_100),
        0.78487,
        TOL);
    assertEquals(
        QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL3, UNSORTED_100),
        0.95335,
        TOL);
    assertEquals(
        QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL4, UNSORTED_100),
        0.0174,
        TOL);
    assertEquals(
        QUANTILE_MIDWAY_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL5, UNSORTED_100),
        0.9948,
        TOL);
  }

  public void regression_test4() {
    assertEquals(
        QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL1, UNSORTED_100),
        0.947663,
        TOL);
    assertEquals(
        QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL2, UNSORTED_100),
        0.791238,
        TOL);
    assertEquals(
        QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL3, UNSORTED_100),
        0.953485,
        TOL);
    assertEquals(
        QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL4, UNSORTED_100),
        0.0174,
        TOL);
    assertEquals(
        QUANTILE_SAMPLE1_INTERPOLATION.quantileWithExtrapolationFromUnsorted(LEVEL5, UNSORTED_100),
        0.9948,
        TOL);
  }

  //-------------------------------------------------------------------------
  public void quantile_weights_indices_test() {
    QuantileResult quantileAbove = QUANTILE_INDEX_ABOVE.quantileResultFromUnsorted(LEVEL2, UNSORTED_100);
    double expectedValueAbove = 0d;
    double sumWeightsAbove = 0d;
    for (int i = 0; i < quantileAbove.getIndices().length; i++) {
      expectedValueAbove += UNSORTED_100.get(quantileAbove.getIndices()[i]) * quantileAbove.getWeights().get(i);
      sumWeightsAbove += quantileAbove.getWeights().get(i);
    }
    assertEquals(quantileAbove.getIndices().length, quantileAbove.getWeights().size());
    assertEquals(sumWeightsAbove, 1d, TOL);
    assertEquals(quantileAbove.getValue(), expectedValueAbove, TOL);

    QuantileResult quantileNearest = QUANTILE_NEAREST_INDEX.quantileResultFromUnsorted(LEVEL2, UNSORTED_100);
    double expectedValueNearest = 0d;
    double sumWeightsNearest = 0d;
    for (int i = 0; i < quantileNearest.getIndices().length; i++) {
      expectedValueNearest += UNSORTED_100.get(quantileNearest.getIndices()[i]) * quantileNearest.getWeights().get(i);
      sumWeightsNearest += quantileNearest.getWeights().get(i);
    }
    assertEquals(quantileNearest.getIndices().length, quantileNearest.getWeights().size());
    assertEquals(sumWeightsNearest, 1d, TOL);
    assertEquals(quantileNearest.getValue(), expectedValueNearest, TOL);

    QuantileResult quantileSample = QUANTILE_SAMPLE1_NEAREST_INDEX.quantileResultFromUnsorted(LEVEL2, UNSORTED_100);
    double expectedValueSample = 0d;
    double sumWeightsSample = 0d;
    for (int i = 0; i < quantileSample.getIndices().length; i++) {
      expectedValueSample += UNSORTED_100.get(quantileSample.getIndices()[i]) * quantileSample.getWeights().get(i);
      sumWeightsSample += quantileSample.getWeights().get(i);
    }
    assertEquals(quantileSample.getIndices().length, quantileSample.getWeights().size());
    assertEquals(sumWeightsSample, 1d, TOL);
    assertEquals(quantileSample.getValue(), expectedValueSample, TOL);

    QuantileResult quantileSampleInterp = QUANTILE_SAMPLE_INTERPOLATION.quantileResultFromUnsorted(LEVEL2, UNSORTED_100);
    double expectedValueSampleInterp = 0d;
    double sumWeightsSampleInterp = 0d;
    for (int i = 0; i < quantileSampleInterp.getIndices().length; i++) {
      expectedValueSampleInterp += UNSORTED_100.get(quantileSampleInterp.getIndices()[i]) * quantileSampleInterp.getWeights().get(
          i);
      sumWeightsSampleInterp += quantileSampleInterp.getWeights().get(i);
    }
    assertEquals(quantileSampleInterp.getIndices().length, quantileSampleInterp.getWeights().size());
    assertEquals(sumWeightsSampleInterp, 1d, TOL);
    assertEquals(quantileSampleInterp.getValue(), expectedValueSampleInterp, TOL);

    QuantileResult quantileSample1Interp = QUANTILE_SAMPLE1_INTERPOLATION.quantileResultFromUnsorted(LEVEL2, UNSORTED_100);
    double expectedValueSample1Interp = 0d;
    double sumWeightsSample1Interp = 0d;
    for (int i = 0; i < quantileSample1Interp.getIndices().length; i++) {
      expectedValueSample1Interp += UNSORTED_100.get(quantileSample1Interp.getIndices()[i]) * quantileSample1Interp.getWeights().get(
          i);
      sumWeightsSample1Interp += quantileSample1Interp.getWeights().get(i);
    }
    assertEquals(quantileSample1Interp.getIndices().length, quantileSample1Interp.getWeights().size());
    assertEquals(sumWeightsSample1Interp, 1d, TOL);
    assertEquals(quantileSample1Interp.getValue(), expectedValueSample1Interp, TOL);

    QuantileResult quantileMidInterp = QUANTILE_MIDWAY_INTERPOLATION.quantileResultFromUnsorted(LEVEL2, UNSORTED_100);
    double expectedValueMidInterp = 0d;
    double sumWeightsMidInterp = 0d;
    for (int i = 0; i < quantileMidInterp.getIndices().length; i++) {
      expectedValueMidInterp += UNSORTED_100.get(quantileMidInterp.getIndices()[i]) * quantileMidInterp.getWeights().get(
          i);
      sumWeightsMidInterp += quantileMidInterp.getWeights().get(i);
    }
    assertEquals(quantileMidInterp.getIndices().length, quantileMidInterp.getWeights().size());
    assertEquals(sumWeightsMidInterp, 1d, TOL);
    assertEquals(quantileMidInterp.getValue(), expectedValueMidInterp, TOL);
  }

  public void es_weights_indices_test() {
    QuantileResult esAbove = QUANTILE_INDEX_ABOVE.expectedShortfallResultFromUnsorted(LEVEL2, UNSORTED_100);
    double expectedValueAbove = 0d;
    double sumWeightsAbove = 0d;
    for (int i = 0; i < esAbove.getIndices().length; i++) {
      expectedValueAbove += UNSORTED_100.get(esAbove.getIndices()[i]) * esAbove.getWeights().get(i);
      sumWeightsAbove += esAbove.getWeights().get(i);
    }
    assertEquals(esAbove.getIndices().length, esAbove.getWeights().size());
    assertEquals(sumWeightsAbove, 1d, TOL);
    assertEquals(esAbove.getValue(), expectedValueAbove, TOL);

    QuantileResult esNearest = QUANTILE_NEAREST_INDEX.expectedShortfallResultFromUnsorted(LEVEL2, UNSORTED_100);
    double expectedValueNearest = 0d;
    double sumWeightsNearest = 0d;
    for (int i = 0; i < esNearest.getIndices().length; i++) {
      expectedValueNearest += UNSORTED_100.get(esNearest.getIndices()[i]) * esNearest.getWeights().get(i);
      sumWeightsNearest += esNearest.getWeights().get(i);
    }
    assertEquals(esNearest.getIndices().length, esNearest.getWeights().size());
    assertEquals(sumWeightsNearest, 1d, TOL);
    assertEquals(esNearest.getValue(), expectedValueNearest, TOL);

    QuantileResult esSample = QUANTILE_SAMPLE1_NEAREST_INDEX.expectedShortfallResultFromUnsorted(LEVEL2, UNSORTED_100);
    double expectedValueSample = 0d;
    double sumWeightsSample = 0d;
    for (int i = 0; i < esSample.getIndices().length; i++) {
      expectedValueSample += UNSORTED_100.get(esSample.getIndices()[i]) * esSample.getWeights().get(i);
      sumWeightsSample += esSample.getWeights().get(i);
    }
    assertEquals(esSample.getIndices().length, esSample.getWeights().size());
    assertEquals(sumWeightsSample, 1d, TOL);
    assertEquals(esSample.getValue(), expectedValueSample, TOL);

    QuantileResult esSampleInterp = QUANTILE_SAMPLE_INTERPOLATION.expectedShortfallResultFromUnsorted(LEVEL2, UNSORTED_100);
    double expectedValueSampleInterp = 0d;
    double sumWeightsSampleInterp = 0d;
    for (int i = 0; i < esSampleInterp.getIndices().length; i++) {
      expectedValueSampleInterp += UNSORTED_100.get(esSampleInterp.getIndices()[i]) * esSampleInterp.getWeights().get(i);
      sumWeightsSampleInterp += esSampleInterp.getWeights().get(i);
    }
    assertEquals(esSampleInterp.getIndices().length, esSampleInterp.getWeights().size());
    assertEquals(sumWeightsSampleInterp, 1d, TOL);
    assertEquals(esSampleInterp.getValue(), expectedValueSampleInterp, TOL);

    QuantileResult esSample1Interp = QUANTILE_SAMPLE1_INTERPOLATION.expectedShortfallResultFromUnsorted(LEVEL2, UNSORTED_100);
    double expectedValueSample1Interp = 0d;
    double sumWeightsSample1Interp = 0d;
    for (int i = 0; i < esSample1Interp.getIndices().length; i++) {
      expectedValueSample1Interp += UNSORTED_100.get(esSample1Interp.getIndices()[i]) * esSample1Interp.getWeights().get(
          i);
      sumWeightsSample1Interp += esSample1Interp.getWeights().get(i);
    }
    assertEquals(esSample1Interp.getIndices().length, esSample1Interp.getWeights().size());
    assertEquals(sumWeightsSample1Interp, 1d, TOL);
    assertEquals(esSample1Interp.getValue(), expectedValueSample1Interp, TOL);

    QuantileResult esMidInterp = QUANTILE_MIDWAY_INTERPOLATION.expectedShortfallResultFromUnsorted(LEVEL2, UNSORTED_100);
    double expectedValueMidInterp = 0d;
    double sumWeightsMidInterp = 0d;
    for (int i = 0; i < esMidInterp.getIndices().length; i++) {
      expectedValueMidInterp += UNSORTED_100.get(esMidInterp.getIndices()[i]) * esMidInterp.getWeights().get(i);
      sumWeightsMidInterp += esMidInterp.getWeights().get(i);
    }
    assertEquals(esMidInterp.getIndices().length, esMidInterp.getWeights().size());
    assertEquals(sumWeightsMidInterp, 1d, TOL);
    assertEquals(esMidInterp.getValue(), expectedValueMidInterp, TOL);
  }

}
