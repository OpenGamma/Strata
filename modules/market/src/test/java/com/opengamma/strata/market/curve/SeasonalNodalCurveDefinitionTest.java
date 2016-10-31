/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.product.swap.type.FixedInflationSwapConventions.GBP_FIXED_ZC_GB_RPI;
import static org.testng.Assert.assertEquals;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.function.DoubleBinaryOperator;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.FixedInflationSwapCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.product.swap.type.FixedInflationSwapTemplate;

/**
 * Test {@link SeasonalNodalCurveDefinition}.
 */
@Test
public class SeasonalNodalCurveDefinitionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 9, 9);
  
  private static final CurveName CURVE_NAME = CurveName.of("Test");
  private static final QuoteId TICKER = QuoteId.of(StandardId.of("OG", "Ticker"));
  private static final ImmutableList<FixedInflationSwapCurveNode> NODES = ImmutableList.of(
      FixedInflationSwapCurveNode.of(FixedInflationSwapTemplate.of(Tenor.TENOR_1Y, GBP_FIXED_ZC_GB_RPI), TICKER),
      FixedInflationSwapCurveNode.of(FixedInflationSwapTemplate.of(Tenor.TENOR_2Y, GBP_FIXED_ZC_GB_RPI), TICKER));
  private static final InterpolatedNodalCurveDefinition UNDERLYING_DEF = InterpolatedNodalCurveDefinition.builder()
      .name(CURVE_NAME)
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.PRICE_INDEX)
      .dayCount(ACT_365F)
      .nodes(NODES)
      .interpolator(CurveInterpolators.LOG_LINEAR)
      .extrapolatorLeft(CurveExtrapolators.FLAT)
      .extrapolatorRight(CurveExtrapolators.FLAT)
      .build();  
  private static final DoubleArray SEASONALITY_ADDITIVE = DoubleArray.of(
          1.0, 1.5, 1.0, -0.5,
          -0.5, -1.0, -1.5, 0.0,
          0.5, 1.0, 1.0, -2.5);
  private static final YearMonth LAST_FIX_MONTH = YearMonth.of(2015, 7);
  private static final double LAST_FIX_VALUE = 240.0d;

  public void test_builder() {
    SeasonalNodalCurveDefinition test = SeasonalNodalCurveDefinition.builder()
        .curveWithoutFixingDefinition(UNDERLYING_DEF)
        .lastFixingMonth(LAST_FIX_MONTH)
        .lastFixingValue(LAST_FIX_VALUE)
        .seasonalityMonthOnMonth(SEASONALITY_ADDITIVE)
        .adjustmentFunction((v, a) -> v * a)
        .adjustmentDerivative((v, a) -> a).build();
    assertEquals(test.getCurveWithoutFixingDefinition(), UNDERLYING_DEF);
    assertEquals(test.getLastFixingMonth(), LAST_FIX_MONTH);
    assertEquals(test.getLastFixingValue(), LAST_FIX_VALUE);
    assertEquals(test.getSeasonalityMonthOnMonth(), SEASONALITY_ADDITIVE);
  }

  //-------------------------------------------------------------------------
  public void test_metadata() {
    SeasonalNodalCurveDefinition test = SeasonalNodalCurveDefinition.builder()
        .curveWithoutFixingDefinition(UNDERLYING_DEF)
        .lastFixingMonth(LAST_FIX_MONTH)
        .lastFixingValue(LAST_FIX_VALUE)
        .seasonalityMonthOnMonth(SEASONALITY_ADDITIVE)
        .adjustmentFunction((v, a) -> v * a)
        .adjustmentDerivative((v, a) -> a).build();
    DefaultCurveMetadata expected = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.PRICE_INDEX)
        .dayCount(ACT_365F)
        .parameterMetadata(NODES.get(0).metadata(VAL_DATE, REF_DATA), NODES.get(1).metadata(VAL_DATE, REF_DATA))
        .build();
    assertEquals(test.metadata(VAL_DATE, REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  public void test_curve() {
    DoubleBinaryOperator mult = (v, a) -> v * a;
    DoubleBinaryOperator deriv = (v, a) -> a;
    SeasonalNodalCurveDefinition test = SeasonalNodalCurveDefinition.builder()
        .curveWithoutFixingDefinition(UNDERLYING_DEF)
        .lastFixingMonth(LAST_FIX_MONTH)
        .lastFixingValue(LAST_FIX_VALUE)
        .seasonalityMonthOnMonth(SEASONALITY_ADDITIVE)
        .adjustmentFunction(mult)
        .adjustmentDerivative(deriv).build();
    DefaultCurveMetadata metadata = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.PRICE_INDEX)
        .dayCount(ACT_365F)
        .parameterMetadata(NODES.get(0).metadata(VAL_DATE, REF_DATA), NODES.get(1).metadata(VAL_DATE, REF_DATA))
        .build();
    LocalDate date0 = NODES.get(0).date(VAL_DATE, REF_DATA);
    LocalDate date1 = NODES.get(1).date(VAL_DATE, REF_DATA);
    DoubleArray param = DoubleArray.of(250.0d, 260.0d);
    InterpolatedNodalCurve expectedUnderlying = InterpolatedNodalCurve.builder()
        .metadata(metadata)
        .xValues(DoubleArray.of(ACT_365F.yearFraction(VAL_DATE, date0), ACT_365F.yearFraction(VAL_DATE, date1)))
        .yValues(param)
        .interpolator(CurveInterpolators.LOG_LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    SeasonalNodalCurve expected = SeasonalNodalCurve
        .of(expectedUnderlying, VAL_DATE, LAST_FIX_MONTH, LAST_FIX_VALUE, SEASONALITY_ADDITIVE, mult, deriv);
    assertEquals(test.curve(VAL_DATE, metadata, param), expected);
  }

  //-------------------------------------------------------------------------
  public void test_toCurveParameterSize() {
    DoubleBinaryOperator mult = (v, a) -> v * a;
    DoubleBinaryOperator deriv = (v, a) -> a;
    SeasonalNodalCurveDefinition test = SeasonalNodalCurveDefinition.builder()
        .curveWithoutFixingDefinition(UNDERLYING_DEF)
        .lastFixingMonth(LAST_FIX_MONTH)
        .lastFixingValue(LAST_FIX_VALUE)
        .seasonalityMonthOnMonth(SEASONALITY_ADDITIVE)
        .adjustmentFunction(mult)
        .adjustmentDerivative(deriv).build();
    assertEquals(test.toCurveParameterSize(), CurveParameterSize.of(CURVE_NAME, NODES.size()));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DoubleBinaryOperator mult = (v, a) -> v * a;
    DoubleBinaryOperator deriv = (v, a) -> a;
    SeasonalNodalCurveDefinition test = SeasonalNodalCurveDefinition.builder()
        .curveWithoutFixingDefinition(UNDERLYING_DEF)
        .lastFixingMonth(LAST_FIX_MONTH)
        .lastFixingValue(LAST_FIX_VALUE)
        .seasonalityMonthOnMonth(SEASONALITY_ADDITIVE)
        .adjustmentFunction(mult)
        .adjustmentDerivative(deriv).build();
    coverImmutableBean(test);
    DoubleBinaryOperator add = (v, a) -> v + a;
    DoubleBinaryOperator derivAdd = (v, a) -> 1.0d;
    InterpolatedNodalCurveDefinition underlyingDef2 = InterpolatedNodalCurveDefinition.builder()
        .name(CurveName.of("foo"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(NODES)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    SeasonalNodalCurveDefinition test2 = SeasonalNodalCurveDefinition.builder()
        .curveWithoutFixingDefinition(underlyingDef2)
        .lastFixingMonth(LAST_FIX_MONTH.plus(Period.ofMonths(1)))
        .lastFixingValue(LAST_FIX_VALUE + 1.0d)
        .seasonalityMonthOnMonth(SEASONALITY_ADDITIVE.multipliedBy(2.0d))
        .adjustmentFunction(add)
        .adjustmentDerivative(derivAdd).build();
    coverBeanEquals(test, test2);
  }

//  public void test_serialization() {
//    DoubleBinaryOperator mult = (v, a) -> v * a;
//    DoubleBinaryOperator deriv = (v, a) -> a;
//    SeasonalNodalCurveDefinition test = SeasonalNodalCurveDefinition.builder()
//        .curveWithoutFixingDefinition(UNDERLYING_DEF)
//        .lastFixingMonth(LAST_FIX_MONTH)
//        .lastFixingValue(LAST_FIX_VALUE)
//        .seasonalityMonthOnMonth(SEASONALITY_ADDITIVE)
//        .adjustmentFunction(mult)
//        .adjustmentDerivative(deriv).build();
//    assertSerialization(test);
//  }
  
}
