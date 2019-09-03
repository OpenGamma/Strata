/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;

/**
 * Test {@link DeformedSurface}.
 */
public class DeformedSurfaceTest {

  private static final int SIZE = 9;
  private static final SurfaceName SURFACE_NAME = SurfaceName.of("TestSurface");
  private static final SurfaceMetadata METADATA_ORG = DefaultSurfaceMetadata.builder()
      .surfaceName(SURFACE_NAME)
      .dayCount(ACT_365F)
      .parameterMetadata(ParameterMetadata.listOfEmpty(SIZE))
      .build();
  private static final DoubleArray XVALUES = DoubleArray.of(0d, 0d, 0d, 2d, 2d, 2d, 4d, 4d, 4d);
  private static final DoubleArray YVALUES = DoubleArray.of(0d, 3d, 4d, 0d, 3d, 4d, 0d, 3d, 4d);
  private static final DoubleArray ZVALUES = DoubleArray.of(5d, 7d, 8d, 6d, 7d, 8d, 8d, 7d, 8d);
  private static final GridSurfaceInterpolator INTERPOLATOR = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final InterpolatedNodalSurface SURFACE_ORG =
      InterpolatedNodalSurface.of(METADATA_ORG, XVALUES, YVALUES, ZVALUES, INTERPOLATOR);
  private static final Function<DoublesPair, ValueDerivatives> FUNCTION = new Function<DoublesPair, ValueDerivatives>() {
    @Override
    public ValueDerivatives apply(DoublesPair x) {
      double value = 1.5 * SURFACE_ORG.zValue(x) * x.getFirst() * x.getSecond();
      DoubleArray derivatives =
          SURFACE_ORG.zValueParameterSensitivity(x).multipliedBy(1.5 * x.getFirst() * x.getSecond()).getSensitivity();
      return ValueDerivatives.of(value, derivatives);
    }
  };
  private static final SurfaceMetadata METADATA = DefaultSurfaceMetadata.of("DeformedTestSurface");

  @Test
  public void test_of() {
    DeformedSurface test = DeformedSurface.of(METADATA, SURFACE_ORG, FUNCTION);
    assertThat(test.getDeformationFunction()).isEqualTo(FUNCTION);
    assertThat(test.getMetadata()).isEqualTo(METADATA);
    assertThat(test.getName()).isEqualTo(METADATA.getSurfaceName());
    assertThat(test.getOriginalSurface()).isEqualTo(SURFACE_ORG);
    assertThat(test.getParameterCount()).isEqualTo(SIZE);
    assertThat(test.getParameter(2)).isEqualTo(SURFACE_ORG.getParameter(2));
    assertThat(test.getParameterMetadata(2)).isEqualTo(SURFACE_ORG.getParameterMetadata(2));
  }

  @Test
  public void test_zValue() {
    double tol = 1.0e-14;
    double x = 2.5;
    double y = 1.44;
    DeformedSurface test = DeformedSurface.of(METADATA, SURFACE_ORG, FUNCTION);
    double computedValue1 = test.zValue(x, y);
    double computedValue2 = test.zValue(DoublesPair.of(x, y));
    UnitParameterSensitivity computedSensi1 = test.zValueParameterSensitivity(x, y);
    UnitParameterSensitivity computedSensi2 = test.zValueParameterSensitivity(DoublesPair.of(x, y));
    ValueDerivatives expected = FUNCTION.apply(DoublesPair.of(x, y));
    assertThat(computedValue1).isEqualTo(expected.getValue());
    assertThat(computedValue2).isEqualTo(expected.getValue());
    assertThat(DoubleArrayMath.fuzzyEquals(
        computedSensi1.getSensitivity().toArray(), expected.getDerivatives().toArray(), tol)).isTrue();
    assertThat(DoubleArrayMath.fuzzyEquals(
        computedSensi2.getSensitivity().toArray(), expected.getDerivatives().toArray(), tol)).isTrue();
  }

  @Test
  public void test_withParameter() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DeformedSurface.of(METADATA, SURFACE_ORG, FUNCTION).withParameter(1, 1.2d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    DeformedSurface test1 = DeformedSurface.of(METADATA, SURFACE_ORG, FUNCTION);
    coverImmutableBean(test1);
    Surface surface1 = InterpolatedNodalSurface.of(DefaultSurfaceMetadata.of("TestSurface1"), XVALUES, YVALUES,
        ZVALUES, INTERPOLATOR);
    DeformedSurface test2 = DeformedSurface.of(DefaultSurfaceMetadata.of("DeformedTestSurface1"), surface1,
        new Function<DoublesPair, ValueDerivatives>() {
          @Override
          public ValueDerivatives apply(DoublesPair x) {
            return ValueDerivatives.of(surface1.zValue(x), surface1.zValueParameterSensitivity(x).getSensitivity());
          }
        });
    coverBeanEquals(test1, test2);
  }

}
