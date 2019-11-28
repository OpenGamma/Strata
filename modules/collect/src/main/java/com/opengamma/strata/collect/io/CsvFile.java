/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
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
 * A quoted field may additionally be prefixed by an equals sign.
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
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using {@link UnicodeBom}.
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
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using {@link UnicodeBom}.
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
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using {@link UnicodeBom}.
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
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using {@link UnicodeBom}.
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
    if (headerRow) {
      for (int i = 0; i < lines.size(); i++) {
        ImmutableList<String> headers = parseLine(lines.get(i), separator);
        if (!headers.isEmpty()) {
          ImmutableMap<String, Integer> searchHeaders = buildSearchHeaders(headers);
          return parseAll(lines, i + 1, separator, headers, searchHeaders);
        }
      }
      throw new IllegalArgumentException("Could not read header row from empty CSV file");
    }
    return parseAll(lines, 0, separator, ImmutableList.of(), ImmutableMap.of());
  }

  //-------------------------------------------------------------------------
  /**
   * Finds the separator used by the specified CSV file.
   * <p>
   * The search includes comma, semicolon, colon, tab and pipe (in that order of priority).
   * <p>
   * The algorithm operates in a number of steps.
   * Firstly, it looks for occurrences where a separator is followed by valid quoted text.
   * If this matches, the separator is assumed to be correct.
   * Secondly, it looks for lines that only consist of a separator.
   * If this matches, the separator is assumed to be correct.
   * Thirdly, it looks to see which separator is the most common on the line.
   * If that separator is also the most common on the next line, and the number of columns matches,
   * the separator is assumed to be correct. Otherwise another line is processed.
   * Thus to match a separator, there must be two lines with the same number of columns.
   * At most, 100 content lines are read from the file.
   * The default is comma if the file is empty.
   * 
   * @param source  the source to read as CSV
   * @return the CSV file
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   */
  public static char findSeparator(CharSource source) {
    String possibleSeparators = ",;\t:|";
    try (BufferedReader breader = source.openBufferedStream()) {
      int bestCount = 0;
      char bestSeparator = ',';
      String line = breader.readLine();
      int contentLines = 0;
      while (line != null && contentLines <= 100) {
        if (line.length() == 0 || line.startsWith("#")) {
          // comment
        } else if (line.startsWith(";") && bestCount == 0 && bestSeparator == ',') {
          // if we see semicolon it could be a start of comment or a semicolon separator
          bestSeparator = ';';
        } else {
          line = simplifyLine(line);
          int lineBestCount = 0;
          char lineBestSeparator = ',';
          for (char separator : possibleSeparators.toCharArray()) {
            // a quote following a separator is a strong marker for the separator
            if (line.contains(separator + "\"\"")) {
              return separator;
            }
            // a line only formed of separators is a strong marker for the separator
            if (line.length() > 1 && line.equals(Strings.repeat(Character.toString(separator), line.length()))) {
              return separator;
            }
            // a minimal line of a separator is a weak marker for the separator
            if (line.length() == 1 && line.charAt(0) == separator && bestCount == 0 && bestSeparator == ',') {
              bestCount = 1;
              bestSeparator = separator;
            }
            // parse the row and see if it is the best match
            if (line.length() > 1) {
              int count = CsvFile.parseLine(line, separator).size();
              if (count > lineBestCount) {
                lineBestCount = count;
                lineBestSeparator = separator;
              }
            }
          }
          if (lineBestCount > 0) {
            contentLines++;
            if (bestCount > 0 && bestCount == lineBestCount && bestSeparator == lineBestSeparator) {
              break;
            }
            bestCount = lineBestCount;
            bestSeparator = lineBestSeparator;
          }
        }
        line = breader.readLine();
      }
      return bestSeparator;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  // simplifies a line, removing quoted sections (which assumes the input is validly quoted)
  private static String simplifyLine(String line) {
    int pos = 0;
    boolean quoteMode = false;
    StringBuilder buf = new StringBuilder(line.length());
    while (pos < line.length()) {
      char ch = line.charAt(pos++);
      if (quoteMode) {
        // currently in quote mode
        if (ch == '"' && pos < line.length() - 1 && line.charAt(pos) == '"') {
          // two double quotes treated as a normal character, as thus skipped
          pos++;
        } else if (ch == '"' || pos == line.length()) {
          // end of quoted section, or end of string with quote not terminated properly
          buf.append('"').append('"');
          quoteMode = false;
        } else {
          // skip characters in quotes
        }
      } else if (ch == '"') {
        // quoted mode
        quoteMode = true;
      } else if (ch == ' ' || ch == '=') {
        // ignore awkward characters
      } else {
        // append all other characters
        buf.append(ch);
      }
    }
    return buf.toString();
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
    ImmutableMap<String, Integer> searchHeaders = buildSearchHeaders(copiedHeaders);
    ImmutableList.Builder<CsvRow> csvRows = ImmutableList.builder();
    int firstLine = copiedHeaders.isEmpty() ? 1 : 2;
    for (int i = 0; i < rows.size(); i++) {
      csvRows.add(new CsvRow(copiedHeaders, searchHeaders, i + firstLine, ImmutableList.copyOf(rows.get(i))));
    }
    return new CsvFile(copiedHeaders, searchHeaders, csvRows.build());
  }

  //------------------------------------------------------------------------
  // parses the CSV file format
  private static CsvFile parseAll(
      List<String> lines,
      int lineIndex,
      char separator,
      ImmutableList<String> headers,
      ImmutableMap<String, Integer> searchHeaders) {

    ImmutableList.Builder<CsvRow> rows = ImmutableList.builder();
    for (int i = lineIndex; i < lines.size(); i++) {
      ImmutableList<String> fields = parseLine(lines.get(i), separator);
      if (!fields.isEmpty()) {
        rows.add(new CsvRow(headers, searchHeaders, i + 1, fields));
      }
    }
    return new CsvFile(headers, searchHeaders, rows.build());
  }

  // parse a single line
  static ImmutableList<String> parseLine(String line, char separator) {
    if (line.length() == 0 || line.startsWith("#") || (line.startsWith(";") && separator != ';')) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    String terminated = line + separator;
    // three modes of parsing - base, value and quote
    // to match other lenient parsers, when quote mode finishes, the mode switches to value with the result combined
    int pos = 0;
    int startPos = 0;
    String value = "";
    boolean valueMode = false;
    boolean quoteMode = false;
    while (pos < terminated.length()) {
      char ch = terminated.charAt(pos++);
      if (quoteMode) {
        // currently in quote mode
        if (ch == '"' && pos < terminated.length() - 1 && terminated.charAt(pos) == '"') {
          // two double quotes will become one
          pos++;
        } else if (ch == '"') {
          // end of quoted section
          value = terminated.substring(startPos, pos - 1).replace("\"\"", "\"");
          startPos = pos;
          quoteMode = false;
        } else if (pos == terminated.length()) {
          // end of string with quote not terminated properly
          builder.add(terminated.substring(startPos, pos - 1).replace("\"\"", "\""));
        }
      } else if (valueMode) {
        // currently in value mode
        if (ch == separator) {
          builder.add(value + terminated.substring(startPos, pos - 1).trim());
          valueMode = false;
          value = "";
        }
      } else if (ch == separator) {
        // handle empty value
        builder.add("");
      } else if (ch == ' ') {
        // ignore spaces after separators
      } else if (ch == '=' && pos < terminated.length() - 1 && terminated.charAt(pos) == '"') {
        // handle convention where ="xxx" means xxx by simply ignoring the equals
      } else if (ch == '"') {
        // quoted mode
        startPos = pos;
        quoteMode = true;
        valueMode = true;
      } else {
        // non-quoted mode
        startPos = pos - 1;
        valueMode = true;
      }
    }
    // check line has content
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
      if (!field.isEmpty()) {
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
   * @param searchHeaders  the headers transformed for searching
   * @param rows  the data rows
   */
  private CsvFile(
      ImmutableList<String> headers,
      ImmutableMap<String, Integer> searchHeaders,
      ImmutableList<CsvRow> rows) {

    this.headers = headers;
    this.searchHeaders = searchHeaders;
    this.rows = rows;
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

  /**
   * Checks if the header is present in the file.
   * <p>
   * Matching is case insensitive.
   * 
   * @param header  the column header to match
   * @return true if the header is present
   */
  public boolean containsHeader(String header) {
    return searchHeaders.containsKey(header.toLowerCase(Locale.ENGLISH));
  }

  /**
   * Checks if the headers are present in the file.
   * <p>
   * Matching is case insensitive.
   * 
   * @param headers  the column headers to match
   * @return true if all the headers are present
   */
  public boolean containsHeaders(Collection<String> headers) {
    return headers.stream().allMatch(this::containsHeader);
  }

  /**
   * Checks if the header pattern is present in the file.
   * <p>
   * Matching is case insensitive.
   * 
   * @param headerPattern  the header pattern to match
   * @return true if the header is present
   */
  public boolean containsHeader(Pattern headerPattern) {
    for (int i = 0; i < headers.size(); i++) {
      if (headerPattern.matcher(headers.get(i)).matches()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns an instance with the specified headers.
   *
   * @param headers  the new headers
   * @return the instance with the specified headers
   */
  public CsvFile withHeaders(List<String> headers) {
    return of(headers, rows.stream().map(CsvRow::fields).collect(toImmutableList()));
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
