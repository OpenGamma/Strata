/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

/**
 * TODO better name
 */
public interface FunctionRepo {

  Class<?> getDefaultFunctionImplementation(String valueName, Class<?> targetType);

  // TODO should implName be a Class?
  Class<?> getFunctionImplementation(String valueName, Class<?> targetTypes, String implName);
}
