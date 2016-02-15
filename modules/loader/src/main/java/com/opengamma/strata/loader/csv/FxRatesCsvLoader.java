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
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;

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
    // builder ensures keys can only be seen once
    ImmutableMap.Builder<FxRateId, FxRate> builder = ImmutableMap.builder();

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
      ImmutableMap.Builder<FxRateId, FxRate> builder) {

    try {
      CsvFile csv = CsvFile.of(resource.getCharSource(), true);
      for (CsvRow row : csv.rows()) {
        String dateText = row.getField(DATE_FIELD);
        LocalDate date = LocalDate.parse(dateText);
        if (date.equals(marketDataDate)) {
          String currencyPairStr = row.getField(CURRENCY_PAIR_FIELD);
          String valueStr = row.getField(VALUE_FIELD);
          CurrencyPair currencyPair = CurrencyPair.parse(currencyPairStr);
          double value = Double.valueOf(valueStr);
          builder.put(FxRateId.of(currencyPair), FxRate.of(currencyPair, value));
        }
      }
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException("Error processing resource as CSV file: " + resource, ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FxRatesCsvLoader() {
  }
}
