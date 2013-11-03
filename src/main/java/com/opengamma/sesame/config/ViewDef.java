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
  // TODO default FunctionConfig for the whole view? would need to merge with column defaults and specific config

  private final String _name;

  private final List<ViewColumn> _columns;

  /* package */ ViewDef(String name, List<ViewColumn> columns) {
    ArgumentChecker.notEmpty(name, "name");
    ArgumentChecker.notNull(columns, "columns");
    _name = name;
    _columns = ImmutableList.copyOf(columns);
  }

  public List<ViewColumn> getColumns() {
    return _columns;
  }
}
