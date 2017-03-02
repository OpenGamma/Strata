/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Unchecked;

/**
 * A CSV file.
 * <p>
 * Represents a CSV file together with the ability to parse it from a {@link CharSource}.
 * The separator may be specified, allowing TSV files (tab-separated) and other similar formats to be parsed.
 * <p>
 * This class loads the entire CSV file into memory.
 * To process the CSV file row-by-row, use {@link CsvIterator}.
 * <p>
 * The CSV file format is a general-purpose comma-separated value format.
 * The format is parsed line-by-line, with lines separated by CR, LF or CRLF.
 * Each line can contain one or more fields.
 * Each field is separated by a comma character ({@literal ,}) or tab.
 * Any field may be quoted using a double quote at the start and end.
 * The content of a quoted field may include commas and additional double quotes.
 * Two adjacent double quotes in a quoted field will be replaced by a single double quote.
 * Quoted fields are not trimmed. Non-quoted fields are trimmed.
 * <p>
 * The first line may be treated as a header row.
 * The header row is accessed separately from the data rows.
 * <p>
 * Blank lines are ignored.
 * Lines may be commented with has '#' or semicolon ';'.
 */
public final class CsvFile {

  /**
   * The header row, ordered as the headers appear in the file.
   */
  private final ImmutableList<String> headers;
  /**
   * The header map, transformed for case-insensitive searching.
   */
  private final ImmutableMap<String, Integer> searchHeaders;
  /**
   * The data rows in the CSV file.
   */
  private final ImmutableList<CsvRow> rows;

  //------------------------------------------------------------------------
  /**
   * Parses the specified source as a CSV file, using a comma as the separator.
   * 
   * @param source  the CSV file resource
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @return the CSV file
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   */
  public static CsvFile of(CharSource source, boolean headerRow) {
    return of(source, headerRow, ',');
  }

  /**
   * Parses the specified source as a CSV file where the separator is specified and might not be a comma.
   * <p>
   * This overload allows the separator to be controlled.
   * For example, a tab-separated file is very similar to a CSV file, the only difference is the separator.
   * 
   * @param source  the file resource
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @param separator  the separator used to separate each field, typically a comma, but a tab is sometimes used
   * @return the CSV file
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   */
  public static CsvFile of(CharSource source, boolean headerRow, char separator) {
    ArgChecker.notNull(source, "source");
    List<String> lines = Unchecked.wrap(() -> source.readLines());
    return create(lines, headerRow, separator);
  }

  /**
   * Parses the specified reader as a CSV file, using a comma as the separator.
   * <p>
   * This factory method takes a {@link Reader}.
   * Callers are encouraged to use {@link CharSource} instead of {@code Reader}
   * as it allows the resource to be safely managed.
   * <p>
   * This factory method allows the separator to be controlled.
   * For example, a tab-separated file is very similar to a CSV file, the only difference is the separator.
   * 
   * @param reader  the file resource
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @return the CSV file
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   */
  public static CsvFile of(Reader reader, boolean headerRow) {
    return of(reader, headerRow, ',');
  }

  /**
   * Parses the specified reader as a CSV file where the separator is specified and might not be a comma.
   * <p>
   * This factory method takes a {@link Reader}.
   * Callers are encouraged to use {@link CharSource} instead of {@code Reader}
   * as it allows the resource to be safely managed.
   * <p>
   * This factory method allows the separator to be controlled.
   * For example, a tab-separated file is very similar to a CSV file, the only difference is the separator.
   * 
   * @param reader  the file resource
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @param separator  the separator used to separate each field, typically a comma, but a tab is sometimes used
   * @return the CSV file
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   */
  public static CsvFile of(Reader reader, boolean headerRow, char separator) {
    ArgChecker.notNull(reader, "source");
    List<String> lines = Unchecked.wrap(() -> CharStreams.readLines(reader));
    return create(lines, headerRow, separator);
  }

  // creates the file
  private static CsvFile create(List<String> lines, boolean headerRow, char separator) {
    ArrayList<ImmutableList<String>> parsedCsv = parseAll(lines, separator);
    if (!headerRow) {
      return new CsvFile(ImmutableList.of(), ImmutableMap.of(), ImmutableList.copyOf(parsedCsv));
    }
    if (parsedCsv.isEmpty()) {
      throw new IllegalArgumentException("Could not read header row from empty CSV file");
    }
    ImmutableList<String> headers = parsedCsv.remove(0);
    return new CsvFile(headers, buildSearchHeaders(headers), ImmutableList.copyOf(parsedCsv));
  }

  //------------------------------------------------------------------------
  /**
   * Obtains an instance from a list of headers and rows.
   * <p>
   * The headers may be an empty list.
   * All the rows must contain a list of the same size, matching the header if present.
   * 
   * @param headers  the headers, empty if no headers
   * @param rows  the data rows
   * @return the CSV file
   * @throws IllegalArgumentException if the rows do not match the headers
   */
  public static CsvFile of(List<String> headers, List<? extends List<String>> rows) {
    ArgChecker.notNull(headers, "headers");
    ArgChecker.notNull(rows, "rows");
    int size = (headers.size() == 0 && rows.size() > 0 ? rows.get(0).size() : headers.size());
    if (rows.stream().filter(row -> row.size() != size).findAny().isPresent()) {
      throw new IllegalArgumentException("Invalid data rows, each row must have same columns as header row");
    }
    ImmutableList<String> copiedHeaders = ImmutableList.copyOf(headers);
    ImmutableList<ImmutableList<String>> copiedRows = rows.stream()
        .map(row -> ImmutableList.copyOf(row))
        .collect(toImmutableList());
    return new CsvFile(copiedHeaders, buildSearchHeaders(copiedHeaders), copiedRows);
  }

  //------------------------------------------------------------------------
  // parses the CSV file format
  private static ArrayList<ImmutableList<String>> parseAll(List<String> lines, char separator) {
    ArrayList<ImmutableList<String>> parsedLines = new ArrayList<>();
    for (String line : lines) {
      ImmutableList<String> parsed = parseLine(line, separator);
      if (!parsed.isEmpty()) {
        parsedLines.add(parsed);
      }
    }
    return parsedLines;
  }

  // parse a single line
  static ImmutableList<String> parseLine(String line, char separator) {
    if (line.length() == 0 || line.startsWith("#") || line.startsWith(";")) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    int start = 0;
    String terminated = line + separator;
    int nextSeparator = terminated.indexOf(separator, start);
    while (nextSeparator >= 0) {
      String possible = terminated.substring(start, nextSeparator).trim();
      if (possible.startsWith("\"")) {
        while (true) {
          if (possible.substring(1).replace("\"\"", "").endsWith("\"")) {
            possible = possible.substring(1, possible.length() - 1).replace("\"\"", "\"");
            break;
          } else {
            nextSeparator = terminated.indexOf(separator, nextSeparator + 1);
            if (nextSeparator < 0) {
              throw new IllegalArgumentException("Mismatched quotes on line: " + line);
            }
            possible = terminated.substring(start, nextSeparator).trim();
          }
        }
      }
      builder.add(possible);
      start = nextSeparator + 1;
      nextSeparator = terminated.indexOf(separator, start);
    }
    ImmutableList<String> fields = builder.build();
    if (!hasContent(fields)) {
      return ImmutableList.of();
    }
    return fields;
  }

  // determines whether there is any content on a line
  // this handles lines that contain separators but nothing else
  private static boolean hasContent(ImmutableList<String> fields) {
    for (String field : fields) {
      if (!field.trim().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  // build the search headers
  static ImmutableMap<String, Integer> buildSearchHeaders(ImmutableList<String> headers) {
    // need to allow duplicate headers and only store the first instance
    Map<String, Integer> searchHeaders = new HashMap<>();
    for (int i = 0; i < headers.size(); i++) {
      String searchHeader = headers.get(i).toLowerCase(Locale.ENGLISH);
      searchHeaders.putIfAbsent(searchHeader, i);
    }
    return ImmutableMap.copyOf(searchHeaders);
  }

  //------------------------------------------------------------------------
  /**
   * Restricted constructor.
   * 
   * @param headers  the header row
   * @param rows  the data rows
   */
  private CsvFile(
      ImmutableList<String> headers,
      ImmutableMap<String, Integer> searchHeaders,
      ImmutableList<ImmutableList<String>> rows) {

    this.headers = headers;
    this.searchHeaders = searchHeaders;
    this.rows = rows.stream()
        .map(cols -> new CsvRow(headers, this.searchHeaders, cols))
        .collect(toImmutableList());
  }

  //------------------------------------------------------------------------
  /**
   * Gets the header row.
   * <p>
   * If there is no header row, an empty list is returned.
   * 
   * @return the header row
   */
  public ImmutableList<String> headers() {
    return headers;
  }

  /**
   * Gets all data rows in the file.
   * 
   * @return the data rows
   */
  public ImmutableList<CsvRow> rows() {
    return rows;
  }

  /**
   * Gets the number of data rows.
   * 
   * @return the number of data rows
   */
  public int rowCount() {
    return rows.size();
  }

  /**
   * Gets a single row.
   * 
   * @param index  the row index, zero-based
   * @return the row
   */
  public CsvRow row(int index) {
    return rows.get(index);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this CSV file equals another.
   * <p>
   * The comparison checks the content.
   * 
   * @param obj  the other file, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof CsvFile) {
      CsvFile other = (CsvFile) obj;
      return headers.equals(other.headers) && rows.equals(other.rows);
    }
    return false;
  }

  /**
   * Returns a suitable hash code for the CSV file.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return headers.hashCode() ^ rows.hashCode();
  }

  /**
   * Returns a string describing the CSV file.
   * 
   * @return the descriptive string
   */
  @Override
  public String toString() {
    return "CsvFile" + headers.toString();
  }

}
