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
  private final ColumnOutput _defaultOutput;

  /** Requirements keyed by target type. */
  private final Map<Class<?>, ColumnOutput> _outputs;

  @ImmutableConstructor
  public ViewColumn(String name, ColumnOutput defaultOutput, Map<Class<?>, ColumnOutput> outputs) {
    ArgumentChecker.notEmpty(name, "name");
    ArgumentChecker.notNull(outputs, "outputs");
    _name = name;
    _defaultOutput = defaultOutput;
    _outputs = ImmutableMap.copyOf(outputs);
  }

  public String getOutputName(Class<?> inputType) {
    ColumnOutput columnOutput = _outputs.get(inputType);
    if (columnOutput != null) {
      return columnOutput.getOutputName();
    } else if (_defaultOutput != null) {
      return _defaultOutput.getOutputName();
    } else {
      return null;
    }
  }

  // TODO do I actually want getFunctionConfig(inputType) here? could merge with the defaults later
  public FunctionConfig getFunctionConfig(Class<?> inputType) {
    ColumnOutput columnOutput = _outputs.get(inputType);
    // TODO merge config so the override config can provide some data and the defaults can provide the rest
    if (columnOutput != null) {
      return columnOutput.getFunctionConfig();
    } else {
      if (_defaultOutput != null) {
        return _defaultOutput.getFunctionConfig();
      } else {
        return FunctionConfig.EMPTY;
      }
    }
  }

  public String getName() {
    return _name;
  }
}
