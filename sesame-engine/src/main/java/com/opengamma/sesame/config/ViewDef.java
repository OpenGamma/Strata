/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.opengamma.util.ArgumentChecker;

/**
 * Defines what columns and outputs are required in a view.
 * TODO this will need to be a Joda bean for serialization
 */
public final class ViewDef {

  private final String _name;
  private final FunctionModelConfig _defaultConfig;
  private final List<ViewColumn> _columns;
  private final List<NonPortfolioOutput> _nonPortfolioOutputs;

  /* package */ ViewDef(String name,
                        FunctionModelConfig defaultConfig,
                        List<ViewColumn> columns,
                        List<NonPortfolioOutput> nonPortfolioOutputs) {
    _defaultConfig = ArgumentChecker.notNull(defaultConfig, "defaultConfig");
    _name = ArgumentChecker.notEmpty(name, "name");
    _columns = ImmutableList.copyOf(ArgumentChecker.notNull(columns, "columns"));
    _nonPortfolioOutputs = ImmutableList.copyOf(ArgumentChecker.notNull(nonPortfolioOutputs, "nonPortfolioOutputs"));

    Set<String> nonPortfolioOutputNames = Sets.newHashSetWithExpectedSize(nonPortfolioOutputs.size());
    for (NonPortfolioOutput output : nonPortfolioOutputs) {
      if (!nonPortfolioOutputNames.add(output.getName())) {
        throw new IllegalArgumentException("Non-portfolio output names must be unique, '" + output.getName() + "' is repeated");
      }
    }
  }

  /* package */ ViewDef(String name, FunctionModelConfig defaultConfig, List<ViewColumn> columns) {
    this(name, defaultConfig, columns, Collections.<NonPortfolioOutput>emptyList());
  }

  public List<ViewColumn> getColumns() {
    return _columns;
  }

  public FunctionModelConfig getDefaultConfig() {
    return _defaultConfig;
  }

  public String getName() {
    return _name;
  }

  public List<NonPortfolioOutput> getNonPortfolioOutputs() {
    return _nonPortfolioOutputs;
  }
}
