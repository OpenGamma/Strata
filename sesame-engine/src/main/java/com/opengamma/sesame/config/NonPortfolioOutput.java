/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import com.opengamma.util.ArgumentChecker;

/**
 * Configuration object that defines a stand-alone output value.
 * <p>
 * Contains the output name and the configuration.
 */
public class NonPortfolioOutput {

  /**
   * The stand-alone output name.
   */
  private final String _name;
  /**
   * The output configuration.
   */
  private final ViewOutput _output;

  /**
   * Creates an instance.
   * 
   * @param name  the stand-alone output name, not null
   * @param output  the output configuration, not null
   */
  public NonPortfolioOutput(String name, ViewOutput output) {
    _name = ArgumentChecker.notEmpty(name, "name");
    _output = ArgumentChecker.notNull(output, "output");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name.
   * 
   * @return the name, not null
   */
  public String getName() {
    return _name;
  }

  /**
   * Gets the output configuration.
   * 
   * @return the configuration, not null
   */
  public ViewOutput getOutput() {
    return _output;
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "NonPortfolioOutput [_name='" + _name + "', _output=" + _output + "]";
  }

}
