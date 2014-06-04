/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph.convert;

import com.opengamma.sesame.function.ParameterType;

/**
 * Converts function constructor argument values between strings and objects.
 */
public interface ArgumentConverter {

  /**
   * Converter that doesn't convert anything.
   */
  public static final ArgumentConverter NO_OP = new ArgumentConverter() {

    /**
     * @param type ignored
     * @return false
     */
    @Override
    public boolean isConvertible(ParameterType type) {
      return false;
    }

    /**
     * @param parameterType ignored
     * @param object ignored
     * @return always throws an exception
     * @throws IllegalStateException always
     */
    @Override
    public String convertToString(ParameterType parameterType, Object object) {
      throw new IllegalStateException("Cannot convert any values");
    }

    /**
     * @param parameterType ignored
     * @param str ignored
     * @return always throws an exception
     * @throws IllegalStateException always
     */
    @Override
    public Object convertFromString(ParameterType parameterType, String str) {
      throw new IllegalStateException("Cannot convert any values");
    }
  };

  /**
   * Returns true if the type can be converted to and from a string
   *
   * @param type the type of the argument's parameter
   * @return true if the type can be converted to and from a string
   */
  boolean isConvertible(ParameterType type);

  /**
   * Converts an object to a string.
   *
   * @param parameterType the type of the parameter where the object is used
   * @param object the object
   * @return the object converted to a string
   */
  String convertToString(ParameterType parameterType, Object object);

  /**
   * Converts a string to an object.
   *
   * @param parameterType the type of the parameter where the object is used
   * @param str the string representation of the object
   * @return the object
   * @throws IllegalArgumentException if the string can't be converted to an object
   */
  Object convertFromString(ParameterType parameterType, String str);
}
