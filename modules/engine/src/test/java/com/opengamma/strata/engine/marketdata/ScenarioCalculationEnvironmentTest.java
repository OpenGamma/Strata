/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.TestObservableKey;
import com.opengamma.strata.engine.calculation.MissingMappingId;
import com.opengamma.strata.engine.calculation.NoMatchingRuleId;

@Test
public class ScenarioCalculationEnvironmentTest {

  /**
   * Tests the special handling of {@link NoMatchingRuleId}
   */
  public void handleNoMatchingRulesId() {
    ScenarioCalculationEnvironment marketData = ScenarioCalculationEnvironment.builder(1, date(2011, 3, 8)).build();
    NoMatchingRuleId id = NoMatchingRuleId.of(TestObservableKey.of("1"));
    String msgRegex = "No market data rules were available to build the market data for.*";
    assertThrows(() -> marketData.getValues(id), IllegalArgumentException.class, msgRegex);
  }

  /**
   * Tests the special handling of {@link MissingMappingId}
   */
  public void handleMissingMappingsId() {
    ScenarioCalculationEnvironment marketData = ScenarioCalculationEnvironment.builder(1, date(2011, 3, 8)).build();
    MissingMappingId id = MissingMappingId.of(TestObservableKey.of("1"));
    String msgRegex = "No market data mapping found for.*";
    assertThrows(() -> marketData.getValues(id), IllegalArgumentException.class, msgRegex);
  }
}
