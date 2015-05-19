/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.scenarios.curves;

import static org.mockito.Mockito.mock;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.ConstantDoublesCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.TestHelper;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.DefaultMarketDataFactory;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.ScenarioMarketData;
import com.opengamma.strata.engine.marketdata.ScenarioMarketDataResult;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.ObservableMarketDataFunction;
import com.opengamma.strata.engine.marketdata.functions.TimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.engine.marketdata.scenarios.PerturbationMapping;
import com.opengamma.strata.engine.marketdata.scenarios.ScenarioDefinition;
import com.opengamma.strata.market.id.DiscountingCurveId;

@Test
public class CurveParallelShiftTest {

  public void absolute() {
    CurveParallelShift shift = CurveParallelShift.absolute(0.1);
    YieldCurve shiftedCurve = shift.apply(YieldCurve.from(ConstantDoublesCurve.from(2d)));
    checkCurveValues(shiftedCurve, 2.1);
  }

  public void relative() {
    CurveParallelShift shift = CurveParallelShift.relative(0.1);
    YieldCurve shiftedCurve = shift.apply(YieldCurve.from(ConstantDoublesCurve.from(2d)));
    checkCurveValues(shiftedCurve, 2.2);
  }

  public void absoluteScenarios() {
    String curveName = "curveName";
    String curveGroupName = "curveGroupName";
    YieldCurve curve = YieldCurve.from(ConstantDoublesCurve.from(2d, curveName));
    PerturbationMapping<YieldCurve> mapping =
        PerturbationMapping.of(
            YieldCurve.class,
            CurveNameFilter.of(curveName),
            CurveParallelShift.absolute(0.1),
            CurveParallelShift.absolute(0.2),
            CurveParallelShift.absolute(0.3));
    DiscountingCurveId curveId = DiscountingCurveId.of(Currency.GBP, curveGroupName);
    BaseMarketData marketData =
        BaseMarketData.builder(TestHelper.date(2011, 3, 8))
            .addValue(curveId, curve)
            .build();
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mapping);
    DefaultMarketDataFactory marketDataFactory =
        new DefaultMarketDataFactory(
            mock(TimeSeriesProvider.class),
            mock(ObservableMarketDataFunction.class),
            FeedIdMapping.identity());
    MarketDataRequirements requirements = MarketDataRequirements.builder().addValues(curveId).build();
    ScenarioMarketDataResult result =
        marketDataFactory.buildScenarioMarketData(
            requirements,
            marketData,
            scenarioDefinition,
            MarketDataConfig.empty());
    ScenarioMarketData scenarioData = result.getMarketData();
    List<YieldCurve> curves = scenarioData.getValues(curveId);
    Assertions.assertThat(curves).hasSize(3);
    checkCurveValues(curves.get(0), 2.1);
    checkCurveValues(curves.get(1), 2.2);
    checkCurveValues(curves.get(2), 2.3);
  }

  // It's not possible to do an equality test on the curves because shifting them wraps them in a different type
  private void checkCurveValues(YieldCurve curve, double expectedValue) {
    for (int i = 0; i < 10; i++) {
      Assertions.assertThat(curve.getInterestRate((double) i)).isEqualTo(expectedValue);
    }
  }
}
