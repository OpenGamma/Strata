/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import com.opengamma.util.ArgumentChecker;

/**
 *
 * TODO this will need to be a Joda bean for serialization
 */
public class NonPortfolioOutput {

  private final String _name;
  private final ViewOutput _output;

  public NonPortfolioOutput(String name, ViewOutput output) {
    _name = ArgumentChecker.notEmpty(name, "name");
    _output = ArgumentChecker.notNull(output, "output");
  }

  public String getName() {
    return _name;
  }

  public ViewOutput getOutput() {
    return _output;
  }
}
