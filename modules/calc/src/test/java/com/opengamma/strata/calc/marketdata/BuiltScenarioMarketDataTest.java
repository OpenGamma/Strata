/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

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
public class BuiltScenarioMarketDataTest {

  private static final LocalDate VAL_DATE = date(2011, 3, 8);
  private static final TestObservableId ID = TestObservableId.of("1");

  //-------------------------------------------------------------------------
  @Test
  public void test_getValue_fxIdentity() {
    BuiltScenarioMarketData test =
        BuiltScenarioMarketData.builder(MarketDataBox.ofSingleValue(VAL_DATE)).build();

    assertThat(test.getScenarioCount()).isEqualTo(1);
    assertThat(test.getValue(FxRateId.of(GBP, GBP))).isEqualTo(MarketDataBox.ofSingleValue(FxRate.of(GBP, GBP, 1)));
  }

  @Test
  public void test_getValue_withKnownFailure() {
    String failureMessage = "Something went wrong";
    BuiltScenarioMarketData test = BuiltScenarioMarketData.builder(MarketDataBox.ofSingleValue(VAL_DATE))
        .addResult(ID, Result.failure(FailureReason.ERROR, failureMessage))
        .build();

    assertThat(test.getValuationDate()).isEqualTo(MarketDataBox.ofSingleValue(VAL_DATE));
    assertThat(test.containsValue(ID)).isFalse();
    assertThat(test.getIds()).isEmpty();
    assertThat(test.findValue(ID)).isEqualTo(Optional.empty());
    assertThatExceptionOfType(FailureException.class)
        .isThrownBy(() -> test.getValue(ID))
        .withMessage(failureMessage);
  }

  @Test
  public void test_getValue_withUnknownFailure() {
    BuiltScenarioMarketData test =
        BuiltScenarioMarketData.builder(MarketDataBox.ofSingleValue(VAL_DATE)).build();

    assertThat(test.getValuationDate()).isEqualTo(MarketDataBox.ofSingleValue(VAL_DATE));
    assertThat(test.containsValue(ID)).isFalse();
    assertThat(test.getIds()).isEmpty();
    assertThat(test.findValue(ID)).isEqualTo(Optional.empty());
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.getValue(ID));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(BuiltScenarioMarketData.empty());
  }

}
