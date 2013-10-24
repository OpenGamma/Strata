/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.sesame.config.UserParam;

/**
 *
 */
public class Name implements NameFunction {

  private final String _name;

  public Name(@UserParam(name = "Name", defaultValue = "sailor") String name) {
    _name = name;
  }

  @Override
  public String getName() {
    return _name;
  }
}
