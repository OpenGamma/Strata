/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata.timeseries;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.examples.marketdata.LoaderUtils;
import com.opengamma.strata.market.id.IndexRateId;

/**
 * Loads a set of historical fixing series into memory from CSV resources.
 * <p>
 * The resources are expected to be in a CSV format, with the following header row:
 * Type, Reference, Date, Value
 * <p>
 * Each fixing series must be contained entirely within a single resource, but each resource may
 * contain more than one series. The fixing series points do not need to be ordered.
 */
public class FixingSeriesCsvLoader {

  private static final String REFERENCE_FIELD = "Reference";
  private static final String DATE_FIELD = "Date";
  private static final String VALUE_FIELD = "Value";

  /**
   * Restricted constructor.
   */
  private FixingSeriesCsvLoader() {
  }

  //-------------------------------------------------------------------------
  /**
   * Loads a set of historical fixing series into memory from CSV resources.
   * 
   * @param fixingSeriesResources  the fixing series CSV resources
   * @return the loaded fixing series, mapped by {@linkplain ObservableId observable ID}
   */
  public static Map<ObservableId, LocalDateDoubleTimeSeries> loadFixingSeries(Collection<ResourceLocator> fixingSeriesResources) {
    ImmutableMap.Builder<ObservableId, LocalDateDoubleTimeSeries> builder = ImmutableMap.builder();
    for (ResourceLocator timeSeriesResource : fixingSeriesResources) {
      // builder ensures keys can only be seen once
      builder.putAll(loadFixingSeries(timeSeriesResource));
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // loads a single fixing series CSV file
  private static Map<ObservableId, LocalDateDoubleTimeSeries> loadFixingSeries(ResourceLocator resourceLocator) {
    Map<ObservableId, LocalDateDoubleTimeSeriesBuilder> builders = new HashMap<>();
    CsvFile csv;
    try {
      csv = CsvFile.of(resourceLocator.getCharSource(), true);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          Messages.format("Error processing resource as CSV file: {}", resourceLocator), e);
    }
    for (int i = 0; i < csv.rowCount(); i++) {
      String reference = csv.field(i, REFERENCE_FIELD);
      String dateText = csv.field(i, DATE_FIELD);
      String valueText = csv.field(i, VALUE_FIELD);

      RateIndex index = LoaderUtils.findIndex(reference);
      ObservableId id = IndexRateId.of(index);

      LocalDate date = LocalDate.parse(dateText);
      double value = Double.parseDouble(valueText);

      LocalDateDoubleTimeSeriesBuilder builder = builders.get(id);
      if (builder == null) {
        builder = LocalDateDoubleTimeSeries.builder();
        builders.put(id, builder);
      }
      builder.put(date, value);
    }

    return builders.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            v -> v.getValue().build()));
  }

}
