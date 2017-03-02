/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Unchecked;

/**
 * Outputs a CSV formatted file.
 * <p>
 * Provides a simple tool for writing a CSV file.
 * <p>
 * Each line in the CSV file will consist of comma separated entries.
 * Each entry may be quoted using a double quote.
 * If an entry contains a double quote, comma or trimmable whitespace, it will be quoted.
 * Two double quotes will be used to escape a double quote.
 */
public final class CsvOutput {

  /**
   * The header row, ordered as the headers appear in the file.
   */
  private final Appendable underlying;
  /**
   * The new line string.
   */
  private final String newLine;
  /**
   * The line item separator
   */
  private final String separator;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance, using the system default line separator and using a comma separator.
   * 
   * @param underlying  the underlying writer
   */
  public CsvOutput(Appendable underlying) {
    this(underlying, System.lineSeparator(), ",");
  }

  /**
   * Creates an instance, allowing the new line character to be controlled and using a comma separator.
   * 
   * @param underlying  the underlying writer
   * @param newLine  the new line string
   */
  public CsvOutput(Appendable underlying, String newLine) {
    this(underlying, newLine, ",");
  }

  /**
   * Creates an instance, allowing the new line character to be controlled, specifying the separator.
   * 
   * @param underlying  the underlying writer
   * @param newLine  the new line string
   * @param separator  the separator used to separate each field, typically a comma, but a tab is sometimes used
   */
  public CsvOutput(Appendable underlying, String newLine, String separator) {
    this.underlying = ArgChecker.notNull(underlying, "underlying");
    this.newLine = newLine;
    this.separator = separator;
  }

  //------------------------------------------------------------------------
  /**
   * Writes CSV lines to the underlying.
   * <p>
   * The boolean flag controls whether each entry is always quoted or only quoted when necessary.
   *
   * @param lines  the lines to write
   * @param alwaysQuote  when true, each column will be quoted, when false, quoting is selective
   * @throws UncheckedIOException if an IO exception occurs
   */
  public void writeLines(Iterable<? extends List<String>> lines, boolean alwaysQuote) {
    ArgChecker.notNull(lines, "lines");
    for (List<String> line : lines) {
      writeLine(line, alwaysQuote);
    }
  }

  /**
   * Writes a CSV line to the underlying, only quoting if needed.
   * <p>
   * This can be used as a method reference from a {@code Stream} pipeline from
   * {@link Stream#forEachOrdered(Consumer)}.
   *
   * @param line  the line to write
   * @throws UncheckedIOException if an IO exception occurs
   */
  public void writeLine(List<String> line) {
    writeLine(line, false);
  }

  /**
   * Writes a CSV line to the underlying.
   * <p>
   * The boolean flag controls whether each entry is always quoted or only quoted when necessary.
   *
   * @param line  the line to write
   * @param alwaysQuote  when true, each column will be quoted, when false, quoting is selective
   * @throws UncheckedIOException if an IO exception occurs
   */
  public void writeLine(List<String> line, boolean alwaysQuote) {
    ArgChecker.notNull(line, "line");
    Unchecked.wrap(() -> underlying.append(formatLine(line, alwaysQuote)).append(newLine));
  }

  // formats the line
  private String formatLine(List<String> line, boolean alwaysQuote) {
    return line.stream()
        .map(entry -> formatEntry(entry, alwaysQuote))
        .collect(Collectors.joining(separator));
  }

  // formats a single entry, quoting if necessary
  private String formatEntry(String entry, boolean alwaysQuote) {
    if (alwaysQuote || isQuotingRequired(entry)) {
      return quotedEntry(entry);
    } else {
      return entry;
    }
  }

  // quoting is required if entry contains quote, comma or trimmable whitespace
  private boolean isQuotingRequired(String line) {
    return line.indexOf('"') >= 0 || line.indexOf(',') >= 0 || line.trim().length() != line.length();
  }

  // quotes the entry
  private String quotedEntry(String entry) {
    StringBuilder buf = new StringBuilder(entry.length() + 8);
    buf.append('"')
        .append(entry.replace("\"", "\"\""))
        .append('"');
    return buf.toString();
  }

}
