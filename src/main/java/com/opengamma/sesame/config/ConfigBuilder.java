/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.financial.security.cashflow.CashFlowSecurity;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.example.CashFlowIdDescriptionFn;
import com.opengamma.sesame.example.DefaultIdSchemeFn;
import com.opengamma.sesame.example.EquityDescriptionFn;
import com.opengamma.sesame.example.OutputNames;

/**
 * Mini DSL for building instances of {@link ViewDef} and related classes in code. See the
 * {@link #main} method for an example.
 */
public final class ConfigBuilder {

  private static final Map<Class<?>, FunctionArguments> EMPTY_ARGUMENTS = Collections.emptyMap();
  private static final Map<Class<?>, Class<?>> EMPTY_OVERRIDES = Collections.emptyMap();

  private ConfigBuilder() {
  }

  public void main(String[] args) {
    ViewDef example1 =
        viewDef("columns only",
                column(OutputNames.DESCRIPTION),
                column(OutputNames.DESCRIPTION,
                       config(
                           implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class),
                           arguments(
                               function(DefaultIdSchemeFn.class,
                                        argument("scheme", ExternalSchemes.ACTIVFEED_TICKER))))),
                column("Bloomberg Ticker", OutputNames.DESCRIPTION,
                       output(EquitySecurity.class,
                              config(
                                  implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class))),
                       output(CashFlowSecurity.class,
                              config(
                                  implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class)))),
                column("ACTIV Symbol", OutputNames.DESCRIPTION,
                       output(EquitySecurity.class,
                              config(
                                  implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class),
                                  arguments(
                                      function(DefaultIdSchemeFn.class,
                                               argument("scheme", ExternalSchemes.ACTIVFEED_TICKER))))),
                       output(CashFlowSecurity.class,
                              config(
                                  implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class),
                                  arguments(
                                      function(DefaultIdSchemeFn.class,
                                               argument("scheme", ExternalSchemes.ACTIVFEED_TICKER)))))));
    System.out.println(example1);

    ViewDef example2 =
        viewDef("columns and other outputs",
                columns(
                    column(OutputNames.DESCRIPTION),
                    column(OutputNames.DESCRIPTION,
                           config(
                               implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class),
                               arguments(
                                   function(DefaultIdSchemeFn.class,
                                            argument("scheme", ExternalSchemes.ACTIVFEED_TICKER))))),
                    column("Bloomberg Ticker", OutputNames.DESCRIPTION,
                           output(EquitySecurity.class,
                                  config(
                                      implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class))),
                           output(CashFlowSecurity.class,
                                  config(
                                      implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class)))),
                    column("ACTIV Symbol", OutputNames.DESCRIPTION,
                           output(EquitySecurity.class,
                                  config(
                                      implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class),
                                      arguments(
                                          function(DefaultIdSchemeFn.class,
                                                   argument("scheme", ExternalSchemes.ACTIVFEED_TICKER))))),
                           output(CashFlowSecurity.class,
                                  config(
                                      implementations(EquityDescriptionFn.class, CashFlowIdDescriptionFn.class),
                                      arguments(
                                          function(DefaultIdSchemeFn.class,
                                                   argument("scheme", ExternalSchemes.ACTIVFEED_TICKER))))))),
                nonPortfolioOutputs(
                    nonPortfolioOutput("USD Discounting Curve",
                                       output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE))));
    System.out.println(example2);

    ViewDef example3 =
        viewDef("other outputs only",
                nonPortfolioOutput("USD Discounting Curve",
                                   output(OutputNames.DISCOUNTING_MULTICURVE_BUNDLE))); // TODO some function config
    System.out.println(example3);
  }

  // TODO alternative viewDef overloads
  //   * taking portfolio and non-portfolio outputs - probably need a wrapper class for each set of configs
  //   * taking only non-portfolio outputs
  // TODO NonPortfolioOutput class, name and ViewOutput fields

  public static ViewDef viewDef(String name, ViewColumn... columns) {
    return new ViewDef(name, FunctionConfig.EMPTY, Arrays.asList(columns));
  }

  public static ViewDef viewDef(String name, FunctionConfig defaultConfig, ViewColumn... columns) {
    return new ViewDef(name, defaultConfig, Arrays.asList(columns));
  }

  public static ViewDef viewDef(String name, NonPortfolioOutput... nonPortfolioOutputs) {
    return new ViewDef(name, FunctionConfig.EMPTY, Collections.<ViewColumn>emptyList(), Arrays.asList(nonPortfolioOutputs));
  }

  public static ViewDef viewDef(String name, Columns columns, OtherOutputs otherOutputs) {
    return new ViewDef(name, columns.getDefaultConfig(), columns.getViewColumns(), otherOutputs.getNonPortfolioOutputs());
  }

  public static ViewDef viewDef(String name, FunctionConfig defaultConfig, Columns columns, OtherOutputs otherOutputs) {
    return new ViewDef(name, defaultConfig, columns.getViewColumns(), otherOutputs.getNonPortfolioOutputs());
  }

  public static Columns columns(ViewColumn... columns) {
    return new Columns(FunctionConfig.EMPTY, columns);
  }

  public static OtherOutputs nonPortfolioOutputs(NonPortfolioOutput... nonPortfolioOutputs) {
    return new OtherOutputs(nonPortfolioOutputs);
  }

  private static Map<Class<?>, ViewOutput> createTargetOutputs(TargetOutput... outputs) {
    Map<Class<?>, ViewOutput> targetOutputs = Maps.newHashMap();
    for (TargetOutput output : outputs) {
      targetOutputs.put(output._inputType, output._output);
    }
    return targetOutputs;
  }

  public static ViewColumn column(String name) {
    return new ViewColumn(name, new ViewOutput(name), Collections.<Class<?>, ViewOutput>emptyMap());
  }

  public static ViewColumn column(String name, String outputName) {
    return new ViewColumn(name, new ViewOutput(outputName), Collections.<Class<?>, ViewOutput>emptyMap());
  }

  public static ViewColumn column(String name, FunctionConfig config) {
    return new ViewColumn(name, new ViewOutput(name, config), Collections.<Class<?>, ViewOutput>emptyMap());
  }

  public static ViewColumn column(String name, TargetOutput... outputs) {
    return new ViewColumn(name, new ViewOutput(name), createTargetOutputs(outputs));
  }

  public static ViewColumn column(String name, String outputName, TargetOutput... outputs) {
    return new ViewColumn(name, new ViewOutput(outputName), createTargetOutputs(outputs));
  }

  public static ViewColumn column(String name, String outputName, FunctionConfig config) {
    return new ViewColumn(name, new ViewOutput(outputName, config), Collections.<Class<?>, ViewOutput>emptyMap());
  }

  public static ViewColumn column(String name, FunctionConfig config, TargetOutput... targetOutputs) {
    return new ViewColumn(name, new ViewOutput(name, config), createTargetOutputs(targetOutputs));
  }

  public static ViewColumn column(String name, String outputName, FunctionConfig config, TargetOutput... targetOutputs) {
    return new ViewColumn(name, new ViewOutput(outputName, config), createTargetOutputs(targetOutputs));
  }

  public static TargetOutput output(String outputName, Class<?> targetType) {
    return new TargetOutput(new ViewOutput(outputName), targetType);
  }

  public static TargetOutput output(String outputName, Class<?> targetType, FunctionConfig config) {
    return new TargetOutput(new ViewOutput(outputName, config), targetType);
  }

  public static ViewOutput output(String outputName, FunctionConfig config) {
    return new ViewOutput(outputName, config);
  }

  public static ViewOutput output(String outputName) {
    return new ViewOutput(outputName, FunctionConfig.EMPTY);
  }

  public static NonPortfolioOutput nonPortfolioOutput(String name, ViewOutput output) {
    return new NonPortfolioOutput(name, output);
  }

  // TODO this is a bad name
  // TODO this needs to inherit the output name from the column. not sure that's going to be easy
  // maybe column output needs to allow a null output name
  public static TargetOutput output(Class<?> targetType, FunctionConfig config) {
    return new TargetOutput(new ViewOutput(null, config), targetType);
  }

  public static FunctionConfig config(Implementations implementations, Arguments arguments) {
    return new SimpleFunctionConfig(implementations._implementations, arguments._arguments);
  }

  public static FunctionConfig config() {
    return new SimpleFunctionConfig(EMPTY_OVERRIDES, EMPTY_ARGUMENTS);
  }


  public static FunctionConfig config(Implementations implementations) {
    return new SimpleFunctionConfig(implementations._implementations, EMPTY_ARGUMENTS);
  }

  public static FunctionConfig config(Arguments arguments) {
    return new SimpleFunctionConfig(EMPTY_OVERRIDES, arguments._arguments);

  }

  public static FunctionConfig config(Arguments arguments, Implementations implementations) {
    return new SimpleFunctionConfig(implementations._implementations, arguments._arguments);
  }

  // TODO this is a misnomer now, there are no default implementation so this doesn't define overrides. implementations?
  public static Implementations implementations(Class<?>... overrides) {
    return new Implementations(overrides);
  }

  public static FnArgs function(Class<?> functionType, Arg... args) {
    return new FnArgs(functionType, args);
  }

  public static Arguments arguments(FnArgs... args) {
    return new Arguments(args);
  }

  public static Arg argument(String name, Object value) {
    return new Arg(name, value);
  }

  public static class Implementations {

    private final Map<Class<?>, Class<?>> _implementations = Maps.newHashMap();

    private Implementations(Class<?>... implementations) {
      if ((implementations.length % 2) != 0) {
        throw new IllegalArgumentException("Overrides must be specified in pairs of interface implementation");
      }
      for (int i = 0; i < implementations.length; i += 2) {
        _implementations.put(implementations[i], implementations[i + 1]);
      }
    }
  }

  public static class Arguments {

    private final Map<Class<?>, FunctionArguments> _arguments = Maps.newHashMap();

    private Arguments(FnArgs... args) {
      for (FnArgs arg : args) {
        _arguments.put(arg._function, arg._args);
      }
    }
  }

  public static class FnArgs {

    private final Class<?> _function;
    private final FunctionArguments _args;

    private FnArgs(Class<?> function, Arg... args) {
      _function = function;
      Map<String, Object> argVals = Maps.newHashMap();
      for (Arg arg : args) {
        argVals.put(arg._name, arg._value);
      }
      _args = new SimpleFunctionArguments(argVals);
    }
  }

  public static class Arg {

    private final String _name;
    private final Object _value;

    private Arg(String name, Object value) {

      _name = name;
      _value = value;
    }
  }

  public static class TargetOutput {
    private final ViewOutput _output;
    private final Class<?> _inputType;

    public TargetOutput(ViewOutput output, Class<?> inputType) {
      _output = output;
      _inputType = inputType;
    }
  }

  private static class Columns {

    private final FunctionConfig _defaultConfig;
    private final List<ViewColumn> _viewColumns;

    private Columns(FunctionConfig defaultConfig, ViewColumn... viewColumns) {
      _defaultConfig = defaultConfig;
      _viewColumns = Arrays.asList(viewColumns);
    }

    public FunctionConfig getDefaultConfig() {
      return _defaultConfig;
    }

    public List<ViewColumn> getViewColumns() {
      return _viewColumns;
    }
  }
  
  private static class OtherOutputs {

    private final List<NonPortfolioOutput> _nonPortfolioOutputs;

    private OtherOutputs(NonPortfolioOutput... nonPortfolioOutputs) {
      _nonPortfolioOutputs = Arrays.asList(nonPortfolioOutputs);
    }

    public List<NonPortfolioOutput> getNonPortfolioOutputs() {
      return _nonPortfolioOutputs;
    }
  }
}
