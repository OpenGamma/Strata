/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Maps;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.financial.security.cashflow.CashFlowSecurity;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.example.CashFlowIdDescription;
import com.opengamma.sesame.example.EquityDescriptionFunction;
import com.opengamma.sesame.example.IdScheme;
import com.opengamma.sesame.example.OutputNames;

/**
 * Fluent API / mini DSL for building instances of {@link ViewDef} and related classes in code. See the
 * {@link #main} method for an example.
 */
public final class ConfigBuilder {

  protected static final Map<Class<?>,FunctionArguments> EMPTY_ARGUMENTS = Collections.emptyMap();
  protected static final Map<Class<?>,Class<?>> EMPTY_OVERRIDES = Collections.emptyMap();

  private ConfigBuilder() {
  }

  public static void main(String[] args) {
    ViewDef viewDef =
        viewDef("name",
                column("Description",
                       output(OutputNames.DESCRIPTION, EquitySecurity.class),
                       output(OutputNames.DESCRIPTION, CashFlowSecurity.class)),
                column("Bloomberg Ticker",
                       output(OutputNames.DESCRIPTION, EquitySecurity.class,
                              config(
                                  overrides(EquityDescriptionFunction.class, CashFlowIdDescription.class))),
                       output(OutputNames.DESCRIPTION, CashFlowSecurity.class,
                              config(
                                  overrides(EquityDescriptionFunction.class, CashFlowIdDescription.class)))),
                column("ACTIV Symbol",
                       output(OutputNames.DESCRIPTION, EquitySecurity.class,
                              config(
                                  overrides(EquityDescriptionFunction.class, CashFlowIdDescription.class),
                                  arguments(
                                      function(IdScheme.class,
                                               argument("scheme", ExternalSchemes.ACTIVFEED_TICKER))))),
                       output(OutputNames.DESCRIPTION, CashFlowSecurity.class,
                              config(
                                  overrides(EquityDescriptionFunction.class, CashFlowIdDescription.class),
                                  arguments(
                                      function(IdScheme.class,
                                               argument("scheme", ExternalSchemes.ACTIVFEED_TICKER)))))));
    System.out.println(viewDef);
  }

  public static ViewDef viewDef(String name, ViewColumn... columns) {
    return new ViewDef(name, Arrays.asList(columns));
  }

  public static ViewColumn column(String name, ColumnOutput... requirements) {
    return new ViewColumn(name, Arrays.asList(requirements));
  }

  public static ViewColumn column(String name, String outputName) {
    //return new ViewColumn(name, Arrays.asList(requirements));
    throw new UnsupportedOperationException();
  }

  public static ColumnOutput output(String outputName, Class<?> targetType) {
    return new ColumnOutput(outputName, targetType);
  }

  // TODO version that just takes config, the target type should be implicit
  public static ColumnOutput output(String outputName, Class<?> targetType, FunctionConfig config) {
    return new ColumnOutput(outputName, targetType, config);
  }

  public static FunctionConfig config(Overrides overrides, Arguments arguments) {
    return new FunctionConfig(overrides._overrides, arguments._arguments);
  }

  public static FunctionConfig config(Overrides overrides) {
    return new FunctionConfig(overrides._overrides, EMPTY_ARGUMENTS);
  }

  public static FunctionConfig config(Arguments arguments) {
    return new FunctionConfig(EMPTY_OVERRIDES, arguments._arguments);

  }

  public static FunctionConfig config(Arguments arguments, Overrides overrides) {
    return new FunctionConfig(overrides._overrides, arguments._arguments);
  }

  public static Overrides overrides(Class<?>... overrides) {
    return new Overrides(overrides);
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

  public static class Overrides {

    private final Map<Class<?>, Class<?>> _overrides = Maps.newHashMap();

    private Overrides(Class<?>... overrides) {
      if ((overrides.length % 2) != 0) {
        throw new IllegalArgumentException("Overrides must be specified in pairs of interface implementation");
      }
      for (int i = 0; i < overrides.length / 2; i += 2) {
        _overrides.put(overrides[i], overrides[i + 1]);
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
      _args = new FunctionArguments(argVals);
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
}
