/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public interface AvailableImplementations {

  Class<?> getDefaultImplementation(Class<?> interfaceType);

  Map<Class<?>, Class<?>> getDefaultImplementations();

  // gives the available implementing types for function interfaces
  // these can be presented to the user when they're setting up the view and choosing implementation overrides
  Set<Class<?>> getImplementationTypes(Class<?> interfaceType);

  void register(Class<?>... types);
}
