/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

/**
 *
 */
public interface DefaultImplementationProvider {

  // return the default (if there is one configured) or the only implementation
  Class<?> getDefaultImplementationType(Class<?> interfaceType);
}
