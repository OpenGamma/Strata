/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.result.FailureException;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.scenario.MarketDataBox;

/**
 * Test {@link BuiltMarketData}.
 */
@Test
public class BuiltMarketDataTest {

  private static final LocalDate VAL_DATE = date(2011, 3, 8);
  private static final TestObservableId ID = TestObservableId.of("1");

  //-------------------------------------------------------------------------
  public void test_withKnownFailure() {
    String failureMessage = "Something went wrong";
    BuiltScenarioMarketData smd = BuiltScenarioMarketData.builder(MarketDataBox.ofSingleValue(VAL_DATE))
        .addResult(ID, Result.failure(FailureReason.ERROR, failureMessage))
        .build();
    BuiltMarketData test = new BuiltMarketData(smd);

    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(test.containsValue(ID), false);
    assertEquals(test.getIds(), ImmutableSet.of());
    assertEquals(test.findValue(ID), Optional.empty());
    assertThrows(() -> test.getValue(ID), FailureException.class, failureMessage);
  }

  public void test_withUnknownFailure() {
    BuiltScenarioMarketData smd =
        BuiltScenarioMarketData.builder(MarketDataBox.ofSingleValue(VAL_DATE)).build();
    BuiltMarketData test = new BuiltMarketData(smd);

    assertEquals(test.getValuationDate(), VAL_DATE);
    assertEquals(test.containsValue(ID), false);
    assertEquals(test.getIds(), ImmutableSet.of());
    assertEquals(test.findValue(ID), Optional.empty());
    assertThrows(() -> test.getValue(ID), MarketDataNotFoundException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(new BuiltMarketData(BuiltScenarioMarketData.empty()));
  }

}
