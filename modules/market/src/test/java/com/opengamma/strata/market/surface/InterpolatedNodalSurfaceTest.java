/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.surface.interpolator.BoundSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;

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
      .parameterMetadata(ParameterMetadata.listOfEmpty(SIZE))
      .build();
  private static final SurfaceMetadata METADATA_ENTRIES2 = DefaultSurfaceMetadata.builder()
      .surfaceName(SURFACE_NAME)
      .dayCount(ACT_365F)
      .parameterMetadata(ParameterMetadata.listOfEmpty(SIZE + 2))
      .build();
  private static final DoubleArray XVALUES = DoubleArray.of(0d, 0d, 0d, 2d, 2d, 2d, 4d, 4d, 4d);
  private static final DoubleArray XVALUES2 = DoubleArray.of(1d, 1d, 1d, 2d, 2d, 2d, 3d, 3d, 3d);
  private static final DoubleArray YVALUES = DoubleArray.of(0d, 3d, 4d, 0d, 3d, 4d, 0d, 3d, 4d);
  private static final DoubleArray YVALUES2 = DoubleArray.of(3d, 4d, 5d, 3d, 4d, 5d, 3d, 4d, 5d);
  private static final DoubleArray ZVALUES = DoubleArray.of(5d, 7d, 8d, 6d, 7d, 8d, 8d, 7d, 8d);
  private static final DoubleArray ZVALUES_BUMPED = DoubleArray.of(3d, 5d, 6d, 4d, 5d, 6d, 6d, 5d, 6d);
  private static final GridSurfaceInterpolator INTERPOLATOR = GridSurfaceInterpolator.of(LINEAR, LINEAR);

  //-------------------------------------------------------------------------
  public void test_of_SurfaceMetadata() {
    InterpolatedNodalSurface test = InterpolatedNodalSurface.of(METADATA_ENTRIES, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getParameter(0)).isEqualTo(ZVALUES.get(0));
    assertThat(test.getParameter(1)).isEqualTo(ZVALUES.get(1));
    assertThat(test.getParameterMetadata(0)).isSameAs(METADATA_ENTRIES.getParameterMetadata().get().get(0));
    assertThat(test.getParameterMetadata(1)).isSameAs(METADATA_ENTRIES.getParameterMetadata().get().get(1));
    assertThat(test.withParameter(0, 2d)).isEqualTo(
        InterpolatedNodalSurface.of(METADATA_ENTRIES, XVALUES, YVALUES, ZVALUES.with(0, 2d), INTERPOLATOR));
    assertThat(test.withPerturbation((i, v, m) -> v - 2d)).isEqualTo(
        InterpolatedNodalSurface.of(METADATA_ENTRIES, XVALUES, YVALUES, ZVALUES_BUMPED, INTERPOLATOR));
    assertThat(test.getInterpolator()).isEqualTo(INTERPOLATOR);
    assertThat(test.getMetadata()).isEqualTo(METADATA_ENTRIES);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES);
    assertThat(test.getZValues()).isEqualTo(ZVALUES);
  }

  public void test_of_invalid() {
    // not enough nodes
    assertThrowsIllegalArg(() -> InterpolatedNodalSurface.of(
        METADATA, DoubleArray.of(1d), DoubleArray.of(2d), DoubleArray.of(3d), INTERPOLATOR));
    // x node size != y node size
    assertThrowsIllegalArg(() -> InterpolatedNodalSurface.of(
        METADATA, XVALUES, DoubleArray.of(1d, 3d), ZVALUES, INTERPOLATOR));
    // x node size != z node size
    assertThrowsIllegalArg(() -> InterpolatedNodalSurface.of(
        METADATA, XVALUES, YVALUES, DoubleArray.of(1d, 3d), INTERPOLATOR));
    // parameter metadata size != node size
    assertThrowsIllegalArg(() -> InterpolatedNodalSurface.of(
        METADATA_ENTRIES, DoubleArray.of(1d, 3d), DoubleArray.of(1d, 3d), DoubleArray.of(1d, 3d), INTERPOLATOR));
    // x not in order
    assertThrowsIllegalArg(() -> InterpolatedNodalSurface.of(
        METADATA, DoubleArray.of(2d, 1d), DoubleArray.of(1d, 1d), DoubleArray.of(2d, 3d), INTERPOLATOR));
    // y not in order
    assertThrowsIllegalArg(() -> InterpolatedNodalSurface.of(
        METADATA, DoubleArray.of(1d, 1d), DoubleArray.of(2d, 1d), DoubleArray.of(2d, 3d), INTERPOLATOR));
  }

  //-------------------------------------------------------------------------
  public void test_lookup() {
    InterpolatedNodalSurface test = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    assertThat(test.zValue(XVALUES.get(0), YVALUES.get(0))).isEqualTo(ZVALUES.get(0));
    assertThat(test.zValue(XVALUES.get(1), YVALUES.get(1))).isEqualTo(ZVALUES.get(1));
    assertThat(test.zValue(XVALUES.get(2), YVALUES.get(2))).isEqualTo(ZVALUES.get(2));
    assertThat(test.zValue(0d, 1.5d)).isEqualTo(6d);
    assertThat(test.zValue(1d, 3d)).isEqualTo(7d);

    BoundSurfaceInterpolator bound = INTERPOLATOR.bind(XVALUES, YVALUES, ZVALUES);
    assertThat(test.zValue(1.5d, 3.7d)).isEqualTo(bound.interpolate(1.5d, 3.7d));
    DoubleArray sensiValues = test.zValueParameterSensitivity(1.5d, 1.5d).getSensitivity();
    DoubleArray sensiValuesInterp = bound.parameterSensitivity(1.5d, 1.5d);
    assertTrue(sensiValues.equalWithTolerance(sensiValuesInterp, 1e-8));
  }

  //-------------------------------------------------------------------------
  public void test_withMetadata() {
    InterpolatedNodalSurface base = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    InterpolatedNodalSurface test = base.withMetadata(METADATA_ENTRIES);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA_ENTRIES);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES);
    assertThat(test.getZValues()).isEqualTo(ZVALUES);
  }

  public void test_withMetadata_badSize() {
    InterpolatedNodalSurface base = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    assertThrowsIllegalArg(() -> base.withMetadata(METADATA_ENTRIES2));
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
    assertThrowsIllegalArg(() -> base.withZValues(DoubleArray.EMPTY));
    assertThrowsIllegalArg(() -> base.withZValues(DoubleArray.of(4d, 6d)));
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
        .interpolator(GridSurfaceInterpolator.of(DOUBLE_QUADRATIC, FLAT, FLAT, LINEAR, FLAT, FLAT))
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    InterpolatedNodalSurface test = InterpolatedNodalSurface.of(METADATA, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
    assertSerialization(test);
  }

}
