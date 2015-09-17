/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.config;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.rate.fra.FraTemplate;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.market.value.ValueType;

/**
 * Test {@link InterpolatedCurveConfig}.
 */
@Test
public class InterpolatedCurveConfigTest {

  private static final CurveName CURVE_NAME = CurveName.of("Test");
  private static final ImmutableList<FraCurveNode> NODES = ImmutableList.of(
      FraCurveNode.of(
          FraTemplate.of(Period.ofMonths(1), GBP_LIBOR_1M),
          QuoteKey.of(StandardId.of("OG", "Ticker"))));

  public void test_builder() {
    InterpolatedCurveConfig test = InterpolatedCurveConfig.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(NODES.get(0))
        .interpolator(Interpolator1DFactory.LINEAR_INSTANCE)
        .extrapolatorLeft(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
        .extrapolatorRight(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
        .build();
    assertEquals(test.getName(), CURVE_NAME);
    assertEquals(test.getXValueType(), ValueType.YEAR_FRACTION);
    assertEquals(test.getYValueType(), ValueType.ZERO_RATE);
    assertEquals(test.getDayCount(), Optional.of(ACT_365F));
    assertEquals(test.getNodes(), NODES);
    assertEquals(test.getInterpolator(), Interpolator1DFactory.LINEAR_INSTANCE);
    assertEquals(test.getExtrapolatorLeft(), Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE);
    assertEquals(test.getExtrapolatorRight(), Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE);
  }

  //-------------------------------------------------------------------------
  public void test_metadata() {
    InterpolatedCurveConfig test = InterpolatedCurveConfig.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(NODES)
        .interpolator(Interpolator1DFactory.LINEAR_INSTANCE)
        .extrapolatorLeft(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
        .extrapolatorRight(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
        .build();
    LocalDate valDate = date(2015, 9, 9);
    DefaultCurveMetadata expected = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .parameterMetadata(NODES.get(0).metadata(valDate))
        .build();
    assertEquals(test.metadata(valDate), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InterpolatedCurveConfig test = InterpolatedCurveConfig.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(NODES)
        .interpolator(Interpolator1DFactory.LINEAR_INSTANCE)
        .extrapolatorLeft(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
        .extrapolatorRight(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
        .build();
    coverImmutableBean(test);
    InterpolatedCurveConfig test2 = InterpolatedCurveConfig.builder()
        .name(CurveName.of("Foo"))
        .nodes(NODES)
        .interpolator(Interpolator1DFactory.LOG_LINEAR_INSTANCE)
        .extrapolatorLeft(Interpolator1DFactory.EXPONENTIAL_EXTRAPOLATOR_INSTANCE)
        .extrapolatorRight(Interpolator1DFactory.EXPONENTIAL_EXTRAPOLATOR_INSTANCE)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    InterpolatedCurveConfig test = InterpolatedCurveConfig.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(NODES)
        .interpolator(Interpolator1DFactory.LINEAR_INSTANCE)
        .extrapolatorLeft(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
        .extrapolatorRight(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
        .build();
    assertSerialization(test);
  }

}
