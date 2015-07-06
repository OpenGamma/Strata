/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.analytics.financial.model.volatility.surface.Delta;
import com.opengamma.strata.basics.currency.CurrencyPair;

/**
 * Test {@link DefaultSurfaceMetadata}.
 */
@Test
public class DefaultSurfaceMetadataTest {

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
  public void test_of_String_noDayCountParameterMetadata() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(NAME);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getDayCount().isPresent()).isFalse();
    assertThat(test.getParameters().isPresent()).isFalse();
  }

  public void test_of_SurfaceName_noDayCountParameterMetadata() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(SURFACE_NAME);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getDayCount().isPresent()).isFalse();
    assertThat(test.getParameters().isPresent()).isFalse();
  }

  public void test_of_String_noParameterMetadata() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(NAME, ACT_365F);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getDayCount().get()).isEqualTo(ACT_365F);
    assertThat(test.getParameters().isPresent()).isFalse();
  }

  public void test_of_SurfaceName_noParameterMetadata() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(SURFACE_NAME, ACT_365F);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getDayCount().get()).isEqualTo(ACT_365F);
    assertThat(test.getParameters().isPresent()).isFalse();
  }

  public void test_of_SurfaceName_noDayCount() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(SURFACE_NAME, METADATA);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getDayCount().isPresent()).isFalse();
    assertThat(test.getParameters().get()).isEqualTo(METADATA);
  }

  public void test_of_SurfaceName() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(SURFACE_NAME, ACT_365F, METADATA);
    assertThat(test.getSurfaceName()).isEqualTo(SURFACE_NAME);
    assertThat(test.getDayCount().get()).isEqualTo(ACT_365F);
    assertThat(test.getParameters().get()).isEqualTo(METADATA);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DefaultSurfaceMetadata test1 = DefaultSurfaceMetadata.of("TestSurface1");
    coverImmutableBean(test1);
    DefaultSurfaceMetadata test2 = DefaultSurfaceMetadata.of(SURFACE_NAME, ACT_365F, METADATA);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    DefaultSurfaceMetadata test = DefaultSurfaceMetadata.of(SURFACE_NAME, METADATA);
    assertSerialization(test);
  }
}
