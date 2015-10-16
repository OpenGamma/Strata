/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.LinearInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.LogLinearInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;

/**
 * Test {@link InterpolatedNodalSurface}.
 */
@Test
public class InterpolatedNodalSurfaceTest {

  private static final int SIZE = 9;
  private static final String NAME = "TestSurface";
  private static final SurfaceName SURFACE_NAME = SurfaceName.of(NAME);
  private static final SurfaceMetadata METADATA = DefaultSurfaceMetadata.of(SURFACE_NAME);
  private static final SurfaceMetadata METADATA_ENTRIES = DefaultSurfaceMetadata.builder()
      .surfaceName(SURFACE_NAME)
      .dayCount(ACT_365F)
      .parameterMetadata(SurfaceParameterMetadata.listOfEmpty(SIZE))
      .build();
  private static final DoubleMatrix1D XVALUES = DoubleMatrix1D.of(0d, 0d, 0d, 2d, 2d, 2d, 4d, 4d, 4d);
  private static final DoubleMatrix1D XVALUES2 = DoubleMatrix1D.of(0d, 2d, 3d, 0d, 2d, 3d, 0d, 2d, 3d);
  private static final DoubleMatrix1D YVALUES = DoubleMatrix1D.of(0d, 3d, 4d, 0d, 3d, 4d, 0d, 3d, 4d);
  private static final DoubleMatrix1D YVALUES2 = DoubleMatrix1D.of(3d, 4d, 5d, 3d, 4d, 5d, 3d, 4d, 5d);
  private static final DoubleMatrix1D ZVALUES = DoubleMatrix1D.of(5d, 7d, 8d, 6d, 7d, 8d, 8d, 7d, 8d);
  private static final DoubleMatrix1D ZVALUES_BUMPED = DoubleMatrix1D.of(3d, 5d, 6d, 4d, 5d, 6d, 6d, 5d, 6d);
  private static final Map<DoublesPair, Double> DATA = ImmutableMap.<DoublesPair, Double>builder()
      .put(DoublesPair.of(0d, 0d), 5d)
      .put(DoublesPair.of(0d, 3d), 7d)
      .put(DoublesPair.of(0d, 4d), 8d)
      .put(DoublesPair.of(2d, 0d), 6d)
      .put(DoublesPair.of(2d, 3d), 7d)
      .put(DoublesPair.of(2d, 4d), 8d)
      .put(DoublesPair.of(4d, 0d), 6d)
      .put(DoublesPair.of(4d, 3d), 5d)
      .put(DoublesPair.of(4d, 4d), 6d)
      .build();
  private static final GridInterpolator2D INTERPOLATOR =
      new GridInterpolator2D(new LinearInterpolator1D(), new LinearInterpolator1D());

  //-------------------------------------------------------------------------
  public void test_of_SurfaceMetadata() {
    InterpolatedNodalSurface test = InterpolatedNodalSurface.of(METADATA_ENTRIES, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getInterpolator()).isEqualTo(INTERPOLATOR);
    assertThat(test.getMetadata()).isEqualTo(METADATA_ENTRIES);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES);
    assertThat(test.getZValues()).isEqualTo(ZVALUES);
  }

  public void test_of_invalid() {
    // not enough nodes
    assertThrowsIllegalArg(() -> InterpolatedNodalSurface.of(
        METADATA, DoubleMatrix1D.of(1d), DoubleMatrix1D.of(2d), DoubleMatrix1D.of(3d), INTERPOLATOR));
    // x node size != y node size
    assertThrowsIllegalArg(() -> InterpolatedNodalSurface.of(
        METADATA, XVALUES, DoubleMatrix1D.of(1d, 3d), ZVALUES, INTERPOLATOR));
    // x node size != z node size
    assertThrowsIllegalArg(() -> InterpolatedNodalSurface.of(
        METADATA, XVALUES, YVALUES, DoubleMatrix1D.of(1d, 3d), INTERPOLATOR));
    // parameter metadata size != node size
    assertThrowsIllegalArg(() -> InterpolatedNodalSurface.of(
        METADATA_ENTRIES, DoubleMatrix1D.of(1d, 3d), DoubleMatrix1D.of(1d, 3d), DoubleMatrix1D.of(1d, 3d), INTERPOLATOR));
  }

  //-------------------------------------------------------------------------
  public void test_lookup() {
    InterpolatedNodalSurface test = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    assertThat(test.zValue(XVALUES.get(0), YVALUES.get(0))).isEqualTo(ZVALUES.get(0));
    assertThat(test.zValue(XVALUES.get(1), YVALUES.get(1))).isEqualTo(ZVALUES.get(1));
    assertThat(test.zValue(XVALUES.get(2), YVALUES.get(2))).isEqualTo(ZVALUES.get(2));
    assertThat(test.zValue(0d, 1.5d)).isEqualTo(6d);
    assertThat(test.zValue(1d, 3d)).isEqualTo(7d);

    Map<Double, Interpolator1DDataBundle> bundle = INTERPOLATOR.getDataBundle(DATA);
    assertThat(test.zValue(1.5d, 3.7d)).isEqualTo(INTERPOLATOR.interpolate(bundle, DoublesPair.of(1.5d, 3.7d)));
    assertThat(test.zValueParameterSensitivity(1.5d, 1.5d)).isEqualTo(
        INTERPOLATOR.getNodeSensitivitiesForValue(bundle, DoublesPair.of(1.5d, 1.5d)));
  }

  public void test_lookup_byPair() {
    InterpolatedNodalSurface test = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    assertThat(test.zValue(DoublesPair.of(XVALUES.get(0), YVALUES.get(0)))).isEqualTo(ZVALUES.get(0));
    assertThat(test.zValue(DoublesPair.of(XVALUES.get(1), YVALUES.get(1)))).isEqualTo(ZVALUES.get(1));
    assertThat(test.zValue(DoublesPair.of(XVALUES.get(2), YVALUES.get(2)))).isEqualTo(ZVALUES.get(2));
    assertThat(test.zValue(DoublesPair.of(0d, 1.5d))).isEqualTo(6d);
    assertThat(test.zValue(DoublesPair.of(1d, 3d))).isEqualTo(7d);

    Map<Double, Interpolator1DDataBundle> bundle = INTERPOLATOR.getDataBundle(DATA);
    assertThat(test.zValue(DoublesPair.of(1.5d, 3.7d))).isEqualTo(INTERPOLATOR.interpolate(bundle, DoublesPair.of(1.5d, 3.7d)));
    assertThat(test.zValueParameterSensitivity(DoublesPair.of(1.5d, 1.5d))).isEqualTo(
        INTERPOLATOR.getNodeSensitivitiesForValue(bundle, DoublesPair.of(1.5d, 1.5d)));
  }

  //-------------------------------------------------------------------------
  public void test_withZValues() {
    InterpolatedNodalSurface base = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    InterpolatedNodalSurface test = base.withZValues(ZVALUES_BUMPED);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES);
    assertThat(test.getZValues()).isEqualTo(ZVALUES_BUMPED);
  }

  public void test_withZValues_badSize() {
    InterpolatedNodalSurface base = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    assertThrowsIllegalArg(() -> base.withZValues(DoubleMatrix1D.EMPTY));
    assertThrowsIllegalArg(() -> base.withZValues(DoubleMatrix1D.of(4d, 6d)));
  }

  //-------------------------------------------------------------------------
  public void test_shiftedBy_operator() {
    InterpolatedNodalSurface base = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    InterpolatedNodalSurface test = base.shiftedBy((x, y, z) -> z - 2d);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES);
    assertThat(test.getZValues()).isEqualTo(ZVALUES_BUMPED);
  }

  public void test_shiftedBy_adjustment() {
    InterpolatedNodalSurface base = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    ImmutableList<ValueAdjustment> adjustments = ImmutableList.of(
        ValueAdjustment.ofReplace(3d),
        ValueAdjustment.ofDeltaAmount(-2d),
        ValueAdjustment.ofDeltaAmount(-2d),
        ValueAdjustment.ofReplace(4d),
        ValueAdjustment.ofDeltaAmount(-2d),
        ValueAdjustment.ofDeltaAmount(-2d),
        ValueAdjustment.ofReplace(6d),
        ValueAdjustment.ofDeltaAmount(-2d),
        ValueAdjustment.ofDeltaAmount(-2d));
    InterpolatedNodalSurface test = base.shiftedBy(adjustments);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES);
    assertThat(test.getZValues()).isEqualTo(ZVALUES_BUMPED);
  }

  public void test_shiftedBy_adjustment_longList() {
    InterpolatedNodalSurface base = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    ImmutableList<ValueAdjustment> adjustments = ImmutableList.of(
        ValueAdjustment.ofReplace(3d),
        ValueAdjustment.ofDeltaAmount(-2d),
        ValueAdjustment.ofDeltaAmount(-2d),
        ValueAdjustment.ofReplace(4d),
        ValueAdjustment.ofDeltaAmount(-2d),
        ValueAdjustment.ofDeltaAmount(-2d),
        ValueAdjustment.ofReplace(6d),
        ValueAdjustment.ofDeltaAmount(-2d),
        ValueAdjustment.ofDeltaAmount(-2d),
        ValueAdjustment.ofDeltaAmount(2d));
    InterpolatedNodalSurface test = base.shiftedBy(adjustments);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES);
    assertThat(test.getZValues()).isEqualTo(ZVALUES_BUMPED);
  }

  public void test_shiftedBy_adjustment_shortList() {
    InterpolatedNodalSurface base = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    ImmutableList<ValueAdjustment> adjustments = ImmutableList.of(
        ValueAdjustment.ofReplace(3d));
    DoubleMatrix1D bumped = DoubleMatrix1D.of(3d, 7d, 8d, 6d, 7d, 8d, 8d, 7d, 8d);
    InterpolatedNodalSurface test = base.shiftedBy(adjustments);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES);
    assertThat(test.getZValues()).isEqualTo(bumped);
  }

  //-------------------------------------------------------------------------
  public void test_applyPerturbation() {
    InterpolatedNodalSurface base = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    ConstantNodalSurface result = ConstantNodalSurface.of(SURFACE_NAME, 7d);
    Surface test = base.applyPerturbation(surface -> result);
    assertThat(test).isSameAs(result);
  }

  public void test_toNodalSurface() {
    InterpolatedNodalSurface base = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    NodalSurface test = base.toNodalSurface();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InterpolatedNodalSurface test = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    coverImmutableBean(test);
    InterpolatedNodalSurface test2 = InterpolatedNodalSurface.builder()
        .metadata(METADATA_ENTRIES)
        .xValues(XVALUES2)
        .yValues(YVALUES2)
        .zValues(ZVALUES_BUMPED)
        .interpolator(new GridInterpolator2D(new LogLinearInterpolator1D(), new LogLinearInterpolator1D()))
        .build();
    coverBeanEquals(test, test2);
  }

}
