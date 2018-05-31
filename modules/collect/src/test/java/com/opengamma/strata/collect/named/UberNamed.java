/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

/**
 * Mock named object.
 */
public interface UberNamed extends Named {

  // for NamedTest
  public static UberNamed of(String name) {
    return CombinedExtendedEnum.of(UberNamed.class).lookup(name);
  }

}
