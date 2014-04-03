/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.analytics.ircurve.strips.CurveNode;
import com.opengamma.financial.analytics.ircurve.strips.CurveNodeWithIdentifier;
import com.opengamma.financial.analytics.ircurve.strips.DataFieldType;
import com.opengamma.financial.analytics.ircurve.strips.RateFutureNode;
import com.opengamma.financial.analytics.ircurve.strips.SwapNode;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.financial.currency.SimpleCurrencyMatrix;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.CurveSpecificationMarketDataFn;
import com.opengamma.sesame.DefaultCurveSpecificationMarketDataFn;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.SimpleEnvironment;
import com.opengamma.sesame.config.DecoratorConfig;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.marketdata.DefaultMarketDataFn;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.Tenor;

@Test(groups = TestGroup.UNIT)
@SuppressWarnings("unchecked")
public class CurveDataParallelShiftDecoratorTest {

  private static final ExternalId ID1 = ExternalId.of("scheme", "1");
  private static final ExternalId ID2 = ExternalId.of("scheme", "2");
  private static final ExternalId ID3 = ExternalId.of("scheme", "3");
  private static final ExternalId ID4 = ExternalId.of("scheme", "4");
  private static final MarketDataSource MARKET_DATA_SOURCE = mock(MarketDataSource.class);
  private static final FieldName FIELD_NAME = FieldName.of(MarketDataRequirementNames.MARKET_VALUE);
  private static final FunctionModelConfig CONFIG =
      config(implementations(Fn.class, Impl.class,
                             MarketDataFn.class, DefaultMarketDataFn.class,
                             CurrencyMatrix.class, SimpleCurrencyMatrix.class,
                             CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class));
  private static final FunctionModelConfig DECORATED_CONFIG =
      DecoratorConfig.decorate(CONFIG, CurveDataParallelShiftDecorator.class);
  private static final Fn FN = FunctionModel.build(Fn.class, DECORATED_CONFIG);
  private static final String CURVE_NAME = "curveName";
  private static final CurveNode NODE1 = node();
  private static final CurveNode NODE2 = node();
  private static final CurveNode NODE3 = futureNode();
  private static final CurveNode NODE4 = node();
  private static final List<CurveNodeWithIdentifier> NODES = Lists.newArrayList(nodeWithId(ID1, NODE1),
                                                                                nodeWithId(ID2, NODE2),
                                                                                nodeWithId(ID3, NODE3),
                                                                                nodeWithId(ID4, NODE4));
  private static final CurveSpecification CURVE_SPEC = new CurveSpecification(LocalDate.now(), CURVE_NAME, NODES);
  private static final double DELTA = 1e-8;

  static {
    when(MARKET_DATA_SOURCE.get(ID1.toBundle(), FIELD_NAME)).thenReturn((Result) Result.success(0.1));
    when(MARKET_DATA_SOURCE.get(ID2.toBundle(), FIELD_NAME)).thenReturn((Result) Result.success(0.2));
    when(MARKET_DATA_SOURCE.get(ID3.toBundle(), FIELD_NAME)).thenReturn((Result) Result.success(0.7));
    when(MARKET_DATA_SOURCE.get(ID4.toBundle(), FIELD_NAME)).thenReturn((Result) Result.success(0.4));
  }

  private static CurveNodeWithIdentifier nodeWithId(ExternalId id, CurveNode node) {
    return new CurveNodeWithIdentifier(node, id, FIELD_NAME.getName(), DataFieldType.OUTRIGHT);
  }

  private static CurveNode node() {
    return new SwapNode(Tenor.DAY,
                        Tenor.EIGHT_MONTHS,
                        ExternalId.of("convention", "payLeg"),
                        ExternalId.of("convention", "receiveLeg"),
                        "nodeMapper");
  }

  private static CurveNode futureNode() {
    return new RateFutureNode(1,
                              Tenor.DAY,
                              Tenor.EIGHT_MONTHS,
                              Tenor.EIGHT_YEARS,
                              ExternalId.of("convention", "foo"),
                              "nodeMapper");
  }

  @Test
  public void absolute() {
    CurveDataParallelShift shift = CurveDataParallelShift.absolute(0.1, CurveSpecificationMatcher.named(CURVE_NAME));
    CurveDataParallelShiftDecorator.Shifts shifts = CurveDataParallelShiftDecorator.shifts(shift);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataParallelShiftDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CURVE_SPEC);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    assertEquals(0.2, shiftedValues.get(ID1.toBundle()), DELTA);
    assertEquals(0.3, shiftedValues.get(ID2.toBundle()), DELTA);
    assertEquals(0.6, shiftedValues.get(ID3.toBundle()), DELTA);
    assertEquals(0.5, shiftedValues.get(ID4.toBundle()), DELTA);
  }

  @Test
  public void relative() {
    CurveDataParallelShift shift = CurveDataParallelShift.relative(0.1, CurveSpecificationMatcher.named(CURVE_NAME));
    CurveDataParallelShiftDecorator.Shifts shifts = CurveDataParallelShiftDecorator.shifts(shift);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataParallelShiftDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CURVE_SPEC);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    assertEquals(0.11, shiftedValues.get(ID1.toBundle()), DELTA);
    assertEquals(0.22, shiftedValues.get(ID2.toBundle()), DELTA);
    assertEquals(0.67, shiftedValues.get(ID3.toBundle()), DELTA);
    assertEquals(0.44, shiftedValues.get(ID4.toBundle()), DELTA);
  }

  @Test
  public void noMatch() {
    CurveDataParallelShift shift = CurveDataParallelShift.absolute(0.1, CurveSpecificationMatcher.named(CURVE_NAME));
    CurveDataParallelShiftDecorator.Shifts shifts = CurveDataParallelShiftDecorator.shifts(shift);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataParallelShiftDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    CurveSpecification curveSpec = new CurveSpecification(LocalDate.now(), "a different name", NODES);
    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, curveSpec);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    assertEquals(0.1, shiftedValues.get(ID1.toBundle()), DELTA);
    assertEquals(0.2, shiftedValues.get(ID2.toBundle()), DELTA);
    assertEquals(0.7, shiftedValues.get(ID3.toBundle()), DELTA);
    assertEquals(0.4, shiftedValues.get(ID4.toBundle()), DELTA);
  }

  @Test
  public void multipleSameType() {
    CurveDataParallelShift shift1 = CurveDataParallelShift.absolute(0.1, CurveSpecificationMatcher.named(CURVE_NAME));
    CurveDataParallelShift shift2 = CurveDataParallelShift.absolute(0.2, CurveSpecificationMatcher.named(CURVE_NAME));
    CurveDataParallelShiftDecorator.Shifts shifts = CurveDataParallelShiftDecorator.shifts(shift1, shift2);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataParallelShiftDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CURVE_SPEC);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    assertEquals(0.4, shiftedValues.get(ID1.toBundle()), DELTA);
    assertEquals(0.5, shiftedValues.get(ID2.toBundle()), DELTA);
    assertEquals(0.4, shiftedValues.get(ID3.toBundle()), DELTA);
    assertEquals(0.7, shiftedValues.get(ID4.toBundle()), DELTA);
  }

  @Test
  public void multipleDifferentTypes() {
    CurveDataParallelShift abs = CurveDataParallelShift.absolute(0.1, CurveSpecificationMatcher.named(CURVE_NAME));
    CurveDataParallelShift rel = CurveDataParallelShift.relative(0.1, CurveSpecificationMatcher.named(CURVE_NAME));
    CurveDataParallelShiftDecorator.Shifts shifts = CurveDataParallelShiftDecorator.shifts(abs, rel);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataParallelShiftDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CURVE_SPEC);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    assertEquals(0.22, shiftedValues.get(ID1.toBundle()), DELTA);
    assertEquals(0.33, shiftedValues.get(ID2.toBundle()), DELTA);
    assertEquals(0.56, shiftedValues.get(ID3.toBundle()), DELTA);
    assertEquals(0.55, shiftedValues.get(ID4.toBundle()), DELTA);
  }

  @Test
  public void multipleReversed() {
    CurveDataParallelShift rel = CurveDataParallelShift.relative(0.1, CurveSpecificationMatcher.named(CURVE_NAME));
    CurveDataParallelShift abs = CurveDataParallelShift.absolute(0.1, CurveSpecificationMatcher.named(CURVE_NAME));
    CurveDataParallelShiftDecorator.Shifts shifts = CurveDataParallelShiftDecorator.shifts(rel, abs);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataParallelShiftDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CURVE_SPEC);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    assertEquals(0.21, shiftedValues.get(ID1.toBundle()), DELTA);
    assertEquals(0.32, shiftedValues.get(ID2.toBundle()), DELTA);
    assertEquals(0.57, shiftedValues.get(ID3.toBundle()), DELTA);
    assertEquals(0.54, shiftedValues.get(ID4.toBundle()), DELTA);
  }

  public interface Fn {

    @Output("Foo")
    Result<Map<ExternalIdBundle, Double>> foo(Environment env, CurveSpecification curveSpec);
  }

  public static class Impl implements Fn {

    private final CurveSpecificationMarketDataFn _marketDataFn;

    public Impl(CurveSpecificationMarketDataFn marketDataFn) {
      _marketDataFn = marketDataFn;
    }

    @Override
    public Result<Map<ExternalIdBundle, Double>> foo(Environment env, CurveSpecification curveSpec) {
      return _marketDataFn.requestData(env, curveSpec);
    }
  }
}
