/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.market.id.IndexRateId;

/**
 * Loads a set of historical fixing series into memory from CSV resources.
 * <p>
 * The resources are expected to be in a CSV format, with the following header row:<br />
 * {@code Reference, Date, Value}.
 * <p>
 * The 'Reference' column is the name of the index that the data is for, such as 'USD-LIBOR-3M'.
 * The 'Date' column is the date that the fixing was taken.
 * The 'Value' column is the fixed value.
 * <p>
 * Each fixing series must be contained entirely within a single resource, but each resource may
 * contain more than one series. The fixing series points do not need to be ordered.
 * <p>
 * For example:
 * <pre>
 * Reference,Date,Value
 * USD-LIBOR-3M,1971-01-04,0.065
 * USD-LIBOR-3M,1971-01-05,0.0638
 * USD-LIBOR-3M,1971-01-06,0.0638
 * </pre>
 */
public class FixingSeriesCsvLoader {

  // CSV column headers
  private static final String REFERENCE_FIELD = "Reference";
  private static final String DATE_FIELD = "Date";
  private static final String VALUE_FIELD = "Value";

  //-------------------------------------------------------------------------
  /**
   * Loads one or more historical fixing series into memory from CSV resources.
   * 
   * @param fixingSeriesResources  the fixing series CSV resources
   * @return the loaded fixing series, mapped by {@linkplain ObservableId observable ID}
   */
  public static ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> loadFixingSeries(
      ResourceLocator... fixingSeriesResources) {

    return loadFixingSeries(Arrays.asList(fixingSeriesResources));
  }

  /**
   * Loads a set of historical fixing series into memory from CSV resources.
   * 
   * @param fixingSeriesResources  the fixing series CSV resources
   * @return the loaded fixing series, mapped by {@linkplain ObservableId observable ID}
   */
  public static ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> loadFixingSeries(
      Collection<ResourceLocator> fixingSeriesResources) {

    ImmutableMap.Builder<ObservableId, LocalDateDoubleTimeSeries> builder = ImmutableMap.builder();
    for (ResourceLocator timeSeriesResource : fixingSeriesResources) {
      // builder ensures keys can only be seen once
      builder.putAll(loadFixingSeries(timeSeriesResource));
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // loads a single fixing series CSV file
  private static ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> loadFixingSeries(ResourceLocator resourceLocator) {
    Map<ObservableId, LocalDateDoubleTimeSeriesBuilder> builders = new HashMap<>();
    try {
      CsvFile csv = CsvFile.of(resourceLocator.getCharSource(), true);
      for (int i = 0; i < csv.rowCount(); i++) {
        String referenceStr = csv.field(i, REFERENCE_FIELD);
        String dateStr = csv.field(i, DATE_FIELD);
        String valueStr = csv.field(i, VALUE_FIELD);

        Index index = LoaderUtils.findIndex(referenceStr);
        ObservableId id = IndexRateId.of(index);
        LocalDate date = LocalDate.parse(dateStr);
        double value = Double.parseDouble(valueStr);

        LocalDateDoubleTimeSeriesBuilder builder = builders.computeIfAbsent(id, k -> LocalDateDoubleTimeSeries.builder());
        builder.put(date, value);
      }
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException(
          Messages.format("Error processing resource as CSV file: {}", resourceLocator), ex);
    }

    return builders.entrySet().stream()
        .collect(toImmutableMap(
            Map.Entry::getKey,
            v -> v.getValue().build()));
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FixingSeriesCsvLoader() {
  }

}
