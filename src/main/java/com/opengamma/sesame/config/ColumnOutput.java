/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import org.joda.beans.ImmutableConstructor;

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

  @ImmutableConstructor
  public ColumnOutput(String outputName, FunctionConfig functionConfig) {
    // TODO should this be nullable so you can specify the config for security type but inherit the output from the column?
    ArgumentChecker.notNull(outputName, "outputName");
    ArgumentChecker.notNull(functionConfig, "functionConfig");
    _outputName = outputName;
    _functionConfig = functionConfig;
  }

  public String getOutputName() {
    return _outputName;
  }

  public FunctionConfig getFunctionConfig() {
    return _functionConfig;
  }
}
