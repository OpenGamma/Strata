/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.beans.test.BeanAssert.assertBeanEquals;

import java.util.List;

import org.joda.beans.Bean;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.opengamma.strata.collect.result.ValueWithFailures;
import com.opengamma.strata.product.Trade;

/**
 * Groups CSV test utils methods used in Granite before they are moved to Strata.
 */
public class CsvTestUtils {

  @SafeVarargs
  public static <T extends Trade & Bean> void checkRoundtrip(
      Class<T> type,
      List<T> loadedTrades,
      T... expectedTrades) {

    StringBuilder buf = new StringBuilder(1024);
    TradeCsvWriter.standard().write(loadedTrades, buf);
    List<CharSource> writtenCsv = ImmutableList.of(CharSource.wrap(buf.toString()));
    ValueWithFailures<List<T>> roundtrip = TradeCsvLoader.standard().parse(writtenCsv, type);
    assertThat(roundtrip.getFailures().size()).as(roundtrip.getFailures().toString()).isEqualTo(0);
    List<T> roundtripTrades = roundtrip.getValue();
    assertThat(roundtripTrades).hasSize(expectedTrades.length);
    for (int i = 0; i < roundtripTrades.size(); i++) {
      assertBeanEquals(expectedTrades[i], roundtripTrades.get(i));
    }
  }
}
