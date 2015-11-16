/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static org.mockito.Mockito.mock;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.marketdata.CalculationMarketDataMap;
import com.opengamma.strata.calc.marketdata.CalculationRequirements;
import com.opengamma.strata.calc.marketdata.DefaultMarketDataFactory;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.ObservableMarketDataFunction;
import com.opengamma.strata.calc.marketdata.function.TimeSeriesProvider;
import com.opengamma.strata.calc.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.calc.marketdata.scenario.PerturbationMapping;
import com.opengamma.strata.calc.marketdata.scenario.ScenarioDefinition;
import com.opengamma.strata.collect.TestHelper;
import com.opengamma.strata.function.marketdata.scenario.curve.CurveNameFilter;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.perturb.CurveParallelShift;
import com.opengamma.strata.market.id.DiscountCurveId;

/**
 * Test usage of {@link CurveParallelShift}.
 */
@Test
public class CurveParallelShiftUsageTest {

  public void absoluteScenarios() {
    CurveName curveName = CurveName.of("curveName");
    CurveGroupName curveGroupName = CurveGroupName.of("curveGroupName");
    Curve curve = ConstantNodalCurve.of(curveName, 2);
    PerturbationMapping<Curve> mapping = PerturbationMapping.of(
        Curve.class,
        CurveNameFilter.of(curveName),
        CurveParallelShifts.absolute(0.1, 0.2, 0.3));
    DiscountCurveId curveId = DiscountCurveId.of(Currency.GBP, curveGroupName);
    MarketEnvironment marketData = MarketEnvironment.builder()
        .valuationDate(TestHelper.date(2011, 3, 8))
        .addValue(curveId, curve)
        .build();
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mapping);
    DefaultMarketDataFactory marketDataFactory = new DefaultMarketDataFactory(
        mock(TimeSeriesProvider.class),
        mock(ObservableMarketDataFunction.class),
        FeedIdMapping.identity());
    CalculationRequirements requirements = CalculationRequirements.builder().addValues(curveId).build();
    CalculationMarketDataMap scenarioData = marketDataFactory.buildCalculationMarketData(
        requirements,
        marketData,
        MarketDataConfig.empty(),
        scenarioDefinition);
    MarketDataBox<Curve> curves = scenarioData.getValue(curveId);
    assertThat(curves.getScenarioCount()).isEqualTo(3);
    checkCurveValues(curves.getValue(0), 2.1);
    checkCurveValues(curves.getValue(1), 2.2);
    checkCurveValues(curves.getValue(2), 2.3);
  }

  // It's not possible to do an equality test on the curves because shifting them wraps them in a different type
  private void checkCurveValues(Curve curve, double expectedValue) {
    for (int i = 0; i < 10; i++) {
      assertThat(curve.yValue((double) i)).isEqualTo(expectedValue);
    }
  }

}
