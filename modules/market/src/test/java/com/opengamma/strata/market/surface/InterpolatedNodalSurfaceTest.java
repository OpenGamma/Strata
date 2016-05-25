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
import static org.testng.Assert.assertEquals;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.LinearInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.LogLinearInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

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
  private static final DoubleArray XVALUES = DoubleArray.of(0d, 0d, 0d, 2d, 2d, 2d, 4d, 4d, 4d);
  private static final DoubleArray XVALUES2 = DoubleArray.of(0d, 2d, 3d, 0d, 2d, 3d, 0d, 2d, 3d);
  private static final DoubleArray YVALUES = DoubleArray.of(0d, 3d, 4d, 0d, 3d, 4d, 0d, 3d, 4d);
  private static final DoubleArray YVALUES2 = DoubleArray.of(3d, 4d, 5d, 3d, 4d, 5d, 3d, 4d, 5d);
  private static final DoubleArray ZVALUES = DoubleArray.of(5d, 7d, 8d, 6d, 7d, 8d, 8d, 7d, 8d);
  private static final DoubleArray ZVALUES_BUMPED = DoubleArray.of(3d, 5d, 6d, 4d, 5d, 6d, 6d, 5d, 6d);
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
    DoubleArray sensiValues = test.zValueParameterSensitivity(1.5d, 1.5d).getSensitivity();
    Map<DoublesPair, Double> sensiValuesMap = INTERPOLATOR.getNodeSensitivitiesForValue(bundle, DoublesPair.of(1.5d, 1.5d));
    for (int i = 0; i < XVALUES.size(); ++i) {
      DoublesPair pair = DoublesPair.of(XVALUES.get(i), YVALUES.get(i));
      assertEquals(sensiValues.get(i), sensiValuesMap.get(pair));
    }
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

    DoubleArray sensiValues = test.zValueParameterSensitivity(DoublesPair.of(1.5d, 1.5d)).getSensitivity();
    Map<DoublesPair, Double> sensiValuesMap = INTERPOLATOR.getNodeSensitivitiesForValue(bundle, DoublesPair.of(1.5d, 1.5d));
    for (int i = 0; i < XVALUES.size(); ++i) {
      DoublesPair pair = DoublesPair.of(XVALUES.get(i), YVALUES.get(i));
      assertEquals(sensiValues.get(i), sensiValuesMap.get(pair));
    }
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
        .interpolator(new GridInterpolator2D(new LogLinearInterpolator1D(), new LogLinearInterpolator1D()))
        .build();
    coverBeanEquals(test, test2);
  }

}
