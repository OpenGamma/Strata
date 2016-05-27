/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.TestObservableId;
import com.opengamma.strata.collect.result.FailureException;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

@Test
public class BuiltScenarioMarketDataTest {

  /**
   * Tests the exception when there is a failure for an item of market data.
   */
  public void failureException() {
    TestObservableId id = TestObservableId.of("1");
    String failureMessage = "Something went wrong";
    BuiltScenarioMarketData marketData = BuiltScenarioMarketData.builder(MarketDataBox.ofSingleValue(date(2011, 3, 8)))
        .addResult(id, Result.failure(FailureReason.ERROR, failureMessage))
        .build();

    assertThrows(() -> marketData.getValue(id), FailureException.class, failureMessage);
  }

}
