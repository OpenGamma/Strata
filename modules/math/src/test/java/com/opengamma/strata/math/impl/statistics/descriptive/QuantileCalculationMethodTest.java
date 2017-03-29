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

  private static final DoubleArray SORTED_100 = DoubleArray.copyOf(new double[]{
      0.0286, 0.0363, 0.0379, 0.0582, 0.0611, 0.0622, 0.0776, 0.0779, 0.0849, 0.0916, 0.1055, 0.1358, 0.1474, 0.1544,
      0.1674, 0.1740, 0.1746, 0.1841, 0.1963, 0.1982, 0.2020, 0.2222, 0.2401, 0.2582, 0.2666, 0.2979, 0.2998, 0.3000,
      0.3028, 0.3057, 0.3508, 0.3734, 0.3781, 0.4011, 0.4197, 0.4463, 0.4481, 0.4863, 0.4878, 0.4908, 0.4942, 0.5029,
      0.5212, 0.5224, 0.5290, 0.5780, 0.5803, 0.5807, 0.5921, 0.6174, 0.6243, 0.6278, 0.6325, 0.6343, 0.6416, 0.6423,
      0.6460, 0.6504, 0.6570, 0.6666, 0.6748, 0.6763, 0.6804, 0.6859, 0.6862, 0.7136, 0.7145, 0.7289, 0.7291, 0.7360,
      0.7444, 0.7532, 0.7543, 0.7602, 0.7714, 0.8024, 0.8053, 0.8075, 0.8190, 0.8190, 0.8216, 0.8234, 0.8399, 0.8483,
      0.8511, 0.8557, 0.8631, 0.8814, 0.8870, 0.8963, 0.9150, 0.9157, 0.9198, 0.9275, 0.9524, 0.9570, 0.9620, 0.9716,
      0.9731, 0.9813});
  private static final int SAMPLE_SIZE_100 = SORTED_100.size();
  private static final DoubleArray SORTED_123 = DoubleArray.copyOf(new double[]{
      0.0286, 0.0363, 0.0379, 0.0582, 0.0611, 0.0622, 0.0776, 0.0779, 0.0822, 0.0849, 0.0916, 0.1055, 0.1358, 0.1474,
      0.1544, 0.1674, 0.1740, 0.1746, 0.1841, 0.1963, 0.1982, 0.2020, 0.2155, 0.2222, 0.2401, 0.2413, 0.2582, 0.2666,
      0.2936, 0.2979, 0.2998, 0.3000, 0.3028, 0.3057, 0.3076, 0.3461, 0.3508, 0.3717, 0.3734, 0.3781, 0.4011, 0.4157,
      0.4197, 0.4285, 0.4463, 0.4481, 0.4534, 0.4690, 0.4863, 0.4878, 0.4908, 0.4942, 0.5029, 0.5083, 0.5108, 0.5212,
      0.5224, 0.5290, 0.5578, 0.5780, 0.5803, 0.5807, 0.5921, 0.6174, 0.6243, 0.6278, 0.6325, 0.6343, 0.6416, 0.6423,
      0.6460, 0.6495, 0.6504, 0.6570, 0.6666, 0.6694, 0.6748, 0.6763, 0.6793, 0.6804, 0.6859, 0.6862, 0.7136, 0.7145,
      0.7289, 0.7291, 0.7360, 0.7444, 0.7532, 0.7543, 0.7602, 0.7714, 0.7768, 0.8024, 0.8053, 0.8075, 0.8190, 0.8190,
      0.8216, 0.8234, 0.8399, 0.8421, 0.8483, 0.8511, 0.8557, 0.8631, 0.8814, 0.8870, 0.8914, 0.8963, 0.9077, 0.9150,
      0.9157, 0.9198, 0.9275, 0.9368, 0.9524, 0.9570, 0.9600, 0.9620, 0.9716, 0.9731, 0.9813});
  private static final int SAMPLE_SIZE_123 = SORTED_123.size();
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

  private static final RungeKuttaIntegrator1D INTEG = new RungeKuttaIntegrator1D();
  private static final double TOL_INTEGRAL = 1.0e-8;

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
