/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.util.Map;

import org.joda.beans.ImmutableConstructor;

import com.google.common.collect.ImmutableMap;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public final class ViewColumn {

  /** Column name. */
  private final String _name;

  /** Default output details for input types that don't specify any. */
  private final ViewOutput _defaultOutput;

  /** Requirements keyed by target type. */
  private final Map<Class<?>, ViewOutput> _outputs;

  @ImmutableConstructor
  public ViewColumn(String name, ViewOutput defaultOutput, Map<Class<?>, ViewOutput> outputs) {
    _name = ArgumentChecker.notEmpty(name, "name");
    _outputs = ImmutableMap.copyOf(ArgumentChecker.notNull(outputs, "outputs"));
    _defaultOutput = defaultOutput;
  }

  public String getOutputName(Class<?> inputType) {
    ViewOutput viewOutput = _outputs.get(inputType);
    if (viewOutput != null && viewOutput.getOutputName() != null) {
      return viewOutput.getOutputName();
    } else if (_defaultOutput != null) {
      return _defaultOutput.getOutputName();
    } else {
      return null;
    }
  }

  public FunctionConfig getFunctionConfig(Class<?> inputType) {
    ViewOutput viewOutput = _outputs.get(inputType);
    if (viewOutput == null && _defaultOutput == null) {
      return FunctionConfig.EMPTY;
    }
    if (viewOutput == null) {
      return _defaultOutput.getFunctionConfig();
    }
    if (_defaultOutput == null) {
      return viewOutput.getFunctionConfig();
    }
    return new CompositeFunctionConfig(viewOutput.getFunctionConfig(), _defaultOutput.getFunctionConfig());
  }

  public String getName() {
    return _name;
  }

  @Override
  public String toString() {
    return "ViewColumn [_name='" + _name + "', _defaultOutput=" + _defaultOutput + ", _outputs=" + _outputs + "]";
  }
}
