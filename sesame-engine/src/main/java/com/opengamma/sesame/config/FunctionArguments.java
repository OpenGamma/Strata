/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

/**
 * Configuration for the user-specified arguments of a functions in the function model.
 */
public interface FunctionArguments {
  // TODO sentinel value for NULL? or return null for null have have a sentinel value for MISSING?

  /**
   * Singleton instance of an empty argument list.
   */
  FunctionArguments EMPTY = new FunctionArguments() {
    @Override
    public Object getArgument(String parameterName) {
      return null;
    }
  };

  //-------------------------------------------------------------------------
  /**
   * Gets the argument for the parameter name.
   * 
   * @param parameterName  the parameter name, not null
   * @return the argument, null if not found
   */
  Object getArgument(String parameterName);

}
