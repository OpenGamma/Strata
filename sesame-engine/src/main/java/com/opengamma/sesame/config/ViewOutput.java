/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import com.opengamma.util.ArgumentChecker;

/**
 * Defines an output value in a view.
 * Contains the output name and the
 */
public final class ViewOutput {

  private final String _outputName;

  private final FunctionConfig _functionConfig;

  public ViewOutput(String outputName) {
    this(outputName, FunctionConfig.EMPTY);
  }

  public ViewOutput(String outputName, FunctionConfig functionConfig) {
    _functionConfig = ArgumentChecker.notNull(functionConfig, "functionConfig");
    _outputName = outputName;
  }

  /**
   * Returns the output name.
   * This can be null if this output applies to a specific input type and the output name is specified for the column.
   * @return The output name, possibly null
   */
  public String getOutputName() {
    return _outputName;
  }

  /**
   * @return The configuration for this output, not null
   */
  public FunctionConfig getFunctionConfig() {
    return _functionConfig;
  }

  @Override
  public String toString() {
    return "ViewOutput [" +
        "_outputName='" + _outputName + "'" +
        ", _functionConfig=" + _functionConfig +
        "]";
  }
}
