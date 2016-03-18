/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.market.FieldName;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.StandardId;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.market.id.QuoteId;

/**
 * Loads a set of quotes into memory from CSV resources.
 * <p>
 * The quotes are expected to be in a CSV format, with the following header row:<br />
 * {@code Valuation Date, Symbology, Ticker, Value}.
 * <ul>
 * <li>The 'Valuation Date' column provides the valuation date, allowing data from different
 *  days to be stored in the same file
 * <li>The 'Symbology' column is the symbology scheme applicable to the ticker.
 * <li>The 'Ticker' column is the identifier within the symbology.
 * <li>The 'Field Name' column is the field name, defaulted to 'MarketValue', allowing
 *  fields such as 'Bid' or 'Ask' to be specified.
 * <li>The 'Value' column is the value of the ticker.
 * </ul>
 * <p>
 * Each quotes file may contain entries for many different dates.
 * <p>
 * For example:
 * <pre>
 * Valuation Date, Symbology, Ticker, Field Name, Value
 * 2014-01-22, OG-Future, Eurex-FGBL-Mar14, MarketValue, 150.43
 * 2014-01-22, OG-FutOpt, Eurex-OGBL-Mar14-C150, MarketValue, 1.5
 * 2014-01-22, OG-Future, CME-ED-Mar14, MarketValue, 99.620
 * </pre>
 * Note that Microsoft Excel prefers the CSV file to have no space after the comma.
 */
public final class QuotesCsvLoader {

  // CSV column headers
  private static final String DATE_FIELD = "Valuation Date";
  private static final String SYMBOLOGY_FIELD = "Symbology";
  private static final String TICKER_FIELD = "Ticker";
  private static final String FIELD_NAME_FIELD = "Field Name";
  private static final String VALUE_FIELD = "Value";

  //-------------------------------------------------------------------------
  /**
   * Loads one or more CSV format quote files for a specific date.
   * <p>
   * Only those quotes that match the specified date will be loaded.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param marketDataDate  the date to load
   * @param resources  the fixing series CSV resources
   * @return the loaded fixing series, mapped by {@linkplain ObservableId observable ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<QuoteId, Double> load(LocalDate marketDataDate, ResourceLocator... resources) {
    return load(marketDataDate, Arrays.asList(resources));
  }

  /**
   * Loads one or more CSV format quote files for a specific date.
   * <p>
   * Only those quotes that match the specified date will be loaded.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param marketDataDate  the date to load
   * @param resources  the fixing series CSV resources
   * @return the loaded fixing series, mapped by {@linkplain ObservableId observable ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<QuoteId, Double> load(LocalDate marketDataDate, Collection<ResourceLocator> resources) {
    // builder ensures keys can only be seen once
    ImmutableMap.Builder<QuoteId, Double> builder = ImmutableMap.builder();
    for (ResourceLocator timeSeriesResource : resources) {
      loadSingle(marketDataDate, timeSeriesResource, builder);
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // loads a single CSV file
  private static void loadSingle(
      LocalDate marketDataDate,
      ResourceLocator resource,
      ImmutableMap.Builder<QuoteId, Double> builder) {

    try {
      CsvFile csv = CsvFile.of(resource.getCharSource(), true);
      for (CsvRow row : csv.rows()) {
        String dateText = row.getField(DATE_FIELD);
        LocalDate date = LocalDate.parse(dateText);
        if (date.equals(marketDataDate)) {
          String symbologyStr = row.getField(SYMBOLOGY_FIELD);
          String tickerStr = row.getField(TICKER_FIELD);
          String fieldNameStr = row.getField(FIELD_NAME_FIELD);
          String valueStr = row.getField(VALUE_FIELD);

          double value = Double.valueOf(valueStr);
          StandardId id = StandardId.of(symbologyStr, tickerStr);
          FieldName fieldName = fieldNameStr.isEmpty() ? FieldName.MARKET_VALUE : FieldName.of(fieldNameStr);

          builder.put(QuoteId.of(id, MarketDataFeed.NONE, fieldName), value);
        }
      }
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException(
          Messages.format("Error processing resource as CSV file: {}", resource), ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private QuotesCsvLoader() {
  }

}
