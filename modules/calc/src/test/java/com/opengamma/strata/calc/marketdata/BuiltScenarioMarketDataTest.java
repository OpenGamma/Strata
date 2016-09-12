/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.result.FailureException;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.scenario.MarketDataBox;

/**
 * Test {@link BuiltScenarioMarketData}.
 */
@Test
public class BuiltScenarioMarketDataTest {

  private static final LocalDate VAL_DATE = date(2011, 3, 8);
  private static final TestObservableId ID = TestObservableId.of("1");

  //-------------------------------------------------------------------------
  public void test_getValue_fxIdentity() {
    BuiltScenarioMarketData test =
        BuiltScenarioMarketData.builder(MarketDataBox.ofSingleValue(VAL_DATE)).build();

    assertEquals(test.getScenarioCount(), 1);
    assertEquals(test.getValue(FxRateId.of(GBP, GBP)), MarketDataBox.ofSingleValue(FxRate.of(GBP, GBP, 1)));
  }

  public void test_getValue_withKnownFailure() {
    String failureMessage = "Something went wrong";
    BuiltScenarioMarketData test = BuiltScenarioMarketData.builder(MarketDataBox.ofSingleValue(VAL_DATE))
        .addResult(ID, Result.failure(FailureReason.ERROR, failureMessage))
        .build();

    assertEquals(test.getValuationDate(), MarketDataBox.ofSingleValue(VAL_DATE));
    assertEquals(test.containsValue(ID), false);
    assertEquals(test.getIds(), ImmutableSet.of());
    assertEquals(test.findValue(ID), Optional.empty());
    assertThrows(() -> test.getValue(ID), FailureException.class, failureMessage);
  }

  public void test_getValue_withUnknownFailure() {
    BuiltScenarioMarketData test =
        BuiltScenarioMarketData.builder(MarketDataBox.ofSingleValue(VAL_DATE)).build();

    assertEquals(test.getValuationDate(), MarketDataBox.ofSingleValue(VAL_DATE));
    assertEquals(test.containsValue(ID), false);
    assertEquals(test.getIds(), ImmutableSet.of());
    assertEquals(test.findValue(ID), Optional.empty());
    assertThrows(() -> test.getValue(ID), MarketDataNotFoundException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(BuiltScenarioMarketData.empty());
  }

}
