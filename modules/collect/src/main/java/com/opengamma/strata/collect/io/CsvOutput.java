/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
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
 * If an entry starts with '=' or '@', it will be quoted.
 * Two double quotes will be used to escape a double quote.
 * <p>
 * There are two modes of output.
 * Standard mode provides the encoding described above which is accepted by most CSV parsers.
 * Safe mode provides extra encoding to protect unsafe content from being run as a script in tools like Excel.
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
   * Whether expressions should be safely encoded
   */
  private final boolean safeExpressions;

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

  // quoting is required if entry contains quote, comma, trimmable whitespace, or starts with an expression character
  private boolean isQuotingRequired(String entry) {
    return entry.indexOf('"') >= 0 ||
        entry.indexOf(',') >= 0 ||
        entry.trim().length() != entry.length() ||
        isExpressionPrefix(entry);
  }

  // checks if quoting should be applied
  private boolean isExpressionPrefix(String entry) {
    if (entry.isEmpty()) {
      return false;
    }
    char first = entry.charAt(0);
    if (first == '=' || first == '@') {
      return true;
    }
    if (safeExpressions && (first == '+' || first == '-')) {
      return !FP_REGEX.matcher(entry.substring(1)).matches();
    }
    return false;
  }

  // quotes the entry
  private String quotedEntry(String entry) {
    StringBuilder buf = new StringBuilder(entry.length() + 8);
    if (safeExpressions && isExpressionPrefix(entry)) {
      buf.append('=');
    }
    buf.append('"');
    buf.append(entry.replace("\"", "\"\""));
    buf.append('"');
    return buf.toString();
  }

}
