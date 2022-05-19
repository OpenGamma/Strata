/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.TestHelper.assertUtilityClass;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.result.FailureAttributeKeys;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test Messages.
 */
public class MessagesTest {

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

  @ParameterizedTest
  @MethodSource("data_formatMessageSingle")
  public void test_formatMessageSingle(String template, Object arg, String expMain, String expExcess) {
    assertThat(Messages.format(template, arg)).isEqualTo(expMain + expExcess);
  }

  @ParameterizedTest
  @MethodSource("data_formatMessageSingle")
  public void test_formatMessageSingle_prefix(String template, Object arg, String expMain, String expExcess) {
    assertThat(Messages.format("::" + Objects.toString(template, ""), arg)).isEqualTo("::" + expMain + expExcess);
  }

  @ParameterizedTest
  @MethodSource("data_formatMessageSingle")
  public void test_formatMessageSingle_suffix(String template, Object arg, String expMain, String expExcess) {
    assertThat(Messages.format(Objects.toString(template, "") + "@@", arg)).isEqualTo(expMain + "@@" + expExcess);
  }

  @ParameterizedTest
  @MethodSource("data_formatMessageSingle")
  public void test_formatMessageSingle_prefixSuffix(String template, Object arg, String expMain, String expExcess) {
    assertThat(Messages.format("::" + Objects.toString(template, "") + "@@", arg))
        .isEqualTo("::" + expMain + "@@" + expExcess);
  }

  //-------------------------------------------------------------------------
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

  @ParameterizedTest
  @MethodSource("data_formatMessage")
  public void test_formatMessage(String template, Object[] args, String expMain, String expExcess) {
    assertThat(Messages.format(template, args)).isEqualTo(expMain + expExcess);
  }

  @ParameterizedTest
  @MethodSource("data_formatMessage")
  public void test_formatMessage_prefix(String template, Object[] args, String expMain, String expExcess) {
    assertThat(Messages.format("::" + Objects.toString(template, ""), args)).isEqualTo("::" + expMain + expExcess);
  }

  @ParameterizedTest
  @MethodSource("data_formatMessage")
  public void test_formatMessage_suffix(String template, Object[] args, String expMain, String expExcess) {
    assertThat(Messages.format(Objects.toString(template, "") + "@@", args)).isEqualTo(expMain + "@@" + expExcess);
  }

  @ParameterizedTest
  @MethodSource("data_formatMessage")
  public void test_formatMessage_prefixSuffix(String template, Object[] args, String expMain, String expExcess) {
    assertThat(Messages.format("::" + Objects.toString(template, "") + "@@", args))
        .isEqualTo("::" + expMain + "@@" + expExcess);
  }

  public static Object[][] data_formatMessageWithAttributes() {
    return new Object[][] {
        // null template
        {null, null, Pair.of("", ImmutableMap.of())},
        {null, new Object[] {}, Pair.of("", ImmutableMap.of())},
        {"", new Object[] {"testValueMissingKey"}, Pair.of(" - [testValueMissingKey]", ImmutableMap.of())},
        {"{}", new Object[] {"testValue"}, Pair.of("testValue", ImmutableMap.of())},
        {"{}", new Object[] {null}, Pair.of("null", ImmutableMap.of())},
        {"{a}", new Object[] {"testValue"},
            Pair.of("testValue", ImmutableMap.of("a", "testValue", "templateLocation", "a:0:9"))},
        {"{a} bcd", new Object[] {"testValue"},
            Pair.of("testValue bcd", ImmutableMap.of("a", "testValue", "templateLocation", "a:0:9"))},
        {"Test {abc} test2 {def} test3", new Object[] {"abcValue", 123456},
            Pair.of(
                "Test abcValue test2 123456 test3",
                ImmutableMap.of("abc", "abcValue", "def", "123456", "templateLocation", "abc:5:8|def:20:6"))},
        {"Test {abc} test2 {} test3", new Object[] {"abcValue", 123456},
            Pair.of(
                "Test abcValue test2 123456 test3",
                ImmutableMap.of("abc", "abcValue", "templateLocation", "abc:5:8"))},
        {"Test {abc} test2 {} test3 {} test4", new Object[] {"abcValue", 123456, 789},
            Pair.of(
                "Test abcValue test2 123456 test3 789 test4",
                ImmutableMap.of("abc", "abcValue", "templateLocation", "abc:5:8"))},
        {"Test {abc} test2 {def} test3", new Object[] {"abcValue", 123456, 789},
            Pair.of(
                "Test abcValue test2 123456 test3 - [789]",
                ImmutableMap.of("abc", "abcValue", "def", "123456", "templateLocation", "abc:5:8|def:20:6|+:32"))},
        {"Test {abc} test2 {abc} test3", new Object[] {"abcValue", "abcValue"},
            Pair.of(
                "Test abcValue test2 abcValue test3",
                ImmutableMap.of("abc", "abcValue", "templateLocation", "abc:5:8|abc:20:8"))},
        {"Test {abc} diff2 {abc} test3", new Object[] {"abcValue", 123456, 789},
            Pair.of(
                "Test abcValue diff2 123456 test3 - [789]",
                ImmutableMap.of("abc", "abcValue", "arg2", "123456", "templateLocation", "abc:5:8|arg2:20:6|+:32"))},
        {"Test {abc} test2 {def} test3", new Object[] {"abcValue"},
            Pair.of("Test abcValue test2 {def} test3", ImmutableMap.of("abc", "abcValue", "templateLocation", "abc:5:8"))},
        {"{a} bcd", new Object[] {"$testValue"},
            Pair.of("$testValue bcd", ImmutableMap.of("a", "$testValue", "templateLocation", "a:0:10"))},
        {"Test {abc} test2 {def} test3 {ghi} test4", new Object[] {"abcValue"},
            Pair.of("Test abcValue test2 {def} test3 {ghi} test4", ImmutableMap.of("abc", "abcValue", "templateLocation", "abc:5:8"))}
    };
  }

  @ParameterizedTest
  @MethodSource("data_formatMessageWithAttributes")
  public void test_formatMessageWithAttributes(
      String template,
      Object[] args,
      Pair<String, Map<String, String>> expectedOutput) {

    Pair<String, Map<String, String>> formatted = Messages.formatWithAttributes(template, args);
    assertThat(formatted.getFirst()).isEqualTo(expectedOutput.getFirst());
    assertThat(formatted.getSecond()).isEqualTo(expectedOutput.getSecond());
  }

  @ParameterizedTest
  @MethodSource("data_formatMessageWithAttributes")
  public void test_recreateTemplate(
      String template,
      Object[] args,
      Pair<String, Map<String, String>> expectedOutput) {

    String message = expectedOutput.getFirst();
    String templateLocation = expectedOutput.getSecond().get(FailureAttributeKeys.TEMPLATE_LOCATION);
    if (templateLocation != null) {
      // patch up the templates
      String adjTemplate = Strings.nullToEmpty(template);
      adjTemplate = adjTemplate.replaceFirst("diff2 \\{abc\\}", "diff2 {arg2}")
          .replaceFirst("test2 \\{\\}", args.length >= 2 ? "test2 " + args[1] : "")
          .replaceFirst("test3 \\{\\}", args.length >= 3 ? "test3 " + args[2] : "");
      assertThat(Messages.recreateTemplate(message, templateLocation)).isEqualTo(adjTemplate);
    }
  }

  @Test
  public void test_recreateTemplateBadLocation() {
    String message = "A B";
    assertThat(Messages.recreateTemplate(message, "a:0:1|b:4:1")).isEqualTo("A B");
  }

  public static Object[][] data_mergeTemplateLocations() {
    return new Object[][] {
        {"x:2:3", "a:0:1", 5, "x:2:3|a:5:1"},
        {"x:2:3", "a:0:1|b:4:1", 5, "x:2:3|a:5:1|b:9:1"},
        {"x:2:3|y:5:1", "a:0:1|b:4:1", 7, "x:2:3|y:5:1|a:7:1|b:11:1"},
        {"x:2:3|y:5:1", "a:0:1|b:4:1|+:8", 7, "x:2:3|y:5:1|a:7:1|b:11:1|+:15"},
        {"x:2:3|y:5:1|+:6", "a:0:1|b:4:1|+:8", 7, "x:2:3|y:5:1|a:7:1|b:11:1|+:15"},
        {"a:0:1|b:4:1", "", 5, "a:0:1|b:4:1"},
        {"+:2", "a:2:1", 6, "a:8:1"},
        {"", "a:0:1|b:4:1", 5, "a:5:1|b:9:1"},
        {null, "a:0:1|b:4:1", 5, "a:5:1|b:9:1"},
        {"x:2:3", "a:0:1", -1, ""},
        {"x:2:3", "a", 6, ""},
        {"x:2:3", "a:1", 6, ""},
        {"x:2:3", "a:1:1:1", 6, ""},
    };
  }

  @ParameterizedTest
  @MethodSource("data_mergeTemplateLocations")
  public void test_mergeTemplateLocations(String loc1, String loc2, int loc1MsgLength, String expected) {
    assertThat(Messages.mergeTemplateLocations(loc1, loc2, loc1MsgLength)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_validUtilityClass() {
    assertUtilityClass(Messages.class);
  }

}
