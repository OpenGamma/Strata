/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;
import com.google.common.math.DoubleMath;

/**
 * Contains utility methods for checking inputs to methods.
 * <p>
 * This utility is used throughout the system to validate inputs to methods.
 * Most of the methods return their validated input, allowing patterns like this:
 * <pre>
 *  // constructor
 *  public Person(String name, int age) {
 *    this.name = ArgChecker.notBlank(name, "name");
 *    this.age = ArgChecker.notNegative(age, "age");
 *  }
 * </pre>
 */
public final class ArgChecker {

  /**
   * Restricted constructor.
   */
  private ArgChecker() {
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified boolean is true.
   * <p>
   * Given the input argument, this returns normally only if it is true.
   * This will typically be the result of a caller-specific check.
   * For example:
   * <pre>
   *  ArgChecker.isTrue(collection.contains("value"));
   * </pre>
   * <p>
   * It is strongly recommended to pass an additional message argument using
   * {@link #isTrue(boolean, String)}.
   * 
   * @param validIfTrue  a boolean resulting from testing an argument
   * @throws IllegalArgumentException if the test value is false
   */
  public static void isTrue(boolean validIfTrue) {
    // return void, not the argument, as no need to check a boolean method argument
    if (!validIfTrue) {
      throw new IllegalArgumentException("Invalid argument, expression must be true");
    }
  }

  /**
   * Checks that the specified boolean is true.
   * <p>
   * Given the input argument, this returns normally only if it is true.
   * This will typically be the result of a caller-specific check.
   * For example:
   * <pre>
   *  ArgChecker.isTrue(collection.contains("value"), "Collection must contain 'value'");
   * </pre>
   * 
   * @param validIfTrue  a boolean resulting from testing an argument
   * @param message  the error message, not null
   * @throws IllegalArgumentException if the test value is false
   */
  public static void isTrue(boolean validIfTrue, String message) {
    // return void, not the argument, as no need to check a boolean method argument
    if (!validIfTrue) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Checks that the specified boolean is true.
   * <p>
   * Given the input argument, this returns normally only if it is true.
   * This will typically be the result of a caller-specific check.
   * For example:
   * <pre>
   *  ArgChecker.isTrue(collection.contains("value"), "Collection must contain 'value': {}", collection);
   * </pre>
   * <p>
   * This returns {@code void}, and not the value being checked, as there is
   * never a good reason to validate a boolean argument value.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#format(String, Object...)} for more details.
   * 
   * @param validIfTrue  a boolean resulting from testing an argument
   * @param message  the error message with {} placeholders, not null
   * @param arg  the message arguments
   * @throws IllegalArgumentException if the test value is false
   */
  public static void isTrue(boolean validIfTrue, String message, Object... arg) {
    // return void, not the argument, as no need to check a boolean method argument
    if (!validIfTrue) {
      throw new IllegalArgumentException(Messages.format(message, arg));
    }
  }

  /**
   * Checks that the specified boolean is true.
   * <p>
   * Given the input argument, this returns normally only if it is true.
   * This will typically be the result of a caller-specific check.
   * For example:
   * <pre>
   *  ArgChecker.isTrue(value &gt; check, "Value must be greater than check: {}", value);
   * </pre>
   * <p>
   * This returns {@code void}, and not the value being checked, as there is
   * never a good reason to validate a boolean argument value.
   * <p>
   * The message is produced using a template that contains zero or one "{}" placeholders.
   * The placeholder, if present, is replaced by the argument.
   * If there is no placeholder, the argument is appended to the end of the message.
   * 
   * @param validIfTrue  a boolean resulting from testing an argument
   * @param message  the error message with {} placeholders, not null
   * @param arg  the message argument
   * @throws IllegalArgumentException if the test value is false
   */
  public static void isTrue(boolean validIfTrue, String message, long arg) {
    // return void, not the argument, as no need to check a boolean method argument
    if (!validIfTrue) {
      throw new IllegalArgumentException(Messages.format(message, arg));
    }
  }

  /**
   * Checks that the specified boolean is true.
   * <p>
   * Given the input argument, this returns normally only if it is true.
   * This will typically be the result of a caller-specific check.
   * For example:
   * <pre>
   *  ArgChecker.isTrue(value &gt; check, "Value must be greater than check: {}", value);
   * </pre>
   * <p>
   * This returns {@code void}, and not the value being checked, as there is
   * never a good reason to validate a boolean argument value.
   * <p>
   * The message is produced using a template that contains zero or one "{}" placeholders.
   * The placeholder, if present, is replaced by the argument.
   * If there is no placeholder, the argument is appended to the end of the message.
   * 
   * @param validIfTrue  a boolean resulting from testing an argument
   * @param message  the error message with {} placeholders, not null
   * @param arg  the message argument
   * @throws IllegalArgumentException if the test value is false
   */
  public static void isTrue(boolean validIfTrue, String message, double arg) {
    // return void, not the argument, as no need to check a boolean method argument
    if (!validIfTrue) {
      throw new IllegalArgumentException(Messages.format(message, arg));
    }
  }

  /**
   * Checks that the specified boolean is false.
   * <p>
   * Given the input argument, this returns normally only if it is false.
   * This will typically be the result of a caller-specific check.
   * For example:
   * <pre>
   *  ArgChecker.isFalse(collection.contains("value"), "Collection must not contain 'value'");
   * </pre>
   * <p>
   * This returns {@code void}, and not the value being checked, as there is
   * never a good reason to validate a boolean argument value.
   * 
   * @param validIfFalse  a boolean resulting from testing an argument
   * @param message  the error message, not null
   * @throws IllegalArgumentException if the test value is true
   */
  public static void isFalse(boolean validIfFalse, String message) {
    // return void, not the argument, as no need to check a boolean method argument
    if (validIfFalse) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Checks that the specified boolean is false.
   * <p>
   * Given the input argument, this returns normally only if it is false.
   * This will typically be the result of a caller-specific check.
   * For example:
   * <pre>
   *  ArgChecker.isFalse(collection.contains("value"), "Collection must not contain 'value': {}", collection);
   * </pre>
   * <p>
   * This returns {@code void}, and not the value being checked, as there is
   * never a good reason to validate a boolean argument value.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#format(String, Object...)} for more details.
   * 
   * @param validIfFalse  a boolean resulting from testing an argument
   * @param message  the error message with {} placeholders, not null
   * @param arg  the message arguments, not null
   * @throws IllegalArgumentException if the test value is true
   */
  public static void isFalse(boolean validIfFalse, String message, Object... arg) {
    // return void, not the argument, as no need to check a boolean method argument
    if (validIfFalse) {
      throw new IllegalArgumentException(Messages.format(message, arg));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified argument is non-null.
   * <p>
   * Given the input argument, this returns only if it is non-null.
   * For example, in a constructor:
   * <pre>
   *  this.name = ArgChecker.notNull(name, "name");
   * </pre>
   * 
   * @param <T>  the type of the input argument reflected in the result
   * @param argument  the argument to check, null throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null
   */
  public static <T> T notNull(T argument, String name) {
    if (argument == null) {
      throw new IllegalArgumentException(notNullMsg(name));
    }
    return argument;
  }

  // extracted to aid inlining performance
  private static String notNullMsg(String name) {
    return "Argument '" + name + "' must not be null";
  }

  /**
   * Checks that the specified item is non-null.
   * <p>
   * Given the input argument, this returns only if it is non-null.
   * One use for this method is in a stream:
   * <pre>
   *  ArgChecker.notNull(coll, "coll")
   *  coll.stream()
   *    .map(ArgChecker::notNullItem)
   *    ...
   * </pre>
   * 
   * @param <T>  the type of the input argument reflected in the result
   * @param argument  the argument to check, null throws an exception
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null
   */
  public static <T> T notNullItem(T argument) {
    if (argument == null) {
      throw new IllegalArgumentException("Argument array/collection/map must not contain null");
    }
    return argument;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified argument is non-null and matches the specified pattern.
   * <p>
   * Given the input argument, this returns only if it is non-null and matches
   * the regular expression pattern specified.
   * For example, in a constructor:
   * <pre>
   *  this.name = ArgChecker.matches(REGEX_NAME, name, "name");
   * </pre>
   * 
   * @param pattern  the pattern to check against, not null
   * @param argument  the argument to check, null throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static String matches(Pattern pattern, String argument, String name) {
    notNull(pattern, "pattern");
    notNull(argument, name);
    if (!pattern.matcher(argument).matches()) {
      throw new IllegalArgumentException(matchesMsg(pattern, name, argument));
    }
    return argument;
  }

  // extracted to aid inlining performance
  private static String matchesMsg(Pattern pattern, String name, String value) {
    return "Argument '" + name + "' with value '" + value + "' must match pattern: " + pattern;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified argument is non-null and only contains the specified characters.
   * <p>
   * Given the input argument, this returns only if it is non-null and matches
   * the {@link CharMatcher} specified.
   * For example, in a constructor:
   * <pre>
   *  this.name = ArgChecker.matches(REGEX_NAME, 1, Integer.MAX_VALUE, name, "name", "[A-Z]+");
   * </pre>
   * 
   * @param matcher  the matcher to check against, not null
   * @param minLength  the minimum length to allow
   * @param maxLength  the minimum length to allow
   * @param argument  the argument to check, null throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @param equivalentRegex  the equivalent regular expression pattern
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static String matches(
      CharMatcher matcher,
      int minLength,
      int maxLength,
      String argument,
      String name,
      String equivalentRegex) {

    notNull(matcher, "pattern");
    notNull(argument, name);
    if (argument.length() < minLength || argument.length() > maxLength || !matcher.matchesAllOf(argument)) {
      throw new IllegalArgumentException(matchesMsg(matcher, name, argument, equivalentRegex));
    }
    return argument;
  }

  // extracted to aid inlining performance
  private static String matchesMsg(CharMatcher matcher, String name, String value, String equivalentRegex) {
    return "Argument '" + name + "' with value '" + value + "' must match pattern: " + equivalentRegex;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified argument is non-null and not blank.
   * <p>
   * Given the input argument, this returns the input only if it is non-null
   * and contains at least one non whitespace character.
   * This is often linked with a call to {@code trim()}.
   * For example, in a constructor:
   * <pre>
   *  this.name = ArgChecker.notBlank(name, "name").trim();
   * </pre>
   * <p>
   * The argument is trimmed using {@link String#trim()} to determine if it is empty.
   * The result is the original argument, not the trimmed one.
   * 
   * @param argument  the argument to check, null or blank throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or blank
   */
  public static String notBlank(String argument, String name) {
    notNull(argument, name);
    if (argument.trim().isEmpty()) {
      throw new IllegalArgumentException(notBlankMsg(name));
    }
    return argument;
  }

  // extracted to aid inlining performance
  private static String notBlankMsg(String name) {
    return "Argument '" + name + "' must not be blank";
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified argument is non-null and not empty.
   * <p>
   * Given the input argument, this returns only if it is non-null and contains
   * at least one character, which may be a whitespace character.
   * See also {@link #notBlank(String, String)}.
   * For example, in a constructor:
   * <pre>
   *  this.name = ArgChecker.notEmpty(name, "name");
   * </pre>
   * 
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static String notEmpty(String argument, String name) {
    notNull(argument, name);
    if (argument.isEmpty()) {
      throw new IllegalArgumentException(notEmptyMsg(name));
    }
    return argument;
  }

  // extracted to aid inlining performance
  private static String notEmptyMsg(String name) {
    return "Argument '" + name + "' must not be empty";
  }

  /**
   * Checks that the specified argument array is non-null and not empty.
   * <p>
   * Given the input argument, this returns only if it is non-null and contains
   * at least one element. The element is not validated and may be null.
   * For example, in a constructor:
   * <pre>
   *  this.names = ArgChecker.notEmpty(names, "names");
   * </pre>
   * 
   * @param <T>  the type of the input array reflected in the result
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static <T> T[] notEmpty(T[] argument, String name) {
    notNull(argument, name);
    if (argument.length == 0) {
      throw new IllegalArgumentException(notEmptyArrayMsg(name));
    }
    return argument;
  }

  // extracted to aid inlining performance
  private static String notEmptyArrayMsg(String name) {
    return "Argument array '" + name + "' must not be empty";
  }

  /**
   * Checks that the specified argument array is non-null and not empty.
   * <p>
   * Given the input argument, this returns only if it is non-null and contains
   * at least one element.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.notEmpty(values, "values");
   * </pre>
   * 
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static int[] notEmpty(int[] argument, String name) {
    notNull(argument, name);
    if (argument.length == 0) {
      throw new IllegalArgumentException(notEmptyArrayMsg(name));
    }
    return argument;
  }

  /**
   * Checks that the specified argument array is non-null and not empty.
   * <p>
   * Given the input argument, this returns only if it is non-null and contains
   * at least one element.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.notEmpty(values, "values");
   * </pre>
   * 
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static long[] notEmpty(long[] argument, String name) {
    notNull(argument, name);
    if (argument.length == 0) {
      throw new IllegalArgumentException(notEmptyArrayMsg(name));
    }
    return argument;
  }

  /**
   * Checks that the specified argument array is non-null and not empty.
   * <p>
   * Given the input argument, this returns only if it is non-null and contains
   * at least one element.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.notEmpty(values, "values");
   * </pre>
   * 
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static double[] notEmpty(double[] argument, String name) {
    notNull(argument, name);
    if (argument.length == 0) {
      throw new IllegalArgumentException(notEmptyArrayMsg(name));
    }
    return argument;
  }

  /**
   * Checks that the specified argument iterable is non-null and not empty.
   * <p>
   * Given the input argument, this returns only if it is non-null and contains
   * at least one element. The element is not validated and may be null.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.notEmpty(values, "values");
   * </pre>
   * 
   * @param <T>  the element type of the input iterable reflected in the result
   * @param <I>  the type of the input iterable, reflected in the result
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static <T, I extends Iterable<T>> I notEmpty(I argument, String name) {
    notNull(argument, name);
    if (!argument.iterator().hasNext()) {
      throw new IllegalArgumentException(notEmptyIterableMsg(name));
    }
    return argument;
  }

  // extracted to aid inlining performance
  private static String notEmptyIterableMsg(String name) {
    return "Argument iterable '" + name + "' must not be empty";
  }

  /**
   * Checks that the specified argument collection is non-null and not empty.
   * <p>
   * Given the input argument, this returns only if it is non-null and contains at least one element.
   * The element is not validated and may contain nulls if the collection allows nulls.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.notEmpty(values, "values");
   * </pre>
   *
   * @param <T>  the element type of the input collection reflected in the result
   * @param <C>  the type of the input collection, reflected in the result
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static <T, C extends Collection<T>> C notEmpty(C argument, String name) {
    notNull(argument, name);
    if (argument.isEmpty()) {
      throw new IllegalArgumentException(notEmptyCollectionMsg(name));
    }
    return argument;
  }

  // extracted to aid inlining performance
  private static String notEmptyCollectionMsg(String name) {
    return "Argument collection '" + name + "' must not be empty";
  }

  /**
   * Checks that the specified argument map is non-null and not empty.
   * <p>
   * Given the input argument, this returns only if it is non-null and contains at least one mapping.
   * The element is not validated and may contain nulls if the collection allows nulls.
   * For example, in a constructor:
   * <pre>
   *  this.keyValues = ArgChecker.notEmpty(keyValues, "keyValues");
   * </pre>
   * 
   * @param <K>  the key type of the input map key, reflected in the result
   * @param <V>  the value type of the input map value, reflected in the result
   * @param <M>  the type of the input map, reflected in the result
   * @param argument  the argument to check, null or empty throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static <K, V, M extends Map<K, V>> M notEmpty(M argument, String name) {
    notNull(argument, name);
    if (argument.isEmpty()) {
      throw new IllegalArgumentException(notEmptyMapMsg(name));
    }
    return argument;
  }

  // extracted to aid inlining performance
  private static String notEmptyMapMsg(String name) {
    return "Argument map '" + name + "' must not be empty";
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified argument array is non-null and contains no nulls.
   * <p>
   * Given the input argument, this returns only if it is non-null and contains no nulls.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.noNulls(values, "values");
   * </pre>
   * 
   * @param <T>  the type of the input array reflected in the result
   * @param argument  the argument to check, null or contains null throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or contains nulls
   */
  public static <T> T[] noNulls(T[] argument, String name) {
    notNull(argument, name);
    for (int i = 0; i < argument.length; i++) {
      if (argument[i] == null) {
        throw new IllegalArgumentException("Argument array '" + name + "' must not contain null at index " + i);
      }
    }
    return argument;
  }

  /**
   * Checks that the specified argument collection is non-null and contains no nulls.
   * <p>
   * Given the input argument, this returns only if it is non-null and contains no nulls.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.noNulls(values, "values");
   * </pre>
   * 
   * @param <T>  the element type of the input iterable reflected in the result
   * @param <I>  the type of the input iterable, reflected in the result
   * @param argument  the argument to check, null or contains null throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or contains nulls
   */
  public static <T, I extends Iterable<T>> I noNulls(I argument, String name) {
    notNull(argument, name);
    for (Object obj : argument) {
      if (obj == null) {
        throw new IllegalArgumentException("Argument iterable '" + name + "' must not contain null");
      }
    }
    return argument;
  }

  /**
   * Checks that the specified argument map is non-null and contains no nulls.
   * <p>
   * Given the input argument, this returns only if it is non-null and contains no nulls.
   * For example, in a constructor:
   * <pre>
   *  this.keyValues = ArgChecker.noNulls(keyValues, "keyValues");
   * </pre>
   * 
   * @param <K>  the key type of the input map key, reflected in the result
   * @param <V>  the value type of the input map value, reflected in the result
   * @param <M>  the type of the input map, reflected in the result
   * @param argument  the argument to check, null or contains null throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or contains nulls
   */
  public static <K, V, M extends Map<K, V>> M noNulls(M argument, String name) {
    notNull(argument, name);
    for (Entry<K, V> entry : argument.entrySet()) {
      if (entry.getKey() == null) {
        throw new IllegalArgumentException("Argument map '" + name + "' must not contain a null key");
      }
      if (entry.getValue() == null) {
        throw new IllegalArgumentException("Argument map '" + name + "' must not contain a null value");
      }
    }
    return argument;
  }

  //-----------------------------------------------------------------------
  /**
   * Checks that the specified argument array is non-null and does not contain any duplicate values.
   * <p>
   * Given the input argument, this returns only if it is non-null and does not contain duplicate values.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.noDuplicates(values, "values");
   * </pre>
   * <p>
   * If you know the argument is sorted increasing then {@link #noDuplicatesSorted(double[], String)} might be more
   * performant.
   *
   * @param argument  the argument to check, null or duplicate values throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null or contains duplicate values
   */
  public static double[] noDuplicates(double[] argument, String name) {
    notNull(argument, name);
    if (argument.length > 1) {
      Set<Double> seen = new LinkedHashSet<>();
      for (double v : argument) {
        if (!seen.add(v)) {
          throw new IllegalArgumentException(noDuplicatesArrayMsg(name));
        }
      }
    }
    return argument;
  }

  // extracted to aid inlining performance
  private static String noDuplicatesArrayMsg(String name) {
    return "Argument array '" + name + "' must not contain duplicates";
  }

  /**
   * Checks that the specified argument array is non-null, sorted, and does not contain any duplicate values.
   * <p>
   * Given the input argument, this returns only if it is non-null, sorted, and does not contain duplicate values.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.noDuplicatesSorted(values, "values");
   * </pre>
   *
   * @param argument  the argument to check, null, out of order or duplicate values throws an exception
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}, not null
   * @throws IllegalArgumentException if the input is null, unsorted, or contains duplicate values
   */
  public static double[] noDuplicatesSorted(double[] argument, String name) {
    notNull(argument, name);
    for (int i = 1; i < argument.length; i++) {
      if (argument[i] == argument[i - 1]) {
        throw new IllegalArgumentException(noDuplicatesArrayMsg(name));
      } else if (argument[i] < argument[i - 1]) {
        throw new IllegalArgumentException(noDuplicatesSortedArrayMsg(name));
      }
    }
    return argument;
  }

  // extracted to aid inlining performance
  private static String noDuplicatesSortedArrayMsg(String name) {
    return "Argument array '" + name + "' must be sorted and not contain duplicates";
  }

  //-------------------------------------------------------------------------

  /**
   * Checks that the argument is not positive.
   * <p>
   * Given the input argument, this returns only if it is less than or equal to zero.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notPositive(amount, "amount");
   * </pre>
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the input is positive
   */
  public static int notPositive(int argument, String name) {
    if (argument > 0) {
      throw new IllegalArgumentException(notPositiveMsg(name, argument));
    }
    return argument;
  }

  private static String notPositiveMsg(String name, Object argument) {
    return "Argument '" + name + "' must not be positive but has value " + argument;
  }

  /**
   * Checks that the argument is not positive.
   * <p>
   * Given the input argument, this returns only if it is less than or equal to zero.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notPositive(amount, "amount");
   * </pre>
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the input is positive
   */
  public static long notPositive(long argument, String name) {
    if (argument > 0) {
      throw new IllegalArgumentException(notPositiveMsg(name, argument));
    }
    return argument;
  }

  /**
   * Checks that the argument is not positive.
   * <p>
   * Given the input argument, this returns only if it is less than or equal to zero.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notPositive(amount, "amount");
   * </pre>
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the input is positive
   */
  public static double notPositive(double argument, String name) {
    if (argument > 0) {
      throw new IllegalArgumentException(notPositiveMsg(name, argument));
    }
    return argument;
  }

  /**
   * Checks that the argument is not positive.
   * <p>
   * Given the input argument, this returns only if it is not null and is less than or equal to zero.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notPositive(amount, "amount");
   * </pre>
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the input is null or positive
   */
  public static Decimal notPositive(Decimal argument, String name) {
    notNull(argument, name);
    if (argument.unscaledValue() > 0) {
      throw new IllegalArgumentException(notPositiveMsg(name, argument));
    }
    return argument;
  }

  /**
   * Checks that the argument is not positive if the arguement is not null.
   * <p>
   * Given the input argument, this returns only if it is null or if it is less than or equal to zero.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notPositiveIfPresent(amount, "amount");
   * </pre>
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the input is not null and is positive
   */
  public static Decimal notPositiveIfPresent(Decimal argument, String name) {
    if (argument != null && argument.unscaledValue() > 0) {
      throw new IllegalArgumentException(notPositiveMsg(name, argument));
    }
    return argument;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is not negative.
   * <p>
   * Given the input argument, this returns only if it is zero or greater.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegative(amount, "amount");
   * </pre>
   * 
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the input is negative
   */
  public static int notNegative(int argument, String name) {
    if (argument < 0) {
      throw new IllegalArgumentException(notNegativeMsg(name, argument));
    }
    return argument;
  }

  // extracted to aid inlining performance
  private static String notNegativeMsg(String name, Object argument) {
    return "Argument '" + name + "' must not be negative but has value " + argument;
  }

  /**
   * Checks that the argument is not negative.
   * <p>
   * Given the input argument, this returns only if it is zero or greater.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegative(amount, "amount");
   * </pre>
   * 
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the input is negative
   */
  public static long notNegative(long argument, String name) {
    if (argument < 0) {
      throw new IllegalArgumentException(notNegativeMsg(name, argument));
    }
    return argument;
  }

  /**
   * Checks that the argument is not negative.
   * <p>
   * Given the input argument, this returns only if it is zero or greater.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegative(amount, "amount");
   * </pre>
   * 
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the input is negative
   */
  public static double notNegative(double argument, String name) {
    if (argument < 0) {
      throw new IllegalArgumentException(notNegativeMsg(name, argument));
    }
    return argument;
  }

  /**
   * Checks that the argument is not negative.
   * <p>
   * Given the input argument, this returns only if it is zero or greater.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegative(amount, "amount");
   * </pre>
   * 
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the input is negative
   */
  public static Decimal notNegative(Decimal argument, String name) {
    if (argument.unscaledValue() < 0) {
      throw new IllegalArgumentException(notNegativeMsg(name, argument));
    }
    return argument;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is a number and not NaN.
   * <p>
   * Given the input argument, this returns only if it is an actual number.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNaN(amount, "amount");
   * </pre>
   *
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the input is NaN
   */
  public static double notNaN(double argument, String name) {
    if (Double.isNaN(argument)) {
      throw new IllegalArgumentException(notNaNMsg(name));
    }
    return argument;
  }

  // extracted to aid inlining performance
  private static String notNaNMsg(String name) {
    return "Argument '" + name + "' must not be NaN";
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is not negative or zero.
   * <p>
   * Given the input argument, this returns only if it is greater than zero.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegativeOrZero(amount, "amount");
   * </pre>
   * 
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the input is negative or zero
   */
  public static int notNegativeOrZero(int argument, String name) {
    if (argument <= 0) {
      throw new IllegalArgumentException(notNegativeOrZeroMsg(name, argument));
    }
    return argument;
  }

  // extracted to aid inlining performance
  private static String notNegativeOrZeroMsg(String name, Object argument) {
    return "Argument '" + name + "' must not be negative or zero but has value " + argument;
  }

  /**
   * Checks that the argument is not negative or zero.
   * <p>
   * Given the input argument, this returns only if it is greater than zero.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegativeOrZero(amount, "amount");
   * </pre>
   * 
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the input is negative or zero
   */
  public static long notNegativeOrZero(long argument, String name) {
    if (argument <= 0) {
      throw new IllegalArgumentException(notNegativeOrZeroMsg(name, argument));
    }
    return argument;
  }

  /**
   * Checks that the argument is not negative or zero.
   * <p>
   * Given the input argument, this returns only if it is greater than zero.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegativeOrZero(amount, "amount");
   * </pre>
   * 
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the input is negative or zero
   */
  public static double notNegativeOrZero(double argument, String name) {
    if (argument <= 0) {
      throw new IllegalArgumentException(notNegativeOrZeroMsg(name, argument));
    }
    return argument;
  }

  /**
   * Checks that the argument is greater than zero to within a given accuracy.
   * <p>
   * Given the input argument, this returns only if it is greater than zero
   * using the {@code eps} accuracy for zero.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegativeOrZero(amount, 0.0001d, "amount");
   * </pre>
   * 
   * @param argument  the value to check
   * @param tolerance  the tolerance to use for zero
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the absolute value of the argument is less than eps
   */
  public static double notNegativeOrZero(double argument, double tolerance, String name) {
    if (DoubleMath.fuzzyEquals(argument, 0, tolerance)) {
      throw new IllegalArgumentException("Argument '" + name + "' must not be zero");
    }
    if (argument < 0) {
      throw new IllegalArgumentException("Argument '" + name + "' must be greater than zero but has value " + argument);
    }
    return argument;
  }

  /**
   * Checks that the argument is not negative or zero.
   * <p>
   * Given the input argument, this returns only if it is greater than zero.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegativeOrZero(amount, "amount");
   * </pre>
   * 
   * @param argument  the argument to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the input is negative or zero
   */
  public static Decimal notNegativeOrZero(Decimal argument, String name) {
    if (argument.unscaledValue() <= 0) {
      throw new IllegalArgumentException(notNegativeOrZeroMsg(name, argument));
    }
    return argument;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is not equal to zero.
   * <p>
   * Given the input argument, this returns only if it is not zero.
   * Both positive and negative zero are checked.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notZero(amount, "amount");
   * </pre>
   *
   * @param argument  the value to check
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the argument is zero
   */
  public static double notZero(double argument, String name) {
    if (argument == 0d || argument == -0d) {
      throw new IllegalArgumentException("Argument '" + name + "' must not be zero");
    }
    return argument;
  }

  /**
   * Checks that the argument is not equal to zero to within a given accuracy.
   * <p>
   * Given the input argument, this returns only if it is not zero comparing
   * using the {@code eps} accuracy.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notZero(amount, 0.0001d, "amount");
   * </pre>
   *
   * @param argument  the value to check
   * @param tolerance  the tolerance to use for zero
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the absolute value of the argument is less than the tolerance
   */
  public static double notZero(double argument, double tolerance, String name) {
    if (DoubleMath.fuzzyEquals(argument, 0d, tolerance)) {
      throw new IllegalArgumentException("Argument '" + name + "' must not be zero");
    }
    return argument;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is within the range defined by {@code low <= x < high}.
   * <p>
   * Given a value, this returns true if it is within the specified range including the
   * lower boundary but excluding the upper boundary.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.inRange(amount, 0d, 1d, "amount");
   * </pre>
   *
   * @param argument  the value to check
   * @param lowInclusive  the low value of the range
   * @param highExclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the argument is outside the valid range
   */
  public static double inRange(double argument, double lowInclusive, double highExclusive, String name) {
    if (argument < lowInclusive || argument >= highExclusive) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} <= '{}' < {}, but found {}", lowInclusive, name, highExclusive, argument));
    }
    return argument;
  }

  /**
   * Checks that the argument is within the range defined by {@code low <= x <= high}.
   * <p>
   * Given a value, this returns true if it is within the specified range including both boundaries.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.inRangeInclusive(amount, 0d, 1d, "amount");
   * </pre>
   *
   * @param argument  the value to check
   * @param lowInclusive  the low value of the range
   * @param highInclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the argument is outside the valid range
   */
  public static double inRangeInclusive(double argument, double lowInclusive, double highInclusive, String name) {
    if (argument < lowInclusive || argument > highInclusive) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} <= '{}' <= {}, but found {}", lowInclusive, name, highInclusive, argument));
    }
    return argument;
  }

  /**
   * Checks that the argument is within the range defined by {@code low < x < high}.
   * <p>
   * Given a value, this returns true if it is within the specified range excluding both boundaries.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.inRangeExclusive(amount, 0d, 1d, "amount");
   * </pre>
   *
   * @param argument  the value to check
   * @param lowExclusive  the low value of the range
   * @param highExclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the argument is outside the valid range
   */
  public static double inRangeExclusive(double argument, double lowExclusive, double highExclusive, String name) {
    if (argument <= lowExclusive || argument >= highExclusive) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} < '{}' < {}, but found {}", lowExclusive, name, highExclusive, argument));
    }
    return argument;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is within the range defined by {@code low <= x < high}.
   * <p>
   * Given a value, this returns true if it is within the specified range including the
   * lower boundary but excluding the upper boundary.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.inRange(amount, 0, 1, "amount");
   * </pre>
   *
   * @param argument  the value to check
   * @param lowInclusive  the low value of the range
   * @param highExclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the argument is outside the valid range
   */
  public static int inRange(int argument, int lowInclusive, int highExclusive, String name) {
    if (argument < lowInclusive || argument >= highExclusive) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} <= '{}' < {}, but found {}", lowInclusive, name, highExclusive, argument));
    }
    return argument;
  }

  /**
   * Checks that the argument is within the range defined by {@code low <= x <= high}.
   * <p>
   * Given a value, this returns true if it is within the specified range including both boundaries.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.inRangeInclusive(amount, 0, 1, "amount");
   * </pre>
   *
   * @param argument  the value to check
   * @param lowInclusive  the low value of the range
   * @param highInclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the argument is outside the valid range
   */
  public static int inRangeInclusive(int argument, int lowInclusive, int highInclusive, String name) {
    if (argument < lowInclusive || argument > highInclusive) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} <= '{}' <= {}, but found {}", lowInclusive, name, highInclusive, argument));
    }
    return argument;
  }

  /**
   * Checks that the argument is within the range defined by {@code low < x < high}.
   * <p>
   * Given a value, this returns true if it is within the specified range excluding both boundaries.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.inRangeExclusive(amount, 0, 1, "amount");
   * </pre>
   *
   * @param argument  the value to check
   * @param lowExclusive  the low value of the range
   * @param highExclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the argument is outside the valid range
   */
  public static int inRangeExclusive(int argument, int lowExclusive, int highExclusive, String name) {
    if (argument <= lowExclusive || argument >= highExclusive) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} < '{}' < {}, but found {}", lowExclusive, name, highExclusive, argument));
    }
    return argument;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is within the range defined by {@code low <= x < high} using the items' natural order.
   * <p>
   * Given a value, this returns true if it is within the specified range including the
   * lower boundary but excluding the upper boundary.
   * For example, in a constructor:
   * <pre>
   *  this.duration = ArgChecker.inRangeComparable(duration, Duration.ZERO, Duration.ofHours(1), "duration");
   * </pre>
   *
   * @param <T>  the type of the value
   * @param argument  the value to check
   * @param lowInclusive  the low value of the range
   * @param highExclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the argument is outside the valid range
   */
  public static <T extends Comparable<T>> T inRangeComparable(T argument, T lowInclusive, T highExclusive, String name) {
    if (argument == null || argument.compareTo(lowInclusive) < 0 || argument.compareTo(highExclusive) >= 0) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} <= '{}' < {}, but found {}", lowInclusive, name, highExclusive, argument));
    }
    return argument;
  }

  /**
   * Checks that the argument is within the range defined by {@code low <= x <= high} using the items' natural order.
   * <p>
   * Given a value, this returns true if it is within the specified range including both boundaries.
   * For example, in a constructor:
   * <pre>
   *  this.duration = ArgChecker.inRangeComparableInclusive(duration, Duration.ZERO, Duration.ofHours(1), "duration");
   * </pre>
   *
   * @param <T>  the type of the value
   * @param argument  the value to check
   * @param lowInclusive  the low value of the range
   * @param highInclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the argument is outside the valid range
   */
  public static <T extends Comparable<T>> T inRangeComparableInclusive(T argument, T lowInclusive, T highInclusive, String name) {
    if (argument == null || argument.compareTo(lowInclusive) < 0 || argument.compareTo(highInclusive) > 0) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} <= '{}' <= {}, but found {}", lowInclusive, name, highInclusive, argument));
    }
    return argument;
  }

  /**
   * Checks that the argument is within the range defined by {@code low < x < high} using the items' natural order.
   * <p>
   * Given a value, this returns true if it is within the specified range excluding both boundaries.
   * For example, in a constructor:
   * <pre>
   *  this.duration = ArgChecker.inRangeComparableExclusive(duration, Duration.ZERO, Duration.ofHours(1), "duration");
   * </pre>
   *
   * @param <T>  the type of the value
   * @param argument  the value to check
   * @param lowExclusive  the low value of the range
   * @param highExclusive  the high value of the range
   * @param name  the name of the argument to use in the error message, not null
   * @return the input {@code argument}
   * @throws IllegalArgumentException if the argument is outside the valid range
   */
  public static <T extends Comparable<T>> T inRangeComparableExclusive(T argument, T lowExclusive, T highExclusive, String name) {
    if (argument == null || argument.compareTo(lowExclusive) <= 0 || argument.compareTo(highExclusive) >= 0) {
      throw new IllegalArgumentException(
          Messages.format("Expected {} < '{}' < {}, but found {}", lowExclusive, name, highExclusive, argument));
    }
    return argument;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the two values are in order and not equal.
   * <p>
   * Given two comparable instances, this checks that the first is "less than" the second.
   * Two equal values also throw the exception.
   *
   * @param <T>  the type
   * @param obj1  the first object, null throws an exception
   * @param obj2  the second object, null throws an exception
   * @param name1  the first argument name, not null
   * @param name2  the second argument name, not null
   * @throws IllegalArgumentException if either input is null or they are not in order
   */
  public static <T> void inOrderNotEqual(Comparable<? super T> obj1, T obj2, String name1, String name2) {
    notNull(obj1, name1);
    notNull(obj2, name2);
    if (obj1.compareTo(obj2) >= 0) {
      throw new IllegalArgumentException(
          Messages.format("Invalid order: Expected '{}' < '{}', but found: '{}' >= '{}'", name1, name2, obj1, obj2));
    }
  }

  /**
   * Checks that the two values are in order or equal.
   * <p>
   * Given two comparable instances, this checks that the first is "less than" or "equal to" the second.
   *
   * @param <T>  the type
   * @param obj1  the first object, null throws an exception
   * @param obj2  the second object, null throws an exception
   * @param name1  the first argument name, not null
   * @param name2  the second argument name, not null
   * @throws IllegalArgumentException if either input is null or they are not in order
   */
  public static <T> void inOrderOrEqual(Comparable<? super T> obj1, T obj2, String name1, String name2) {
    notNull(obj1, name1);
    notNull(obj2, name2);
    if (obj1.compareTo(obj2) > 0) {
      throw new IllegalArgumentException(
          Messages.format("Invalid order: Expected '{}' <= '{}', but found: '{}' > '{}'", name1, name2, obj1, obj2));
    }
  }

}
