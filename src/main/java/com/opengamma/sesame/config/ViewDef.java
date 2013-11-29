/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public final class ViewDef {

  // TODO non-column outputs

  private final String _name;
  private final FunctionConfig _defaultConfig;
  private final List<ViewColumn> _columns;

  /* package */ ViewDef(String name, FunctionConfig defaultConfig, List<ViewColumn> columns) {
    _defaultConfig = ArgumentChecker.notNull(defaultConfig, "defaultConfig");
    _name = ArgumentChecker.notEmpty(name, "name");
    _columns = ImmutableList.copyOf(ArgumentChecker.notNull(columns, "columns"));
  }

  public List<ViewColumn> getColumns() {
    return _columns;
  }

  public FunctionConfig getDefaultConfig() {
    return _defaultConfig;
  }

  public String getName() {
    return _name;
  }
}
