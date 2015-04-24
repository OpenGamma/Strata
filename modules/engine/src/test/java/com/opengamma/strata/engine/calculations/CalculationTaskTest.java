/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.engine.calculations.function.EngineSingleFunction;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.marketdata.id.DiscountingCurveId;
import com.opengamma.strata.marketdata.id.MarketDataFeed;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.ObservableId;
import com.opengamma.strata.marketdata.key.DiscountingCurveKey;
import com.opengamma.strata.marketdata.key.IndexRateKey;

@Test
public class CalculationTaskTest {

  public void requirements() {
    String curveGroupName = "curve group";
    MarketDataFeed marketDataFeed = MarketDataFeed.of("MarketDataVendor");
    MarketDataMappings marketDataMappings =
        MarketDataMappings.builder()
            .curveGroup(curveGroupName)
            .marketDataFeed(marketDataFeed)
            .build();
    CalculationTask task =
        new CalculationTask(new TestTarget(), 0, 0, new TestFunction(), marketDataMappings, ReportingRules.EMPTY);
    MarketDataRequirements requirements = task.requirements();
    Set<? extends MarketDataId<?>> nonObservables = requirements.getNonObservables();
    ImmutableSet<? extends ObservableId> observables = requirements.getObservables();
    ImmutableSet<ObservableId> timeSeries = requirements.getTimeSeries();

    MarketDataId<?> toisId = IndexRateKey.of(OvernightIndices.CHF_TOIS).toObservableId(marketDataFeed);
    assertThat(timeSeries).hasSize(1);
    assertThat(timeSeries.iterator().next()).isEqualTo(toisId);

    MarketDataId<?> curveId = DiscountingCurveId.of(Currency.GBP, curveGroupName);
    assertThat(nonObservables).hasSize(1);
    assertThat(nonObservables.iterator().next()).isEqualTo(curveId);

    MarketDataId<?> liborId = IndexRateKey.of(IborIndices.CHF_LIBOR_12M).toObservableId(marketDataFeed);
    assertThat(observables).hasSize(1);
    assertThat(observables.iterator().next()).isEqualTo(liborId);
  }

  private static class TestTarget implements CalculationTarget { }

  public static final class TestFunction implements EngineSingleFunction<TestTarget, Object> {

    @Override
    public CalculationRequirements requirements(TestTarget target) {
      return CalculationRequirements.builder()
          .singleValueRequirements(
              ImmutableSet.of(
                  DiscountingCurveKey.of(Currency.GBP),
                  IndexRateKey.of(IborIndices.CHF_LIBOR_12M)))
          .timeSeriesRequirements(IndexRateKey.of(OvernightIndices.CHF_TOIS))
          .build();
    }

    @Override
    public Object execute(TestTarget target, CalculationMarketData marketData) {
      return "bar";
    }
  }
}
