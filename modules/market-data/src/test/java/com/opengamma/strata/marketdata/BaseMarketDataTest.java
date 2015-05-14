/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.marketdata;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.collect.TestHelper;
import com.opengamma.strata.marketdata.id.MissingMappingId;
import com.opengamma.strata.marketdata.id.NoMatchingRuleId;
import com.opengamma.strata.marketdata.key.RateIndexCurveKey;

@Test
public class BaseMarketDataTest {

  /**
   * Tests the special handling of {@link NoMatchingRuleId}
   */
  public void handleNoMatchingRulesId() {
    BaseMarketData marketData = BaseMarketData.builder(TestHelper.date(2011, 3, 8)).build();
    NoMatchingRuleId id = NoMatchingRuleId.of(RateIndexCurveKey.of(IborIndices.USD_LIBOR_1M));
    String msgRegex = "No market data rules were available to build the market data for.*";
    TestHelper.assertThrows(() -> marketData.getValue(id), IllegalArgumentException.class, msgRegex);
  }

  /**
   * Tests the special handling of {@link MissingMappingId}
   */
  public void handleMissingMappingsId() {
    BaseMarketData marketData = BaseMarketData.builder(TestHelper.date(2011, 3, 8)).build();
    MissingMappingId id = MissingMappingId.of(RateIndexCurveKey.of(IborIndices.USD_LIBOR_1M));
    String msgRegex = "No market data mapping found for.*";
    TestHelper.assertThrows(() -> marketData.getValue(id), IllegalArgumentException.class, msgRegex);
  }
}
