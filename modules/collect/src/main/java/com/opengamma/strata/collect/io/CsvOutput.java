/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Outputs a CSV formatted file.
 * <p>
 * Provides a simple tool for writing a CSV file.
 * <p>
 * Each line in the CSV file will consist of comma separated entries.
 * Each entry may be quoted using a double quote.
 * If an entry contains a double quote, comma or trimmable whitespace, it will be quoted.
 * If an entry starts with '=' or '@', it will be quoted.
 * Two double quotes will be used to escape a double quote.
 * <p>
 * There are two modes of output.
 * Standard mode provides the encoding described above which is accepted by most CSV parsers.
 * Safe mode provides extra encoding to protect unsafe content from being run as a script in tools like Excel.
 * <p>
 * Instances of this class contain mutable state.
 * A new instance must be created for each file to be output.
 */
public final class CsvOutput {

  // regex for a number
  private static final String DIGITS = "([0-9]+)";
  private static final String EXPONENT = "[eE][+-]?" + DIGITS;
  private static final Pattern FP_REGEX = Pattern.compile(
      "(" + DIGITS + "(\\.)?(" + DIGITS + "?)(" + EXPONENT + ")?)|" +
          "(\\.(" + DIGITS + ")(" + EXPONENT + ")?)");
  private static final String COMMA = ",";
  private static final String NEW_LINE = System.lineSeparator();

  /**
   * The header row, ordered as the headers appear in the file.
   */
  private final Appendable underlying;
  /**
   * The new line string.
   */
  private final String newLine;
  /**
   * The line item separator.
   */
  private final String separator;
  /**
   * Whether expressions should be safely encoded.
   */
  private final boolean safeExpressions;
  /**
   * Whether the writer is currently at the start of a line.
   */
  private boolean lineStarted;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance, using the system default line separator and using a comma separator.
   * <p>
   * See the standard quoting rules in the class-level documentation.
   * 
   * @param underlying  the destination to write to
   * @return the CSV outputter
   */
  public static CsvOutput standard(Appendable underlying) {
    return new CsvOutput(underlying, NEW_LINE, COMMA, false);
  }

  /**
   * Creates an instance, allowing the new line character to be controlled and using a comma separator.
   * <p>
   * See the standard quoting rules in the class-level documentation.
   * 
   * @param underlying  the destination to write to
   * @param newLine  the new line string
   * @return the CSV outputter
   */
  public static CsvOutput standard(Appendable underlying, String newLine) {
    return new CsvOutput(underlying, newLine, COMMA, false);
  }

  /**
   * Creates an instance, allowing the new line character to be controlled, specifying the separator.
   * <p>
   * See the standard quoting rules in the class-level documentation.
   * 
   * @param underlying  the destination to write to
   * @param newLine  the new line string
   * @param separator  the separator used to separate each field, typically a comma, but a tab is sometimes used
   * @return the CSV outputter
   */
  public static CsvOutput standard(Appendable underlying, String newLine, String separator) {
    return new CsvOutput(underlying, newLine, separator, false);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance, using the system default line separator and using a comma separator.
   * <p>
   * This applies the standard quoting rules from the class-level documentation, plus an additional rule.
   * If an entry starts with an expression character, '=', '@', '+' or '-', the entry
   * will be quoted and the quote section will be preceeded by equals.
   * Thus, the string '=Foo' will be written as '="=Foo"'.
   * This avoids the string being treated as an expression by tools like Excel.
   * Simple numbers are not quoted.
   * Thus, the number '-1234' will still be written as '-1234'.
   * 
   * @param underlying  the destination to write to
   * @return the CSV outputter
   */
  public static CsvOutput safe(Appendable underlying) {
    return new CsvOutput(underlying, NEW_LINE, COMMA, true);
  }

  /**
   * Creates an instance, allowing the new line character to be controlled and using a comma separator.
   * <p>
   * This applies the standard quoting rules from the class-level documentation, plus an additional rule.
   * If an entry starts with an expression character, '=', '@', '+' or '-', the entry
   * will be quoted and the quote section will be preceeded by equals.
   * Thus, the string '=Foo' will be written as '="=Foo"'.
   * This avoids the string being treated as an expression by tools like Excel.
   * Simple numbers are not quoted.
   * Thus, the number '-1234' will still be written as '-1234'.
   * 
   * @param underlying  the destination to write to
   * @param newLine  the new line string
   * @return the CSV outputter
   */
  public static CsvOutput safe(Appendable underlying, String newLine) {
    return new CsvOutput(underlying, newLine, COMMA, true);
  }

  /**
   * Creates an instance, allowing the new line character to be controlled, specifying the separator.
   * <p>
   * This applies the standard quoting rules from the class-level documentation, plus an additional rule.
   * If an entry starts with an expression character, '=', '@', '+' or '-', the entry
   * will be quoted and the quote section will be preceeded by equals.
   * Thus, the string '=Foo' will be written as '="=Foo"'.
   * This avoids the string being treated as an expression by tools like Excel.
   * Simple numbers are not quoted.
   * Thus, the number '-1234' will still be written as '-1234'.
   * 
   * @param underlying  the destination to write to
   * @param newLine  the new line string
   * @param separator  the separator used to separate each field, typically a comma, but a tab is sometimes used
   * @return the CSV outputter
   */
  public static CsvOutput safe(Appendable underlying, String newLine, String separator) {
    return new CsvOutput(underlying, newLine, separator, true);
  }

  //-------------------------------------------------------------------------
  // creates an instance
  private CsvOutput(Appendable underlying, String newLine, String separator, boolean safeExpressions) {
    this.underlying = ArgChecker.notNull(underlying, "underlying");
    this.newLine = newLine;
    this.separator = separator;
    this.safeExpressions = safeExpressions;
  }

  //------------------------------------------------------------------------
  /**
   * Writes multiple CSV lines to the underlying.
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
   * Writes a single CSV line to the underlying, only quoting if needed.
   * <p>
   * This can be used as a method reference from a {@code Stream} pipeline from
   * {@link Stream#forEachOrdered(Consumer)}.
   * <p>
   * This method writes each cell in the specified list to the underlying, followed by
   * a new line character.
   *
   * @param line  the line to write
   * @throws UncheckedIOException if an IO exception occurs
   */
  public void writeLine(List<String> line) {
    writeLine(line, false);
  }

  /**
   * Writes a single CSV line to the underlying.
   * <p>
   * The boolean flag controls whether each entry is always quoted or only quoted when necessary.
   * <p>
   * This method writes each cell in the specified list to the underlying, followed by
   * a new line character.
   *
   * @param line  the line to write
   * @param alwaysQuote  when true, each column will be quoted, when false, quoting is selective
   * @throws UncheckedIOException if an IO exception occurs
   */
  public void writeLine(List<String> line, boolean alwaysQuote) {
    ArgChecker.notNull(line, "line");
    for (String cell : line) {
      writeCell(cell, alwaysQuote);
    }
    writeNewLine();
  }

  //-------------------------------------------------------------------------
  /**
   * Writes a single cell to the current line, only quoting if needed.
   * <p>
   * When using this method, either {@link #writeNewLine()} or one of the {@code writeLine}
   * methods must be called at the end of the line.
   *
   * @param cell  the cell to write
   * @return this, for method chaining
   * @throws UncheckedIOException if an IO exception occurs
   */
  public CsvOutput writeCell(String cell) {
    writeCell(cell, false);
    return this;
  }

  /**
   * Writes a single cell to the current line.
   * <p>
   * The boolean flag controls whether each entry is always quoted or only quoted when necessary.
   * <p>
   * When using this method, either {@link #writeNewLine()} or one of the {@code writeLine}
   * methods must be called at the end of the line.
   *
   * @param cell  the cell to write
   * @param alwaysQuote  when true, the cell will be quoted, when false, quoting is selective
   * @return this, for method chaining
   * @throws UncheckedIOException if an IO exception occurs
   */
  public CsvOutput writeCell(String cell, boolean alwaysQuote) {
    try {
      if (lineStarted) {
        underlying.append(separator);
      }
      if (alwaysQuote || isQuotingRequired(cell)) {
        outputQuotedCell(cell);
      } else {
        underlying.append(cell);
      }
      lineStarted = true;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return this;
  }

  /**
   * Writes a new line character.
   *
   * @return this, for method chaining
   * @throws UncheckedIOException if an IO exception occurs
   */
  public CsvOutput writeNewLine() {
    try {
      underlying.append(newLine);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    lineStarted = false;
    return this;
  }

  //-------------------------------------------------------------------------
  // quoting is required if entry contains quote, comma, trimmable whitespace, or starts with an expression character
  private boolean isQuotingRequired(String cell) {
    return cell.indexOf('"') >= 0 ||
        cell.indexOf(',') >= 0 ||
        cell.trim().length() != cell.length() ||
        isExpressionPrefix(cell);
  }

  // checks if quoting should be applied
  private boolean isExpressionPrefix(String cell) {
    if (cell.isEmpty()) {
      return false;
    }
    char first = cell.charAt(0);
    if (first == '=' || first == '@') {
      return true;
    }
    if (safeExpressions && (first == '+' || first == '-')) {
      return !FP_REGEX.matcher(cell.substring(1)).matches();
    }
    return false;
  }

  // quotes the entry
  private void outputQuotedCell(String cell) throws IOException {
    if (safeExpressions && isExpressionPrefix(cell)) {
      underlying.append('=');
    }
    underlying.append('"');
    underlying.append(cell.replace("\"", "\"\""));
    underlying.append('"');
  }

}
