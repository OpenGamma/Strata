/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.sesame.OutputName;
import com.opengamma.util.ArgumentChecker;

/**
 * Configuration object that defines a column in a view.
 * <p>
 * Contains the column name and the configuration.
 * A different {@code ViewOutput} configuration object may be supplied
 * for each target input type.
 */
public final class ViewColumn {

  /**
   * Column name.
   */
  private final String _name;
  /**
   * Default output details for input types that don't specify any.
   */
  private final ViewOutput _defaultOutput;
  /**
   * Requirements keyed by target type.
   */
  private final ImmutableMap<Class<?>, ViewOutput> _outputs;

  /**
   * Creates an instance.
   * 
   * @param columnName  the column name, not null
   * @param defaultOutput  the default values for outputs, may be null
   * @param outputs  the map of outputs by input type, no nulls, not null
   */
  public ViewColumn(String columnName, ViewOutput defaultOutput, Map<Class<?>, ViewOutput> outputs) {
    _name = ArgumentChecker.notEmpty(columnName, "columnName");
    _outputs = ImmutableMap.copyOf(ArgumentChecker.notNull(outputs, "outputs"));
    _defaultOutput = defaultOutput;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the column name.
   * 
   * @return the column name, not null
   */
  public String getName() {
    return _name;
  }

  /**
   * Gets the output name for an input type.
   * 
   * @param inputType  the input type, not null
   * @return the output name, null if not found
   */
  public OutputName getOutputName(Class<?> inputType) {
    ViewOutput viewOutput = _outputs.get(inputType);
    if (viewOutput != null && viewOutput.getOutputName() != null) {
      return viewOutput.getOutputName();
    } else if (_defaultOutput != null) {
      return _defaultOutput.getOutputName();
    } else {
      return null;
    }
  }

  /**
   * Gets the function configuration for an input type.
   * 
   * @param inputType  the input type, not null
   * @return the function configuration, null if not found
   */
  public FunctionModelConfig getFunctionConfig(Class<?> inputType) {
    ViewOutput viewOutput = _outputs.get(inputType);
    if (viewOutput == null && _defaultOutput == null) {
      return FunctionModelConfig.EMPTY;
    }
    if (viewOutput == null) {
      return _defaultOutput.getFunctionModelConfig();
    }
    if (_defaultOutput == null) {
      return viewOutput.getFunctionModelConfig();
    }
    return new CompositeFunctionModelConfig(viewOutput.getFunctionModelConfig(), _defaultOutput.getFunctionModelConfig());
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "ViewColumn [_name='" + _name + "', _defaultOutput=" + _defaultOutput + ", _outputs=" + _outputs + "]";
  }

}
