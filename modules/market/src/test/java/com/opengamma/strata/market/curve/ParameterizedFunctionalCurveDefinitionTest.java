/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.curve.CurveNodeClashAction.DROP_OTHER;
import static com.opengamma.strata.market.curve.CurveNodeClashAction.DROP_THIS;
import static com.opengamma.strata.market.curve.CurveNodeClashAction.EXCEPTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

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

  @Test
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
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getYValueType()).isEqualTo(ValueType.ZERO_RATE);
    assertThat(test.getDayCount()).isEqualTo(Optional.of(ACT_365F));
    assertThat(test.getNodes()).isEqualTo(NODES);
    assertThat(test.getValueFunction()).isEqualTo(VALUE_FUNCTION);
    assertThat(test.getDerivativeFunction()).isEqualTo(DERIVATIVE_FUNCTION);
    assertThat(test.getSensitivityFunction()).isEqualTo(SENSITIVITY_FUNCTION);
    assertThat(test.getInitialGuess()).isEqualTo(INITIAL_PARAMS);
    assertThat(test.getParameterCount()).isEqualTo(3);
    assertThat(test.getParameterMetadata()).isEqualTo(PARAM_METADATA);
  }

  @Test
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
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getYValueType()).isEqualTo(ValueType.ZERO_RATE);
    assertThat(test.getDayCount()).isEqualTo(Optional.of(ACT_365F));
    assertThat(test.getNodes()).isEqualTo(NODES);
    assertThat(test.getValueFunction()).isEqualTo(VALUE_FUNCTION);
    assertThat(test.getDerivativeFunction()).isEqualTo(DERIVATIVE_FUNCTION);
    assertThat(test.getSensitivityFunction()).isEqualTo(SENSITIVITY_FUNCTION);
    assertThat(test.getInitialGuess()).isEqualTo(INITIAL_PARAMS);
    assertThat(test.getParameterCount()).isEqualTo(3);
    assertThat(test.getParameterMetadata()).isEqualTo(ParameterMetadata.listOfEmpty(3));
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(test.filtered(VAL_DATE, REF_DATA).getNodes()).containsExactly(node2, node3);
  }

  @Test
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
    assertThat(test.filtered(VAL_DATE, REF_DATA).getNodes()).containsExactly(node1, node3);
  }

  @Test
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
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.filtered(VAL_DATE, REF_DATA))
        .withMessageStartingWith("Curve node dates clash");
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(test.filtered(VAL_DATE, REF_DATA).getNodes()).containsExactly(node1, node3);
  }

  @Test
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
    assertThat(test.filtered(VAL_DATE, REF_DATA).getNodes()).containsExactly(node2, node3);
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(test.filtered(VAL_DATE, REF_DATA).getNodes()).containsExactly(node1, node2);
  }

  @Test
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
    assertThat(test.filtered(VAL_DATE, REF_DATA).getNodes()).containsExactly(node1, node3);
  }

  @Test
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
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.filtered(VAL_DATE, REF_DATA))
        .withMessageStartingWith("Curve node dates clash");
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(test.filtered(VAL_DATE, REF_DATA).getNodes()).containsExactly(node1, node4, node6);
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(test.metadata(VAL_DATE, REF_DATA)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(test.curve(VAL_DATE, metadata, parameters)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(test.toCurveParameterSize()).isEqualTo(CurveParameterSize.of(CURVE_NAME, INITIAL_PARAMS.size()));
  }

  //-------------------------------------------------------------------------
  @Test
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
