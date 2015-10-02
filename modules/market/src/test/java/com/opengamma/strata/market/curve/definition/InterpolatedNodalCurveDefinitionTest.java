/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.definition;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
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
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.rate.fra.FraTemplate;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.market.value.ValueType;
import com.opengamma.strata.math.impl.interpolation.Interpolator1DFactory;

/**
 * Test {@link InterpolatedNodalCurveDefinition}.
 */
@Test
public class InterpolatedNodalCurveDefinitionTest {

  private static final LocalDate VAL_DATE = date(2015, 9, 9);
  private static final LocalDate DATE1 = GBLO.nextOrSame(VAL_DATE.plusMonths(2));
  private static final LocalDate DATE2 = GBLO.nextOrSame(VAL_DATE.plusMonths(4));
  private static final CurveName CURVE_NAME = CurveName.of("Test");
  private static final ImmutableList<FraCurveNode> NODES = ImmutableList.of(
      FraCurveNode.of(
          FraTemplate.of(Period.ofMonths(1), GBP_LIBOR_1M),
          QuoteKey.of(StandardId.of("OG", "Ticker"))),
      FraCurveNode.of(
          FraTemplate.of(Period.ofMonths(3), GBP_LIBOR_1M),
          QuoteKey.of(StandardId.of("OG", "Ticker"))));

  public void test_builder() {
    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(NODES)
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
    assertEquals(test.getParameterCount(), 2);
  }

  //-------------------------------------------------------------------------
  public void test_createMetadata() {
    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(NODES)
        .interpolator(Interpolator1DFactory.LINEAR_INSTANCE)
        .extrapolatorLeft(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
        .extrapolatorRight(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
        .build();
    DefaultCurveMetadata expected = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .parameterMetadata(NODES.get(0).metadata(VAL_DATE), NODES.get(1).metadata(VAL_DATE))
        .build();
    assertEquals(test.metadata(VAL_DATE), expected);
  }

  //-------------------------------------------------------------------------
  public void test_createCurve() {
    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(NODES)
        .interpolator(Interpolator1DFactory.LINEAR_INSTANCE)
        .extrapolatorLeft(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
        .extrapolatorRight(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
        .build();
    DefaultCurveMetadata metadata = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .parameterMetadata(NODES.get(0).metadata(VAL_DATE), NODES.get(1).metadata(VAL_DATE))
        .build();
    InterpolatedNodalCurve expected = InterpolatedNodalCurve.builder()
        .metadata(metadata)
        .xValues(new double[] {ACT_365F.yearFraction(VAL_DATE, DATE1), ACT_365F.yearFraction(VAL_DATE, DATE2)})
        .yValues(new double[] {1d, 1.5d})
        .interpolator(Interpolator1DFactory.LINEAR_INSTANCE)
        .extrapolatorLeft(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
        .extrapolatorRight(Interpolator1DFactory.FLAT_EXTRAPOLATOR_INSTANCE)
        .build();
    assertEquals(test.curve(VAL_DATE, new double[] {1d, 1.5d}), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
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
    InterpolatedNodalCurveDefinition test2 = InterpolatedNodalCurveDefinition.builder()
        .name(CurveName.of("Foo"))
        .nodes(NODES.get(0))
        .interpolator(Interpolator1DFactory.LOG_LINEAR_INSTANCE)
        .extrapolatorLeft(Interpolator1DFactory.EXPONENTIAL_EXTRAPOLATOR_INSTANCE)
        .extrapolatorRight(Interpolator1DFactory.EXPONENTIAL_EXTRAPOLATOR_INSTANCE)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
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
