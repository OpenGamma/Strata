/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.meta.SimpleCurveNodeMetadata;
import com.opengamma.strata.market.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolator;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1DFactory;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test {@link InterpolatedNodalCurve}.
 */
@Test
public class InterpolatedNodalCurveTest {

  private static final int SIZE = 3;
  private static final String TNR_1Y = "1Y";

  private static final String NAME = "TestCurve";
  private static final CurveName CURVE_NAME = CurveName.of(NAME);
  private static final CurveMetadata METADATA = Curves.zeroRates(CURVE_NAME, ACT_365F);
  private static final CurveMetadata METADATA_ENTRIES =
      Curves.zeroRates(CURVE_NAME, ACT_365F, CurveParameterMetadata.listOfEmpty(SIZE));
  private static final DoubleArray XVALUES = DoubleArray.of(1d, 2d, 3d);
  private static final DoubleArray XVALUES2 = DoubleArray.of(0d, 2d, 3d);
  private static final DoubleArray YVALUES = DoubleArray.of(5d, 7d, 8d);
  private static final DoubleArray YVALUES_BUMPED = DoubleArray.of(3d, 5d, 6d);
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LOG_LINEAR;
  private static final CurveExtrapolator FLAT_EXTRAPOLATOR = CurveExtrapolators.FLAT;

  //-------------------------------------------------------------------------
  public void test_of_CurveMetadata() {
    InterpolatedNodalCurve test = InterpolatedNodalCurve.of(METADATA_ENTRIES, XVALUES, YVALUES, INTERPOLATOR);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getExtrapolatorLeft().getName()).isEqualTo(FLAT_EXTRAPOLATOR.getName());
    assertThat(test.getInterpolator().getName()).isEqualTo(INTERPOLATOR.getName());
    assertThat(test.getExtrapolatorRight().getName()).isEqualTo(FLAT_EXTRAPOLATOR.getName());
    assertThat(test.getMetadata()).isEqualTo(METADATA_ENTRIES);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES);
  }

  public void test_of_invalid() {
    // not enough nodes
    assertThrowsIllegalArg(() -> InterpolatedNodalCurve.of(
        METADATA, DoubleArray.of(1d), DoubleArray.of(1d), INTERPOLATOR));
    // x node size != y node size
    assertThrowsIllegalArg(() -> InterpolatedNodalCurve.of(
        METADATA, XVALUES, DoubleArray.of(1d, 3d), INTERPOLATOR));
    // parameter metadata size != node size
    assertThrowsIllegalArg(() -> InterpolatedNodalCurve.of(
        METADATA_ENTRIES, DoubleArray.of(1d, 3d), DoubleArray.of(1d, 3d), INTERPOLATOR));
    // x not in order
    assertThrowsIllegalArg(() -> InterpolatedNodalCurve.of(
        METADATA, DoubleArray.of(2d, 1d), DoubleArray.of(2d, 3d), INTERPOLATOR));
  }

  //-------------------------------------------------------------------------
  public void test_lookup() {
    InterpolatedNodalCurve test = InterpolatedNodalCurve.of(METADATA, XVALUES, YVALUES, INTERPOLATOR);
    Interpolator1D combined = new CombinedInterpolatorExtrapolator(
        Interpolator1DFactory.LOG_LINEAR_INSTANCE,
        Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE,
        Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE);
    Interpolator1DDataBundle bundle = combined.getDataBundle(XVALUES.toArray(), YVALUES.toArray());
    assertThat(test.yValue(XVALUES.get(0))).isEqualTo(YVALUES.get(0));
    assertThat(test.yValue(XVALUES.get(1))).isEqualTo(YVALUES.get(1));
    assertThat(test.yValue(XVALUES.get(2))).isEqualTo(YVALUES.get(2));
    assertThat(test.yValue(10d)).isEqualTo(combined.interpolate(bundle, 10d));

    assertThat(test.yValueParameterSensitivity(10d)).isEqualTo(
        CurveUnitParameterSensitivity.of(METADATA, DoubleArray.copyOf(combined.getNodeSensitivitiesForValue(bundle, 10d))));

    assertThat(test.firstDerivative(10d)).isEqualTo(combined.firstDerivative(bundle, 10d));
  }

  //-------------------------------------------------------------------------
  public void test_withYValues() {
    InterpolatedNodalCurve base = InterpolatedNodalCurve.of(METADATA, XVALUES, YVALUES, INTERPOLATOR);
    InterpolatedNodalCurve test = base.withYValues(YVALUES_BUMPED);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES_BUMPED);
  }

  public void test_withYValues_badSize() {
    InterpolatedNodalCurve base = InterpolatedNodalCurve.of(METADATA, XVALUES, YVALUES, INTERPOLATOR);
    assertThrowsIllegalArg(() -> base.withYValues(DoubleArray.EMPTY));
    assertThrowsIllegalArg(() -> base.withYValues(DoubleArray.of(4d, 6d)));
  }

  //-------------------------------------------------------------------------
  public void test_shiftedBy_operator() {
    InterpolatedNodalCurve base = InterpolatedNodalCurve.of(METADATA, XVALUES, YVALUES, INTERPOLATOR);
    InterpolatedNodalCurve test = base.shiftedBy((x, y) -> y - 2d);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES_BUMPED);
  }

  public void test_shiftedBy_adjustment() {
    InterpolatedNodalCurve base = InterpolatedNodalCurve.of(METADATA, XVALUES, YVALUES, INTERPOLATOR);
    ImmutableList<ValueAdjustment> adjustments = ImmutableList.of(
        ValueAdjustment.ofReplace(3d), ValueAdjustment.ofDeltaAmount(-2d), ValueAdjustment.ofDeltaAmount(-2d));
    InterpolatedNodalCurve test = base.shiftedBy(adjustments);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES_BUMPED);
  }

  public void test_shiftedBy_adjustment_longList() {
    InterpolatedNodalCurve base = InterpolatedNodalCurve.of(METADATA, XVALUES, YVALUES, INTERPOLATOR);
    ImmutableList<ValueAdjustment> adjustments = ImmutableList.of(
        ValueAdjustment.ofReplace(3d),
        ValueAdjustment.ofDeltaAmount(-2d),
        ValueAdjustment.ofDeltaAmount(-2d),
        ValueAdjustment.ofDeltaAmount(2d));
    InterpolatedNodalCurve test = base.shiftedBy(adjustments);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES_BUMPED);
  }

  public void test_shiftedBy_adjustment_shortList() {
    InterpolatedNodalCurve base = InterpolatedNodalCurve.of(METADATA, XVALUES, YVALUES, INTERPOLATOR);
    ImmutableList<ValueAdjustment> adjustments = ImmutableList.of(
        ValueAdjustment.ofReplace(3d));
    DoubleArray bumped = DoubleArray.of(3d, 7d, 8d);
    InterpolatedNodalCurve test = base.shiftedBy(adjustments);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(bumped);
  }

  //-------------------------------------------------------------------------
  public void test_withNode_atStart_noMetadata() {
    InterpolatedNodalCurve base = InterpolatedNodalCurve.of(METADATA_ENTRIES, XVALUES, YVALUES, INTERPOLATOR);
    InterpolatedNodalCurve test = base.withNode(0, 0.5d, 4d);
    DoubleArray x = DoubleArray.of(0.5d, 1d, 2d, 3d);
    DoubleArray y = DoubleArray.of(4d, 5d, 7d, 8d);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE + 1);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(x);
    assertThat(test.getYValues()).isEqualTo(y);
  }

  public void test_withNode_atEnd_noMetadata() {
    InterpolatedNodalCurve base = InterpolatedNodalCurve.of(METADATA_ENTRIES, XVALUES, YVALUES, INTERPOLATOR);
    InterpolatedNodalCurve test = base.withNode(SIZE, 4d, 9d);
    DoubleArray x = DoubleArray.of(1d, 2d, 3d, 4d);
    DoubleArray y = DoubleArray.of(5d, 7d, 8d, 9d);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE + 1);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(x);
    assertThat(test.getYValues()).isEqualTo(y);
  }

  public void test_withNode_atStart_metadata() {
    InterpolatedNodalCurve base = InterpolatedNodalCurve.of(METADATA_ENTRIES, XVALUES, YVALUES, INTERPOLATOR);
    SimpleCurveNodeMetadata item = SimpleCurveNodeMetadata.of(date(2015, 6, 30), TNR_1Y);
    InterpolatedNodalCurve test = base.withNode(0, item, 0.5d, 4d);
    DoubleArray x = DoubleArray.of(0.5d, 1d, 2d, 3d);
    DoubleArray y = DoubleArray.of(4d, 5d, 7d, 8d);
    List<CurveParameterMetadata> list = new ArrayList<>();
    list.add(item);
    list.addAll(CurveParameterMetadata.listOfEmpty(SIZE));
    CurveMetadata expectedMetadata = Curves.zeroRates(CURVE_NAME, ACT_365F, list);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE + 1);
    assertThat(test.getMetadata()).isEqualTo(expectedMetadata);
    assertThat(test.getXValues()).isEqualTo(x);
    assertThat(test.getYValues()).isEqualTo(y);
  }

  public void test_withNode_atEnd_metadata_onCurveWithoutMetadata() {
    InterpolatedNodalCurve base = InterpolatedNodalCurve.of(METADATA, XVALUES, YVALUES, INTERPOLATOR);
    SimpleCurveNodeMetadata item = SimpleCurveNodeMetadata.of(date(2015, 6, 30), TNR_1Y);
    InterpolatedNodalCurve test = base.withNode(0, item, 0.5d, 4d);
    DoubleArray x = DoubleArray.of(0.5d, 1d, 2d, 3d);
    DoubleArray y = DoubleArray.of(4d, 5d, 7d, 8d);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE + 1);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(x);
    assertThat(test.getYValues()).isEqualTo(y);
  }

  //-------------------------------------------------------------------------
  public void test_applyPerturbation() {
    InterpolatedNodalCurve base = InterpolatedNodalCurve.of(METADATA, XVALUES, YVALUES, INTERPOLATOR);
    ConstantNodalCurve result = ConstantNodalCurve.of(CURVE_NAME, 7d);
    Curve test = base.applyPerturbation(curve -> result);
    assertThat(test).isSameAs(result);
  }

  public void test_toNodalCurve() {
    InterpolatedNodalCurve base = InterpolatedNodalCurve.of(METADATA, XVALUES, YVALUES, INTERPOLATOR);
    NodalCurve test = base.toNodalCurve();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InterpolatedNodalCurve test = InterpolatedNodalCurve.of(METADATA, XVALUES, YVALUES, INTERPOLATOR);
    coverImmutableBean(test);
    InterpolatedNodalCurve test2 = InterpolatedNodalCurve.builder()
        .metadata(METADATA_ENTRIES)
        .xValues(XVALUES2)
        .yValues(YVALUES_BUMPED)
        .extrapolatorLeft(CurveExtrapolators.LOG_LINEAR)
        .interpolator(CurveInterpolators.DOUBLE_QUADRATIC)
        .extrapolatorRight(CurveExtrapolators.LOG_LINEAR)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    InterpolatedNodalCurve test = InterpolatedNodalCurve.of(METADATA, XVALUES, YVALUES, INTERPOLATOR);
    assertSerialization(test);
  }

}
