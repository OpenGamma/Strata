/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LOG_LINEAR;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Tests {@link SabrSwaptionDefinition}.
 */
public class SabrSwaptionDefinitionTest {

  private static final SwaptionVolatilitiesName NAME = SwaptionVolatilitiesName.of("Test");
  private static final SwaptionVolatilitiesName NAME2 = SwaptionVolatilitiesName.of("Test2");
  private static final FixedIborSwapConvention CONVENTION = FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M;
  private static final FixedIborSwapConvention CONVENTION2 = FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;
  private static final DayCount DAY_COUNT = DayCounts.ACT_360;
  private static final DayCount DAY_COUNT2 = DayCounts.ACT_365F;
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final SurfaceInterpolator INTERPOLATOR_2D2 = GridSurfaceInterpolator.of(LINEAR, LOG_LINEAR);

  //-------------------------------------------------------------------------
  @Test
  public void of() {
    SabrSwaptionDefinition test = SabrSwaptionDefinition.of(NAME, CONVENTION, DAY_COUNT, INTERPOLATOR_2D);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getConvention()).isEqualTo(CONVENTION);
    assertThat(test.getDayCount()).isEqualTo(DAY_COUNT);
    assertThat(test.getInterpolator()).isEqualTo(INTERPOLATOR_2D);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SabrSwaptionDefinition test = SabrSwaptionDefinition.of(NAME, CONVENTION, DAY_COUNT, INTERPOLATOR_2D);
    coverImmutableBean(test);
    SabrSwaptionDefinition test2 = SabrSwaptionDefinition.of(NAME2, CONVENTION2, DAY_COUNT2, INTERPOLATOR_2D2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    SabrSwaptionDefinition test = SabrSwaptionDefinition.of(NAME, CONVENTION, DAY_COUNT, INTERPOLATOR_2D);
    assertSerialization(test);
  }

}
