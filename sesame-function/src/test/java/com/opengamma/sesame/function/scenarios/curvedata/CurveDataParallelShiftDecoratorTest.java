/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Map;

import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.opengamma.financial.analytics.curve.CurveSpecification;
import com.opengamma.financial.currency.CurrencyMatrix;
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
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
@SuppressWarnings("unchecked")
public class CurveDataParallelShiftDecoratorTest {

  private static final MarketDataSource MARKET_DATA_SOURCE =
      MapMarketDataSource.builder()
          .add(CurveTestUtils.ID1, 0.1)
          .add(CurveTestUtils.ID2, 0.2)
          .add(CurveTestUtils.ID3, 0.7)
          .add(CurveTestUtils.ID4, 0.4)
          .build();

  private static final FunctionModelConfig CONFIG =
      config(implementations(Fn.class, Impl.class,
                             MarketDataFn.class, DefaultMarketDataFn.class,
                             CurrencyMatrix.class, SimpleCurrencyMatrix.class,
                             CurveSpecificationMarketDataFn.class, DefaultCurveSpecificationMarketDataFn.class));
  private static final FunctionModelConfig DECORATED_CONFIG =
      DecoratorConfig.decorate(CONFIG, CurveDataParallelShiftDecorator.class);
  private static final Fn FN = FunctionModel.build(Fn.class, DECORATED_CONFIG);

  private static final CurveSpecificationMatcher MATCHER = CurveSpecificationMatcher.named(CurveTestUtils.CURVE_NAME);

  @Test
  public void absolute() {
    CurveDataParallelShift shift = CurveDataParallelShift.absolute(0.1, MATCHER);
    CurveDataParallelShiftDecorator.Shifts shifts = CurveDataParallelShiftDecorator.shifts(shift);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataParallelShiftDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CurveTestUtils.CURVE_SPEC);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    CurveTestUtils.checkValues(shiftedValues, 0.2, 0.3, 0.6, 0.5);
  }

  @Test
  public void relative() {
    CurveDataParallelShift shift = CurveDataParallelShift.relative(0.1, MATCHER);
    CurveDataParallelShiftDecorator.Shifts shifts = CurveDataParallelShiftDecorator.shifts(shift);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataParallelShiftDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CurveTestUtils.CURVE_SPEC);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    CurveTestUtils.checkValues(shiftedValues, 0.11, 0.22, 0.67, 0.44);
  }

  @Test
  public void noMatch() {
    CurveDataParallelShift shift = CurveDataParallelShift.absolute(0.1, MATCHER);
    CurveDataParallelShiftDecorator.Shifts shifts = CurveDataParallelShiftDecorator.shifts(shift);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataParallelShiftDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    CurveSpecification curveSpec = new CurveSpecification(LocalDate.now(), "a different name", CurveTestUtils.NODES);
    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, curveSpec);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    CurveTestUtils.checkValues(shiftedValues, 0.1, 0.2, 0.7, 0.4);
  }

  @Test
  public void multipleSameType() {
    CurveDataParallelShift shift1 = CurveDataParallelShift.absolute(0.1, MATCHER);
    CurveDataParallelShift shift2 = CurveDataParallelShift.absolute(0.2, MATCHER);
    CurveDataParallelShiftDecorator.Shifts shifts = CurveDataParallelShiftDecorator.shifts(shift1, shift2);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataParallelShiftDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CurveTestUtils.CURVE_SPEC);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    CurveTestUtils.checkValues(shiftedValues, 0.4, 0.5, 0.4, 0.7);
  }

  @Test
  public void multipleDifferentTypes() {
    CurveDataParallelShift abs = CurveDataParallelShift.absolute(0.1, MATCHER);
    CurveDataParallelShift rel = CurveDataParallelShift.relative(0.1, MATCHER);
    CurveDataParallelShiftDecorator.Shifts shifts = CurveDataParallelShiftDecorator.shifts(abs, rel);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataParallelShiftDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CurveTestUtils.CURVE_SPEC);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    CurveTestUtils.checkValues(shiftedValues, 0.22, 0.33, 0.56, 0.55);
  }

  @Test
  public void multipleReversed() {
    CurveDataParallelShift rel = CurveDataParallelShift.relative(0.1, MATCHER);
    CurveDataParallelShift abs = CurveDataParallelShift.absolute(0.1, MATCHER);
    CurveDataParallelShiftDecorator.Shifts shifts = CurveDataParallelShiftDecorator.shifts(rel, abs);
    Map<Class<?>, Object> scenarioArgs = ImmutableMap.<Class<?>, Object>of(CurveDataParallelShiftDecorator.class, shifts);
    SimpleEnvironment env = new SimpleEnvironment(ZonedDateTime.now(), MARKET_DATA_SOURCE, scenarioArgs);

    Result<Map<ExternalIdBundle, Double>> result = FN.foo(env, CurveTestUtils.CURVE_SPEC);
    assertTrue(result.isSuccess());
    Map<ExternalIdBundle, Double> shiftedValues = result.getValue();
    CurveTestUtils.checkValues(shiftedValues, 0.21, 0.32, 0.57, 0.54);
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
