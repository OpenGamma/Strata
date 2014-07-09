/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.validate;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

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
   * Given the input parameter, this returns normally only if it is true.
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
    // return void, not the parameter, as no need to check a boolean method parameter
    if (!validIfTrue) {
      throw new IllegalArgumentException(message);
    }
  }
  
  /**
   * Checks that the specified boolean is true.
   * <p>
   * Given the input parameter, this returns normally only if it is true.
   * This will typically be the result of a caller-specific check.
   * For example:
   * <pre>
   *  ArgChecker.isTrue(collection.contains("value"), "Collection must contain 'value': {}", collection);
   * </pre>
   * <p>
   * This returns {@code void}, and not the value being checked, as there is
   * never a good reason to validate a boolean parameter value.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link ArgChecker#formatMessage(String, Object...)} for more details.
   * 
   * @param validIfTrue  a boolean resulting from testing an argument
   * @param message  the error message with {} placeholders, not null
   * @param arg  the message arguments
   * @throws IllegalArgumentException if the test value is false
   */
  public static void isTrue(boolean validIfTrue, String message, Object... arg) {
    // return void, not the parameter, as no need to check a boolean method parameter
    if (!validIfTrue) {
      throw new IllegalArgumentException(formatMessage(message, arg));
    }
  }  

  /**
   * Checks that the specified boolean is false.
   * <p>
   * Given the input parameter, this returns normally only if it is false.
   * This will typically be the result of a caller-specific check.
   * For example:
   * <pre>
   *  ArgChecker.isFalse(collection.contains("value"), "Collection must not contain 'value'");
   * </pre>
   * <p>
   * This returns {@code void}, and not the value being checked, as there is
   * never a good reason to validate a boolean parameter value.
   * 
   * @param validIfFalse  a boolean resulting from testing an argument
   * @param message  the error message, not null
   * @throws IllegalArgumentException if the test value is true
   */
  public static void isFalse(boolean validIfFalse, String message) {
    // return void, not the parameter, as no need to check a boolean method parameter
    if (validIfFalse) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Checks that the specified boolean is false.
   * <p>
   * Given the input parameter, this returns normally only if it is false.
   * This will typically be the result of a caller-specific check.
   * For example:
   * <pre>
   *  ArgChecker.isFalse(collection.contains("value"), "Collection must not contain 'value': {}", collection);
   * </pre>
   * <p>
   * This returns {@code void}, and not the value being checked, as there is
   * never a good reason to validate a boolean parameter value.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link ArgChecker#formatMessage(String, Object...)} for more details.
   * 
   * @param validIfFalse  a boolean resulting from testing an argument
   * @param message  the error message with {} placeholders, not null
   * @param arg  the message arguments, not null
   * @throws IllegalArgumentException if the test value is true
   */
  public static void isFalse(boolean validIfFalse, String message, Object... arg) {
    // return void, not the parameter, as no need to check a boolean method parameter
    if (validIfFalse) {
      throw new IllegalArgumentException(formatMessage(message, arg));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified parameter is non-null.
   * <p>
   * Given the input parameter, this returns only if it is non-null.
   * For example, in a constructor:
   * <pre>
   *  this.name = ArgChecker.notNull(name, "name");
   * </pre>
   * 
   * @param <T>  the type of the input parameter reflected in the result
   * @param parameter  the parameter to check, null throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null
   */
  public static <T> T notNull(T parameter, String name) {
    if (parameter == null) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must not be null");
    }
    return parameter;
  }

  /**
   * Checks that the specified item is non-null.
   * <p>
   * Given the input parameter, this returns only if it is non-null.
   * One use for this method is in a stream:
   * <pre>
   *  ArgChecker.notNull(coll, "coll")
   *  coll.stream()
   *    .map(ArgChecker::notNullItem)
   *    ...
   * </pre>
   * 
   * @param <T>  the type of the input parameter reflected in the result
   * @param parameter  the parameter to check, null throws an exception
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null
   */
  public static <T> T notNullItem(T parameter) {
    if (parameter == null) {
      throw new IllegalArgumentException("Input parameter array/collection/map must not contain null");
    }
    return parameter;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified parameter is non-null and matches the specified pattern.
   * <p>
   * Given the input parameter, this returns only if it is non-null and matches
   * the regular expression pattern specified.
   * For example, in a constructor:
   * <pre>
   *  this.name = ArgChecker.matches(REGEX_NAME, name, "name");
   * </pre>
   * 
   * @param pattern  the pattern to check against, not null
   * @param parameter  the parameter to check, null throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static String matches(Pattern pattern, String parameter, String name) {
    notNull(pattern, "pattern");
    notNull(parameter, name);
    if (pattern.matcher(parameter).matches() == false) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must match pattern: " + pattern);
    }
    return parameter;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified parameter is non-null and not blank.
   * <p>
   * Given the input parameter, this returns the input only if it is non-null
   * and contains at least one non whitespace character.
   * This is often linked with a call to {@code trim()}.
   * For example, in a constructor:
   * <pre>
   *  this.name = ArgChecker.notBlank(name, "name").trim();
   * </pre>
   * <p>
   * The parameter is trimmed using {@link String#trim()} to determine if it is empty.
   * The result is the original parameter, not the trimmed one.
   * 
   * @param parameter  the parameter to check, null or blank throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or blank
   */
  public static String notBlank(String parameter, String name) {
    notNull(parameter, name);
    if (parameter.trim().isEmpty()) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must not be empty");
    }
    return parameter;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified parameter is non-null and not empty.
   * <p>
   * Given the input parameter, this returns only if it is non-null and contains
   * at least one character, which may be a whitespace character.
   * See also {@link #notBlank(String, String)}.
   * For example, in a constructor:
   * <pre>
   *  this.name = ArgChecker.notEmpty(name, "name");
   * </pre>
   * 
   * @param parameter  the parameter to check, null or empty throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static String notEmpty(String parameter, String name) {
    notNull(parameter, name);
    if (parameter.isEmpty()) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must not be empty");
    }
    return parameter;
  }

  /**
   * Checks that the specified parameter array is non-null and not empty.
   * <p>
   * Given the input parameter, this returns only if it is non-null and contains
   * at least one element. The element is not validated and may be null.
   * For example, in a constructor:
   * <pre>
   *  this.names = ArgChecker.notEmpty(names, "names");
   * </pre>
   * 
   * @param <T>  the type of the input array reflected in the result
   * @param parameter  the parameter to check, null or empty throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static <T> T[] notEmpty(T[] parameter, String name) {
    notNull(parameter, name);
    if (parameter.length == 0) {
      throw new IllegalArgumentException("Input parameter array '" + name + "' must not be empty");
    }
    return parameter;
  }

  /**
   * Checks that the specified parameter array is non-null and not empty.
   * <p>
   * Given the input parameter, this returns only if it is non-null and contains
   * at least one element.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.notEmpty(values, "values");
   * </pre>
   * 
   * @param parameter  the parameter to check, null or empty throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static int[] notEmpty(int[] parameter, String name) {
    notNull(parameter, name);
    if (parameter.length == 0) {
      throw new IllegalArgumentException("Input parameter array '" + name + "' must not be empty");
    }
    return parameter;
  }

  /**
   * Checks that the specified parameter array is non-null and not empty.
   * <p>
   * Given the input parameter, this returns only if it is non-null and contains
   * at least one element.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.notEmpty(values, "values");
   * </pre>
   * 
   * @param parameter  the parameter to check, null or empty throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static long[] notEmpty(long[] parameter, String name) {
    notNull(parameter, name);
    if (parameter.length == 0) {
      throw new IllegalArgumentException("Input parameter array '" + name + "' must not be empty");
    }
    return parameter;
  }

  /**
   * Checks that the specified parameter array is non-null and not empty.
   * <p>
   * Given the input parameter, this returns only if it is non-null and contains
   * at least one element.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.notEmpty(values, "values");
   * </pre>
   * 
   * @param parameter  the parameter to check, null or empty throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static double[] notEmpty(double[] parameter, String name) {
    notNull(parameter, name);
    if (parameter.length == 0) {
      throw new IllegalArgumentException("Input parameter array '" + name + "' must not be empty");
    }
    return parameter;
  }

  /**
   * Checks that the specified parameter iterable is non-null and not empty.
   * <p>
   * Given the input parameter, this returns only if it is non-null and contains
   * at least one element. The element is not validated and may be null.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.notEmpty(values, "values");
   * </pre>
   * 
   * @param <T>  the element type of the input iterable reflected in the result
   * @param <I>  the type of the input iterable, reflected in the result
   * @param parameter  the parameter to check, null or empty throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static <T, I extends Iterable<T>> I notEmpty(I parameter, String name) {
    notNull(parameter, name);
    if (!parameter.iterator().hasNext()) {
      throw new IllegalArgumentException("Input parameter iterable '" + name + "' must not be empty");
    }
    return parameter;
  }

  /**
   * Checks that the specified parameter collection is non-null and not empty.
   * <p>
   * Given the input parameter, this returns only if it is non-null and contains at least one element.
   * The element is not validated and may contain nulls if the collection allows nulls.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.notEmpty(values, "values");
   * </pre>
   *
   * @param <T>  the element type of the input collection reflected in the result
   * @param <C>  the type of the input collection, reflected in the result
   * @param parameter  the parameter to check, null or empty throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static <T, C extends Collection<T>> C notEmpty(C parameter, String name) {
    notNull(parameter, name);
    if (parameter.isEmpty()) {
      throw new IllegalArgumentException("Input parameter collection '" + name + "' must not be empty");
    }
    return parameter;
  }

  /**
   * Checks that the specified parameter map is non-null and not empty.
   * <p>
   * Given the input parameter, this returns only if it is non-null and contains at least one mapping.
   * The element is not validated and may contain nulls if the collection allows nulls.
   * For example, in a constructor:
   * <pre>
   *  this.keyValues = ArgChecker.notEmpty(keyValues, "keyValues");
   * </pre>
   * 
   * @param <K>  the key type of the input map key, reflected in the result
   * @param <V>  the value type of the input map value, reflected in the result
   * @param <M>  the type of the input map, reflected in the result
   * @param parameter  the parameter to check, null or empty throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  public static <K, V, M extends Map<K, V>> M notEmpty(M parameter, String name) {
    notNull(parameter, name);
    if (parameter.isEmpty()) {
      throw new IllegalArgumentException("Input parameter map '" + name + "' must not be empty");
    }
    return parameter;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified parameter array is non-null and contains no nulls.
   * <p>
   * Given the input parameter, this returns only if it is non-null and contains no nulls.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.noNulls(values, "values");
   * </pre>
   * 
   * @param <T>  the type of the input array reflected in the result
   * @param parameter  the parameter to check, null or contains null throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or contains nulls
   */
  public static <T> T[] noNulls(T[] parameter, String name) {
    notNull(parameter, name);
    for (int i = 0; i < parameter.length; i++) {
      if (parameter[i] == null) {
        throw new IllegalArgumentException("Input parameter array '" + name + "' must not contain null at index " + i);
      }
    }
    return parameter;
  }

  /**
   * Checks that the specified parameter collection is non-null and contains no nulls.
   * <p>
   * Given the input parameter, this returns only if it is non-null and contains no nulls.
   * For example, in a constructor:
   * <pre>
   *  this.values = ArgChecker.noNulls(values, "values");
   * </pre>
   * 
   * @param <T>  the element type of the input iterable reflected in the result
   * @param <I>  the type of the input iterable, reflected in the result
   * @param parameter  the parameter to check, null or contains null throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or contains nulls
   */
  public static <T, I extends Iterable<T>> I noNulls(I parameter, String name) {
    notNull(parameter, name);
    for (Object obj : parameter) {
      if (obj == null) {
        throw new IllegalArgumentException("Input parameter iterable '" + name + "' must not contain null");
      }
    }
    return parameter;
  }

  /**
   * Checks that the specified parameter map is non-null and contains no nulls.
   * <p>
   * Given the input parameter, this returns only if it is non-null and contains no nulls.
   * For example, in a constructor:
   * <pre>
   *  this.keyValues = ArgChecker.noNulls(keyValues, "keyValues");
   * </pre>
   * 
   * @param <K>  the key type of the input map key, reflected in the result
   * @param <V>  the value type of the input map value, reflected in the result
   * @param <M>  the type of the input map, reflected in the result
   * @param parameter  the parameter to check, null or contains null throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or contains nulls
   */
  public static <K, V, M extends Map<K, V>> M noNulls(M parameter, String name) {
    notNull(parameter, name);
    for (Entry<K, V> entry : parameter.entrySet()) {
      if (entry.getKey() == null) {
        throw new IllegalArgumentException("Input parameter map '" + name + "' must not contain a null key");
      }
      if (entry.getValue() == null) {
        throw new IllegalArgumentException("Input parameter map '" + name + "' must not contain a null value");
      }
    }
    return parameter;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is not negative.
   * <p>
   * Given the input parameter, this returns only if it is zero or greater.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegative(amount, "amount");
   * </pre>
   * 
   * @param parameter  the parameter to check
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}
   * @throws IllegalArgumentException if the input is negative
   */
  public static int notNegative(int parameter, String name) {
    if (parameter < 0) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must not be negative");
    }
    return parameter;
  }

  /**
   * Checks that the argument is not negative.
   * <p>
   * Given the input parameter, this returns only if it is zero or greater.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegative(amount, "amount");
   * </pre>
   * 
   * @param parameter  the parameter to check
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}
   * @throws IllegalArgumentException if the input is negative
   */
  public static long notNegative(long parameter, String name) {
    if (parameter < 0) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must not be negative");
    }
    return parameter;
  }

  /**
   * Checks that the argument is not negative.
   * <p>
   * Given the input parameter, this returns only if it is zero or greater.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegative(amount, "amount");
   * </pre>
   * 
   * @param parameter  the parameter to check
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}
   * @throws IllegalArgumentException if the input is negative
   */
  public static double notNegative(double parameter, String name) {
    if (parameter < 0) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must not be negative");
    }
    return parameter;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the argument is not negative or zero.
   * <p>
   * Given the input parameter, this returns only if it is greater than zero.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegativeOrZero(amount, "amount");
   * </pre>
   * 
   * @param parameter  the parameter to check
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}
   * @throws IllegalArgumentException if the input is negative or zero
   */
  public static int notNegativeOrZero(int parameter, String name) {
    if (parameter <= 0) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must not be negative or zero");
    }
    return parameter;
  }

  /**
   * Checks that the argument is not negative or zero.
   * <p>
   * Given the input parameter, this returns only if it is greater than zero.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegativeOrZero(amount, "amount");
   * </pre>
   * 
   * @param parameter  the parameter to check
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}
   * @throws IllegalArgumentException if the input is negative or zero
   */
  public static long notNegativeOrZero(long parameter, String name) {
    if (parameter <= 0) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must not be negative or zero");
    }
    return parameter;
  }

  /**
   * Checks that the argument is not negative or zero.
   * <p>
   * Given the input parameter, this returns only if it is greater than zero.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegativeOrZero(amount, "amount");
   * </pre>
   * 
   * @param parameter  the parameter to check
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}
   * @throws IllegalArgumentException if the input is negative or zero
   */
  public static double notNegativeOrZero(double parameter, String name) {
    if (parameter <= 0) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must not be negative or zero");
    }
    return parameter;
  }

  /**
   * Checks that the argument is greater than zero to within a given accuracy.
   * <p>
   * Given the input parameter, this returns only if it is greater than zero
   * using the {@code eps} accuracy for zero.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notNegativeOrZero(amount, 0.0001d, "amount");
   * </pre>
   * 
   * @param parameter  the value to check
   * @param tolerance  the tolerance to use for zero
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}
   * @throws IllegalArgumentException if the absolute value of the argument is less than eps
   */
  public static double notNegativeOrZero(double parameter, double tolerance, String name) {
    if (CompareUtils.closeEquals(parameter, 0, tolerance)) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must not be zero");
    }
    if (parameter < 0) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must be greater than zero");
    }
    return parameter;
  }

  //-------------------------------------------------------------------------

  /**
   * Checks that the argument is not equal to zero to within a given accuracy.
   * <p>
   * Given the input parameter, this returns only if it is not zero comparing
   * using the {@code eps} accuracy.
   * For example, in a constructor:
   * <pre>
   *  this.amount = ArgChecker.notZero(amount, 0.0001d, "amount");
   * </pre>
   *
   * @param parameter  the value to check
   * @param tolerance  the tolerance to use for zero
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}
   * @throws IllegalArgumentException if the absolute value of the argument is less than eps
   */
  public static double notZero(double parameter, double tolerance, String name) {
    if (CompareUtils.closeEquals(parameter, 0d, tolerance)) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must not be zero");
    }
    return parameter;
  }
  //-------------------------------------------------------------------------

  /**
   * Checks a collection for null elements.
   * <p>
   * Given a collection, this returns true if any element is null.
   *
   * @param iterable  the collection to test, null throws an exception
   * @return true if the collection contains a null element
   * @throws IllegalArgumentException if the collection is null
   */
  public static boolean hasNullElement(Iterable<?> iterable) {
    notNull(iterable, "iterable");
    for (Object o : iterable) {
      if (o == null) {
        return true;
      }
    }
    return false;
  }
  /**
   * Checks a collection of doubles for negative elements.
   * <p>
   * Given a collection, this returns true if any element is negative.
   *
   * @param iterable  the collection to test, null or contains null throws an exception
   * @return true if the collection contains a negative element
   * @throws IllegalArgumentException if the collection is null or any element is null
   */
  public static boolean hasNegativeElement(Iterable<Double> iterable) {
    notNull(iterable, "collection");
    for (Double d : iterable) {
      notNull(d, "collection element");
      if (d < 0) {
        return true;
      }
    }
    return false;
  }

  //-------------------------------------------------------------------------

  /**
   * Checks that a value is within the range low &lt; x &lt; high.
   * <p>
   * Given a value, this returns true if it is within the specified range
   * excluding the boundaries.
   *
   * @param low  the low value of the range
   * @param high  the high value of the range
   * @param value  the value
   * @return true if low &lt; x &lt; high
   */
  public static boolean isInRangeExclusive(double low, double high, double value) {
    return (value > low && value < high);
  }
  /**
   * Checks that a value is within the range low &lt;= x &lt;= high.
   * <p>
   * Given a value, this returns true if it is within the specified range
   * including both boundaries.
   *
   * @param low  the low value of the range
   * @param high  the high value of the range
   * @param value  the value
   * @return true if low &lt;= x &lt;= high
   */
  public static boolean isInRangeInclusive(double low, double high, double value) {
    return (value >= low && value <= high);
  }

  /**
   * Checks that a value is within the range low &lt; x &lt;= high.
   * <p>
   * Given a value, this returns true if it is within the specified range
   * excluding the lower boundary but including the upper boundary.
   *
   * @param low  the low value of the range
   * @param high  the high value of the range
   * @param value  the value
   * @return true if low &lt; x &lt;= high
   */

  public static boolean isInRangeExcludingLow(double low, double high, double value) {
    return (value > low && value <= high);
  }

  /**
   * Checks that a value is within the range low &lt;= x &lt; high.
   * <p>
   * Given a value, this returns true if it is within the specified range
   * excluding the upper boundary but including the lower boundary.
   *
   * @param low  the low value of the range
   * @param high  the high value of the range
   * @param value  the value
   * @return true if low &lt;= x &lt; high
   */
  public static boolean isInRangeExcludingHigh(double low, double high, double value) {
    return (value >= low && value < high);
  }

  //-------------------------------------------------------------------------

  /**
   * Checks that the two values are in order or equal.
   * <p>
   * Given two comparable instances, this checks that the first is "lower than"
   * or "equal to" the second.
   *
   * @param <T>  the type
   * @param obj1  the first object, null throws an exception
   * @param obj2  the second object, null throws an exception
   * @param param1  the first parameter name, not null
   * @param param2  the second parameter name, not null
   * @throws IllegalArgumentException if either input is null or they are not in order
   */
  public static <T> void inOrderOrEqual(Comparable<? super T> obj1, T obj2, String param1, String param2) {
    notNull(obj1, param1);
    notNull(obj2, param2);
    if (obj1.compareTo(obj2) > 0) {
      throw new IllegalArgumentException("Input parameter '" + param1 + "' must be before '" + param2 + "'");
    }
  }

  //-------------------------------------------------------------------------
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
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message.
   * No attempt is made to format the arguments.
   * 
   * @param messageTemplate  the message template with "{}" placeholders, not null
   * @param arg  the message arguments, not null
   * @return the formatted message
   */
  public static String formatMessage(String messageTemplate, Object... args) {
    // this could be located in its own class, such as MessageUtils

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

}
