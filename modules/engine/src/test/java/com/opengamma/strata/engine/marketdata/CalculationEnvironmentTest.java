/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.TestObservableId;
import com.opengamma.strata.basics.market.TestObservableKey;
import com.opengamma.strata.collect.result.FailureException;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.calculation.MissingMappingId;
import com.opengamma.strata.engine.calculation.NoMatchingRuleId;

@Test
public class CalculationEnvironmentTest {

  /**
   * Tests the special handling of {@link NoMatchingRuleId}
   */
  public void handleNoMatchingRulesId() {
    CalculationEnvironment marketData = CalculationEnvironment.builder(date(2011, 3, 8)).build();
    NoMatchingRuleId id = NoMatchingRuleId.of(TestObservableKey.of("1"));
    String msgRegex = "No market data rules were available to build the market data for.*";
    assertThrows(() -> marketData.getValue(id), IllegalArgumentException.class, msgRegex);
  }

  /**
   * Tests the special handling of {@link MissingMappingId}
   */
  public void handleMissingMappingsId() {
    CalculationEnvironment marketData = CalculationEnvironment.builder(date(2011, 3, 8)).build();
    MissingMappingId id = MissingMappingId.of(TestObservableKey.of("1"));
    String msgRegex = "No market data mapping found for.*";
    assertThrows(() -> marketData.getValue(id), IllegalArgumentException.class, msgRegex);
  }

  /**
   * Tests the exception when there is a failure for an item of market data.
   */
  public void failureException() {
    TestObservableId id = TestObservableId.of("1");
    String failureMessage = "Something went wrong";
    CalculationEnvironment marketData = CalculationEnvironment
        .builder(date(2011, 3, 8))
        .addResult(id, Result.failure(FailureReason.ERROR, failureMessage))
        .build();

    assertThrows(() -> marketData.getValue(id), FailureException.class, failureMessage);
  }
}
