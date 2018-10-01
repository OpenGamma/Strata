/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * Contains utility methods for managing messages.
 */
public final class Messages {

  private static final Pattern REGEX_PATTERN = Pattern.compile("\\{(\\w*)\\}"); //This will match both {}, and {anything}

  /**
   * Restricted constructor.
   */
  private Messages() {
  }

  //-------------------------------------------------------------------------
  /**
   * Formats a templated message inserting a single argument.
   * <p>
   * This method combines a template message with a single argument.
   * It can be useful to delay string concatenation, which is sometimes a performance issue.
   * The approach is similar to SLF4J MessageFormat, Guava Preconditions and String format().
   * <p>
   * The message template contains zero to many "{}" placeholders.
   * The first placeholder is replaced by the string form of the argument.
   * Subsequent placeholders are not replaced.
   * If there is no placeholder, then the argument is appended to the end of the message.
   * No attempt is made to format the argument.
   * <p>
   * This method is null tolerant to ensure that use in exception construction will
   * not throw another exception, which might hide the intended exception.
   *
   * @param messageTemplate  the message template with "{}" placeholders, null returns empty string
   * @param arg  the message argument, null treated as string "null"
   * @return the formatted message
   */
  public static String format(String messageTemplate, Object arg) {
    if (messageTemplate == null) {
      return format("", arg);
    }
    int placeholderPos = messageTemplate.indexOf("{}", 0);
    String argStr = String.valueOf(arg);
    StringBuilder builder = new StringBuilder(messageTemplate.length() + argStr.length() + 3);
    if (placeholderPos >= 0) {
      builder
          .append(messageTemplate.substring(0, placeholderPos))
          .append(argStr)
          .append(messageTemplate.substring(placeholderPos + 2));
    } else {
      builder.append(messageTemplate).append(" - [").append(argStr).append(']');
    }
    return builder.toString();
  }

  /**
   * Formats a templated message inserting arguments.
   * <p>
   * This method combines a template message with a list of specific arguments.
   * It can be useful to delay string concatenation, which is sometimes a performance issue.
   * The approach is similar to SLF4J MessageFormat, Guava Preconditions and String format().
   * <p>
   * The message template contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the end of the message.
   * No attempt is made to format the arguments.
   * <p>
   * This method is null tolerant to ensure that use in exception construction will
   * not throw another exception, which might hide the intended exception.
   * 
   * @param messageTemplate  the message template with "{}" placeholders, null returns empty string
   * @param args  the message arguments, null treated as empty array
   * @return the formatted message
   */
  public static String format(String messageTemplate, Object... args) {
    if (messageTemplate == null) {
      return format("", args);
    }
    if (args == null) {
      return format(messageTemplate, new Object[0]);
    }
    // try to make builder big enough for the message and the args
    StringBuilder builder = new StringBuilder(messageTemplate.length() + args.length * 20);
    // insert placeholders
    int argIndex = 0;
    int curPos = 0;
    int nextPlaceholderPos = messageTemplate.indexOf("{}", curPos);
    while (nextPlaceholderPos >= 0 && argIndex < args.length) {
      builder.append(messageTemplate.substring(curPos, nextPlaceholderPos)).append(args[argIndex]);
      argIndex++;
      curPos = nextPlaceholderPos + 2;
      nextPlaceholderPos = messageTemplate.indexOf("{}", curPos);
    }
    // append remainder of message template
    builder.append(messageTemplate.substring(curPos));
    // append remaining args
    if (argIndex < args.length) {
      builder.append(" - [");
      for (int i = argIndex; i < args.length; i++) {
        if (i > argIndex) {
          builder.append(", ");
        }
        builder.append(args[i]);
      }
      builder.append(']');
    }
    return builder.toString();
  }

  /**
   * Formats a templated message inserting named arguments.
   * <p>
   * A typical template would look like:
   * <pre>
   * Messages.formatWithAttributes("Foo={foo}, Bar={}", "abc", 123)
   * </pre>
   * This will return a {@link Pair} with a String and a Map.
   * The String will be the formatted message: {@code "Foo=abc, Bar=123"}.
   * The Map will look like: <code>{"foo": "123"}</code>.
   * <p>
   * This method combines a template message with a list of specific arguments.
   * It can be useful to delay string concatenation, which is sometimes a performance issue.
   * The approach is similar to SLF4J MessageFormat, Guava Preconditions and String format().
   * <p>
   * The message template contains zero to many "{name}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the message.
   * No attempt is made to format the arguments.
   * <p>
   * This method is null tolerant to ensure that use in exception construction will
   * not throw another exception, which might hide the intended exception.
   *
   * @param messageTemplate  the message template with "{}" placeholders, null returns empty string
   * @param args  the message arguments, null treated as empty array
   * @return the formatted message
   */
  public static Pair<String, Map<String, String>> formatWithAttributes(String messageTemplate, Object... args) {
    if (messageTemplate == null) {
      return formatWithAttributes("", args);
    }
    if (args == null) {
      return formatWithAttributes(messageTemplate);
    }

    // do not use an ImmutableMap, as we avoid throwing exceptions in case of duplicate keys.
    Map<String, String> attributes = new HashMap<>();
    Matcher matcher = REGEX_PATTERN.matcher(messageTemplate);
    int argIndex = 0;

    StringBuffer outputMessageBuffer = new StringBuffer();
    while (matcher.find()) {
      // if the number of placeholders is greater than the number of arguments, then not all placeholders are replaced.
      if (argIndex >= args.length) {
        continue;
      }

      String attributeName = matcher.group(1); // extract the attribute name
      String replacement = String.valueOf(args[argIndex]).replace("$", "\\$");
      matcher.appendReplacement(outputMessageBuffer, replacement);
      if (!attributeName.isEmpty()) {
        attributes.put(attributeName, replacement);
      }
      argIndex++;
    }
    matcher.appendTail(outputMessageBuffer);

    // append remaining args
    if (argIndex < args.length) {
      outputMessageBuffer.append(" - [");
      for (int i = argIndex; i < args.length; i++) {
        if (i > argIndex) {
          outputMessageBuffer.append(", ");
        }
        outputMessageBuffer.append(args[i]);
      }
      outputMessageBuffer.append(']');
    }

    return Pair.of(outputMessageBuffer.toString(), ImmutableMap.copyOf(attributes));
  }

}
