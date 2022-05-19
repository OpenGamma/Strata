/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.Guavate.substringAfterFirst;
import static com.opengamma.strata.collect.Guavate.substringBeforeFirst;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.result.FailureAttributeKeys;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * Contains utility methods for managing messages.
 */
public final class Messages {

  private static final Pattern REGEX_WORDS_AND_EMPTY = Pattern.compile("\\{(\\w*)\\}"); //This will match both {}, and {anything}
  private static final Splitter TEMPLATE_LOCATION_SPLITTER = Splitter.on('|');
  private static final Splitter TEMPLATE_PART_SPLITTER = Splitter.on(':');

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
   * Formats a templated message inserting named arguments, returning the implied attribute map.
   * <p>
   * A typical template would look like:
   * <pre>
   * Messages.formatWithAttributes("Foo={foo}, Bar={}", "abc", 123)
   * </pre>
   * This will return a {@link Pair} with a String and a Map.
   * The String will be the formatted message: {@code "Foo=abc, Bar=123"}.
   * The Map will look like: <code>{"foo": "abc"}</code>.
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
   * <p>
   * If the template contains a named placeholder, then the output will contain a populated attribute map
   * for all placeholders including those without names. The output will include a 'templateLocation'
   * attribute identifying the location of the named placeholders.
   *
   * @param messageTemplate  the message template with "{}" and "{name}" placeholders, null returns empty string
   * @param args  the message arguments, null treated as empty array
   * @return the formatted message
   */
  public static Pair<String, Map<String, String>> formatWithAttributes(String messageTemplate, Object... args) {
    // NOTE: a templateLocation is used (rather than just storing the template) to avoid holding two very similar
    // copies of the message in memory, which could be significant if the message is long

    if (messageTemplate == null) {
      return formatWithAttributes("", args);
    }
    if (args == null) {
      return formatWithAttributes(messageTemplate);
    }

    // do not use an ImmutableMap, as we avoid throwing exceptions in case of duplicate keys.
    Map<String, String> attributes = new HashMap<>();
    Matcher matcher = REGEX_WORDS_AND_EMPTY.matcher(messageTemplate);
    int argIndex = 0;

    StringBuffer outputMessageBuffer = new StringBuffer(128);
    StringBuffer templateLocationBuffer = new StringBuffer(32);
    boolean hasNamed = false;
    int previousEnd = 0;
    while (matcher.find()) {
      // if the number of placeholders is greater than the number of arguments, then not all placeholders are replaced.
      if (argIndex >= args.length) {
        continue;
      }

      String attributeName = matcher.group(1);
      String replacement = String.valueOf(args[argIndex]);
      outputMessageBuffer.append(messageTemplate.substring(previousEnd, matcher.start()));
      outputMessageBuffer.append(replacement);
      if (!attributeName.isEmpty()) {
        hasNamed = true;
        String oldAttrValue = attributes.get(attributeName);
        if (oldAttrValue != null && !oldAttrValue.equals(replacement)) {
          attributeName = "arg" + (argIndex + 1);
        }
        attributes.put(attributeName, replacement);
        // each location is stored as 'name:startPos:length'
        // length is stored to ensure the location does not depend on the attribute values
        templateLocationBuffer.append(templateLocationBuffer.length() > 0 ? "|" : "")
            .append(attributeName)
            .append(':')
            .append(outputMessageBuffer.length() - replacement.length())
            .append(':')
            .append(replacement.length());
      }
      previousEnd = matcher.end();
      argIndex++;
    }
    outputMessageBuffer.append(messageTemplate.substring(previousEnd));

    // append remaining args
    if (argIndex < args.length) {
      if (hasNamed) {
        templateLocationBuffer.append(templateLocationBuffer.length() > 0 ? "|" : "")
            .append("+:")
            .append(outputMessageBuffer.length());
      }
      outputMessageBuffer.append(" - [");
      for (int i = argIndex; i < args.length; i++) {
        if (i > argIndex) {
          outputMessageBuffer.append(", ");
        }
        outputMessageBuffer.append(args[i]);
      }
      outputMessageBuffer.append(']');
    }

    // capture the template if named arguments were used
    if (hasNamed) {
      attributes.put(FailureAttributeKeys.TEMPLATE_LOCATION, templateLocationBuffer.toString());
    } else {
      attributes.clear();
    }
    return Pair.of(outputMessageBuffer.toString(), ImmutableMap.copyOf(attributes));
  }

  /**
   * Recreates the template from the message and templateLocation code.
   * <p>
   * This method returns the input message if the input is not valid.
   * 
   * @param message  the message, null tolerant
   * @param templateLocation  the template location, null tolerant
   * @return the message
   */
  public static String recreateTemplate(String message, String templateLocation) {
    if (Strings.nullToEmpty(message).isEmpty() || Strings.nullToEmpty(templateLocation).isEmpty()) {
      return message;
    }
    try {
      StringBuffer buf = new StringBuffer();
      int lastPos = 0;
      for (String entry : TEMPLATE_LOCATION_SPLITTER.split(templateLocation)) {
        String attrName = substringBeforeFirst(entry, ":");
        String remainder = substringAfterFirst(entry, ":");
        if (attrName.equals("+")) {
          int pos = Integer.parseInt(remainder);
          buf.append(message.substring(lastPos, pos));
          return buf.toString();
        }
        int pos = Integer.parseInt(substringBeforeFirst(remainder, ":"));
        int len = Integer.parseInt(substringAfterFirst(remainder, ":"));
        buf.append(message.substring(lastPos, pos)).append('{').append(attrName).append('}');
        lastPos = pos + len;
      }
      buf.append(message.substring(lastPos));
      return buf.toString();
    } catch (RuntimeException ex) {
      return message;
    }
  }

  /**
   * Merges two template locations.
   * <p>
   * This takes two template locations and the length of the first message and returns the combined template.
   * 
   * @param location1  the first location, null tolerant
   * @param location2  the second location, null tolerant
   * @param message1Length  the length of the formatted message associated with location1
   * @return the merged location, empty if unable to merge
   */
  public static String mergeTemplateLocations(String location1, String location2, int message1Length) {
    if (message1Length < 0) {
      return "";
    }
    if (Strings.nullToEmpty(location2).isEmpty()) {
      return Strings.nullToEmpty(location1);
    }
    try {
      // remove any + suffix from first template
      String adjLocation1 = Strings.nullToEmpty(location1);
      adjLocation1 = substringBeforeFirst(Strings.nullToEmpty(location1), "|+:");
      adjLocation1 = adjLocation1.startsWith("+:") ? "" : adjLocation1;
      // build the resulting template by appending the adjusted first template, then each part of the second template
      StringBuilder buf = new StringBuilder(64);
      buf.append(adjLocation1);
      for (String entry : TEMPLATE_LOCATION_SPLITTER.split(location2)) {
        List<String> parts = TEMPLATE_PART_SPLITTER.splitToList(entry);
        switch (parts.size()) {
          case 2: {
            // +:len, need to increase pos
            String attrName = parts.get(0);
            if (!attrName.equals("+")) {
              return "";
            }
            int pos = Integer.parseInt(parts.get(1));
            buf.append(buf.length() == 0 ? "" : "|").append(attrName).append(':').append(pos + message1Length);
            break;
          }
          case 3: {
            // attrName:pos:len, need to increase pos
            String attrName = parts.get(0);
            int pos = Integer.parseInt(parts.get(1));
            int len = Integer.parseInt(parts.get(2));
            buf.append(buf.length() == 0 ? "" : "|").append(attrName).append(':').append(pos + message1Length).append(':').append(len);
            break;
          }
          default:
            return "";
        }
      }
      return buf.toString();
    } catch (RuntimeException ex) {
      return "";
    }
  }

}
