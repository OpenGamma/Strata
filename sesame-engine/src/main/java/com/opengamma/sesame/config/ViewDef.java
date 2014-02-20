/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.opengamma.util.ArgumentChecker;

/**
 * Configuration object that defines the columns and outputs in a view.
 * <p>
 * A view is defined in terms of {@link ViewColumn} objects, each of
 * which has a {@link ViewOutput} for each target input type.
 * Stand-alone outputs, not associated with input targets, can also be specified.
 */
public final class ViewDef {

  /**
   * The view name.
   */
  private final String _name;
  /**
   * The default configuration for the entire view.
   */
  private final FunctionModelConfig _defaultConfig;
  /**
   * The columns in the view.
   */
  private final ImmutableList<ViewColumn> _columns;
  /**
   * The list of outputs that stand-alone and are not connected to a column.
   */
  private final ImmutableList<NonPortfolioOutput> _nonPortfolioOutputs;

  /**
   * Creates an instance.
   * 
   * @param name  the view name, not null
   * @param defaultConfig  the default configuration, not null
   * @param columns  the list of columns, not null
   */
  public ViewDef(String name, FunctionModelConfig defaultConfig, List<ViewColumn> columns) {
    this(name, defaultConfig, columns, ImmutableList.<NonPortfolioOutput>of());
  }

  /**
   * Creates an instance.
   * 
   * @param name  the view name, not null
   * @param defaultConfig  the default configuration, not null
   * @param columns  the list of columns, not null
   * @param nonPortfolioOutputs  the list of stand-alone outputs, not null
   */
  public ViewDef(String name,
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

  //-------------------------------------------------------------------------
  /**
   * Gets the view name.
   * 
   * @return the view name, not null
   */
  public String getName() {
    return _name;
  }

  /**
   * Gets the default configuration.
   * 
   * @return the default configuration, not null
   */
  public FunctionModelConfig getDefaultConfig() {
    return _defaultConfig;
  }

  /**
   * Gets the list of columns in the view.
   * 
   * @return the list of columns, not null
   */
  public ImmutableList<ViewColumn> getColumns() {
    return _columns;
  }

  /**
   * Gets the list of stand-alone outputs, not null
   * 
   * @return the stand-alone outputs, not null
   */
  public ImmutableList<NonPortfolioOutput> getNonPortfolioOutputs() {
    return _nonPortfolioOutputs;
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "ViewDef [_name='" + _name + "', _defaultConfig=" + _defaultConfig +
        ", _columns=" + _columns + ", _nonPortfolioOutputs=" + _nonPortfolioOutputs + "]";
  }

}
