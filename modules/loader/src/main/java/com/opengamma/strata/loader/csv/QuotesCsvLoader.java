/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.market.observable.QuoteId;

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
   * @param resources  the CSV resources
   * @return the loaded quotes, mapped by {@linkplain QuoteId quote ID}
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
   * @param resources  the CSV resources
   * @return the loaded quotes, mapped by {@linkplain QuoteId quote ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<QuoteId, Double> load(LocalDate marketDataDate, Collection<ResourceLocator> resources) {
    Collection<CharSource> charSources = resources.stream().map(r -> r.getCharSource()).collect(toList());
    return parse(d -> marketDataDate.equals(d), charSources).getOrDefault(marketDataDate, ImmutableMap.of());
  }

  //-------------------------------------------------------------------------
  /**
   * Loads one or more CSV format quote files for a set of dates.
   * <p>
   * Only those quotes that match one of the specified dates will be loaded.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param marketDataDates  the set of dates to load
   * @param resources  the CSV resources
   * @return the loaded quotes, mapped by {@link LocalDate} and {@linkplain QuoteId quote ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<LocalDate, ImmutableMap<QuoteId, Double>> load(
      Set<LocalDate> marketDataDates,
      ResourceLocator... resources) {

    return load(marketDataDates, Arrays.asList(resources));
  }

  /**
   * Loads one or more CSV format quote files for a set of dates.
   * <p>
   * Only those quotes that match one of the specified dates will be loaded.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param marketDataDates  the dates to load
   * @param resources  the CSV resources
   * @return the loaded quotes, mapped by {@link LocalDate} and {@linkplain QuoteId quote ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<LocalDate, ImmutableMap<QuoteId, Double>> load(
      Set<LocalDate> marketDataDates,
      Collection<ResourceLocator> resources) {

    Collection<CharSource> charSources = resources.stream().map(r -> r.getCharSource()).collect(toList());
    return parse(d -> marketDataDates.contains(d), charSources);
  }

  //-------------------------------------------------------------------------
  /**
   * Loads one or more CSV format quote files.
   * <p>
   * All dates that are found will be returned.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param resources  the CSV resources
   * @return the loaded quotes, mapped by {@link LocalDate} and {@linkplain QuoteId quote ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<LocalDate, ImmutableMap<QuoteId, Double>> loadAllDates(ResourceLocator... resources) {
    return loadAllDates(Arrays.asList(resources));
  }

  /**
   * Loads one or more CSV format quote files.
   * <p>
   * All dates that are found will be returned.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param resources  the CSV resources
   * @return the loaded quotes, mapped by {@link LocalDate} and {@linkplain QuoteId quote ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<LocalDate, ImmutableMap<QuoteId, Double>> loadAllDates(
      Collection<ResourceLocator> resources) {

    Collection<CharSource> charSources = resources.stream().map(r -> r.getCharSource()).collect(toList());
    return parse(d -> true, charSources);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses one or more CSV format quote files.
   * <p>
   * A predicate is specified that is used to filter the dates that are returned.
   * This could match a single date, a set of dates or all dates.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param datePredicate  the predicate used to select the dates
   * @param charSources  the CSV character sources
   * @return the loaded quotes, mapped by {@link LocalDate} and {@linkplain QuoteId quote ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<LocalDate, ImmutableMap<QuoteId, Double>> parse(
      Predicate<LocalDate> datePredicate,
      Collection<CharSource> charSources) {

    // builder ensures keys can only be seen once
    Map<LocalDate, ImmutableMap.Builder<QuoteId, Double>> mutableMap = new HashMap<>();
    for (CharSource charSource : charSources) {
      parseSingle(datePredicate, charSource, mutableMap);
    }
    ImmutableMap.Builder<LocalDate, ImmutableMap<QuoteId, Double>> builder = ImmutableMap.builder();
    for (Entry<LocalDate, Builder<QuoteId, Double>> entry : mutableMap.entrySet()) {
      builder.put(entry.getKey(), entry.getValue().build());
    }
    return builder.build();
  }

  // loads a single CSV file, filtering by date
  private static void parseSingle(
      Predicate<LocalDate> datePredicate,
      CharSource resource,
      Map<LocalDate, ImmutableMap.Builder<QuoteId, Double>> mutableMap) {

    try {
      CsvFile csv = CsvFile.of(resource, true);
      for (CsvRow row : csv.rows()) {
        String dateText = row.getField(DATE_FIELD);
        LocalDate date = LocalDate.parse(dateText);
        if (datePredicate.test(date)) {
          String symbologyStr = row.getField(SYMBOLOGY_FIELD);
          String tickerStr = row.getField(TICKER_FIELD);
          String fieldNameStr = row.getField(FIELD_NAME_FIELD);
          String valueStr = row.getField(VALUE_FIELD);

          double value = Double.valueOf(valueStr);
          StandardId id = StandardId.of(symbologyStr, tickerStr);
          FieldName fieldName = fieldNameStr.isEmpty() ? FieldName.MARKET_VALUE : FieldName.of(fieldNameStr);

          ImmutableMap.Builder<QuoteId, Double> builderForDate = mutableMap.computeIfAbsent(date, k -> ImmutableMap.builder());
          builderForDate.put(QuoteId.of(id, fieldName), value);
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
