/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
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
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.observable.QuoteId;

/**
 * Test {@link InterpolatedNodalCurveDefinition}.
 */
public class InterpolatedNodalCurveDefinitionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 9, 9);
  private static final LocalDate DATE1 = GBLO.resolve(REF_DATA).nextOrSame(VAL_DATE.plusMonths(2));
  private static final LocalDate DATE2 = GBLO.resolve(REF_DATA).nextOrSame(VAL_DATE.plusMonths(4));
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

  @Test
  public void test_builder() {
    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(NODES)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getXValueType()).isEqualTo(ValueType.YEAR_FRACTION);
    assertThat(test.getYValueType()).isEqualTo(ValueType.ZERO_RATE);
    assertThat(test.getDayCount()).isEqualTo(Optional.of(ACT_365F));
    assertThat(test.getNodes()).isEqualTo(NODES);
    assertThat(test.getInterpolator()).isEqualTo(CurveInterpolators.LINEAR);
    assertThat(test.getExtrapolatorLeft()).isEqualTo(CurveExtrapolators.FLAT);
    assertThat(test.getExtrapolatorRight()).isEqualTo(CurveExtrapolators.FLAT);
    assertThat(test.getParameterCount()).isEqualTo(2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_filtered_dropThis_atStart() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(3), GBP_LIBOR_1M, TICKER, DROP_THIS_2D);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(4), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(nodes)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    assertThat(test.filtered(VAL_DATE, REF_DATA).getNodes()).containsExactly(node2, node3);
  }

  @Test
  public void test_filtered_dropOther_atStart() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(3), GBP_LIBOR_1M, TICKER, DROP_OTHER_2D);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(4), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(nodes)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    assertThat(test.filtered(VAL_DATE, REF_DATA).getNodes()).containsExactly(node1, node3);
  }

  @Test
  public void test_filtered_exception_atStart() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(3), GBP_LIBOR_1M, TICKER, EXCEPTION_2D);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(4), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(nodes)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.filtered(VAL_DATE, REF_DATA))
        .withMessageStartingWith("Curve node dates clash");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_filtered_dropThis_middle() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(3), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(4), GBP_LIBOR_1M, TICKER, DROP_THIS_2D);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(nodes)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    assertThat(test.filtered(VAL_DATE, REF_DATA).getNodes()).containsExactly(node1, node3);
  }

  @Test
  public void test_filtered_dropOther_middle() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(3), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(4), GBP_LIBOR_1M, TICKER, DROP_OTHER_2D);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(nodes)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    assertThat(test.filtered(VAL_DATE, REF_DATA).getNodes()).containsExactly(node2, node3);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_filtered_dropThis_atEnd() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(5), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(10), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER, DROP_THIS_2D);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(nodes)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    assertThat(test.filtered(VAL_DATE, REF_DATA).getNodes()).containsExactly(node1, node2);
  }

  @Test
  public void test_filtered_dropOther_atEnd() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(5), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(10), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER, DROP_OTHER_2D);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(nodes)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    assertThat(test.filtered(VAL_DATE, REF_DATA).getNodes()).containsExactly(node1, node3);
  }

  @Test
  public void test_filtered_exception_atEnd() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(5), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(10), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER, EXCEPTION_2D);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);

    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(nodes)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
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
    DummyFraCurveNode node4 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER, DROP_OTHER_2D);
    DummyFraCurveNode node5 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, TICKER);
    DummyFraCurveNode node6 = DummyFraCurveNode.of(Period.ofDays(15), GBP_LIBOR_1M, TICKER);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3, node4, node5, node6);

    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(nodes)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    assertThat(test.filtered(VAL_DATE, REF_DATA).getNodes()).containsExactly(node1, node4, node6);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_metadata() {
    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(NODES)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    DefaultCurveMetadata expected = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .parameterMetadata(NODES.get(0).metadata(VAL_DATE, REF_DATA), NODES.get(1).metadata(VAL_DATE, REF_DATA))
        .build();
    assertThat(test.metadata(VAL_DATE, REF_DATA)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_curve() {
    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(NODES)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    DefaultCurveMetadata metadata = DefaultCurveMetadata.builder()
        .curveName(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .parameterMetadata(NODES.get(0).metadata(VAL_DATE, REF_DATA), NODES.get(1).metadata(VAL_DATE, REF_DATA))
        .build();
    InterpolatedNodalCurve expected = InterpolatedNodalCurve.builder()
        .metadata(metadata)
        .xValues(DoubleArray.of(ACT_365F.yearFraction(VAL_DATE, DATE1), ACT_365F.yearFraction(VAL_DATE, DATE2)))
        .yValues(DoubleArray.of(1d, 1.5d))
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    assertThat(test.curve(VAL_DATE, metadata, DoubleArray.of(1d, 1.5d))).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toCurveParameterSize() {
    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(NODES)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    assertThat(test.toCurveParameterSize()).isEqualTo(CurveParameterSize.of(CURVE_NAME, NODES.size()));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(NODES)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    coverImmutableBean(test);
    InterpolatedNodalCurveDefinition test2 = InterpolatedNodalCurveDefinition.builder()
        .name(CurveName.of("Foo"))
        .nodes(NODES2)
        .interpolator(CurveInterpolators.LOG_LINEAR)
        .extrapolatorLeft(CurveExtrapolators.LOG_LINEAR)
        .extrapolatorRight(CurveExtrapolators.LOG_LINEAR)
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    InterpolatedNodalCurveDefinition test = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(NODES)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    assertSerialization(test);
  }

}
