/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static com.opengamma.sesame.function.scenarios.curvedata.CurveDataPointShifts.PointShift;
import static com.opengamma.sesame.function.scenarios.curvedata.CurveDataPointShiftsDecorator.Shifts;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.currency.SimpleCurrencyMatrix;
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
import com.opengamma.sesame.marketdata.MapMarketDataSource;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.Tenor;

@Test(groups = TestGroup.UNIT)
public class CurveDataPointShiftsDecoratorTest {

  private static final MarketDataSource MARKET_DATA_SOURCE =
      MapMarketDataSource.builder()
          .add(CurveTestUtils.ID1, 0.1)
          .add(CurveTestUtils.ID2, 0.2)
          .add(CurveTestUtils.ID3, 0.7)
          .add(CurveTestUtils.ID4, 0.4)
          .build();

  private static final List<PointShift> SHIFT_LIST = Lists.newArrayList(PointShift.of(Tenor.ofMonths(3), 0.1),
                                                                        PointShift.of(Tenor.ofMonths(6), 0.2),
                                                                        PointShift.of(Tenor.ofMonths(9), 0.3),
                                                                        PointShift.of(Tenor.ofYears(1), 0.4));

  private static final CurveSpecificationMatcher MATCHER = CurveSpecificationMatcher.named(CurveTestUtils.CURVE_NAME);

  private static final FunctionModelConfig CONFIG =
      config(implementations(Fn.class, Impl.class,
                             MarketDataFn.class, DefaultMarketDataFn.class,
                             CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class),
             arguments(function(DefaultMarketDataFn.class, argument("currencyMatrix", new SimpleCurrencyMatrix()))));
  private static final FunctionModelConfig DECORATED_CONFIG =
      DecoratorConfig.decorate(CONFIG, CurveDataPointShiftsDecorator.class);
  private static final Fn FN = FunctionModel.build(Fn.class, DECORATED_CONFIG);

  @Test
  public void absolute() {
    CurveDataPointShifts pointShifts = CurveDataPointShifts.absolute(MATCHER, SHIFT_LIST);
    CurveDataPointShiftsDecorator.Shifts shifts = CurveDataPointShiftsDecorator.shifts(pointShifts);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataPointShiftsDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CurveTestUtils.CURVE_SPEC);
    assertTrue(result.isSuccess());
    CurveTestUtils.checkValues(result.getValue(), 0.2, 0.4, 0.4, 0.8);
  }

  @Test
  public void relative() {
    CurveDataPointShifts pointShifts = CurveDataPointShifts.relative(MATCHER, SHIFT_LIST);
    Shifts shifts = CurveDataPointShiftsDecorator.shifts(pointShifts);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataPointShiftsDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CurveTestUtils.CURVE_SPEC);
    assertTrue(result.isSuccess());
    CurveTestUtils.checkValues(result.getValue(), 0.11, 0.24, 0.61, 0.56);
  }

  @Test
  public void noMatch() {
    CurveDataPointShifts pointShifts = CurveDataPointShifts.absolute(MATCHER, SHIFT_LIST);
    Shifts shifts = CurveDataPointShiftsDecorator.shifts(pointShifts);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataPointShiftsDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    CurveSpecification curveSpec = new CurveSpecification(LocalDate.now(), "a different name", CurveTestUtils.NODES);
    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, curveSpec);
    assertTrue(result.isSuccess());
    CurveTestUtils.checkValues(result.getValue(), 0.1, 0.2, 0.7, 0.4);
  }

  @Test
  public void multipleSameType() {
    CurveDataPointShifts pointShifts1 = CurveDataPointShifts.absolute(MATCHER, SHIFT_LIST);
    CurveDataPointShifts pointShifts2 = CurveDataPointShifts.absolute(MATCHER, SHIFT_LIST);
    Shifts shifts = CurveDataPointShiftsDecorator.shifts(pointShifts1, pointShifts2);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataPointShiftsDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CurveTestUtils.CURVE_SPEC);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    CurveTestUtils.checkValues(shiftedValues, 0.3, 0.6, 0.1, 1.2);
  }

  @Test
  public void multipleDifferentTypes() {
    CurveDataPointShifts abs = CurveDataPointShifts.absolute(MATCHER, SHIFT_LIST);
    CurveDataPointShifts rel = CurveDataPointShifts.relative(MATCHER, SHIFT_LIST);
    Shifts shifts = CurveDataPointShiftsDecorator.shifts(abs, rel);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataPointShiftsDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CurveTestUtils.CURVE_SPEC);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    CurveTestUtils.checkValues(shiftedValues, 0.22, 0.48, 0.22, 1.12);
  }

  @Test
  public void multipleReversed() {
    CurveDataPointShifts rel = CurveDataPointShifts.relative(MATCHER, SHIFT_LIST);
    CurveDataPointShifts abs = CurveDataPointShifts.absolute(MATCHER, SHIFT_LIST);
    Shifts shifts = CurveDataPointShiftsDecorator.shifts(rel, abs);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataPointShiftsDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CurveTestUtils.CURVE_SPEC);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    CurveTestUtils.checkValues(shiftedValues, 0.21, 0.44, 0.31, 0.96);
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
