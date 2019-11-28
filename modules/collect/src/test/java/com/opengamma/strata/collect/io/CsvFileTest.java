/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.File;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

/**
 * Test {@link CsvFile}.
 */
public class CsvFileTest {

  private static final Object ANOTHER_TYPE = "";

  private static final String CSV1 = "" +
      "h1,h2\n" +
      "r11,r12\n" +
      "r21 ,r22\n" +
      "r31,";

  private static final String CSV1T = "" +
      "h1\th2\n" +
      "r11\tr12\n" +
      "r21\tr22";

  private static final String CSV2 = "" +
      "h1,h2\n" +
      "#r11,r12\n" +
      ";r11,r12\n" +
      "\n" +
      "r21,r22\n";

  private static final String CSV3 = "" +
      "r11,r12\n" +
      ",\n" +
      "r21,r22\n";

  private static final String CSV4 = "" +
      "\"alpha\",\"be, \"\"at\"\", one\"\n" +
      "\"alpha\"\",\"be\"\"\", \"\"at\"\", one\"\n" +
      "r21,\" r22 \"\n";

  private static final String CSV4B = "" +
      "=\"alpha\",=\"be, \"\"at\"\", one\"\n" +
      "r21,=\" r22 \"\n";

  private static final String CSV5 = "" +
      "a,b,c,b,c\n" +
      "aa,b1,c1,b2,c2\n";

  private static final String CSV6 = "" +
      "a,b,c\n" +
      "r11\n" +
      "r21,r22";

  private static final String CSV7 = "" +
      "# Comment about the file\n" +
      "h1,h2\n" +
      "r1,r2\n";

  //-------------------------------------------------------------------------
  @Test
  public void test_of_ioException() {
    assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> CsvFile.of(Files.asCharSource(new File("src/test/resources"), StandardCharsets.UTF_8), false));
  }

  @Test
  public void test_of_empty_no_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(""), false);
    assertThat(csvFile.headers().size()).isEqualTo(0);
    assertThat(csvFile.rowCount()).isEqualTo(0);
    assertThat(csvFile.containsHeader("Foo")).isFalse();
    assertThat(csvFile.containsHeaders(ImmutableSet.of())).isTrue();
    assertThat(csvFile.containsHeaders(ImmutableSet.of("foo"))).isFalse();
    assertThat(csvFile.containsHeader(Pattern.compile("Foo"))).isFalse();
  }

  @Test
  public void test_of_empty_with_header() {
    assertThatIllegalArgumentException().isThrownBy(() -> CsvFile.of(CharSource.wrap(""), true));
  }

  @Test
  public void test_of_simple_no_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), false);
    assertThat(csvFile.headers().size()).isEqualTo(0);
    assertThat(csvFile.rowCount()).isEqualTo(4);
    assertThat(csvFile.containsHeader("Foo")).isFalse();
    assertThat(csvFile.containsHeader(Pattern.compile("Foo"))).isFalse();
    assertThat(csvFile.row(0).lineNumber()).isEqualTo(1);
    assertThat(csvFile.row(1).lineNumber()).isEqualTo(2);
    assertThat(csvFile.row(2).lineNumber()).isEqualTo(3);
    assertThat(csvFile.row(3).lineNumber()).isEqualTo(4);

    assertThat(csvFile.row(0).headers().size()).isEqualTo(0);
    assertThat(csvFile.row(0).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(0).field(0)).isEqualTo("h1");
    assertThat(csvFile.row(0).field(1)).isEqualTo("h2");
    assertThat(csvFile.row(1).headers().size()).isEqualTo(0);
    assertThat(csvFile.row(1).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(1).field(0)).isEqualTo("r11");
    assertThat(csvFile.row(1).field(1)).isEqualTo("r12");
    assertThat(csvFile.row(2).headers().size()).isEqualTo(0);
    assertThat(csvFile.row(2).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(2).field(0)).isEqualTo("r21");
    assertThat(csvFile.row(2).field(1)).isEqualTo("r22");

    assertThat(csvFile.row(0).subRow(0).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(0).subRow(1).fieldCount()).isEqualTo(1);
    assertThat(csvFile.row(0).subRow(2).fieldCount()).isEqualTo(0);
  }

  @Test
  public void test_of_simple_no_header_tabs() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1T), false, '\t');
    assertThat(csvFile.headers().size()).isEqualTo(0);
    assertThat(csvFile.containsHeader("Foo")).isFalse();
    assertThat(csvFile.containsHeader(Pattern.compile("Foo"))).isFalse();
    assertThat(csvFile.rowCount()).isEqualTo(3);
    assertThat(csvFile.row(0).lineNumber()).isEqualTo(1);
    assertThat(csvFile.row(1).lineNumber()).isEqualTo(2);
    assertThat(csvFile.row(2).lineNumber()).isEqualTo(3);

    assertThat(csvFile.row(0).headers().size()).isEqualTo(0);
    assertThat(csvFile.row(0).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(0).field(0)).isEqualTo("h1");
    assertThat(csvFile.row(0).field(1)).isEqualTo("h2");
    assertThat(csvFile.row(1).headers().size()).isEqualTo(0);
    assertThat(csvFile.row(1).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(1).field(0)).isEqualTo("r11");
    assertThat(csvFile.row(1).field(1)).isEqualTo("r12");
    assertThat(csvFile.row(2).headers().size()).isEqualTo(0);
    assertThat(csvFile.row(2).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(2).field(0)).isEqualTo("r21");
    assertThat(csvFile.row(2).field(1)).isEqualTo("r22");
  }

  @Test
  public void test_of_simple_with_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), true);
    assertThat(csvFile.containsHeader("Foo")).isFalse();
    assertThat(csvFile.containsHeader("h1")).isTrue();
    assertThat(csvFile.containsHeaders(ImmutableSet.of())).isTrue();
    assertThat(csvFile.containsHeaders(ImmutableSet.of("h1"))).isTrue();
    assertThat(csvFile.containsHeaders(ImmutableSet.of("h1", "h2"))).isTrue();
    assertThat(csvFile.containsHeaders(ImmutableSet.of("h1", "h2", "h3"))).isFalse();
    assertThat(csvFile.containsHeader(Pattern.compile("Foo"))).isFalse();
    assertThat(csvFile.containsHeader(Pattern.compile("h[0-9]"))).isTrue();
    ImmutableList<String> headers = csvFile.headers();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get(0)).isEqualTo("h1");
    assertThat(headers.get(1)).isEqualTo("h2");
    assertThat(csvFile.rowCount()).isEqualTo(3);
    assertThat(csvFile.row(0).lineNumber()).isEqualTo(2);
    assertThat(csvFile.row(1).lineNumber()).isEqualTo(3);
    assertThat(csvFile.row(2).lineNumber()).isEqualTo(4);

    assertThat(csvFile.row(0).headers()).isEqualTo(headers);
    assertThat(csvFile.row(0).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(0).field(0)).isEqualTo("r11");
    assertThat(csvFile.row(0).field(1)).isEqualTo("r12");
    assertThat(csvFile.row(1).headers()).isEqualTo(headers);
    assertThat(csvFile.row(1).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(1).field(0)).isEqualTo("r21");
    assertThat(csvFile.row(1).field(1)).isEqualTo("r22");

    assertThat(csvFile.row(0).getField("h1")).isEqualTo("r11");
    assertThat(csvFile.row(0).getField("h2")).isEqualTo("r12");
    assertThat(csvFile.row(1).getField("h1")).isEqualTo("r21");
    assertThat(csvFile.row(1).getField("h2")).isEqualTo("r22");
    assertThatIllegalArgumentException().isThrownBy(() -> csvFile.row(0).getField("zzz"));

    assertThat(csvFile.row(0).getValue("h1")).isEqualTo("r11");
    assertThat(csvFile.row(0).getValue("h2")).isEqualTo("r12");
    assertThat(csvFile.row(1).getValue("h1")).isEqualTo("r21");
    assertThat(csvFile.row(1).getValue("h2")).isEqualTo("r22");
    assertThatIllegalArgumentException().isThrownBy(() -> csvFile.row(0).getValue("zzz"));
    assertThatIllegalArgumentException().isThrownBy(() -> csvFile.row(2).getValue("h2"));

    assertThat(csvFile.row(0).findField("h1")).isEqualTo(Optional.of("r11"));
    assertThat(csvFile.row(0).findField("h2")).isEqualTo(Optional.of("r12"));
    assertThat(csvFile.row(1).findField("h1")).isEqualTo(Optional.of("r21"));
    assertThat(csvFile.row(1).findField("h2")).isEqualTo(Optional.of("r22"));
    assertThat(csvFile.row(0).findField("zzz")).isEqualTo(Optional.empty());

    assertThat(csvFile.row(0).findValue("h1")).isEqualTo(Optional.of("r11"));
    assertThat(csvFile.row(0).findValue("h2")).isEqualTo(Optional.of("r12"));
    assertThat(csvFile.row(1).findValue("h1")).isEqualTo(Optional.of("r21"));
    assertThat(csvFile.row(1).findValue("h2")).isEqualTo(Optional.of("r22"));
    assertThat(csvFile.row(0).findValue("zzz")).isEqualTo(Optional.empty());
    assertThat(csvFile.row(2).findValue("h2")).isEqualTo(Optional.empty());

    assertThat(csvFile.row(0).getField(Pattern.compile("h[13]"))).isEqualTo("r11");
    assertThat(csvFile.row(0).getField(Pattern.compile("h[24]"))).isEqualTo("r12");
    assertThatIllegalArgumentException().isThrownBy(() -> csvFile.row(0).getField(Pattern.compile("zzz")));

    assertThat(csvFile.row(0).getValue(Pattern.compile("h[13]"))).isEqualTo("r11");
    assertThat(csvFile.row(0).getValue(Pattern.compile("h[24]"))).isEqualTo("r12");
    assertThatIllegalArgumentException().isThrownBy(() -> csvFile.row(0).getValue(Pattern.compile("zzz")));
    assertThatIllegalArgumentException().isThrownBy(() -> csvFile.row(2).getValue(Pattern.compile("h2")));

    assertThat(csvFile.row(0).findField(Pattern.compile("h[13]"))).hasValue("r11");
    assertThat(csvFile.row(0).findField(Pattern.compile("h[24]"))).hasValue("r12");
    assertThat(csvFile.row(0).findField(Pattern.compile("zzz"))).isEqualTo(Optional.empty());

    assertThat(csvFile.row(0).findValue(Pattern.compile("h[13]"))).hasValue("r11");
    assertThat(csvFile.row(0).findValue(Pattern.compile("h[24]"))).hasValue("r12");
    assertThat(csvFile.row(0).findValue(Pattern.compile("zzz"))).isEqualTo(Optional.empty());
    assertThat(csvFile.row(2).findValue(Pattern.compile("h2"))).isEqualTo(Optional.empty());

    assertThat(csvFile.row(0).subRow(0).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(0).subRow(1).fieldCount()).isEqualTo(1);
    assertThat(csvFile.row(0).subRow(2).fieldCount()).isEqualTo(0);

    assertThat(csvFile.row(0).subRow(0, 0).fieldCount()).isEqualTo(0);
    assertThat(csvFile.row(0).subRow(0, 1).fieldCount()).isEqualTo(1);
    assertThat(csvFile.row(0).subRow(2, 2).fieldCount()).isEqualTo(0);
  }

  @Test
  public void test_of_duplicate_headers() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV5), true);
    assertThat(csvFile.headers()).isEqualTo(ImmutableList.of("a", "b", "c", "b", "c"));
    assertThat(csvFile.containsHeader("Foo")).isFalse();
    assertThat(csvFile.containsHeader("a")).isTrue();
    assertThat(csvFile.row(0).getField("a")).isEqualTo("aa");
    assertThat(csvFile.row(0).getField("b")).isEqualTo("b1");
    assertThat(csvFile.row(0).getField("c")).isEqualTo("c1");

    assertThat(csvFile.row(0).subRow(1, 3).getField("b")).isEqualTo("b1");
    assertThat(csvFile.row(0).subRow(1, 3).getField("c")).isEqualTo("c1");
    assertThat(csvFile.row(0).subRow(3).getField("b")).isEqualTo("b2");
    assertThat(csvFile.row(0).subRow(3).getField("c")).isEqualTo("c2");
  }

  @Test
  public void test_of_short_data_row() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV6), true);
    assertThat(csvFile.headers()).isEqualTo(ImmutableList.of("a", "b", "c"));
    assertThat(csvFile.row(0).getField("a")).isEqualTo("r11");
    assertThat(csvFile.row(0).getField("b")).isEqualTo("");
    assertThat(csvFile.row(0).getField("c")).isEqualTo("");
    assertThat(csvFile.row(0).field(0)).isEqualTo("r11");
    assertThat(csvFile.row(0).field(1)).isEqualTo("");
    assertThat(csvFile.row(0).field(2)).isEqualTo("");
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> csvFile.row(0).field(4));

    assertThat(csvFile.row(1).getField("a")).isEqualTo("r21");
    assertThat(csvFile.row(1).getField("b")).isEqualTo("r22");
    assertThat(csvFile.row(1).getField("c")).isEqualTo("");
  }

  @Test
  public void test_of_comment_blank_no_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV2), false);
    assertThat(csvFile.headers().size()).isEqualTo(0);
    assertThat(csvFile.rowCount()).isEqualTo(2);
    assertThat(csvFile.row(0).lineNumber()).isEqualTo(1);
    assertThat(csvFile.row(1).lineNumber()).isEqualTo(5);

    assertThat(csvFile.row(0).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(0).field(0)).isEqualTo("h1");
    assertThat(csvFile.row(0).field(1)).isEqualTo("h2");
    assertThat(csvFile.row(1).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(1).field(0)).isEqualTo("r21");
    assertThat(csvFile.row(1).field(1)).isEqualTo("r22");
  }

  @Test
  public void test_of_comment_blank_with_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV2), true);
    ImmutableList<String> headers = csvFile.headers();
    assertThat(headers.size()).isEqualTo(2);
    assertThat(headers.get(0)).isEqualTo("h1");
    assertThat(headers.get(1)).isEqualTo("h2");
    assertThat(csvFile.rows().size()).isEqualTo(1);
    assertThat(csvFile.rowCount()).isEqualTo(1);
    assertThat(csvFile.row(0).lineNumber()).isEqualTo(5);

    assertThat(csvFile.row(0).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(0).field(0)).isEqualTo("r21");
    assertThat(csvFile.row(0).field(1)).isEqualTo("r22");
    assertThat(csvFile.rows().get(0)).isEqualTo(csvFile.row(0));
  }

  @Test
  public void test_of_simple_no_header_access_by_field() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), false);
    assertThat(csvFile.row(0).findField("h1")).isEqualTo(Optional.empty());
    assertThatIllegalArgumentException().isThrownBy(() -> csvFile.row(0).getField("h1"));
  }

  @Test
  public void test_of_simple_with_header_access_by_invalid_field() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), true);
    assertThat(csvFile.row(0).findField("h3")).isEqualTo(Optional.empty());
    assertThatIllegalArgumentException().isThrownBy(() -> csvFile.row(0).getField("h3"));
  }

  @Test
  public void test_of_blank_row() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV3), false);
    assertThat(csvFile.rowCount()).isEqualTo(2);
    assertThat(csvFile.row(0).lineNumber()).isEqualTo(1);
    assertThat(csvFile.row(1).lineNumber()).isEqualTo(3);

    assertThat(csvFile.row(0).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(0).field(0)).isEqualTo("r11");
    assertThat(csvFile.row(0).field(1)).isEqualTo("r12");
    assertThat(csvFile.row(1).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(1).field(0)).isEqualTo("r21");
    assertThat(csvFile.row(1).field(1)).isEqualTo("r22");
  }

  @Test
  public void test_of_blank_row_variants() {
    assertThat(CsvFile.of(CharSource.wrap(""), false).rowCount()).isEqualTo(0);
    assertThat(CsvFile.of(CharSource.wrap(","), false).rowCount()).isEqualTo(0);
    assertThat(CsvFile.of(CharSource.wrap(",,,"), false).rowCount()).isEqualTo(0);
    assertThat(CsvFile.of(CharSource.wrap(" , , , "), false).rowCount()).isEqualTo(0);
    assertThat(CsvFile.of(CharSource.wrap(" , ,\" \", "), false).rowCount()).isEqualTo(1);  // not blank
  }

  @Test
  public void test_of_headerComment() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV7), true);
    assertThat(csvFile.rowCount()).isEqualTo(1);
    assertThat(csvFile.row(0).lineNumber()).isEqualTo(3);

    assertThat(csvFile.headers().size()).isEqualTo(2);
    assertThat(csvFile.headers().get(0)).isEqualTo("h1");
    assertThat(csvFile.headers().get(1)).isEqualTo("h2");
    assertThat(csvFile.row(0).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(0).field(0)).isEqualTo("r1");
    assertThat(csvFile.row(0).field(1)).isEqualTo("r2");
  }

  @Test
  public void test_of_quoting() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV4), false);
    assertThat(csvFile.rowCount()).isEqualTo(3);
    assertThat(csvFile.row(0).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(0).field(0)).isEqualTo("alpha");
    assertThat(csvFile.row(0).field(1)).isEqualTo("be, \"at\", one");
    assertThat(csvFile.row(1).fieldCount()).isEqualTo(3);
    assertThat(csvFile.row(1).field(0)).isEqualTo("alpha\",be\"\"\"");
    assertThat(csvFile.row(1).field(1)).isEqualTo("at\"\"");
    assertThat(csvFile.row(1).field(2)).isEqualTo("one\"");
    assertThat(csvFile.row(2).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(2).field(0)).isEqualTo("r21");
    assertThat(csvFile.row(2).field(1)).isEqualTo(" r22 ");
  }

  @Test
  public void test_of_quotingWithEquals() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV4B), false);
    assertThat(csvFile.rowCount()).isEqualTo(2);
    assertThat(csvFile.row(0).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(0).field(0)).isEqualTo("alpha");
    assertThat(csvFile.row(0).field(1)).isEqualTo("be, \"at\", one");
    assertThat(csvFile.row(1).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(1).field(0)).isEqualTo("r21");
    assertThat(csvFile.row(1).field(1)).isEqualTo(" r22 ");
  }

  @Test
  public void test_of_quoting_oddStart() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap("a,b\"c\"d\",e"), false);
    assertThat(csvFile.rowCount()).isEqualTo(1);
    assertThat(csvFile.row(0).fieldCount()).isEqualTo(3);
    assertThat(csvFile.row(0).field(0)).isEqualTo("a");
    assertThat(csvFile.row(0).field(1)).isEqualTo("b\"c\"d\"");
    assertThat(csvFile.row(0).field(2)).isEqualTo("e");
  }

  @Test
  public void test_of_quoting_oddMiddle() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap("a,\"b\"c\"d\",e"), false);
    assertThat(csvFile.rowCount()).isEqualTo(1);
    assertThat(csvFile.row(0).fieldCount()).isEqualTo(3);
    assertThat(csvFile.row(0).field(0)).isEqualTo("a");
    assertThat(csvFile.row(0).field(1)).isEqualTo("bc\"d\"");
    assertThat(csvFile.row(0).field(2)).isEqualTo("e");
  }

  @Test
  public void test_of_quoting_oddEnd() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap("a,\"b\"cd\",e"), false);
    assertThat(csvFile.rowCount()).isEqualTo(1);
    assertThat(csvFile.row(0).fieldCount()).isEqualTo(3);
    assertThat(csvFile.row(0).field(0)).isEqualTo("a");
    assertThat(csvFile.row(0).field(1)).isEqualTo("bcd\"");
    assertThat(csvFile.row(0).field(2)).isEqualTo("e");
  }

  @Test
  public void test_of_quoting_equalsEnd() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap("a,="), false);
    assertThat(csvFile.rowCount()).isEqualTo(1);
    assertThat(csvFile.row(0).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(0).field(0)).isEqualTo("a");
    assertThat(csvFile.row(0).field(1)).isEqualTo("=");
  }

  public static Object[][] data_mismatched() {
    return new Object[][] {
        {"x,\"", ""},
        {"x,\"a\"\"", "a\""},
        {"x,\"alpha", "alpha"},
        {"x,\"al,pha", "al,pha"},
        {"x,al\"pha", "al\"pha"},
        {"x,al\"\"pha", "al\"\"pha"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_mismatched")
  public void test_of_quoting_mismatched(String input, String expected) {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(input), false);
    assertThat(csvFile.rowCount()).isEqualTo(1);
    assertThat(csvFile.row(0).fieldCount()).isEqualTo(2);
    assertThat(csvFile.row(0).field(0)).isEqualTo("x");
    assertThat(csvFile.row(0).field(1)).isEqualTo(expected);
  }

  @Test
  public void test_of_quotingTrimmed() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap("a\n\" x \"\n\" \""), true);
    assertThat(csvFile.rowCount()).isEqualTo(2);
    assertThat(csvFile.row(0).field(0)).isEqualTo(" x ");
    assertThat(csvFile.row(1).field(0)).isEqualTo(" ");
    assertThat(csvFile.row(0).getField("a")).isEqualTo(" x ");
    assertThat(csvFile.row(1).getField("a")).isEqualTo(" ");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_lists_noHeader() {
    List<String> headers = Collections.emptyList();
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "x"), Arrays.asList("b", "y"));
    CsvFile csvFile = CsvFile.of(headers, rows);
    assertThat(csvFile.headers()).isEqualTo(headers);
    assertThat(csvFile.rows().size()).isEqualTo(2);
    assertThat(csvFile.row(0).fields()).isEqualTo(Arrays.asList("a", "x"));
    assertThat(csvFile.row(1).fields()).isEqualTo(Arrays.asList("b", "y"));
  }

  @Test
  public void test_of_lists_noHeaderNoRows() {
    List<String> headers = Collections.emptyList();
    List<List<String>> rows = Collections.emptyList();
    CsvFile csvFile = CsvFile.of(headers, rows);
    assertThat(csvFile.headers()).isEqualTo(headers);
    assertThat(csvFile.rows()).isEqualTo(rows);
  }

  @Test
  public void test_of_lists_header() {
    List<String> headers = Arrays.asList("1", "2");
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "x"), Arrays.asList("b", "y"));
    CsvFile csvFile = CsvFile.of(headers, rows);
    assertThat(csvFile.headers()).isEqualTo(headers);
    assertThat(csvFile.rows().size()).isEqualTo(2);
    assertThat(csvFile.row(0).fields()).isEqualTo(Arrays.asList("a", "x"));
    assertThat(csvFile.row(1).fields()).isEqualTo(Arrays.asList("b", "y"));
  }

  @Test
  public void test_of_lists_sizeMismatch() {
    List<String> headers = Arrays.asList("1", "2");
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "x"), Arrays.asList("b"));
    assertThatIllegalArgumentException().isThrownBy(() -> CsvFile.of(headers, rows));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_empty_no_header_reader() {
    CsvFile csvFile = CsvFile.of(new StringReader(""), false, ',');
    assertThat(csvFile.headers().size()).isEqualTo(0);
    assertThat(csvFile.rowCount()).isEqualTo(0);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_with_headers() {
    CsvFile csvFile = CsvFile.of(ImmutableList.of("A", "B"), ImmutableList.of(ImmutableList.of("1", "2")));

    ImmutableList<String> newHeaders = ImmutableList.of("C", "D");
    CsvFile withNewHeaders = csvFile.withHeaders(newHeaders);

    assertThat(withNewHeaders.headers()).isEqualTo(newHeaders);
    assertThat(withNewHeaders.rows()).hasSize(1);
    assertThat(withNewHeaders.rows().get(0).fields()).isEqualTo(csvFile.row(0).fields());
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_findSeparator() {
    return new Object[][] {
        {"", ','},
        {"#comment\nhead1,head2,head3\nvalue1,value2,value3\n", ','},
        {"#comment\nhead1;head2;head3\nvalue1;value2;value3\n", ';'},
        {"#comment\nhead1:head2:head3\nvalue1:value2:value3\n", ':'},
        {"#comment\nhead1\thead2\thead3\nvalue1\tvalue2\tvalue3\n", '\t'},
        {"#comment\nhead1|head2|head3\nvalue1|value2|value3\n", '|'},
        {"a", ','},
        {",", ','},
        {";", ';'},
        {":", ':'},
        {"\t", '\t'},
        {"|", '|'},
        {",,", ','},
        {";;", ';'},
        {"::", ':'},
        {"\t\t", '\t'},
        {"||", '|'},
        {"a,", ','},
        {"a;", ';'},
        {"a:", ':'},
        {"a\t", '\t'},
        {"a|", '|'},
        {"a,b,c", ','},
        {"a;b,c", ','},
        {"a;b;c,d", ';'},
        {"a;b;c,d\nabc,d", ','},
        {"a;b;c,d\na;bc,d\\nabc,d", ','},
        {"a;b;c|d\nabc|d", '|'},
        {"a;b;c|d\n\nabc|d", '|'},
        {"a,\"b;;;;;;;;;;;;;;\",c", ','},
        {"a\tb\tc,d", '\t'},
        {"a|b|c,d", '|'},
        {"a:b:c,d", ':'},
        {"foo;bar;1,25;3,25\nbar;baz;4,75;3,50", ';'},
        {"foo;bar;1,25;3,25\n;;;\nbar;baz;4,75;3,50", ';'},
        {"foo;bar;1,25;3,25\n,,,\nbar;baz;4,75;3,50", ','},
        {"# hello\n;\na,b,c", ','},
        {";woohoo\na;b\n#comment", ';'},
        {";woohoo\na;b,c\n#comment", ','},
    };
  }

  @ParameterizedTest
  @MethodSource("data_findSeparator")
  public void test_findSeparator(String input, char expected) {
    assertThat(CsvFile.findSeparator(CharSource.wrap(input))).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCodeToString() {
    CsvFile a1 = CsvFile.of(CharSource.wrap(CSV1), true);
    CsvFile a2 = CsvFile.of(CharSource.wrap(CSV1), true);
    CsvFile b = CsvFile.of(CharSource.wrap(CSV2), true);
    CsvFile c = CsvFile.of(CharSource.wrap(CSV3), false);
    // file
    assertThat(a1.equals(a1)).isTrue();
    assertThat(a1.equals(a2)).isTrue();
    assertThat(a1.equals(b)).isFalse();
    assertThat(a1.equals(c)).isFalse();
    assertThat(a1.equals(null)).isFalse();
    assertThat(a1.equals(ANOTHER_TYPE)).isFalse();
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
    assertThat(a1.toString()).isNotNull();
    // row
    assertThat(a1.row(0).equals(a1.row(0))).isTrue();
    assertThat(a1.row(0).equals(a2.row(0))).isTrue();
    assertThat(a1.row(0).equals(b.row(0))).isFalse();
    assertThat(c.row(0).equals(c.row(1))).isFalse();
    assertThat(a1.row(0).equals(ANOTHER_TYPE)).isFalse();
    assertThat(a1.row(0).equals(null)).isFalse();
    assertThat(a1.row(0).hashCode()).isEqualTo(a2.row(0).hashCode());
    assertThat(a1.row(0)).isNotNull();
  }

}
