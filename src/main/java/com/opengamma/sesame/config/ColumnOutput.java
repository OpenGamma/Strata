/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import com.opengamma.util.ArgumentChecker;

/**
 * TODO an interface over the top of this and FunctionConfig
 */
public final class ColumnOutput {

  private final String _outputName;

  private final FunctionConfig _functionConfig;

  public ColumnOutput(String outputName) {
    this(outputName, FunctionConfig.EMPTY);
  }

  public ColumnOutput(String outputName, FunctionConfig functionConfig) {
    ArgumentChecker.notNull(functionConfig, "functionConfig");
    _outputName = outputName;
    _functionConfig = functionConfig;
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
}
