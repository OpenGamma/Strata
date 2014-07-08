/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

import java.util.regex.Pattern;

/**
 * Contains utility methods for checking inputs to methods.
 */
final class ArgChecker {
  // this is a package-scoped copy of the main ArgChecker to avoid creating a dependency

  /**
   * Restricted constructor.
   */
  private ArgChecker() {
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified parameter is non-null.
   * <p>
   * Given the input parameter, this returns only if it is non-null.
   * 
   * @param <T>  the type of the input parameter reflected in the result
   * @param parameter  the parameter to check, null throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null
   */
  static <T> T notNull(T parameter, String name) {
    if (parameter == null) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must not be null");
    }
    return parameter;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified parameter is non-null and matches the specified pattern.
   * <p>
   * Given the input parameter, this returns only if it is non-null and matches
   * the regular expression pattern specified.
   * 
   * @param pattern  the pattern to check against, not null
   * @param parameter  the parameter to check, null throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or empty
   */
  static String matches(Pattern pattern, String parameter, String name) {
    notNull(pattern, "pattern");
    notNull(parameter, name);
    if (pattern.matcher(parameter).matches() == false) {
      throw new IllegalArgumentException("Input parameter '" + name + "' must match pattern: " + pattern);
    }
    return parameter;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks that the specified parameter array is non-null and contains no nulls.
   * <p>
   * Given the input parameter, this returns only if it is non-null and contains no nulls.
   * 
   * @param <T>  the type of the input array reflected in the result
   * @param parameter  the parameter to check, null or contains null throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or contains nulls
   */
  static <T> T[] noNulls(T[] parameter, String name) {
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
   * 
   * @param <T>  the element type of the input iterable reflected in the result
   * @param <I>  the type of the input iterable, reflected in the result
   * @param parameter  the parameter to check, null or contains null throws an exception
   * @param name  the name of the parameter to use in the error message, not null
   * @return the input {@code parameter}, not null
   * @throws IllegalArgumentException if the input is null or contains nulls
   */
  static <T, I extends Iterable<T>> I noNulls(I parameter, String name) {
    notNull(parameter, name);
    for (Object obj : parameter) {
      if (obj == null) {
        throw new IllegalArgumentException("Input parameter iterable '" + name + "' must not contain null");
      }
    }
    return parameter;
  }

}
