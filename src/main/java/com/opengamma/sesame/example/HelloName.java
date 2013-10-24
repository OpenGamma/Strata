/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.config.Target;

/**
 *
 */
public class HelloName implements HelloWorldFunction {

  private final NameFunction _nameFunction;

  public HelloName(NameFunction nameFunction) {
    _nameFunction = nameFunction;
  }

  @Override
  public String getGreeting(@Target EquitySecurity security) {
    return "Hello " + _nameFunction.getName();
  }
}
