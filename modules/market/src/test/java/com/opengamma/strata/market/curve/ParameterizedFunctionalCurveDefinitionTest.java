/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.curve.CurveNodeClashAction.DROP_OTHER;
import static com.opengamma.strata.market.curve.CurveNodeClashAction.DROP_THIS;
import static com.opengamma.strata.market.curve.CurveNodeClashAction.EXCEPTION;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorParameterMetadata;

/**
 * Test {@link ParameterizedFunctionalCurveDefinition}.
 */
@Test
public class ParameterizedFunctionalCurveDefinitionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 9, 9);
  private static final CurveName CURVE_NAME = CurveName.of("Test");
  private static final QuoteId TICKER = QuoteId.of(StandardId.of("OG", "Ticker"));
  private static final ImmutableList<DummyFraCurveNode> NODES = ImmutableList.of(
      DummyFraCurveNode.of(Period.ofMonths(1), GBP_LIBOR_1M, TICKER),
      DummyFraCurveNode.of(Period.ofMonths(3), GBP_LIBOR_1M, TICKER));
  private static final ImmutableList<DummyFraCurveNode> NODES2 = ImmutableList.of(
      DummyFraCurveNode.of(Period.ofMonths(1), GBP_LIBOR_1M, TICKER),
      DummyFraCurveNode.of(Period.ofMonths(2), GBP_LIBOR_1M, TICKER));
  private static final CurveNodeDateOrder DROP_THIS_2D = CurveNodeDateOrder.of(2, DROP_THIS);
  private static final CurveNodeDateOrder DROP_OTHER_2D = CurveNodeDateOrder.of(2, DROP_OTHER);
  private static final CurveNodeDateOrder EXCEPTION_2D = CurveNodeDateOrder.of(2, EXCEPTION);

  private static final List<Double> INITIAL_PARAMS = DoubleArray.of(1.0, 1.0, 1.0).toList();
  private static final ImmutableList<ParameterMetadata> PARAM_METADATA;
  static {
    TenorParameterMetadata param1 = TenorParameterMetadata.of(Tenor.TENOR_1Y);
    TenorParameterMetadata param2 = TenorParameterMetadata.of(Tenor.TENOR_5Y);
    TenorParameterMetadata param3 = TenorParameterMetadata.of(Tenor.TENOR_10Y);
    PARAM_METADATA = ImmutableList.of(param1, param2, param3);
  }

  private static final BiFunction<DoubleArray, Double, Double> VALUE_FUNCTION =
      new BiFunction<DoubleArray, Double, Double>() {
        @Override
        public Double apply(DoubleArray t, Double u) {
          return t.get(0) + Math.sin(t.get(1) + t.get(2) * u);
        }
      };
  private static final BiFunction<DoubleArray, Double, Double> DERIVATIVE_FUNCTION =
      new BiFunction<DoubleArray, Double, Double>() {
        @Override
        public Double apply(DoubleArray t, Double u) {
          return t.get(2) * Math.cos(t.get(1) + t.get(2) * u);
        }
      };
  private static final BiFunction<DoubleArray, Double, DoubleArray> SENSITIVITY_FUNCTION =
      new BiFunction<DoubleArray, Double, DoubleArray>() {
        @Override
        public DoubleArray apply(DoubleArray t, Double u) {
          return DoubleArray.of(1d, Math.cos(t.get(1) + t.get(2) * u),
              u * Math.cos(t.get(1) + t.get(2) * u));
        }
      };

  public void test_builder() {
    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(NODES)
        .parameterMetadata(PARAM_METADATA)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    assertEquals(test.getName(), CURVE_NAME);
    assertEquals(test.getXValueType(), ValueType.YEAR_FRACTION);
    assertEquals(test.getYValueType(), ValueType.ZERO_RATE);
    assertEquals(test.getDayCount(), Optional.of(ACT_365F));
    assertEquals(test.getNodes(), NODES);
    assertEquals(test.getValueFunction(), VALUE_FUNCTION);
    assertEquals(test.getDerivativeFunction(), DERIVATIVE_FUNCTION);
    assertEquals(test.getSensitivityFunction(), SENSITIVITY_FUNCTION);
    assertEquals(test.getInitialGuess(), INITIAL_PARAMS);
    assertEquals(test.getParameterCount(), 3);
    assertEquals(test.getParameterMetadata(), PARAM_METADATA);
  }

  public void test_builder_noParamMetadata() {
    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(NODES)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    assertEquals(test.getName(), CURVE_NAME);
    assertEquals(test.getXValueType(), ValueType.YEAR_FRACTION);
    assertEquals(test.getYValueType(), ValueType.ZERO_RATE);
    assertEquals(test.getDayCount(), Optional.of(ACT_365F));
    assertEquals(test.getNodes(), NODES);
    assertEquals(test.getValueFunction(), VALUE_FUNCTION);
    assertEquals(test.getDerivativeFunction(), DERIVATIVE_FUNCTION);
    assertEquals(test.getSensitivityFunction(), SENSITIVITY_FUNCTION);
    assertEquals(test.getInitialGuess(), INITIAL_PARAMS);
    assertEquals(test.getParameterCount(), 3);
    assertEquals(test.getParameterMetadata(), ParameterMetadata.listOfEmpty(3));
  }

  //-------------------------------------------------------------------------
  public void test_filtered_dropThis_atStart() {
    DummyFraCurveNode node1 =
        DummyFraCurveNode.of(Period.ofDays(3), GBP_LIBOR_1M, TICKER, DROP_THIS_2D);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(4), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(nodes)
        .parameterMetadata(PARAM_METADATA)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    assertEquals(test.filtered(VAL_DATE, REF_DATA).getNodes(), ImmutableList.of(node2, node3));
  }

  public void test_filtered_dropOther_atStart() {
    DummyFraCurveNode node1 =
        DummyFraCurveNode.of(Period.ofDays(3), GBP_LIBOR_1M, TICKER, DROP_OTHER_2D);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(4), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(nodes)
        .parameterMetadata(PARAM_METADATA)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    assertEquals(test.filtered(VAL_DATE, REF_DATA).getNodes(), ImmutableList.of(node1, node3));
  }

  public void test_filtered_exception_atStart() {
    DummyFraCurveNode node1 =
        DummyFraCurveNode.of(Period.ofDays(3), GBP_LIBOR_1M, TICKER, EXCEPTION_2D);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(4), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(nodes)
        .parameterMetadata(PARAM_METADATA)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    assertThrowsIllegalArg(() -> test.filtered(VAL_DATE, REF_DATA), "Curve node dates clash.*");
  }

  //-------------------------------------------------------------------------
  public void test_filtered_dropThis_middle() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(3), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node2 =
        DummyFraCurveNode.of(Period.ofDays(4), GBP_LIBOR_1M, TICKER, DROP_THIS_2D);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(nodes)
        .parameterMetadata(PARAM_METADATA)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    assertEquals(test.filtered(VAL_DATE, REF_DATA).getNodes(), ImmutableList.of(node1, node3));
  }

  public void test_filtered_dropOther_middle() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(3), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node2 =
        DummyFraCurveNode.of(Period.ofDays(4), GBP_LIBOR_1M, TICKER, DROP_OTHER_2D);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(nodes)
        .parameterMetadata(PARAM_METADATA)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    assertEquals(test.filtered(VAL_DATE, REF_DATA).getNodes(), ImmutableList.of(node2, node3));
  }

  //-------------------------------------------------------------------------
  public void test_filtered_dropThis_atEnd() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(5), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(10), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node3 =
        DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER, DROP_THIS_2D);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(nodes)
        .parameterMetadata(PARAM_METADATA)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    assertEquals(test.filtered(VAL_DATE, REF_DATA).getNodes(), ImmutableList.of(node1, node2));
  }

  public void test_filtered_dropOther_atEnd() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(5), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(10), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node3 =
        DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER, DROP_OTHER_2D);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(nodes)
        .parameterMetadata(PARAM_METADATA)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    assertEquals(test.filtered(VAL_DATE, REF_DATA).getNodes(), ImmutableList.of(node1, node3));
  }

  public void test_filtered_exception_atEnd() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(5), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(10), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node3 =
        DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER, EXCEPTION_2D);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(nodes)
        .parameterMetadata(PARAM_METADATA)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    assertThrowsIllegalArg(() -> test.filtered(VAL_DATE, REF_DATA), "Curve node dates clash.*");
  }

  //-------------------------------------------------------------------------
  public void test_filtered_dropOther_multiple() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(5), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(10), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node4 =
        DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER, DROP_OTHER_2D);
    DummyFraCurveNode node5 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node6 = DummyFraCurveNode.of(Period.ofDays(15), GBP_LIBOR_1M, TICKER);
    ImmutableList<DummyFraCurveNode> nodes =
        ImmutableList.of(node1, node2, node3, node4, node5, node6);

    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(nodes)
        .parameterMetadata(PARAM_METADATA)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    assertEquals(test.filtered(VAL_DATE, REF_DATA).getNodes(),
        ImmutableList.of(node1, node4, node6));
  }

  //-------------------------------------------------------------------------
  public void test_metadata() {
    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(NODES)
        .parameterMetadata(PARAM_METADATA)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    DefaultCurveMetadata expected = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .parameterMetadata(PARAM_METADATA)
        .build();
    assertEquals(test.metadata(VAL_DATE, REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  public void test_curve() {
    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(NODES)
        .parameterMetadata(PARAM_METADATA)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    DefaultCurveMetadata metadata = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .parameterMetadata(PARAM_METADATA)
        .build();
    DoubleArray parameters = DoubleArray.of(1d, 1.5d, -0.5d);
    ParameterizedFunctionalCurve expected = ParameterizedFunctionalCurve.builder()
        .metadata(metadata)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .parameters(parameters)
        .build();
    assertEquals(test.curve(VAL_DATE, metadata, parameters), expected);
  }

  //-------------------------------------------------------------------------
  public void test_toCurveParameterSize() {
    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(NODES)
        .parameterMetadata(PARAM_METADATA)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    assertEquals(test.toCurveParameterSize(), CurveParameterSize.of(CURVE_NAME, INITIAL_PARAMS.size()));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ParameterizedFunctionalCurveDefinition test = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(ACT_365F)
        .valueFunction(VALUE_FUNCTION)
        .derivativeFunction(DERIVATIVE_FUNCTION)
        .sensitivityFunction(SENSITIVITY_FUNCTION)
        .initialGuess(INITIAL_PARAMS)
        .name(CURVE_NAME)
        .nodes(NODES)
        .parameterMetadata(PARAM_METADATA)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .build();
    coverImmutableBean(test);
    ImmutableList<Double> initial = ImmutableList.of(12d);
    BiFunction<DoubleArray, Double, Double> value =
        new BiFunction<DoubleArray, Double, Double>() {
          @Override
          public Double apply(DoubleArray t, Double u) {
            return t.get(0) * u;
          }
        };
    BiFunction<DoubleArray, Double, Double> deriv =
        new BiFunction<DoubleArray, Double, Double>() {
          @Override
          public Double apply(DoubleArray t, Double u) {
            return t.get(0);
          }
        };
    BiFunction<DoubleArray, Double, DoubleArray> sensi =
        new BiFunction<DoubleArray, Double, DoubleArray>() {
          @Override
          public DoubleArray apply(DoubleArray t, Double u) {
            return DoubleArray.of(u);
          }
        };
    ParameterizedFunctionalCurveDefinition test2 = ParameterizedFunctionalCurveDefinition.builder()
        .dayCount(DayCounts.ACT_365L)
        .valueFunction(value)
        .derivativeFunction(deriv)
        .sensitivityFunction(sensi)
        .initialGuess(initial)
        .name(CURVE_NAME)
        .nodes(NODES2)
        .xValueType(ValueType.MONTHS)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .build();
    coverBeanEquals(test, test2);
  }

}
