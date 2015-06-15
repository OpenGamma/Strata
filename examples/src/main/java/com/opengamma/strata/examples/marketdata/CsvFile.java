/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.opencsv.CSVParser;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;

/**
 * A CSV file.
 * <p>
 * Represents a CSV file together with the ability to parse it from a {@link CharSource}.
 * <p>
 * The CSV file format is a general-purpose comma-separated value format.
 * Each line can contain one or more fields.
 * Each field is separated by a comma character ({@literal ,}).
 * <p>
 * The first line may be treated as a header row.
 * The header row is accessed separately from the data rows.
 * <p>
 * Blank lines are ignored.
 * Lines may be commented with has '#' or semicolon ';'.
 */
public final class CsvFile {

  /**
   * The header row, as the headers appear in the file.
   */
  private final ImmutableList<String> headers;

  /**
   * The header row, transformed for case-insensitive searching.
   */
  private final ImmutableList<String> searchHeaders;

  /**
   * The data rows in the CSV file.
   */
  private final ImmutableList<ImmutableList<String>> lines;

  //------------------------------------------------------------------------
  /**
   * Parses the specified source as a CSV file.
   * 
   * @param source  the CSV file resource
   * @param headerRow  whether the source has a header row
   * @return the CSV file
   */
  public static CsvFile of(CharSource source, boolean headerRow) {
    ArgChecker.notNull(source, "source");
    List<ImmutableList<String>> lines;
    try {
      lines = parse(source);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    if (headerRow) {
      if (lines.isEmpty()) {
        throw new IllegalArgumentException("Could not read header row from empty CSV file");
      }
      ImmutableList<String> headers = lines.remove(0);
      ImmutableList<ImmutableList<String>> dataLines = ImmutableList.copyOf(lines);
      return new CsvFile(headers, dataLines);
    } else {
      return new CsvFile(ImmutableList.copyOf(lines));
    }
  }

  //------------------------------------------------------------------------
  // parses the CSV file format
  private static List<ImmutableList<String>> parse(CharSource source) throws IOException {
    CSVParser parser = new CSVParser();
    List<ImmutableList<String>> lines = new ArrayList<ImmutableList<String>>();
    try (BufferedReader br = source.openBufferedStream()) {
      String line = br.readLine();
      while (line != null) {
        line = line.trim();
        if (!StringUtils.isBlank(line) && !line.startsWith("#") && !line.startsWith(";")) {
          String[] parsedLine = parser.parseLine(line);
          if (hasContent(parsedLine)) {
            lines.add(ImmutableList.copyOf(parsedLine));
          }
        }
        line = br.readLine();
      }
    }
    return lines;
  }

  // determines whether there is any content on a line
  private static boolean hasContent(String[] line) {
    for (String field : line) {
      if (!StringUtils.isBlank(field)) {
        return true;
      }
    }
    return false;
  }

  //------------------------------------------------------------------------
  /**
   * Restricted constructor.
   * <p>
   * Creates an instance containing only data lines.
   * 
   * @param lines  the data lines
   */
  private CsvFile(ImmutableList<ImmutableList<String>> lines) {
    this(null, lines);
  }

  /**
   * Restricted constructor.
   * <p>
   * Creates an instance containing a header row and data lines, with a header map for retrieving
   * fields in the expected order.
   * 
   * @param headers  the header row
   * @param searchHeaders  the headers used for case-insensitive searching
   * @param lines  the data lines
   */
  private CsvFile(ImmutableList<String> headers, ImmutableList<ImmutableList<String>> lines) {
    this.headers = headers;
    this.searchHeaders = headers == null ? null :
        headers.stream()
            .map(h -> h.toLowerCase())
            .collect(toImmutableList());
    this.lines = lines;
  }

  //------------------------------------------------------------------------
  /**
   * Returns the header row, if specified when parsing the file.
   * 
   * @return the header row
   */
  public Optional<ImmutableList<String>> headers() {
    return Optional.ofNullable(headers);
  }

  /**
   * Returns all data lines in the file.
   * 
   * @return the data lines
   */
  public ImmutableList<ImmutableList<String>> lines() {
    return lines;
  }

  /**
   * Returns the number of data lines.
   * 
   * @return the number of data lines
   */
  public int lineCount() {
    return lines.size();
  }

  /**
   * Returns a single line.
   * 
   * @param index  the line index
   * @return the line
   */
  public ImmutableList<String> line(int index) {
    return lines.get(index);
  }

  /**
   * Gets a single field value from a line by column header.
   * 
   * @param lineIndex  the line index
   * @param header  the column header
   * @return the field value
   */
  public String field(int lineIndex, String header) {
    if (headers == null) {
      throw new UnsupportedOperationException("CSV file does not contain a header row");
    }
    int headerIndex = searchHeaders.indexOf(header.toLowerCase());
    if (headerIndex == -1) {
      throw new IllegalArgumentException(
          Messages.format("Header not found: {}", header));
    }
    return line(lineIndex).get(headerIndex);
  }

}
