/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.result.FailureException;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.scenario.MarketDataBox;

/**
 * Test {@link BuiltMarketData}.
 */
public class BuiltMarketDataTest {

  private static final LocalDate VAL_DATE = date(2011, 3, 8);
  private static final TestObservableId ID = TestObservableId.of("1");

  //-------------------------------------------------------------------------
  @Test
  public void test_withKnownFailure() {
    String failureMessage = "Something went wrong";
    BuiltScenarioMarketData smd = BuiltScenarioMarketData.builder(MarketDataBox.ofSingleValue(VAL_DATE))
        .addResult(ID, Result.failure(FailureReason.ERROR, failureMessage))
        .build();
    BuiltMarketData test = new BuiltMarketData(smd);

    assertThat(test.getValuationDate()).isEqualTo(VAL_DATE);
    assertThat(test.containsValue(ID)).isFalse();
    assertThat(test.getIds()).isEmpty();
    assertThat(test.findValue(ID)).isEqualTo(Optional.empty());
    assertThatExceptionOfType(FailureException.class)
        .isThrownBy(() -> test.getValue(ID))
        .withMessage(failureMessage);
  }

  @Test
  public void test_withUnknownFailure() {
    BuiltScenarioMarketData smd =
        BuiltScenarioMarketData.builder(MarketDataBox.ofSingleValue(VAL_DATE)).build();
    BuiltMarketData test = new BuiltMarketData(smd);

    assertThat(test.getValuationDate()).isEqualTo(VAL_DATE);
    assertThat(test.containsValue(ID)).isFalse();
    assertThat(test.getIds()).isEmpty();
    assertThat(test.findValue(ID)).isEqualTo(Optional.empty());
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.getValue(ID));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(new BuiltMarketData(BuiltScenarioMarketData.empty()));
  }

}
