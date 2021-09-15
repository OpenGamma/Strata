/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * Test {@link HybridNodalCurve}.
 */
public class HybridNodalCurveTest {

  private static final LocalDate VALUATION_DATE = LocalDate.of(2020, 1, 6);
  private static final String TNR_1Y = "1Y";

  private static final DayCount DC_DF = DayCounts.ACT_365F;

  private static final CurveName CURVE_NAME = CurveName.of("DiscountCurve");

  private static final DoubleArray ALL_XVALUES =
      DoubleArray.of(0.0, 0.08, 0.25, 0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0);
  private static final DoubleArray ALL_YVALUES =
      DoubleArray.of(1, 0.998, 0.994, 0.9875, 0.975, 0.95, 0.876, 0.755, 0.538, 0.365);
  private static final DoubleArray ALL_YVALUES_BUMPED =
      DoubleArray.of(0.990, 0.988, 0.984, 0.9775, 0.965, 0.940, 0.866, 0.745, 0.528, 0.355);

  private static final DefaultCurveMetadata META_TEMPLATE =
      DefaultCurveMetadata.builder()
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.DISCOUNT_FACTOR)
          .dayCount(DC_DF)
          .curveName(CURVE_NAME)
          .build();

  private static final DefaultCurveMetadata META_TEMPLATE_ZERO =
      DefaultCurveMetadata.builder()
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(DayCounts.ACT_360)
          .curveName("ZeroCurve")
          .build();

  private static final int SPLICE_INDEX = 4;
  private static final CurveMetadata CURVE_METADATA = META_TEMPLATE.toBuilder().build();
  private static final int METADATA_SIZE = ALL_XVALUES.size();
  private static final CurveMetadata CURVE_METADATA_ENTRIES =
      Curves.zeroRates(CURVE_METADATA.getCurveName(), ACT_365F, ParameterMetadata.listOfEmpty(METADATA_SIZE));
  private static final CurveMetadata METADATA_NOPARAM = Curves.zeroRates(CURVE_METADATA.getCurveName(), ACT_365F);

  // Note that splice index is inclusive for both left and right curves
  private static final DoubleArray LEFT_CURVE_XVALUES = ALL_XVALUES.subArray(0, SPLICE_INDEX + 1);
  private static final DoubleArray LEFT_CURVE_YVALUES = ALL_YVALUES.subArray(0, SPLICE_INDEX + 1);
  private static final CurveInterpolator LEFT_CURVE_INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveExtrapolator LEFT_CURVE_EXTRAPOLATOR = CurveExtrapolators.FLAT;

  private static final DoubleArray RIGHT_CURVE_XVALUES = ALL_XVALUES.subArray(SPLICE_INDEX);
  private static final DoubleArray RIGHT_CURVE_YVALUES = ALL_YVALUES.subArray(SPLICE_INDEX);
  private static final CurveInterpolator RIGHT_CURVE_INTERPOLATOR =
      CurveInterpolators.LOG_NATURAL_SPLINE_MONOTONE_CUBIC;
  private static final CurveExtrapolator RIGHT_CURVE_EXTRAPOLATOR = CurveExtrapolators.FLAT;

  private static final Curve LEFT_NODAL_CURVE = InterpolatedNodalCurve.of(
      CURVE_METADATA,
      LEFT_CURVE_XVALUES,
      LEFT_CURVE_YVALUES,
      LEFT_CURVE_INTERPOLATOR,
      LEFT_CURVE_EXTRAPOLATOR,
      LEFT_CURVE_EXTRAPOLATOR);

  private static final Curve RIGHT_NODAL_CURVE = InterpolatedNodalCurve.of(
      CURVE_METADATA,
      RIGHT_CURVE_XVALUES,
      RIGHT_CURVE_YVALUES,
      RIGHT_CURVE_INTERPOLATOR,
      RIGHT_CURVE_EXTRAPOLATOR,
      RIGHT_CURVE_EXTRAPOLATOR);

  private static final HybridNodalCurve HYBRID_NODAL_CURVE = HybridNodalCurve.of(
      CURVE_METADATA,
      ALL_XVALUES,
      ALL_YVALUES,
      SPLICE_INDEX,
      LEFT_CURVE_INTERPOLATOR,
      RIGHT_CURVE_INTERPOLATOR,
      LEFT_CURVE_EXTRAPOLATOR,
      RIGHT_CURVE_EXTRAPOLATOR);

  private static final double CURVE_TOLERANCE = 1e-10;

  private static final double TIME_SHIFT = 0.12345;
  private static final double TIME_AT_SPLICE = ALL_XVALUES.get(SPLICE_INDEX);
  private static final double TIME_BEFORE_SPLICE = TIME_AT_SPLICE - TIME_SHIFT;
  private static final double TIME_AFTER_SPLICE = TIME_AT_SPLICE + TIME_SHIFT;

  //-------------------------------------------------------------------------
  @Test
  // check for various bad inputs during construction
  public void test_of_curve() {
    assertThat(LEFT_NODAL_CURVE).isEqualTo(HYBRID_NODAL_CURVE.getLeftCurve());
  }

  //-------------------------------------------------------------------------
  @Test
  // check for various bad inputs during construction
  public void test_of_invalid() {
    // not enough points for monotonic spline
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HybridNodalCurve.of(
            CURVE_METADATA,
            DoubleArray.of(0d, 1d, 2d),
            DoubleArray.of(1d, 1d, 1d),
            1,
            LEFT_CURVE_INTERPOLATOR,
            RIGHT_CURVE_INTERPOLATOR,
            LEFT_CURVE_EXTRAPOLATOR,
            RIGHT_CURVE_EXTRAPOLATOR));

    // spline index too high
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HybridNodalCurve.of(
            CURVE_METADATA,
            DoubleArray.of(0d, 1d, 2d),
            DoubleArray.of(1d, 1d, 1d),
            3,
            CurveInterpolators.LINEAR,
            CurveInterpolators.LINEAR,
            CurveExtrapolators.FLAT,
            CurveExtrapolators.FLAT));

    // spline index negative
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HybridNodalCurve.of(
            CURVE_METADATA,
            DoubleArray.of(0d, 1d, 2d),
            DoubleArray.of(1d, 1d, 1d),
            -1,
            CurveInterpolators.LINEAR,
            CurveInterpolators.LINEAR,
            CurveExtrapolators.FLAT,
            CurveExtrapolators.FLAT));

    // mismatch in x and y array sizes
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HybridNodalCurve.of(
            CURVE_METADATA,
            DoubleArray.of(0d, 1d, 2d),
            DoubleArray.of(1d, 1d, 1d, 1d),
            1,
            CurveInterpolators.LINEAR,
            CurveInterpolators.LINEAR,
            CurveExtrapolators.FLAT,
            CurveExtrapolators.FLAT));

    // mismatch in parameter metadata size and node size
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HybridNodalCurve.of(
            CURVE_METADATA_ENTRIES,
            DoubleArray.of(0d, 1d, 2d, 3d),
            DoubleArray.of(1d, 1d, 1d, 1d),
            2,
            CurveInterpolators.LINEAR,
            CurveInterpolators.LINEAR,
            CurveExtrapolators.FLAT,
            CurveExtrapolators.FLAT));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withValues() {
    HybridNodalCurve base = HybridNodalCurve.of(
        CURVE_METADATA_ENTRIES,
        ALL_XVALUES,
        ALL_YVALUES,
        SPLICE_INDEX,
        LEFT_CURVE_INTERPOLATOR,
        RIGHT_CURVE_INTERPOLATOR,
        LEFT_CURVE_EXTRAPOLATOR,
        RIGHT_CURVE_EXTRAPOLATOR);

    HybridNodalCurve test = base.withValues(ALL_XVALUES, ALL_YVALUES);

    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(METADATA_SIZE);
    assertThat(test.getMetadata()).isEqualTo(CURVE_METADATA_ENTRIES);
    assertThat(test.getXValues()).isEqualTo(ALL_XVALUES);
    assertThat(test.getYValues()).isEqualTo(ALL_YVALUES);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.withValues(DoubleArray.of(0d, 1d, 2d, 3d), DoubleArray.of(1d, 1d, 1d, 1d)));
  }

  @Test
  // check that the hybrid curve is spliced correctly
  public void test_splice() {
    assertThat(SPLICE_INDEX).isEqualTo(HYBRID_NODAL_CURVE.getLeftCurve().getXValues().size() - 1);
  }

  @Test
  // check that the hybrid curve correctly reproduces the nodes times and value of the original data
  public void test_splice_times_and_values() {
    assertThat(HYBRID_NODAL_CURVE.getXValues()).isEqualTo(ALL_XVALUES);
    assertThat(HYBRID_NODAL_CURVE.getYValues()).isEqualTo(ALL_YVALUES);
  }

  @Test
  // Check that the left and right curves are the equal to the each of the corresponding InterpolatedNodalCurves 
  // created manually
  public void test_left_right_consistency() {
    assertThat(LEFT_NODAL_CURVE).isEqualTo(HYBRID_NODAL_CURVE.getLeftCurve());
    assertThat(RIGHT_NODAL_CURVE).isEqualTo(HYBRID_NODAL_CURVE.getRightCurve());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_y_sensitivity() {
    DoubleArray fourZeros = DoubleArray.filled(4);
    DoubleArray fiveZeros = DoubleArray.filled(5);
    {
      DoubleArray leftCurveSensitivity =
          LEFT_NODAL_CURVE.yValueParameterSensitivity(TIME_BEFORE_SPLICE).getSensitivity();
      DoubleArray hybridCurveSensitivity =
          HYBRID_NODAL_CURVE.yValueParameterSensitivity(TIME_BEFORE_SPLICE).getSensitivity();
      assertThat(hybridCurveSensitivity.size()).isEqualTo(HYBRID_NODAL_CURVE.getParameterCount());
      assertThat(hybridCurveSensitivity).isEqualTo(leftCurveSensitivity.concat(fiveZeros));
    }
    {
      DoubleArray leftCurveSensitivity = LEFT_NODAL_CURVE.yValueParameterSensitivity(TIME_AT_SPLICE).getSensitivity();
      DoubleArray rightCurveSensitivity = RIGHT_NODAL_CURVE.yValueParameterSensitivity(TIME_AT_SPLICE).getSensitivity();
      DoubleArray hybridCurveSensitivity =
          HYBRID_NODAL_CURVE.yValueParameterSensitivity(TIME_AT_SPLICE).getSensitivity();
      assertThat(hybridCurveSensitivity.size()).isEqualTo(HYBRID_NODAL_CURVE.getParameterCount());
      assertThat(hybridCurveSensitivity).isEqualTo(leftCurveSensitivity.concat(fiveZeros));
      assertThat(hybridCurveSensitivity).isEqualTo(fourZeros.concat(rightCurveSensitivity));
    }
    {
      DoubleArray rightCurveSensitivity =
          RIGHT_NODAL_CURVE.yValueParameterSensitivity(TIME_AFTER_SPLICE).getSensitivity();
      DoubleArray hybridCurveSensitivity =
          HYBRID_NODAL_CURVE.yValueParameterSensitivity(TIME_AFTER_SPLICE).getSensitivity();
      assertThat(hybridCurveSensitivity.size()).isEqualTo(HYBRID_NODAL_CURVE.getParameterCount());
      assertThat(hybridCurveSensitivity).isEqualTo(fourZeros.concat(rightCurveSensitivity));
    }
  }

  @Test
  public void test_createParameterSensitivity() {
    assertThat(HYBRID_NODAL_CURVE.createParameterSensitivity(ALL_YVALUES)).isEqualTo(
        UnitParameterSensitivity.of(
            CURVE_NAME,
            HYBRID_NODAL_CURVE.getMetadata().getParameterMetadata().get(),
            ALL_YVALUES));
    assertThat(HYBRID_NODAL_CURVE.createParameterSensitivity(USD, ALL_YVALUES)).isEqualTo(
        CurrencyParameterSensitivity.of(
            CURVE_NAME,
            HYBRID_NODAL_CURVE.getMetadata().getParameterMetadata().get(),
            Currency.USD,
            ALL_YVALUES));
  }

  @Test
  public void test_withPerturbation() {
    //???
    assertThat(HYBRID_NODAL_CURVE.withPerturbation((i, v, m) -> v - 0.01)).isEqualTo(
        HybridNodalCurve.of(
            CURVE_METADATA,
            ALL_XVALUES,
            ALL_YVALUES_BUMPED,
            SPLICE_INDEX,
            LEFT_CURVE_INTERPOLATOR,
            RIGHT_CURVE_INTERPOLATOR,
            LEFT_CURVE_EXTRAPOLATOR,
            RIGHT_CURVE_EXTRAPOLATOR));
  }

  @Test
  public void test_firstDerivative() {
    assertThat(HYBRID_NODAL_CURVE.firstDerivative(TIME_BEFORE_SPLICE)).isCloseTo(LEFT_NODAL_CURVE.firstDerivative(
        TIME_BEFORE_SPLICE), Percentage.withPercentage(CURVE_TOLERANCE));
    assertThat(HYBRID_NODAL_CURVE.firstDerivative(TIME_AFTER_SPLICE)).isCloseTo(RIGHT_NODAL_CURVE.firstDerivative(
        TIME_AFTER_SPLICE), Percentage.withPercentage(CURVE_TOLERANCE));

    double leftDelta = LEFT_NODAL_CURVE.firstDerivative(TIME_AT_SPLICE);
    double rightDelta = RIGHT_NODAL_CURVE.firstDerivative(TIME_AT_SPLICE);
    double hybridDelta = HYBRID_NODAL_CURVE.firstDerivative(TIME_AT_SPLICE);
    assertThat(hybridDelta).isCloseTo(0.5 * (leftDelta + rightDelta), Percentage.withPercentage(CURVE_TOLERANCE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withNode() {
    HybridNodalCurve base = HybridNodalCurve.of(
        METADATA_NOPARAM,
        DoubleArray.of(0d, 1d, 2d, 3d),
        DoubleArray.of(1d, 1d, 1d, 1d),
        1,
        CurveInterpolators.LINEAR,
        CurveInterpolators.LINEAR,
        CurveExtrapolators.FLAT,
        CurveExtrapolators.FLAT);

    LabelDateParameterMetadata item = LabelDateParameterMetadata.of(VALUATION_DATE, TNR_1Y);
    assertThatIllegalArgumentException().isThrownBy(() -> base.withNode(0.5d, 0.5d, item));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withMetadata() {
    HybridNodalCurve base = HybridNodalCurve.of(
        METADATA_NOPARAM,
        DoubleArray.of(0d, 1d, 2d, 3d),
        DoubleArray.of(1d, 1d, 1d, 1d),
        1,
        CurveInterpolators.LINEAR,
        CurveInterpolators.LINEAR,
        CurveExtrapolators.FLAT,
        CurveExtrapolators.FLAT);

    HybridNodalCurve test = base.withMetadata(CURVE_METADATA);
    assertThat(test.getMetadata()).isNotEqualTo(base.getMetadata());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withParameter() {
    HybridNodalCurve base = HybridNodalCurve.of(
        METADATA_NOPARAM,
        DoubleArray.of(0d, 1d, 2d, 3d),
        DoubleArray.of(1d, 1d, 1d, 1d),
        1,
        CurveInterpolators.LINEAR,
        CurveInterpolators.LINEAR,
        CurveExtrapolators.FLAT,
        CurveExtrapolators.FLAT);

    HybridNodalCurve testParam0 = base.withParameter(0, 0d);
    HybridNodalCurve testParam1 = base.withParameter(1, 0.1d);
    HybridNodalCurve testParam3 = base.withParameter(3, 3d);

    assertThat(testParam0.getYValues().get(0)).isEqualTo(0d);
    assertThat(testParam1.getYValues().get(1)).isEqualTo(0.1d);
    assertThat(testParam3.getYValues().get(3)).isEqualTo(3d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withYValues() {
    HybridNodalCurve base = HybridNodalCurve.of(
        METADATA_NOPARAM,
        DoubleArray.of(0d, 1d, 2d, 3d),
        DoubleArray.of(1d, 1d, 1d, 1d),
        1,
        CurveInterpolators.LINEAR,
        CurveInterpolators.LINEAR,
        CurveExtrapolators.FLAT,
        CurveExtrapolators.FLAT);

    HybridNodalCurve test = base.withYValues(DoubleArray.of(0d, 0d, 0d, 0d));
    assertThat(test.getXValues()).isEqualTo(base.getXValues());
    assertThat(test.getYValues()).isNotEqualTo(base.getYValues());
    assertThat(test.getMetadata()).isEqualTo(base.getMetadata());

    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.withYValues(DoubleArray.of(1d, 1d, 1d)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_getParameter() {
    HybridNodalCurve base = HybridNodalCurve.of(
        METADATA_NOPARAM,
        DoubleArray.of(0d, 1d, 2d, 3d),
        DoubleArray.of(1d, 1d, 1d, 1d),
        1,
        CurveInterpolators.LINEAR,
        CurveInterpolators.LINEAR,
        CurveExtrapolators.FLAT,
        CurveExtrapolators.FLAT);

    assertThat(base.getParameter(0)).isEqualTo(1d);
    assertThat(base.getParameter(1)).isEqualTo(1d);
    assertThat(base.getParameter(2)).isEqualTo(1d);
    assertThat(base.getParameter(3)).isEqualTo(1d);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.getParameter(5));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    HybridNodalCurve test = HybridNodalCurve.of(
        CURVE_METADATA,
        ALL_XVALUES,
        ALL_YVALUES,
        SPLICE_INDEX,
        LEFT_CURVE_INTERPOLATOR,
        RIGHT_CURVE_INTERPOLATOR,
        LEFT_CURVE_EXTRAPOLATOR,
        RIGHT_CURVE_EXTRAPOLATOR);
    coverImmutableBean(test);
    HybridNodalCurve test2 = HybridNodalCurve.of(
        META_TEMPLATE_ZERO.toBuilder().build(),
        DoubleArray.of(0d, 1d, 2d, 3d, 5d),
        DoubleArray.of(1d, 1d, 1d, 1d, 1d),
        2,
        CurveInterpolators.LOG_LINEAR,
        CurveInterpolators.DOUBLE_QUADRATIC,
        CurveExtrapolators.QUADRATIC_LEFT,
        CurveExtrapolators.LINEAR);
    coverBeanEquals(test, test2);
    assertThat(test2.values().toMap()).isEqualTo(ImmutableMap.of(0d, 1d, 1d, 1d, 2d, 1d, 3d, 1d, 5d, 1d));
  }

  @Test
  public void test_serialization() {
    HybridNodalCurve test = HybridNodalCurve.of(
        CURVE_METADATA,
        ALL_XVALUES,
        ALL_YVALUES,
        SPLICE_INDEX,
        LEFT_CURVE_INTERPOLATOR,
        RIGHT_CURVE_INTERPOLATOR,
        LEFT_CURVE_EXTRAPOLATOR,
        RIGHT_CURVE_EXTRAPOLATOR);
    assertSerialization(test);
  }

}
