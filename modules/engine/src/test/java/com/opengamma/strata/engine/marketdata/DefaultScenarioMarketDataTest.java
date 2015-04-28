/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.engine.calculations.MissingMappingId;
import com.opengamma.strata.engine.calculations.NoMatchingRuleId;
import com.opengamma.strata.marketdata.key.IndexCurveKey;

@Test
public class DefaultScenarioMarketDataTest {

  /**
   * Tests the special handling of {@link NoMatchingRuleId}
   */
  public void handleNoMatchingRulesId() {
    ScenarioMarketData marketData = ScenarioMarketData.builder(1, date(2011, 3, 8)).build();
    NoMatchingRuleId id = NoMatchingRuleId.of(IndexCurveKey.of(IborIndices.USD_LIBOR_1M));
    String msgRegex = "No market data rules were available to build the market data for.*";
    assertThrows(() -> marketData.getValues(id), IllegalArgumentException.class, msgRegex);
  }

  /**
   * Tests the special handling of {@link MissingMappingId}
   */
  public void handleMissingMappingsId() {
    ScenarioMarketData marketData = ScenarioMarketData.builder(1, date(2011, 3, 8)).build();
    MissingMappingId id = MissingMappingId.of(IndexCurveKey.of(IborIndices.USD_LIBOR_1M));
    String msgRegex = "No market data mapping found for market data.*";
    assertThrows(() -> marketData.getValues(id), IllegalArgumentException.class, msgRegex);
  }
}
