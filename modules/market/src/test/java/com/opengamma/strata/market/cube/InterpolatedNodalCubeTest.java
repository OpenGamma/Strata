/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.PCHIP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.cube.interpolator.BoundCubeInterpolator;
import com.opengamma.strata.market.cube.interpolator.GridCubeInterpolator;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Test {@link InterpolatedNodalCube}.
 */
public class InterpolatedNodalCubeTest {

  private static final int SIZE = 18;
  private static final String NAME = "TestCube";
  private static final CubeName CUBE_NAME = CubeName.of(NAME);
  private static final CubeMetadata METADATA = DefaultCubeMetadata.of(CUBE_NAME);
  private static final CubeMetadata METADATA_ENTRIES = DefaultCubeMetadata.builder()
      .cubeName(CUBE_NAME)
      .dayCount(ACT_365F)
      .parameterMetadata(ParameterMetadata.listOfEmpty(SIZE))
      .build();
  private static final CubeMetadata METADATA_ENTRIES2 = DefaultCubeMetadata.builder()
      .cubeName(CUBE_NAME)
      .dayCount(ACT_365F)
      .parameterMetadata(ParameterMetadata.listOfEmpty(SIZE + 2))
      .build();
  private static final DoubleArray XVALUES = DoubleArray.of(0d, 0d, 0d, 0d, 0d, 0d, 2d, 2d, 2d, 2d, 2d, 2d, 4d, 4d, 4d, 4d, 4d, 4d);
  private static final DoubleArray XVALUES2 = DoubleArray.of(1d, 1d, 1d, 1d, 1d, 1d, 2d, 2d, 2d, 2d, 2d, 2d, 3d, 3d, 3d, 3d, 3d, 3d);
  private static final DoubleArray YVALUES = DoubleArray.of(0d, 0d, 3d, 3d, 4d, 4d, 0d, 0d, 3d, 3d, 4d, 4d, 0d, 0d, 3d, 3d, 4d, 4d);
  private static final DoubleArray YVALUES2 = DoubleArray.of(3d, 3d, 4d, 4d, 5d, 5d, 3d, 3d, 4d, 4d, 5d, 5d, 3d, 3d, 4d, 4d, 5d, 5d);
  private static final DoubleArray ZVALUES = DoubleArray.of(5d, 7d, 5d, 7d, 5d, 7d, 5d, 7d, 5d, 7d, 5d, 7d, 5d, 7d, 5d, 7d, 5d, 7d);
  private static final DoubleArray ZVALUES2 = DoubleArray.of(3d, 4d, 3d, 4d, 3d, 4d, 3d, 4d, 3d, 4d, 3d, 4d, 3d, 4d, 3d, 4d, 3d, 4d);
  private static final DoubleArray WVALUES = DoubleArray.of(5d, 7d, 8d, 6d, 7d, 8d, 8d, 7d, 8d, 5d, 7d, 8d, 6d, 7d, 8d, 8d, 7d, 8d);
  private static final DoubleArray WVALUES_BUMPED = DoubleArray.of(3d, 5d, 6d, 4d, 5d, 6d, 6d, 5d, 6d, 3d, 5d, 6d, 4d, 5d, 6d, 6d, 5d, 6d);
  private static final GridCubeInterpolator INTERPOLATOR = GridCubeInterpolator.of(LINEAR, LINEAR, LINEAR);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_CubeMetadata() {
    InterpolatedNodalCube test = InterpolatedNodalCube.of(
        METADATA_ENTRIES,
        XVALUES,
        YVALUES,
        ZVALUES,
        WVALUES,
        INTERPOLATOR);
    assertThat(test.getName()).isEqualTo(CUBE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getParameter(0)).isEqualTo(ZVALUES.get(0));
    assertThat(test.getParameter(1)).isEqualTo(ZVALUES.get(1));
    assertThat(test.getParameterMetadata(0)).isSameAs(METADATA_ENTRIES.getParameterMetadata().get().get(0));
    assertThat(test.getParameterMetadata(1)).isSameAs(METADATA_ENTRIES.getParameterMetadata().get().get(1));
    assertThat(test.withParameter(0, 2d)).isEqualTo(
        InterpolatedNodalCube.of(METADATA_ENTRIES, XVALUES, YVALUES, ZVALUES, WVALUES.with(0, 2d), INTERPOLATOR));
    assertThat(test.withPerturbation((i, v, m) -> v - 2d)).isEqualTo(
        InterpolatedNodalCube.of(METADATA_ENTRIES, XVALUES, YVALUES, ZVALUES, WVALUES_BUMPED, INTERPOLATOR));
    assertThat(test.getInterpolator()).isEqualTo(INTERPOLATOR);
    assertThat(test.getMetadata()).isEqualTo(METADATA_ENTRIES);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES);
    assertThat(test.getZValues()).isEqualTo(ZVALUES);
    assertThat(test.getWValues()).isEqualTo(WVALUES);
  }

  @Test
  public void test_of_invalid() {
    // not enough nodes
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedNodalCube.of(
            METADATA, DoubleArray.of(1d), DoubleArray.of(2d), DoubleArray.of(3d), DoubleArray.of(3d), INTERPOLATOR));
    // x node size != y node size
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedNodalCube.of(
            METADATA, XVALUES, DoubleArray.of(1d, 3d, 4d, 3d), ZVALUES, WVALUES, INTERPOLATOR));
    // x node size != z node size
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedNodalCube.of(
            METADATA, XVALUES, YVALUES, DoubleArray.of(1d, 3d), WVALUES, INTERPOLATOR));
    // x node size != w node size
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedNodalCube.of(
            METADATA, XVALUES, YVALUES, WVALUES, DoubleArray.of(1d, 3d), INTERPOLATOR));
    // parameter metadata size != node size
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedNodalCube.of(
            METADATA_ENTRIES,
            DoubleArray.of(1d, 3d),
            DoubleArray.of(1d, 3d),
            DoubleArray.of(1d, 3d),
            DoubleArray.of(1d, 3d),
            INTERPOLATOR));
    // x not in order
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedNodalCube.of(
            METADATA,
            DoubleArray.of(2d, 1d),
            DoubleArray.of(1d, 1d),
            DoubleArray.of(2d, 3d),
            DoubleArray.of(2d, 3d),
            INTERPOLATOR));
    // y not in order
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedNodalCube.of(
            METADATA,
            DoubleArray.of(1d, 1d),
            DoubleArray.of(2d, 1d),
            DoubleArray.of(2d, 3d),
            DoubleArray.of(2d, 3d),
            INTERPOLATOR));
    // z not in order
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedNodalCube.of(
            METADATA,
            DoubleArray.of(1d, 1d),
            DoubleArray.of(1d, 1d),
            DoubleArray.of(4d, 3d),
            DoubleArray.of(2d, 3d),
            INTERPOLATOR));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_lookup() {
    InterpolatedNodalCube test = InterpolatedNodalCube.of(METADATA, XVALUES, YVALUES, ZVALUES, WVALUES, INTERPOLATOR);
    assertThat(test.wValue(XVALUES.get(0), YVALUES.get(0), ZVALUES.get(0))).isEqualTo(WVALUES.get(0));
    assertThat(test.wValue(XVALUES.get(1), YVALUES.get(1), ZVALUES.get(1))).isEqualTo(WVALUES.get(1));
    assertThat(test.wValue(XVALUES.get(2), YVALUES.get(2), ZVALUES.get(2))).isEqualTo(WVALUES.get(2));
    assertThat(test.wValue(0d, 1.5d, 5.5d)).isEqualTo(6.5d);
    assertThat(test.wValue(1d, 3d, 6.5d)).isEqualTo(6.125d);

    BoundCubeInterpolator bound = INTERPOLATOR.bind(XVALUES, YVALUES, ZVALUES, WVALUES);
    assertThat(test.wValue(1.5d, 3.7d, 5.2d)).isEqualTo(bound.interpolate(1.5d, 3.7d, 5.2d));
    DoubleArray sensiValues = test.wValueParameterSensitivity(1.5d, 1.5d, 5.6d).getSensitivity();
    DoubleArray sensiValuesInterp = bound.parameterSensitivity(1.5d, 1.5d, 5.6d);
    assertThat(sensiValues.equalWithTolerance(sensiValuesInterp, 1e-8)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withMetadata() {
    InterpolatedNodalCube base = InterpolatedNodalCube.of(METADATA, XVALUES, YVALUES, ZVALUES, WVALUES, INTERPOLATOR);
    InterpolatedNodalCube test = base.withMetadata(METADATA_ENTRIES);
    assertThat(test.getName()).isEqualTo(CUBE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA_ENTRIES);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES);
    assertThat(test.getZValues()).isEqualTo(ZVALUES);
    assertThat(test.getWValues()).isEqualTo(WVALUES);
  }

  @Test
  public void test_withMetadata_badSize() {
    InterpolatedNodalCube base = InterpolatedNodalCube.of(METADATA, XVALUES, YVALUES, ZVALUES, WVALUES, INTERPOLATOR);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.withMetadata(METADATA_ENTRIES2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withZValues() {
    InterpolatedNodalCube base = InterpolatedNodalCube.of(METADATA, XVALUES, YVALUES, ZVALUES, WVALUES, INTERPOLATOR);
    InterpolatedNodalCube test = base.withWValues(WVALUES_BUMPED);
    assertThat(test.getName()).isEqualTo(CUBE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues()).isEqualTo(XVALUES);
    assertThat(test.getYValues()).isEqualTo(YVALUES);
    assertThat(test.getZValues()).isEqualTo(ZVALUES);
    assertThat(test.getWValues()).isEqualTo(WVALUES_BUMPED);
  }

  @Test
  public void test_withZValues_badSize() {
    InterpolatedNodalCube base = InterpolatedNodalCube.of(METADATA, XVALUES, YVALUES, ZVALUES, WVALUES, INTERPOLATOR);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.withWValues(DoubleArray.EMPTY));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.withWValues(DoubleArray.of(4d, 6d)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    InterpolatedNodalCube test = InterpolatedNodalCube.of(METADATA, XVALUES, YVALUES, ZVALUES, WVALUES, INTERPOLATOR);
    coverImmutableBean(test);
    InterpolatedNodalCube test2 = InterpolatedNodalCube.builder()
        .metadata(METADATA_ENTRIES)
        .xValues(XVALUES2)
        .yValues(YVALUES2)
        .zValues(ZVALUES2)
        .wValues(WVALUES_BUMPED)
        .interpolator(GridCubeInterpolator.of(DOUBLE_QUADRATIC, FLAT, FLAT, LINEAR, FLAT, FLAT, PCHIP, FLAT, FLAT))
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    InterpolatedNodalCube test = InterpolatedNodalCube.of(METADATA, XVALUES, YVALUES, ZVALUES, WVALUES, INTERPOLATOR);
    assertSerialization(test);
  }

}
