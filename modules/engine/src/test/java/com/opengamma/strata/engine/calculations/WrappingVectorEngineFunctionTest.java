/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.date;

import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.DefaultCalculationMarketData;
import com.opengamma.strata.engine.marketdata.ScenarioMarketData;
import com.opengamma.strata.engine.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.marketdata.id.IndexRateId;
import com.opengamma.strata.marketdata.key.IndexRateKey;

@Test
public class WrappingVectorEngineFunctionTest {

  private static final WrappingVectorEngineFunction<TestTarget, Double> FUNCTION =
      new WrappingVectorEngineFunction<>(new ScalarFn());

  /**
   * Tests that the wrapping function returns the object returned by the wrapped function when
   * there is one scenario.
   */
  public void oneScenario() {
    ScenarioMarketData marketData =
        ScenarioMarketData.builder(1, date(2011, 3, 8))
            .addValues(IndexRateId.of(IborIndices.USD_LIBOR_1M), 2d)
            .build();
    CalculationMarketData calculationData = calculationData(marketData);
    TestTarget target = new TestTarget(2);
    Object result = FUNCTION.execute(target, calculationData, ReportingRules.targetCurrency());

    assertThat(result).isEqualTo(4d);
  }

  /**
   * Tests that the wrapping function returns a list containing the values returned by the wrapped
   * function when there are multiple scenarios.
   */
  @SuppressWarnings("unchecked")
  public void multipleScenarios() {
    ScenarioMarketData marketData =
        ScenarioMarketData.builder(3, date(2011, 3, 8))
            .addValues(IndexRateId.of(IborIndices.USD_LIBOR_1M), 2d, 3d, 4d)
            .build();
    CalculationMarketData calculationData = calculationData(marketData);
    TestTarget target = new TestTarget(2);
    Object result = FUNCTION.execute(target, calculationData, ReportingRules.targetCurrency());

    assertThat(result).isInstanceOf(List.class);
    assertThat((List<Double>) result)
        .hasSize(3)
        .containsExactly(4d, 6d, 8d);
  }

  private static CalculationMarketData calculationData(ScenarioMarketData marketData) {
    return new DefaultCalculationMarketData(marketData, MarketDataMappings.empty());
  }

  private static final class TestTarget implements CalculationTarget {

    private final Integer value;

    private TestTarget(Integer value) {
      this.value = value;
    }
  }

  private static final class ScalarFn implements ScalarEngineFunction<TestTarget, Double> {

    @Override
    public CalculationRequirements requirements(TestTarget target) {
      return CalculationRequirements.EMPTY;
    }

    @Override
    public Double execute(TestTarget target, SingleCalculationMarketData marketData, ReportingRules reportingRules) {
      Double rate = marketData.getValue(IndexRateKey.of(IborIndices.USD_LIBOR_1M));
      return target.value * rate;
    }
  }
}
