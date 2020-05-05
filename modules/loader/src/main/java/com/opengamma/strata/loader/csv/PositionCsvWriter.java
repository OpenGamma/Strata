/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.loader.csv.PositionCsvLoader.CCP_FIELD;
import static com.opengamma.strata.loader.csv.PositionCsvLoader.DESCRIPTION_FIELD;
import static com.opengamma.strata.loader.csv.PositionCsvLoader.ID_FIELD;
import static com.opengamma.strata.loader.csv.PositionCsvLoader.ID_SCHEME_FIELD;
import static com.opengamma.strata.loader.csv.PositionCsvLoader.NAME_FIELD;
import static com.opengamma.strata.loader.csv.PositionCsvLoader.TYPE_FIELD;
import static java.util.stream.Collectors.groupingBy;

import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.etd.EtdFuturePosition;
import com.opengamma.strata.product.etd.EtdOptionPosition;

/**
 * Writes positions to a CSV file.
 * <p>
 * This takes a Strata {@link Position} instance and creates a matching CSV file.
 */
public final class PositionCsvWriter {

  /**
   * The writers.
   */
  private static final ImmutableMap<Class<?>, PositionTypeCsvWriter<?>> WRITERS =
      ImmutableMap.<Class<?>, PositionTypeCsvWriter<?>>builder()
          .put(EtdFuturePosition.class, EtdFuturePositionCsvPlugin.INSTANCE)
          .put(EtdOptionPosition.class, EtdOptionPositionCsvPlugin.INSTANCE)
          .build();
  /**
   * The header order.
   */
  private static final ImmutableList<String> HEADER_ORDER = ImmutableList.of(
      TYPE_FIELD);
  /**
   * The header comparator.
   */
  private static final Comparator<String> HEADER_COMPARATOR = (str1, str2) -> {
    int index1 = HEADER_ORDER.indexOf(str1);
    int index2 = HEADER_ORDER.indexOf(str2);
    int i1 = index1 >= 0 ? index1 : HEADER_ORDER.size();
    int i2 = index2 >= 0 ? index2 : HEADER_ORDER.size();
    return i1 - i2;
  };

  /**
   * Obtains a standard instance of the writer.
   *
   * @return the writer
   */
  public static PositionCsvWriter standard() {
    return new PositionCsvWriter();
  }

  private PositionCsvWriter() {
  }

  /**
   * Write positions to an appendable in the applicable full details position format.
   * <p>
   * The output is written in full details position format.
   *
   * @param positions the positions to write
   * @param output    the appendable to write to
   * @throws IllegalArgumentException if a position is not supported
   * @throws UncheckedIOException     if an IO error occurs
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void write(List<? extends Position> positions, Appendable output) {
    List<String> headers = headers(positions);
    CsvOutput.CsvRowOutputWithHeaders csv = CsvOutput.standard(output, "\n").withHeaders(headers, false);
    for (Position position : positions) {
      PositionInfo info = position.getInfo();
      info.getId().ifPresent(id -> csv.writeCell(ID_SCHEME_FIELD, id.getScheme()));
      info.getId().ifPresent(id -> csv.writeCell(ID_FIELD, id.getValue()));
      info.findAttribute(AttributeType.DESCRIPTION).ifPresent(str -> csv.writeCell(DESCRIPTION_FIELD, str));
      info.findAttribute(AttributeType.NAME).ifPresent(str -> csv.writeCell(NAME_FIELD, str));
      info.findAttribute(AttributeType.CCP).ifPresent(str -> csv.writeCell(CCP_FIELD, str));
      PositionTypeCsvWriter detailsWriter = WRITERS.get(position.getClass());
      if (detailsWriter == null) {
        throw new IllegalArgumentException("Unable to write position to CSV: " + position.getClass().getSimpleName());
      }
      detailsWriter.writeCsv(csv, position);
    }
  }

  // collect the set of headers that are needed
  @SuppressWarnings({"rawtypes", "unchecked"})
  private List<String> headers(List<? extends Position> positions) {
    Set<String> headers = new LinkedHashSet<>();

    // common headers
    headers.add(TYPE_FIELD);

    if (positions.stream().anyMatch(position -> position.getInfo().getId().isPresent())) {
      headers.add(ID_SCHEME_FIELD);
      headers.add(ID_FIELD);
    }
    if (positions.stream().anyMatch(position -> position.getInfo().findAttribute(AttributeType.DESCRIPTION).isPresent())) {
      headers.add(DESCRIPTION_FIELD);
    }
    if (positions.stream().anyMatch(position -> position.getInfo().findAttribute(AttributeType.NAME).isPresent())) {
      headers.add(NAME_FIELD);
    }
    if (positions.stream().anyMatch(position -> position.getInfo().findAttribute(AttributeType.CCP).isPresent())) {
      headers.add(CCP_FIELD);
    }

    // additional headers
    Map<Class<?>, List<Position>> splitByType = positions.stream().collect(groupingBy(Position::getClass));
    for (Entry<Class<?>, List<Position>> entry : splitByType.entrySet()) {
      PositionTypeCsvWriter detailsWriter = WRITERS.get(entry.getKey());
      if (detailsWriter == null) {
        throw new IllegalArgumentException(
            "Unable to write position to CSV: " + entry.getKey().getSimpleName());
      }
      headers.addAll(detailsWriter.headers(entry.getValue()));
    }

    return headers.stream().sorted(HEADER_COMPARATOR).collect(toImmutableList());
  }

}
