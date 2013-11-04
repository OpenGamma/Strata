/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

/**
 *
 */
public interface FunctionArguments {

  FunctionArguments EMPTY = new FunctionArguments() {
    @Override
    public Object getArgument(String parameterName) {
      return null;
    }
  };

  Object getArgument(String parameterName);
}
