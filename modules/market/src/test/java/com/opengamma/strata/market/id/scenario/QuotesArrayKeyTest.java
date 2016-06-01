/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.market.id.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.market.id.QuoteId;
import com.opengamma.strata.market.value.scenario.QuotesArray;

@Test
public class QuotesArrayKeyTest {

  private static final QuotesArrayId KEY = QuotesArrayId.of(StandardId.of("test", "1"), FieldName.of("fieldName"));

  public void getMarketDataKey() {
    QuoteId quoteId = QuoteId.of(StandardId.of("test", "1"), FieldName.of("fieldName"), ObservableSource.NONE);
    assertThat(KEY.getMarketDataId()).isEqualTo(quoteId);
    assertThat(QuotesArrayId.of(quoteId)).isEqualTo(KEY);
  }

  public void getMarketDataType() {
    assertThat(KEY.getScenarioMarketDataType()).isEqualTo(QuotesArray.class);
  }

  public void createScenarioValue() {
    MarketDataBox<Double> box = MarketDataBox.ofScenarioValues(1d, 2d, 3d);
    QuotesArray quotesArray = KEY.createScenarioValue(box, 3);
    assertThat(quotesArray.getQuotes()).isEqualTo(DoubleArray.of(1d, 2d, 3d));
  }

  public void createScenarioValueFromSingleValue() {
    MarketDataBox<Double> box = MarketDataBox.ofSingleValue(3d);
    QuotesArray quotesArray = KEY.createScenarioValue(box, 3);
    assertThat(quotesArray.getQuotes()).isEqualTo(DoubleArray.of(3d, 3d, 3d));
  }
}
