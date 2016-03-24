/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;

/**
 * Test {@link ConstantNodalSurface}.
 */
@Test
public class ConstantNodalSurfaceTest {

  private static final String NAME = "TestSurface";
  private static final SurfaceName SURFACE_NAME = SurfaceName.of(NAME);
  private static final SurfaceMetadata METADATA = DefaultSurfaceMetadata.of(SURFACE_NAME);
  private static final double VALUE = 6d;

  //-------------------------------------------------------------------------
  public void test_of_String() {
    ConstantNodalSurface test = ConstantNodalSurface.of(NAME, VALUE);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues().toArray()).containsExactly(0d);
    assertThat(test.getYValues().toArray()).containsExactly(0d);
    assertThat(test.getZValues().toArray()).containsExactly(VALUE);
  }

  public void test_of_SurfaceName() {
    ConstantNodalSurface test = ConstantNodalSurface.of(SURFACE_NAME, VALUE);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues().toArray()).containsExactly(0d);
    assertThat(test.getYValues().toArray()).containsExactly(0d);
    assertThat(test.getZValues().toArray()).containsExactly(VALUE);
  }

  public void test_of_SurfaceMetadata() {
    ConstantNodalSurface test = ConstantNodalSurface.of(METADATA, VALUE);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues().toArray()).containsExactly(0d);
    assertThat(test.getYValues().toArray()).containsExactly(0d);
    assertThat(test.getZValues().toArray()).containsExactly(VALUE);
  }

  //-------------------------------------------------------------------------
  public void test_lookup() {
    ConstantNodalSurface test = ConstantNodalSurface.of(SURFACE_NAME, VALUE);
    assertThat(test.zValue(0d, 0d)).isEqualTo(VALUE);
    assertThat(test.zValue(-10d, 10d)).isEqualTo(VALUE);
    assertThat(test.zValue(100d, -100d)).isEqualTo(VALUE);

    assertThat(test.zValueParameterSensitivity(0d, 0d).getSensitivity().get(0)).isEqualTo(1d);
    assertThat(test.zValueParameterSensitivity(-10d, 10d).getSensitivity().get(0)).isEqualTo(1d);
    assertThat(test.zValueParameterSensitivity(100d, -100d).getSensitivity().get(0)).isEqualTo(1d);
  }

  public void test_lookup_byPair() {
    ConstantNodalSurface test = ConstantNodalSurface.of(SURFACE_NAME, VALUE);
    assertThat(test.zValue(DoublesPair.of(0d, 0d))).isEqualTo(VALUE);
    assertThat(test.zValue(DoublesPair.of(-10d, 10d))).isEqualTo(VALUE);
    assertThat(test.zValue(DoublesPair.of(100d, -100d))).isEqualTo(VALUE);

    assertThat(test.zValueParameterSensitivity(DoublesPair.of(0d, 0d)).getSensitivity().get(0)).isEqualTo(1d);
    assertThat(test.zValueParameterSensitivity(DoublesPair.of(-10d, 10d)).getSensitivity().get(0)).isEqualTo(1d);
    assertThat(test.zValueParameterSensitivity(DoublesPair.of(100d, -100d)).getSensitivity().get(0)).isEqualTo(1d);
  }

  //-------------------------------------------------------------------------
  public void test_withZValues() {
    ConstantNodalSurface base = ConstantNodalSurface.of(SURFACE_NAME, VALUE);
    ConstantNodalSurface test = base.withZValues(DoubleArray.of(4d));
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues().toArray()).containsExactly(0d);
    assertThat(test.getYValues().toArray()).containsExactly(0d);
    assertThat(test.getZValues().toArray()).containsExactly(4d);
  }

  public void test_withZValues_badSize() {
    ConstantNodalSurface base = ConstantNodalSurface.of(SURFACE_NAME, VALUE);
    assertThrowsIllegalArg(() -> base.withZValues(DoubleArray.EMPTY));
    assertThrowsIllegalArg(() -> base.withZValues(DoubleArray.of(4d, 6d)));
  }

  public void test_shiftedBy_operator() {
    ConstantNodalSurface base = ConstantNodalSurface.of(SURFACE_NAME, VALUE);
    ConstantNodalSurface test = base.shiftedBy((x, y, z) -> z - 2d);
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues().toArray()).containsExactly(0d);
    assertThat(test.getYValues().toArray()).containsExactly(0d);
    assertThat(test.getZValues().toArray()).containsExactly(4d);
  }

  public void test_shiftedBy_adjustment() {
    ConstantNodalSurface base = ConstantNodalSurface.of(SURFACE_NAME, VALUE);
    ConstantNodalSurface test = base.shiftedBy(ImmutableList.of(ValueAdjustment.ofReplace(4d)));
    assertThat(test.getName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(1);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getXValues().toArray()).containsExactly(0d);
    assertThat(test.getYValues().toArray()).containsExactly(0d);
    assertThat(test.getZValues().toArray()).containsExactly(4d);
  }

  //-------------------------------------------------------------------------
  public void test_applyPerturbation() {
    ConstantNodalSurface base = ConstantNodalSurface.of(SURFACE_NAME, VALUE);
    ConstantNodalSurface result = ConstantNodalSurface.of(SURFACE_NAME, 7d);
    Surface test = base.applyPerturbation(surface -> result);
    assertThat(test).isSameAs(result);
  }

  public void test_toNodalSurface() {
    ConstantNodalSurface base = ConstantNodalSurface.of(SURFACE_NAME, VALUE);
    NodalSurface test = base.toNodalSurface();
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ConstantNodalSurface test = ConstantNodalSurface.of(SURFACE_NAME, VALUE);
    coverImmutableBean(test);
    ConstantNodalSurface test2 = ConstantNodalSurface.of("Coverage", 9d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ConstantNodalSurface test = ConstantNodalSurface.of(SURFACE_NAME, VALUE);
    assertSerialization(test);
  }

}
