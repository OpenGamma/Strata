/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import com.opengamma.sesame.OutputName;
import com.opengamma.util.ArgumentChecker;

/**
 * Configuration object that defines an output value in a view.
 * <p>
 * Contains the output name and the configuration.
 */
public final class ViewOutput {

  /**
   * The output name, null if inherited from the column.
   */
  private final OutputName _outputName;
  /**
   * The configuration.
   */
  private final FunctionModelConfig _functionModelConfig;

  /**
   * Creates an instance with empty configuration.
   * 
   * @param outputName  the output name, null to inherit from the column
   */
  public ViewOutput(OutputName outputName) {
    this(outputName, FunctionModelConfig.EMPTY);
  }

  /**
   * Creates an instance.
   * 
   * @param functionModelConfig  the configuration, not null
   */
  public ViewOutput(FunctionModelConfig functionModelConfig) {
    this(null, functionModelConfig);
  }

  /**
   * Creates an instance.
   * 
   * @param outputName  the output name, null to inherit from the column
   * @param functionModelConfig  the configuration, not null
   */
  public ViewOutput(OutputName outputName, FunctionModelConfig functionModelConfig) {
    _functionModelConfig = ArgumentChecker.notNull(functionModelConfig, "functionConfig");
    _outputName = outputName;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the output name.
   * <p>
   * This is null if inherited from the column.
   * 
   * @return the output name, null if inherited from the column
   */
  public OutputName getOutputName() {
    return _outputName;
  }

  /**
   * Gets the function configuration.
   * 
   * @return the configuration for this output, not null
   */
  public FunctionModelConfig getFunctionModelConfig() {
    return _functionModelConfig;
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "ViewOutput [" +
        "_outputName='" + _outputName + "'" +
        ", _functionConfig=" + _functionModelConfig +
        "]";
  }

}
