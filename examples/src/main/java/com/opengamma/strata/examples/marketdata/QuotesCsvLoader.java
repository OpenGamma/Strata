/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.market.id.QuoteId;

/**
 * Loads a set of quotes into memory by reading from CSV resources.
 * <p>
 * The quotes file contains the following header row:
 * Valuation Date, Scheme, Ticker, Value
 */
public final class QuotesCsvLoader {

  private static final String DATE_NAME = "Date";
  private static final String SCHEME_NAME = "Scheme";
  private static final String TICKER_NAME = "Ticker";
  private static final String VALUE_NAME = "Value";

  /**
   * Restricted constructor.
   */
  private QuotesCsvLoader() {
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the available quotes from the CSV resources.
   * 
   * @param quotesResource  the CSV resource
   * @param marketDataDate  the date to load
   * @return the loaded quotes, mapped by an identifying key
   */
  public static Map<QuoteId, Double> loadQuotes(ResourceLocator quotesResource, LocalDate marketDataDate) {
    Map<QuoteId, Double> map = new HashMap<>();
    CsvFile csv = CsvFile.of(quotesResource.getCharSource(), true);
    for (int i = 0; i < csv.rowCount(); i++) {
      String dateText = csv.field(i, DATE_NAME);
      LocalDate date = LocalDate.parse(dateText);
      if (!date.equals(marketDataDate)) {
        continue;
      }
      String schemeText = csv.field(i, SCHEME_NAME);
      String tickerText = csv.field(i, TICKER_NAME);
      String valueText = csv.field(i, VALUE_NAME);
      double value = Double.valueOf(valueText);

      map.put(QuoteId.of(StandardId.of(schemeText, tickerText)), value);
    }
    return map;
  }

}
