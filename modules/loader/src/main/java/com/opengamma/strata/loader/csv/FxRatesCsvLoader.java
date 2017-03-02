/*
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
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.data.FxRateId;

/**
 * Loads a set of FX rates into memory from CSV resources.
 * <p>
 * The rates are expected to be in a CSV format, with the following header row:<br />
 * {@code Valuation Date, Currency Pair, Value}.
 * <ul>
 * <li>The 'Valuation Date' column provides the valuation date, allowing data from different
 *  days to be stored in the same file
 * <li>The 'Currency Pair' column is the currency pair in the format 'EUR/USD'.
 * <li>The 'Value' column is the value of the rate.
 * </ul>
 * <p>
 * Each file may contain entries for many different dates.
 * <p>
 * For example:
 * <pre>
 * Valuation Date, Currency Pair, Value
 * 2014-01-22, EUR/USD, 1.10
 * 2014-01-22, GBP/USD, 1.50
 * 2014-01-23, EUR/USD, 1.11
 * </pre>
 * Note that Microsoft Excel prefers the CSV file to have no space after the comma.
 */
public final class FxRatesCsvLoader {

  // CSV column headers
  private static final String DATE_FIELD = "Valuation Date";
  private static final String CURRENCY_PAIR_FIELD = "Currency Pair";
  private static final String VALUE_FIELD = "Value";

  //-------------------------------------------------------------------------
  /**
   * Loads one or more CSV format FX rate files for a specific date.
   * <p>
   * Only those rates that match the specified date will be loaded.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param marketDataDate  the date to load
   * @param resources  the CSV resources
   * @return the loaded FX rates, mapped by {@linkplain FxRateId rate ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<FxRateId, FxRate> load(LocalDate marketDataDate, ResourceLocator... resources) {
    return load(marketDataDate, Arrays.asList(resources));
  }

  /**
   * Loads one or more CSV format FX rate files for a specific date.
   * <p>
   * Only those rates that match the specified date will be loaded.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param marketDataDate  the date to load
   * @param resources  the CSV resources
   * @return the loaded FX rates, mapped by {@linkplain FxRateId rate ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<FxRateId, FxRate> load(LocalDate marketDataDate, Collection<ResourceLocator> resources) {
    Collection<CharSource> charSources = resources.stream().map(r -> r.getCharSource()).collect(toList());
    return parse(d -> marketDataDate.equals(d), charSources).getOrDefault(marketDataDate, ImmutableMap.of());
  }

  //-------------------------------------------------------------------------
  /**
   * Loads one or more CSV format FX rate files for a set of dates.
   * <p>
   * Only those rates that match one of the specified dates will be loaded.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param marketDataDates  the set of dates to load
   * @param resources  the CSV resources
   * @return the loaded FX rates, mapped by {@link LocalDate} and {@linkplain FxRateId rate ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<LocalDate, ImmutableMap<FxRateId, FxRate>> load(
      Set<LocalDate> marketDataDates,
      ResourceLocator... resources) {

    return load(marketDataDates, Arrays.asList(resources));
  }

  /**
   * Loads one or more CSV format FX rate files for a set of dates.
   * <p>
   * Only those rates that match one of the specified dates will be loaded.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param marketDataDates  the dates to load
   * @param resources  the CSV resources
   * @return the loaded FX rates, mapped by {@link LocalDate} and {@linkplain FxRateId rate ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<LocalDate, ImmutableMap<FxRateId, FxRate>> load(
      Set<LocalDate> marketDataDates,
      Collection<ResourceLocator> resources) {

    Collection<CharSource> charSources = resources.stream().map(r -> r.getCharSource()).collect(toList());
    return parse(d -> marketDataDates.contains(d), charSources);
  }

  //-------------------------------------------------------------------------
  /**
   * Loads one or more CSV format FX rate files.
   * <p>
   * All dates that are found will be returned.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param resources  the CSV resources
   * @return the loaded FX rates, mapped by {@link LocalDate} and {@linkplain FxRateId rate ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<LocalDate, ImmutableMap<FxRateId, FxRate>> loadAllDates(ResourceLocator... resources) {
    return loadAllDates(Arrays.asList(resources));
  }

  /**
   * Loads one or more CSV format FX rate files.
   * <p>
   * All dates that are found will be returned.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param resources  the CSV resources
   * @return the loaded FX rates, mapped by {@link LocalDate} and {@linkplain FxRateId rate ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<LocalDate, ImmutableMap<FxRateId, FxRate>> loadAllDates(
      Collection<ResourceLocator> resources) {

    Collection<CharSource> charSources = resources.stream().map(r -> r.getCharSource()).collect(toList());
    return parse(d -> true, charSources);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses one or more CSV format FX rate files.
   * <p>
   * A predicate is specified that is used to filter the dates that are returned.
   * This could match a single date, a set of dates or all dates.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param datePredicate  the predicate used to select the dates
   * @param charSources  the CSV character sources
   * @return the loaded FX rates, mapped by {@link LocalDate} and {@linkplain FxRateId rate ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<LocalDate, ImmutableMap<FxRateId, FxRate>> parse(
      Predicate<LocalDate> datePredicate,
      Collection<CharSource> charSources) {

    // builder ensures keys can only be seen once
    Map<LocalDate, ImmutableMap.Builder<FxRateId, FxRate>> mutableMap = new HashMap<>();
    for (CharSource charSource : charSources) {
      parseSingle(datePredicate, charSource, mutableMap);
    }
    ImmutableMap.Builder<LocalDate, ImmutableMap<FxRateId, FxRate>> builder = ImmutableMap.builder();
    for (Entry<LocalDate, Builder<FxRateId, FxRate>> entry : mutableMap.entrySet()) {
      builder.put(entry.getKey(), entry.getValue().build());
    }
    return builder.build();
  }

  // loads a single CSV file, filtering by date
  private static void parseSingle(
      Predicate<LocalDate> datePredicate,
      CharSource resource,
      Map<LocalDate, ImmutableMap.Builder<FxRateId, FxRate>> mutableMap) {

    try {
      CsvFile csv = CsvFile.of(resource, true);
      for (CsvRow row : csv.rows()) {
        String dateText = row.getField(DATE_FIELD);
        LocalDate date = LocalDate.parse(dateText);
        if (datePredicate.test(date)) {
          String currencyPairStr = row.getField(CURRENCY_PAIR_FIELD);
          String valueStr = row.getField(VALUE_FIELD);
          CurrencyPair currencyPair = CurrencyPair.parse(currencyPairStr);
          double value = Double.valueOf(valueStr);

          ImmutableMap.Builder<FxRateId, FxRate> builderForDate = mutableMap.computeIfAbsent(date, k -> ImmutableMap.builder());
          builderForDate.put(FxRateId.of(currencyPair), FxRate.of(currencyPair, value));
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
  private FxRatesCsvLoader() {
  }
}
