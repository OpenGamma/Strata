/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.analytics.financial.model.volatility.surface.Delta;
import com.opengamma.strata.basics.currency.CurrencyPair;

/**
 * Test {@link SurfaceMetadata}.
 */
@Test
public class SurfaceMetadataTest {

  private static final String NAME = "TestSurface";
  private static final SurfaceName SURFACE_NAME = SurfaceName.of(NAME);

  private static final ImmutableList<SurfaceParameterMetadata> METADATA;
  static {
    FxVolatilitySurfaceYearFractionNodeMetadata data1 = FxVolatilitySurfaceYearFractionNodeMetadata.of(
        1.5d, new Delta(0.25d), CurrencyPair.of(GBP, USD));
    FxVolatilitySurfaceYearFractionNodeMetadata data2 = FxVolatilitySurfaceYearFractionNodeMetadata.of(
        0.25d, new Delta(0.9d), CurrencyPair.of(GBP, USD));
    METADATA = ImmutableList.of(data1, data2);
  }

  //-------------------------------------------------------------------------
  public void test_of_String_noParameterMetadata() {
    SurfaceMetadata test = SurfaceMetadata.of(NAME);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getDayCount().isPresent()).isFalse();
    assertThat(test.getParameters().isPresent()).isFalse();
  }

  public void test_of_SurfaceName_noParameterMetadata() {
    SurfaceMetadata test = SurfaceMetadata.of(SURFACE_NAME);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getDayCount().isPresent()).isFalse();
    assertThat(test.getParameters().isPresent()).isFalse();
  }

  public void test_of_String() {
    SurfaceMetadata test = SurfaceMetadata.of(NAME, METADATA);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getParameters().isPresent()).isTrue();
    assertThat(test.getParameters().get()).isEqualTo(METADATA);
  }

  public void test_of_SurfaceName() {
    SurfaceMetadata test = SurfaceMetadata.of(SURFACE_NAME, METADATA);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getDayCount().isPresent()).isFalse();
    assertThat(test.getParameters().get()).isEqualTo(METADATA);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SurfaceMetadata test1 = SurfaceMetadata.of("TestSurface1");
    coverImmutableBean(test1);
    SurfaceMetadata test2 = SurfaceMetadata.of(SURFACE_NAME, METADATA);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    SurfaceMetadata test = SurfaceMetadata.of(NAME, METADATA);
    assertSerialization(test);
  }

}
