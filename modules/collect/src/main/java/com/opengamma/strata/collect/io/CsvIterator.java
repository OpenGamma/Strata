/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.PeekingIterator;
import com.google.common.io.CharSource;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Unchecked;

/**
 * Iterator over the rows of a CSV file.
 * <p>
 * Provides the ability to iterate over a CSV file together with the ability to parse it from a {@link CharSource}.
 * The separator may be specified, allowing TSV files (tab-separated) and other similar formats to be parsed.
 * See {@link CsvFile} for more details of the CSV format.
 * <p>
 * This class processes the CSV file row-by-row.
 * To load the entire CSV file into memory, use {@link CsvFile}.
 * <p>
 * This class must be used in a try-with-resources block to ensure that the underlying CSV file is closed:
 * <pre>
 *  try (CsvIterator csvIterator = CsvIterator.of(source, true)) {
 *    // use the CsvIterator
 *  }
 * </pre>
 * One way to use the iterable is with the for-each loop, using a lambda to adapt {@code Iterator} to {@code Iterable}:
 * <pre>
 *  try (CsvIterator csvIterator = CsvIterator.of(source, true)) {
 *    for (CsvRow row : () -> csvIterator) {
 *      // process the row
 *    }
 *  }
 * </pre>
 * This class also allows the headers to be obtained without reading the whole CSV file:
 * <pre>
 *  try (CsvIterator csvIterator = CsvIterator.of(source, true)) {
 *    ImmutableList{@literal <String>} headers = csvIterator.headers();
 *  }
 * </pre>
 */
public final class CsvIterator implements AutoCloseable, PeekingIterator<CsvRow> {

  /**
   * The buffered reader.
   */
  private final BufferedReader reader;
  /**
   * The separator
   */
  private final char separator;
  /**
   * The header row, ordered as the headers appear in the file.
   */
  private final ImmutableList<String> headers;
  /**
   * The header map, transformed for case-insensitive searching.
   */
  private final ImmutableMap<String, Integer> searchHeaders;
  /**
   * The next row.
   */
  private CsvRow nextRow;

  //------------------------------------------------------------------------
  /**
   * Parses the specified source as a CSV file, using a comma as the separator.
   * <p>
   * This method opens the CSV file for reading.
   * The caller is responsible for closing it by calling {@link #close()}.
   * 
   * @param source  the CSV file resource
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @return the CSV file
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   */
  public static CsvIterator of(CharSource source, boolean headerRow) {
    return of(source, headerRow, ',');
  }

  /**
   * Parses the specified source as a CSV file where the separator is specified and might not be a comma.
   * <p>
   * This overload allows the separator to be controlled.
   * For example, a tab-separated file is very similar to a CSV file, the only difference is the separator.
   * <p>
   * This method opens the CSV file for reading.
   * The caller is responsible for closing it by calling {@link #close()}.
   * 
   * @param source  the file resource
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @param separator  the separator used to separate each field, typically a comma, but a tab is sometimes used
   * @return the CSV file
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   */
  public static CsvIterator of(CharSource source, boolean headerRow, char separator) {
    ArgChecker.notNull(source, "source");
    @SuppressWarnings("resource")
    BufferedReader reader = Unchecked.wrap(() -> source.openBufferedStream());
    return create(reader, headerRow, separator);
  }

  /**
   * Parses the specified reader as a CSV file, using a comma as the separator.
   * <p>
   * This factory method allows the separator to be controlled.
   * For example, a tab-separated file is very similar to a CSV file, the only difference is the separator.
   * <p>
   * The caller is responsible for closing the reader, such as by calling {@link #close()}.
   * 
   * @param reader  the file reader
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @return the CSV file
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   */
  public static CsvIterator of(Reader reader, boolean headerRow) {
    return of(reader, headerRow, ',');
  }

  /**
   * Parses the specified reader as a CSV file where the separator is specified and might not be a comma.
   * <p>
   * This factory method allows the separator to be controlled.
   * For example, a tab-separated file is very similar to a CSV file, the only difference is the separator.
   * <p>
   * The caller is responsible for closing the reader, such as by calling {@link #close()}.
   * 
   * @param reader  the file reader
   * @param headerRow  whether the source has a header row, an empty source must still contain the header
   * @param separator  the separator used to separate each field, typically a comma, but a tab is sometimes used
   * @return the CSV file
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   */
  public static CsvIterator of(Reader reader, boolean headerRow, char separator) {
    ArgChecker.notNull(reader, "reader");
    @SuppressWarnings("resource")
    BufferedReader breader = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    return create(breader, headerRow, separator);
  }

  // create the iterator
  private static CsvIterator create(BufferedReader breader, boolean headerRow, char separator) {
    try {
      if (!headerRow) {
        return new CsvIterator(breader, separator, ImmutableList.of(), ImmutableMap.of());
      }
      String line = breader.readLine();
      if (line == null) {
        throw new IllegalArgumentException("Could not read header row from empty CSV file");
      }
      ImmutableList<String> headers = CsvFile.parseLine(line, separator);
      return new CsvIterator(breader, separator, headers, CsvFile.buildSearchHeaders(headers));

    } catch (RuntimeException ex) {
      try {
        breader.close();
      } catch (IOException ex2) {
        ex.addSuppressed(ex2);
      }
      throw ex;

    } catch (IOException ex) {
      try {
        breader.close();
      } catch (IOException ex2) {
        ex.addSuppressed(ex2);
      }
      throw new UncheckedIOException(ex);
    }
  }

  //------------------------------------------------------------------------
  /**
   * Restricted constructor.
   * 
   * @param reader  the buffered reader
   * @param headers  the header row
   * @param searchHeaders  the search headers
   */
  private CsvIterator(
      BufferedReader reader,
      char separator,
      ImmutableList<String> headers,
      ImmutableMap<String, Integer> searchHeaders) {

    this.reader = reader;
    this.separator = separator;
    this.headers = headers;
    this.searchHeaders = searchHeaders;
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
   * Returns a stream that wraps this iterator.
   * <p>
   * The stream will process any remaining rows in the CSV file.
   * As such, it is recommended that callers should use this method or the iterator methods and not both.
   * 
   * @return the stream wrapping this iterator
   */
  public Stream<CsvRow> asStream() {
    Spliterator<CsvRow> spliterator = Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED | Spliterator.NONNULL);
    return StreamSupport.stream(spliterator, false);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks whether there is another row in the CSV file.
   * 
   * @return true if there is another row, false if not
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   */
  @Override
  public boolean hasNext() {
    if (nextRow != null) {
      return true;
    } else {
      String line = null;
      while ((line = Unchecked.wrap(() -> reader.readLine())) != null) {
        ImmutableList<String> fields = CsvFile.parseLine(line, separator);
        if (!fields.isEmpty()) {
          nextRow = new CsvRow(headers, searchHeaders, fields);
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Peeks the next row from the CSV file without changing the iteration position.
   * 
   * @return the peeked row
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   * @throws NoSuchElementException if the end of file has been reached
   */
  @Override
  public CsvRow peek() {
    if (nextRow != null || hasNext()) {
      return nextRow;
    } else {
      throw new NoSuchElementException("CsvIterator has reached the end of the file");
    }
  }

  /**
   * Returns the next row from the CSV file.
   * 
   * @return the next row
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   * @throws NoSuchElementException if the end of file has been reached
   */
  @Override
  public CsvRow next() {
    if (nextRow != null || hasNext()) {
      CsvRow row = nextRow;
      nextRow = null;
      return row;
    } else {
      throw new NoSuchElementException("CsvIterator has reached the end of the file");
    }
  }

  /**
   * Returns the next batch of rows from the CSV file.
   * <p>
   * This will return up to the specified number of rows from the file at the current iteration point.
   * An empty list is returned if there are no more rows.
   * 
   * @param count  the number of rows to try and get, negative returns an empty list
   * @return the next batch of rows, up to the number requested
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   */
  public List<CsvRow> nextBatch(int count) {
    List<CsvRow> rows = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      if (hasNext()) {
        rows.add(next());
      }
    }
    return rows;
  }

  /**
   * Throws an exception as remove is not supported.
   * 
   * @throws UnsupportedOperationException always
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("CsvIterator does not support remove()");
  }

  /**
   * Closes the underlying reader.
   * 
   * @throws UncheckedIOException if an IO exception occurs
   */
  @Override
  public void close() {
    Unchecked.wrap(() -> reader.close());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a string describing the CSV iterator.
   * 
   * @return the descriptive string
   */
  @Override
  public String toString() {
    return "CsvIterator" + headers.toString();
  }

}
