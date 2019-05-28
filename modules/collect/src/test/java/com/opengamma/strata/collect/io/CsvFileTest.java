/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.File;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

/**
 * Test {@link CsvFile}.
 */
@Test
public class CsvFileTest {

  private static final Object ANOTHER_TYPE = "";

  private final String CSV1 = "" +
      "h1,h2\n" +
      "r11,r12\n" +
      "r21 ,r22\n" +
      "r31,";

  private final String CSV1T = "" +
      "h1\th2\n" +
      "r11\tr12\n" +
      "r21\tr22";

  private final String CSV2 = "" +
      "h1,h2\n" +
      "#r11,r12\n" +
      ";r11,r12\n" +
      "\n" +
      "r21,r22\n";

  private final String CSV3 = "" +
      "r11,r12\n" +
      ",\n" +
      "r21,r22\n";

  private final String CSV4 = "" +
      "\"alpha\",\"be, \"\"at\"\", one\"\n" +
      "\"alpha\"\",\"be\"\"\", \"\"at\"\", one\"\n" +
      "r21,\" r22 \"\n";

  private final String CSV4B = "" +
      "=\"alpha\",=\"be, \"\"at\"\", one\"\n" +
      "r21,=\" r22 \"\n";

  private final String CSV5 = "" +
      "a,b,c,b,c\n" +
      "aa,b1,c1,b2,c2\n";

  private final String CSV6 = "" +
      "a,b,c\n" +
      "r11\n" +
      "r21,r22";

  private final String CSV7 = "" +
      "# Comment about the file\n" +
      "h1,h2\n" +
      "r1,r2\n";

  //-------------------------------------------------------------------------
  public void test_of_ioException() {
    assertThrows(
        () -> CsvFile.of(Files.asCharSource(new File("src/test/resources"), StandardCharsets.UTF_8), false),
        UncheckedIOException.class);
  }

  public void test_of_empty_no_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(""), false);
    assertEquals(csvFile.headers().size(), 0);
    assertEquals(csvFile.rowCount(), 0);
    assertEquals(csvFile.containsHeader("Foo"), false);
    assertEquals(csvFile.containsHeader(Pattern.compile("Foo")), false);
  }

  public void test_of_empty_with_header() {
    assertThrowsIllegalArg(() -> CsvFile.of(CharSource.wrap(""), true));
  }

  public void test_of_simple_no_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), false);
    assertEquals(csvFile.headers().size(), 0);
    assertEquals(csvFile.rowCount(), 4);
    assertEquals(csvFile.containsHeader("Foo"), false);
    assertEquals(csvFile.containsHeader(Pattern.compile("Foo")), false);
    assertEquals(csvFile.row(0).lineNumber(), 1);
    assertEquals(csvFile.row(1).lineNumber(), 2);
    assertEquals(csvFile.row(2).lineNumber(), 3);
    assertEquals(csvFile.row(3).lineNumber(), 4);

    assertEquals(csvFile.row(0).headers().size(), 0);
    assertEquals(csvFile.row(0).fieldCount(), 2);
    assertEquals(csvFile.row(0).field(0), "h1");
    assertEquals(csvFile.row(0).field(1), "h2");
    assertEquals(csvFile.row(1).headers().size(), 0);
    assertEquals(csvFile.row(1).fieldCount(), 2);
    assertEquals(csvFile.row(1).field(0), "r11");
    assertEquals(csvFile.row(1).field(1), "r12");
    assertEquals(csvFile.row(2).headers().size(), 0);
    assertEquals(csvFile.row(2).fieldCount(), 2);
    assertEquals(csvFile.row(2).field(0), "r21");
    assertEquals(csvFile.row(2).field(1), "r22");

    assertEquals(csvFile.row(0).subRow(0).fieldCount(), 2);
    assertEquals(csvFile.row(0).subRow(1).fieldCount(), 1);
    assertEquals(csvFile.row(0).subRow(2).fieldCount(), 0);
  }

  public void test_of_simple_no_header_tabs() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1T), false, '\t');
    assertEquals(csvFile.headers().size(), 0);
    assertEquals(csvFile.containsHeader("Foo"), false);
    assertEquals(csvFile.containsHeader(Pattern.compile("Foo")), false);
    assertEquals(csvFile.rowCount(), 3);
    assertEquals(csvFile.row(0).lineNumber(), 1);
    assertEquals(csvFile.row(1).lineNumber(), 2);
    assertEquals(csvFile.row(2).lineNumber(), 3);

    assertEquals(csvFile.row(0).headers().size(), 0);
    assertEquals(csvFile.row(0).fieldCount(), 2);
    assertEquals(csvFile.row(0).field(0), "h1");
    assertEquals(csvFile.row(0).field(1), "h2");
    assertEquals(csvFile.row(1).headers().size(), 0);
    assertEquals(csvFile.row(1).fieldCount(), 2);
    assertEquals(csvFile.row(1).field(0), "r11");
    assertEquals(csvFile.row(1).field(1), "r12");
    assertEquals(csvFile.row(2).headers().size(), 0);
    assertEquals(csvFile.row(2).fieldCount(), 2);
    assertEquals(csvFile.row(2).field(0), "r21");
    assertEquals(csvFile.row(2).field(1), "r22");
  }

  public void test_of_simple_with_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), true);
    assertEquals(csvFile.containsHeader("Foo"), false);
    assertEquals(csvFile.containsHeader("h1"), true);
    assertEquals(csvFile.containsHeader(Pattern.compile("Foo")), false);
    assertEquals(csvFile.containsHeader(Pattern.compile("h[0-9]")), true);
    ImmutableList<String> headers = csvFile.headers();
    assertEquals(headers.size(), 2);
    assertEquals(headers.get(0), "h1");
    assertEquals(headers.get(1), "h2");
    assertEquals(csvFile.rowCount(), 3);
    assertEquals(csvFile.row(0).lineNumber(), 2);
    assertEquals(csvFile.row(1).lineNumber(), 3);
    assertEquals(csvFile.row(2).lineNumber(), 4);

    assertEquals(csvFile.row(0).headers(), headers);
    assertEquals(csvFile.row(0).fieldCount(), 2);
    assertEquals(csvFile.row(0).field(0), "r11");
    assertEquals(csvFile.row(0).field(1), "r12");
    assertEquals(csvFile.row(1).headers(), headers);
    assertEquals(csvFile.row(1).fieldCount(), 2);
    assertEquals(csvFile.row(1).field(0), "r21");
    assertEquals(csvFile.row(1).field(1), "r22");

    assertEquals(csvFile.row(0).getField("h1"), "r11");
    assertEquals(csvFile.row(0).getField("h2"), "r12");
    assertEquals(csvFile.row(1).getField("h1"), "r21");
    assertEquals(csvFile.row(1).getField("h2"), "r22");
    assertThrowsIllegalArg(() -> csvFile.row(0).getField("zzz"));

    assertEquals(csvFile.row(0).getValue("h1"), "r11");
    assertEquals(csvFile.row(0).getValue("h2"), "r12");
    assertEquals(csvFile.row(1).getValue("h1"), "r21");
    assertEquals(csvFile.row(1).getValue("h2"), "r22");
    assertThrowsIllegalArg(() -> csvFile.row(0).getValue("zzz"));
    assertThrowsIllegalArg(() -> csvFile.row(2).getValue("h2"));

    assertEquals(csvFile.row(0).findField("h1"), Optional.of("r11"));
    assertEquals(csvFile.row(0).findField("h2"), Optional.of("r12"));
    assertEquals(csvFile.row(1).findField("h1"), Optional.of("r21"));
    assertEquals(csvFile.row(1).findField("h2"), Optional.of("r22"));
    assertEquals(csvFile.row(0).findField("zzz"), Optional.empty());

    assertEquals(csvFile.row(0).findValue("h1"), Optional.of("r11"));
    assertEquals(csvFile.row(0).findValue("h2"), Optional.of("r12"));
    assertEquals(csvFile.row(1).findValue("h1"), Optional.of("r21"));
    assertEquals(csvFile.row(1).findValue("h2"), Optional.of("r22"));
    assertEquals(csvFile.row(0).findValue("zzz"), Optional.empty());
    assertEquals(csvFile.row(2).findValue("h2"), Optional.empty());

    assertEquals(csvFile.row(0).getField(Pattern.compile("h[13]")), "r11");
    assertEquals(csvFile.row(0).getField(Pattern.compile("h[24]")), "r12");
    assertThrowsIllegalArg(() -> csvFile.row(0).getField(Pattern.compile("zzz")));

    assertEquals(csvFile.row(0).getValue(Pattern.compile("h[13]")), "r11");
    assertEquals(csvFile.row(0).getValue(Pattern.compile("h[24]")), "r12");
    assertThrowsIllegalArg(() -> csvFile.row(0).getValue(Pattern.compile("zzz")));
    assertThrowsIllegalArg(() -> csvFile.row(2).getValue(Pattern.compile("h2")));

    assertEquals(csvFile.row(0).findField(Pattern.compile("h[13]")), Optional.of("r11"));
    assertEquals(csvFile.row(0).findField(Pattern.compile("h[24]")), Optional.of("r12"));
    assertEquals(csvFile.row(0).findField(Pattern.compile("zzz")), Optional.empty());

    assertEquals(csvFile.row(0).findValue(Pattern.compile("h[13]")), Optional.of("r11"));
    assertEquals(csvFile.row(0).findValue(Pattern.compile("h[24]")), Optional.of("r12"));
    assertEquals(csvFile.row(0).findValue(Pattern.compile("zzz")), Optional.empty());
    assertEquals(csvFile.row(2).findValue(Pattern.compile("h2")), Optional.empty());

    assertEquals(csvFile.row(0).subRow(0).fieldCount(), 2);
    assertEquals(csvFile.row(0).subRow(1).fieldCount(), 1);
    assertEquals(csvFile.row(0).subRow(2).fieldCount(), 0);

    assertEquals(csvFile.row(0).subRow(0, 0).fieldCount(), 0);
    assertEquals(csvFile.row(0).subRow(0, 1).fieldCount(), 1);
    assertEquals(csvFile.row(0).subRow(2, 2).fieldCount(), 0);
  }

  public void test_of_duplicate_headers() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV5), true);
    assertEquals(csvFile.headers(), ImmutableList.of("a", "b", "c", "b", "c"));
    assertEquals(csvFile.containsHeader("Foo"), false);
    assertEquals(csvFile.containsHeader("a"), true);
    assertEquals(csvFile.row(0).getField("a"), "aa");
    assertEquals(csvFile.row(0).getField("b"), "b1");
    assertEquals(csvFile.row(0).getField("c"), "c1");

    assertEquals(csvFile.row(0).subRow(1, 3).getField("b"), "b1");
    assertEquals(csvFile.row(0).subRow(1, 3).getField("c"), "c1");
    assertEquals(csvFile.row(0).subRow(3).getField("b"), "b2");
    assertEquals(csvFile.row(0).subRow(3).getField("c"), "c2");
  }

  public void test_of_short_data_row() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV6), true);
    assertEquals(csvFile.headers(), ImmutableList.of("a", "b", "c"));
    assertEquals(csvFile.row(0).getField("a"), "r11");
    assertEquals(csvFile.row(0).getField("b"), "");
    assertEquals(csvFile.row(0).getField("c"), "");
    assertEquals(csvFile.row(0).field(0), "r11");
    assertEquals(csvFile.row(0).field(1), "");
    assertEquals(csvFile.row(0).field(2), "");
    assertThrows(() -> csvFile.row(0).field(4), IndexOutOfBoundsException.class);

    assertEquals(csvFile.row(1).getField("a"), "r21");
    assertEquals(csvFile.row(1).getField("b"), "r22");
    assertEquals(csvFile.row(1).getField("c"), "");
  }

  public void test_of_comment_blank_no_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV2), false);
    assertEquals(csvFile.headers().size(), 0);
    assertEquals(csvFile.rowCount(), 2);
    assertEquals(csvFile.row(0).lineNumber(), 1);
    assertEquals(csvFile.row(1).lineNumber(), 5);

    assertEquals(csvFile.row(0).fieldCount(), 2);
    assertEquals(csvFile.row(0).field(0), "h1");
    assertEquals(csvFile.row(0).field(1), "h2");
    assertEquals(csvFile.row(1).fieldCount(), 2);
    assertEquals(csvFile.row(1).field(0), "r21");
    assertEquals(csvFile.row(1).field(1), "r22");
  }

  public void test_of_comment_blank_with_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV2), true);
    ImmutableList<String> headers = csvFile.headers();
    assertEquals(headers.size(), 2);
    assertEquals(headers.get(0), "h1");
    assertEquals(headers.get(1), "h2");
    assertEquals(csvFile.rows().size(), 1);
    assertEquals(csvFile.rowCount(), 1);
    assertEquals(csvFile.row(0).lineNumber(), 5);

    assertEquals(csvFile.row(0).fieldCount(), 2);
    assertEquals(csvFile.row(0).field(0), "r21");
    assertEquals(csvFile.row(0).field(1), "r22");
    assertEquals(csvFile.rows().get(0), csvFile.row(0));
  }

  public void test_of_simple_no_header_access_by_field() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), false);
    assertEquals(csvFile.row(0).findField("h1"), Optional.empty());
    assertThrowsIllegalArg(() -> csvFile.row(0).getField("h1"));
  }

  public void test_of_simple_with_header_access_by_invalid_field() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), true);
    assertEquals(csvFile.row(0).findField("h3"), Optional.empty());
    assertThrowsIllegalArg(() -> csvFile.row(0).getField("h3"));
  }

  public void test_of_blank_row() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV3), false);
    assertEquals(csvFile.rowCount(), 2);
    assertEquals(csvFile.row(0).lineNumber(), 1);
    assertEquals(csvFile.row(1).lineNumber(), 3);

    assertEquals(csvFile.row(0).fieldCount(), 2);
    assertEquals(csvFile.row(0).field(0), "r11");
    assertEquals(csvFile.row(0).field(1), "r12");
    assertEquals(csvFile.row(1).fieldCount(), 2);
    assertEquals(csvFile.row(1).field(0), "r21");
    assertEquals(csvFile.row(1).field(1), "r22");
  }

  public void test_of_blank_row_variants() {
    assertEquals(CsvFile.of(CharSource.wrap(""), false).rowCount(), 0);
    assertEquals(CsvFile.of(CharSource.wrap(","), false).rowCount(), 0);
    assertEquals(CsvFile.of(CharSource.wrap(",,,"), false).rowCount(), 0);
    assertEquals(CsvFile.of(CharSource.wrap(" , , , "), false).rowCount(), 0);
    assertEquals(CsvFile.of(CharSource.wrap(" , ,\" \", "), false).rowCount(), 1);  // not blank
  }

  public void test_of_headerComment() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV7), true);
    assertEquals(csvFile.rowCount(), 1);
    assertEquals(csvFile.row(0).lineNumber(), 3);

    assertEquals(csvFile.headers().size(), 2);
    assertEquals(csvFile.headers().get(0), "h1");
    assertEquals(csvFile.headers().get(1), "h2");
    assertEquals(csvFile.row(0).fieldCount(), 2);
    assertEquals(csvFile.row(0).field(0), "r1");
    assertEquals(csvFile.row(0).field(1), "r2");
  }

  public void test_of_quoting() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV4), false);
    assertEquals(csvFile.rowCount(), 3);
    assertEquals(csvFile.row(0).fieldCount(), 2);
    assertEquals(csvFile.row(0).field(0), "alpha");
    assertEquals(csvFile.row(0).field(1), "be, \"at\", one");
    assertEquals(csvFile.row(1).fieldCount(), 3);
    assertEquals(csvFile.row(1).field(0), "alpha\",be\"\"\"");
    assertEquals(csvFile.row(1).field(1), "at\"\"");
    assertEquals(csvFile.row(1).field(2), "one\"");
    assertEquals(csvFile.row(2).fieldCount(), 2);
    assertEquals(csvFile.row(2).field(0), "r21");
    assertEquals(csvFile.row(2).field(1), " r22 ");
  }

  public void test_of_quotingWithEquals() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV4B), false);
    assertEquals(csvFile.rowCount(), 2);
    assertEquals(csvFile.row(0).fieldCount(), 2);
    assertEquals(csvFile.row(0).field(0), "alpha");
    assertEquals(csvFile.row(0).field(1), "be, \"at\", one");
    assertEquals(csvFile.row(1).fieldCount(), 2);
    assertEquals(csvFile.row(1).field(0), "r21");
    assertEquals(csvFile.row(1).field(1), " r22 ");
  }

  public void test_of_quoting_oddStart() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap("a,b\"c\"d\",e"), false);
    assertEquals(csvFile.rowCount(), 1);
    assertEquals(csvFile.row(0).fieldCount(), 3);
    assertEquals(csvFile.row(0).field(0), "a");
    assertEquals(csvFile.row(0).field(1), "b\"c\"d\"");
    assertEquals(csvFile.row(0).field(2), "e");
  }

  public void test_of_quoting_oddMiddle() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap("a,\"b\"c\"d\",e"), false);
    assertEquals(csvFile.rowCount(), 1);
    assertEquals(csvFile.row(0).fieldCount(), 3);
    assertEquals(csvFile.row(0).field(0), "a");
    assertEquals(csvFile.row(0).field(1), "bc\"d\"");
    assertEquals(csvFile.row(0).field(2), "e");
  }

  public void test_of_quoting_oddEnd() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap("a,\"b\"cd\",e"), false);
    assertEquals(csvFile.rowCount(), 1);
    assertEquals(csvFile.row(0).fieldCount(), 3);
    assertEquals(csvFile.row(0).field(0), "a");
    assertEquals(csvFile.row(0).field(1), "bcd\"");
    assertEquals(csvFile.row(0).field(2), "e");
  }

  public void test_of_quoting_equalsEnd() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap("a,="), false);
    assertEquals(csvFile.rowCount(), 1);
    assertEquals(csvFile.row(0).fieldCount(), 2);
    assertEquals(csvFile.row(0).field(0), "a");
    assertEquals(csvFile.row(0).field(1), "=");
  }

  @DataProvider(name = "mismatched")
  Object[][] data_mismatched() {
    return new Object[][] {
        {"x,\"", ""},
        {"x,\"a\"\"", "a\""},
        {"x,\"alpha", "alpha"},
        {"x,\"al,pha", "al,pha"},
        {"x,al\"pha", "al\"pha"},
        {"x,al\"\"pha", "al\"\"pha"},
    };
  }

  @Test(dataProvider = "mismatched")
  public void test_of_quoting_mismatched(String input, String expected) {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(input), false);
    assertEquals(csvFile.rowCount(), 1);
    assertEquals(csvFile.row(0).fieldCount(), 2);
    assertEquals(csvFile.row(0).field(0), "x");
    assertEquals(csvFile.row(0).field(1), expected);
  }

  public void test_of_quotingTrimmed() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap("a\n\" x \"\n\" \""), true);
    assertEquals(csvFile.rowCount(), 2);
    assertEquals(csvFile.row(0).field(0), " x ");
    assertEquals(csvFile.row(1).field(0), " ");
    assertEquals(csvFile.row(0).getField("a"), " x ");
    assertEquals(csvFile.row(1).getField("a"), " ");
  }

  //-------------------------------------------------------------------------
  public void test_of_lists_noHeader() {
    List<String> headers = Collections.emptyList();
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "x"), Arrays.asList("b", "y"));
    CsvFile csvFile = CsvFile.of(headers, rows);
    assertEquals(csvFile.headers(), headers);
    assertEquals(csvFile.rows().size(), 2);
    assertEquals(csvFile.row(0).fields(), Arrays.asList("a", "x"));
    assertEquals(csvFile.row(1).fields(), Arrays.asList("b", "y"));
  }

  public void test_of_lists_noHeaderNoRows() {
    List<String> headers = Collections.emptyList();
    List<List<String>> rows = Collections.emptyList();
    CsvFile csvFile = CsvFile.of(headers, rows);
    assertEquals(csvFile.headers(), headers);
    assertEquals(csvFile.rows(), rows);
  }

  public void test_of_lists_header() {
    List<String> headers = Arrays.asList("1", "2");
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "x"), Arrays.asList("b", "y"));
    CsvFile csvFile = CsvFile.of(headers, rows);
    assertEquals(csvFile.headers(), headers);
    assertEquals(csvFile.rows().size(), 2);
    assertEquals(csvFile.row(0).fields(), Arrays.asList("a", "x"));
    assertEquals(csvFile.row(1).fields(), Arrays.asList("b", "y"));
  }

  public void test_of_lists_sizeMismatch() {
    List<String> headers = Arrays.asList("1", "2");
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "x"), Arrays.asList("b"));
    assertThrowsIllegalArg(() -> CsvFile.of(headers, rows));
  }

  //-------------------------------------------------------------------------
  public void test_of_empty_no_header_reader() {
    CsvFile csvFile = CsvFile.of(new StringReader(""), false, ',');
    assertEquals(csvFile.headers().size(), 0);
    assertEquals(csvFile.rowCount(), 0);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "findSeparator")
  Object[][] data_findSeparator() {
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

  @Test(dataProvider = "findSeparator")
  public void test_findSeparator(String input, char expected) {
    assertEquals(CsvFile.findSeparator(CharSource.wrap(input)), expected);
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCodeToString() {
    CsvFile a1 = CsvFile.of(CharSource.wrap(CSV1), true);
    CsvFile a2 = CsvFile.of(CharSource.wrap(CSV1), true);
    CsvFile b = CsvFile.of(CharSource.wrap(CSV2), true);
    CsvFile c = CsvFile.of(CharSource.wrap(CSV3), false);
    // file
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
    assertEquals(a1.equals(null), false);
    assertEquals(a1.equals(ANOTHER_TYPE), false);
    assertEquals(a1.hashCode(), a2.hashCode());
    assertNotNull(a1.toString());
    // row
    assertEquals(a1.row(0).equals(a1.row(0)), true);
    assertEquals(a1.row(0).equals(a2.row(0)), true);
    assertEquals(a1.row(0).equals(b.row(0)), false);
    assertEquals(c.row(0).equals(c.row(1)), false);
    assertEquals(a1.row(0).equals(ANOTHER_TYPE), false);
    assertEquals(a1.row(0).equals(null), false);
    assertEquals(a1.row(0).hashCode(), a2.row(0).hashCode());
    assertNotNull(a1.row(0).toString());
  }

}
