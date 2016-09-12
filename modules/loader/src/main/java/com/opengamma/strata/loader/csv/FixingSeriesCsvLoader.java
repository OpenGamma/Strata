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

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.market.observable.IndexQuoteId;

/**
 * Loads a set of historical fixing series into memory from CSV resources.
 * <p>
 * The resources are expected to be in a CSV format, with the following header row:<br />
 * {@code Reference, Date, Value}.
 * <ul>
 * <li>The 'Reference' column is the name of the index that the data is for, such as 'USD-LIBOR-3M'.
 * <li>The 'Date' column is the date that the fixing was taken.
 * <li>The 'Value' column is the fixed value.
 * </ul>
 * <p>
 * Each fixing series must be contained entirely within a single resource, but each resource may
 * contain more than one series. The fixing series points do not need to be ordered.
 * <p>
 * For example:
 * <pre>
 * Reference, Date, Value
 * USD-LIBOR-3M, 1971-01-04, 0.065
 * USD-LIBOR-3M, 1971-01-05, 0.0638
 * USD-LIBOR-3M, 1971-01-06, 0.0638
 * </pre>
 * Note that Microsoft Excel prefers the CSV file to have no space after the comma.
 */
public final class FixingSeriesCsvLoader {

  // CSV column headers
  private static final String REFERENCE_FIELD = "Reference";
  private static final String DATE_FIELD = "Date";
  private static final String VALUE_FIELD = "Value";

  //-------------------------------------------------------------------------
  /**
   * Loads one or more CSV format fixing series files.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param resources  the fixing series CSV resources
   * @return the loaded fixing series, mapped by {@linkplain ObservableId observable ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> load(ResourceLocator... resources) {
    return load(Arrays.asList(resources));
  }

  /**
   * Loads one or more CSV format fixing series files.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param resources  the fixing series CSV resources
   * @return the loaded fixing series, mapped by {@linkplain ObservableId observable ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> load(Collection<ResourceLocator> resources) {
    Collection<CharSource> charSources = resources.stream().map(r -> r.getCharSource()).collect(toList());
    return parse(charSources);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses one or more CSV format fixing series files.
   * <p>
   * If the files contain a duplicate entry an exception will be thrown.
   * 
   * @param charSources  the fixing series CSV character sources
   * @return the loaded fixing series, mapped by {@linkplain ObservableId observable ID}
   * @throws IllegalArgumentException if the files contain a duplicate entry
   */
  public static ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> parse(Collection<CharSource> charSources) {
    // builder ensures keys can only be seen once
    ImmutableMap.Builder<ObservableId, LocalDateDoubleTimeSeries> builder = ImmutableMap.builder();
    for (CharSource charSource : charSources) {
      builder.putAll(parseSingle(charSource));
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // loads a single fixing series CSV file
  private static ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> parseSingle(CharSource resource) {
    Map<ObservableId, LocalDateDoubleTimeSeriesBuilder> builders = new HashMap<>();
    try {
      CsvFile csv = CsvFile.of(resource, true);
      for (CsvRow row : csv.rows()) {
        String referenceStr = row.getField(REFERENCE_FIELD);
        String dateStr = row.getField(DATE_FIELD);
        String valueStr = row.getField(VALUE_FIELD);

        Index index = LoaderUtils.findIndex(referenceStr);
        ObservableId id = IndexQuoteId.of(index);
        LocalDate date = LocalDate.parse(dateStr);
        double value = Double.parseDouble(valueStr);

        LocalDateDoubleTimeSeriesBuilder builder = builders.computeIfAbsent(id, k -> LocalDateDoubleTimeSeries.builder());
        builder.put(date, value);
      }
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException(
          Messages.format("Error processing resource as CSV file: {}", resource), ex);
    }
    return MapStream.of(builders).mapValues(builder -> builder.build()).toMap();
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FixingSeriesCsvLoader() {
  }

}
