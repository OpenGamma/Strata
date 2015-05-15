/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.TestId;
import com.opengamma.strata.engine.marketdata.TestKey;
import com.opengamma.strata.engine.marketdata.TestMapping;
import com.opengamma.strata.engine.marketdata.TestObservableKey;
import com.opengamma.strata.engine.marketdata.mapping.DefaultMarketDataMappings;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;

@Test
public class CalculationTaskTest {

  public void requirements() {
    MarketDataFeed marketDataFeed = MarketDataFeed.of("MarketDataVendor");
    MarketDataMappings marketDataMappings =
        DefaultMarketDataMappings.builder()
            .mappings(ImmutableMap.of(TestKey.class, new TestMapping("foo")))
            .marketDataFeed(marketDataFeed)
            .build();
    CalculationTask task =
        new CalculationTask(new TestTarget(), 0, 0, new TestFunction(), marketDataMappings, ReportingRules.empty());
    MarketDataRequirements requirements = task.requirements();
    Set<? extends MarketDataId<?>> nonObservables = requirements.getNonObservables();
    ImmutableSet<? extends ObservableId> observables = requirements.getObservables();
    ImmutableSet<ObservableId> timeSeries = requirements.getTimeSeries();

    MarketDataId<?> timeSeriesId = TestObservableKey.of("3").toObservableId(marketDataFeed);
    assertThat(timeSeries).hasSize(1);
    assertThat(timeSeries.iterator().next()).isEqualTo(timeSeriesId);

    MarketDataId<?> nonObservableId = TestId.of("1");
    assertThat(nonObservables).hasSize(1);
    assertThat(nonObservables.iterator().next()).isEqualTo(nonObservableId);

    MarketDataId<?> observableId = TestObservableKey.of("2").toObservableId(marketDataFeed);
    assertThat(observables).hasSize(1);
    assertThat(observables.iterator().next()).isEqualTo(observableId);
  }

  private static class TestTarget implements CalculationTarget { }

  public static final class TestFunction implements CalculationSingleFunction<TestTarget, Object> {

    @Override
    public CalculationRequirements requirements(TestTarget target) {
      return CalculationRequirements.builder()
          .singleValueRequirements(
              ImmutableSet.of(
                  TestKey.of("1"),
                  TestObservableKey.of("2")))
          .timeSeriesRequirements(TestObservableKey.of("3"))
          .build();
    }

    @Override
    public Object execute(TestTarget target, CalculationMarketData marketData) {
      return "bar";
    }
  }
}
