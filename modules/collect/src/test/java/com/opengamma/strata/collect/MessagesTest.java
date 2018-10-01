/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.TestHelper.assertUtilityClass;
import static org.testng.Assert.assertEquals;

import java.util.Map;
import java.util.Objects;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test Messages.
 */
@Test
public class MessagesTest {

  @DataProvider(name = "formatMessageSingle")
  public static Object[][] data_formatMessageSingle() {
    return new Object[][] {
        // null template
        {null, null, "", " - [null]"},
        {null, "", "", " - []"},
        // null in array
        {"", null, "", " - [null]"},
        {"{}", null, "null", ""},
        {"{}{}", null, "null{}", ""},
        {"{} and {}", null, "null and {}", ""},
        // empty string in array
        {"", "", "", " - []"},
        {"{}", "", "", ""},
        {"{}{}", "", "{}", ""},
        {"{} and {}", "", " and {}", ""},
        // main tests
        {"{}", 67, "67", ""},
        {"{}{}", 67, "67{}", ""},
        {"{} and {}", 67, "67 and {}", ""},
    };
  }

  @Test(dataProvider = "formatMessageSingle")
  public void test_formatMessageSingle(String template, Object arg, String expMain, String expExcess) {
    assertEquals(Messages.format(template, arg), expMain + expExcess);
  }

  @Test(dataProvider = "formatMessageSingle")
  public void test_formatMessageSingle_prefix(String template, Object arg, String expMain, String expExcess) {
    assertEquals(Messages.format("::" + Objects.toString(template, ""), arg), "::" + expMain + expExcess);
  }

  @Test(dataProvider = "formatMessageSingle")
  public void test_formatMessageSingle_suffix(String template, Object arg, String expMain, String expExcess) {
    assertEquals(Messages.format(Objects.toString(template, "") + "@@", arg), expMain + "@@" + expExcess);
  }

  @Test(dataProvider = "formatMessageSingle")
  public void test_formatMessageSingle_prefixSuffix(String template, Object arg, String expMain, String expExcess) {
    assertEquals(Messages.format("::" + Objects.toString(template, "") + "@@", arg), "::" + expMain + "@@" + expExcess);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "formatMessage")
  public static Object[][] data_formatMessage() {
    return new Object[][] {
        // null template
        {null, null, "", ""},
        {null, new Object[] {}, "", ""},
        {null, new Object[] {null}, "", " - [null]"},
        {null, new Object[] {67}, "", " - [67]"},
        // null array
        {"", null, "", ""},
        {"{}", null, "{}", ""},
        {"{}{}", null, "{}{}", ""},
        {"{} and {}", null, "{} and {}", ""},
        // null in array
        {"", new Object[] {null}, "", " - [null]"},
        {"{}", new Object[] {null}, "null", ""},
        {"{}{}", new Object[] {null}, "null{}", ""},
        {"{} and {}", new Object[] {null}, "null and {}", ""},
        // main tests
        {"", new Object[] {}, "", ""},
        {"{}", null, "{}", ""},
        {"{}", new Object[] {}, "{}", ""},
        {"{}", new Object[] {67}, "67", ""},
        {"{}", new Object[] {67, 78}, "67", " - [78]"},
        {"{}", new Object[] {67, 78, 89}, "67", " - [78, 89]"},
        {"{}", new Object[] {67, 78, 89, 90}, "67", " - [78, 89, 90]"},
        {"{}{}", null, "{}{}", ""},
        {"{}{}", new Object[] {}, "{}{}", ""},
        {"{}{}", new Object[] {67}, "67{}", ""},
        {"{}{}", new Object[] {67, 78}, "6778", ""},
        {"{}{}", new Object[] {67, 78, 89}, "6778", " - [89]"},
        {"{}{}", new Object[] {67, 78, 89, 90}, "6778", " - [89, 90]"},
        {"{} and {}", null, "{} and {}", ""},
        {"{} and {}", new Object[] {}, "{} and {}", ""},
        {"{} and {}", new Object[] {67}, "67 and {}", ""},
        {"{} and {}", new Object[] {67, 78}, "67 and 78", ""},
        {"{} and {}", new Object[] {67, 78, 89}, "67 and 78", " - [89]"},
        {"{} and {}", new Object[] {67, 78, 89, 90}, "67 and 78", " - [89, 90]"},
        {"{}, {} and {}", new Object[] {}, "{}, {} and {}", ""},
        {"{}, {} and {}", new Object[] {67}, "67, {} and {}", ""},
        {"{}, {} and {}", new Object[] {67, 78}, "67, 78 and {}", ""},
        {"{}, {} and {}", new Object[] {67, 78, 89}, "67, 78 and 89", ""},
        {"{}, {} and {}", new Object[] {67, 78, 89, 90}, "67, 78 and 89", " - [90]"},
        {"ABC", new Object[] {}, "ABC", ""},
        {"Message {}, {}, {}", new Object[] {"A", 2, 3.}, "Message A, 2, 3.0", ""},
        {"Message {}, {} blah", new Object[] {"A", 2, 3.}, "Message A, 2 blah", " - [3.0]"},
        {"Message {}, {}", new Object[] {"A", 2, 3., true}, "Message A, 2", " - [3.0, true]"},
        {"Message {}, {}, {}, {} blah", new Object[] {"A", 2, 3.}, "Message A, 2, 3.0, {} blah", ""},
    };
  }

  @Test(dataProvider = "formatMessage")
  public void test_formatMessage(String template, Object[] args, String expMain, String expExcess) {
    assertEquals(Messages.format(template, args), expMain + expExcess);
  }

  @Test(dataProvider = "formatMessage")
  public void test_formatMessage_prefix(String template, Object[] args, String expMain, String expExcess) {
    assertEquals(Messages.format("::" + Objects.toString(template, ""), args), "::" + expMain + expExcess);
  }

  @Test(dataProvider = "formatMessage")
  public void test_formatMessage_suffix(String template, Object[] args, String expMain, String expExcess) {
    assertEquals(Messages.format(Objects.toString(template, "") + "@@", args), expMain + "@@" + expExcess);
  }

  @Test(dataProvider = "formatMessage")
  public void test_formatMessage_prefixSuffix(String template, Object[] args, String expMain, String expExcess) {
    assertEquals(Messages.format("::" + Objects.toString(template, "") + "@@", args), "::" + expMain + "@@" + expExcess);
  }

  @DataProvider(name = "formatMessageWithAttributes")
  public static Object[][] data_formatMessageWithAttributes() {
    return new Object[][]{
        // null template
        {null, null, Pair.of("", ImmutableMap.of())},
        {null, new Object[] {}, Pair.of("", ImmutableMap.of())},
        {"", new Object[] {"testValueMissingKey"}, Pair.of(" - [testValueMissingKey]", ImmutableMap.of())},
        {"{}", new Object[] {"testValue"}, Pair.of("testValue", ImmutableMap.of())},
        {"{}", new Object[] {null}, Pair.of("null", ImmutableMap.of())},
        {"{a}", new Object[] {"testValue"}, Pair.of("testValue", ImmutableMap.of("a", "testValue"))},
        {"{a} bcd", new Object[] {"testValue"}, Pair.of("testValue bcd", ImmutableMap.of("a", "testValue"))},
        {"Test {abc} test2 {def} test3", new Object[] {"abcValue", 123456}, Pair.of("Test abcValue test2 123456 test3", ImmutableMap.of("abc", "abcValue", "def", "123456"))},
        {"Test {abc} test2 {} test3", new Object[] {"abcValue", 123456}, Pair.of("Test abcValue test2 123456 test3", ImmutableMap.of("abc", "abcValue"))},
        {"Test {abc} test2 {} test3 {} test4", new Object[] {"abcValue", 123456, 789}, Pair.of("Test abcValue test2 123456 test3 789 test4", ImmutableMap.of("abc", "abcValue"))},
        {"Test {abc} test2 {def} test3", new Object[] {"abcValue", 123456, 789}, Pair.of("Test abcValue test2 123456 test3 - [789]", ImmutableMap.of("abc", "abcValue", "def", "123456"))},
        {"Test {abc} test2 {abc} test3", new Object[] {"abcValue", 123456, 789}, Pair.of("Test abcValue test2 123456 test3 - [789]", ImmutableMap.of("abc", "123456"))},
        {"Test {abc} test2 {def} test3", new Object[] {"abcValue"}, Pair.of("Test abcValue test2 {def} test3", ImmutableMap.of("abc", "abcValue"))},
        {"{a} bcd", new Object[] {"$testValue"}, Pair.of("$testValue bcd", ImmutableMap.of("a", "\\$testValue"))}, //The $ must be escaped
        {"Test {abc} test2 {def} test3 {ghi} test4", new Object[] {"abcValue"}, Pair.of("Test abcValue test2 {def} test3 {ghi} test4", ImmutableMap.of("abc", "abcValue"))}
    };
    }

  @Test(dataProvider = "formatMessageWithAttributes")
  public void test_formatMessageWithAttributes(String template, Object[] args, Pair<String, Map<String, String>> expectedOutput) {
    assertEquals(Messages.formatWithAttributes(template, args), expectedOutput);
  }

  //-------------------------------------------------------------------------
  public void test_validUtilityClass() {
    assertUtilityClass(Messages.class);
  }

}
