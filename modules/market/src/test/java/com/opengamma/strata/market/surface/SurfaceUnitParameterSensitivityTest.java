/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link SurfaceUnitParameterSensitivity}.
 */
@Test
public class SurfaceUnitParameterSensitivityTest {

  private static final double FACTOR1 = 3.14;
  private static final DoubleArray VECTOR_USD1 = DoubleArray.of(100, 200, 300, 123);
  private static final DoubleArray VECTOR_USD_FACTOR =
      DoubleArray.of(100 * FACTOR1, 200 * FACTOR1, 300 * FACTOR1, 123 * FACTOR1);
  private static final DoubleArray VECTOR_EUR1 = DoubleArray.of(1000, 250, 321, 123, 321);
  private static final SurfaceName NAME1 = SurfaceName.of("NAME-1");
  private static final SurfaceMetadata METADATA1 = DefaultSurfaceMetadata.of(NAME1);
  private static final SurfaceName NAME2 = SurfaceName.of("NAME-2");
  private static final SurfaceMetadata METADATA2 = DefaultSurfaceMetadata.of(NAME2);

  //-------------------------------------------------------------------------
  public void test_of_metadata() {
    SurfaceUnitParameterSensitivity test = SurfaceUnitParameterSensitivity.of(METADATA1, VECTOR_USD1);
    assertThat(test.getMetadata()).isEqualTo(METADATA1);
    assertThat(test.getSurfaceName()).isEqualTo(NAME1);
    assertThat(test.getParameterCount()).isEqualTo(VECTOR_USD1.size());
    assertThat(test.getSensitivity()).isEqualTo(VECTOR_USD1);
  }

  public void test_of_metadata_badMetadata() {
    DefaultSurfaceMetadata metadata = DefaultSurfaceMetadata.builder()
        .surfaceName(NAME1)
        .parameterMetadata(SurfaceParameterMetadata.listOfEmpty(VECTOR_USD1.size() + 1))
        .build();
    assertThrowsIllegalArg(() -> SurfaceUnitParameterSensitivity.of(metadata, VECTOR_USD1));
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    SurfaceUnitParameterSensitivity base = SurfaceUnitParameterSensitivity.of(METADATA1, VECTOR_USD1);
    SurfaceUnitParameterSensitivity test = base.multipliedBy(FACTOR1);
    assertThat(test).isEqualTo(SurfaceUnitParameterSensitivity.of(METADATA1, VECTOR_USD_FACTOR));
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    SurfaceUnitParameterSensitivity base = SurfaceUnitParameterSensitivity.of(METADATA1, VECTOR_USD1);
    SurfaceUnitParameterSensitivity test = base.withSensitivity(VECTOR_USD_FACTOR);
    assertThat(test).isEqualTo(SurfaceUnitParameterSensitivity.of(METADATA1, VECTOR_USD_FACTOR));
    assertThrowsIllegalArg(() -> base.withSensitivity(DoubleArray.of(1d)));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SurfaceUnitParameterSensitivity test1 = SurfaceUnitParameterSensitivity.of(METADATA1, VECTOR_USD1);
    coverImmutableBean(test1);
    SurfaceUnitParameterSensitivity test2 = SurfaceUnitParameterSensitivity.of(METADATA2, VECTOR_EUR1);
    coverBeanEquals(test1, test2);
  }

}
