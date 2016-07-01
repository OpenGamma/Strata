/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.curve;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataFactory;
import com.opengamma.strata.calc.marketdata.MarketDataFilter;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.ObservableDataProvider;
import com.opengamma.strata.calc.marketdata.PerturbationMapping;
import com.opengamma.strata.calc.marketdata.ScenarioDefinition;
import com.opengamma.strata.calc.marketdata.TimeSeriesProvider;
import com.opengamma.strata.collect.TestHelper;
import com.opengamma.strata.data.scenario.ImmutableScenarioMarketData;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParallelShifts;

/**
 * Test usage of {@link CurveParallelShifts}.
 */
@Test
public class CurveParallelShiftsUsageTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public void absoluteScenarios() {
    CurveName curveName = CurveName.of("curveName");
    CurveGroupName curveGroupName = CurveGroupName.of("curveGroupName");
    Curve curve = ConstantCurve.of(curveName, 2);
    PerturbationMapping<Curve> mapping = PerturbationMapping.of(
        Curve.class,
        MarketDataFilter.ofName(curveName),
        CurveParallelShifts.absolute(0.1, 0.2, 0.3));
    CurveId curveId = CurveId.of(curveGroupName, curveName);
    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(TestHelper.date(2011, 3, 8))
        .addValue(curveId, curve)
        .build();
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mapping);
    MarketDataFactory marketDataFactory =
        MarketDataFactory.of(mock(ObservableDataProvider.class), mock(TimeSeriesProvider.class));
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(curveId).build();
    ScenarioMarketData scenarioData = marketDataFactory.createMultiScenario(
        requirements,
        MarketDataConfig.empty(),
        marketData,
        REF_DATA,
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
