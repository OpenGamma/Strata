/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config.pricing;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A container for a function group and a set of constructor arguments used when building function instances.
 * <p>
 * The function group and arguments contained in this class are the unit of configuration
 * returned by a set of {@link PricingRules}.
 */
public final class ConfiguredFunctionGroup {

  /** The function group. */
  private final FunctionGroup<?> functionGroup;

  /**
   * The constructor arguments used when creating function instances.
   * The argument values are keyed by the name of the corresponding constructor parameter.
   */
  private final Map<String, Object> arguments;

  /**
   * Returns a configured function group containing the specified function group and arguments.
   *
   * @param functionGroup  the group definition
   * @param arguments  the map of arguments to pass in
   * @return a configured function group containing the specified function group and arguments
   */
  public static ConfiguredFunctionGroup of(FunctionGroup<?> functionGroup, Map<String, Object> arguments) {
    return new ConfiguredFunctionGroup(functionGroup, arguments);
  }

  /**
   * Returns a configured function group containing the specified function group and no arguments.
   *
   * @param functionGroup  the group definition
   * @return a configured function group containing the specified function group and no arguments
   */
  public static ConfiguredFunctionGroup of(FunctionGroup<?> functionGroup) {
    return new ConfiguredFunctionGroup(functionGroup, ImmutableMap.of());
  }

  private ConfiguredFunctionGroup(FunctionGroup<?> functionGroup, Map<String, Object> arguments) {
    this.functionGroup = ArgChecker.notNull(functionGroup, "functionGroup");
    this.arguments = ArgChecker.notNull(arguments, "arguments");
  }

  /**
   * Returns the function group.
   *
   * @return the function group.
   */
  public FunctionGroup<?> getFunctionGroup() {
    return functionGroup;
  }

  /**
   * Returns the constructor arguments used when creating function instances.
   * <p>
   * The argument values are keyed by the name of the corresponding constructor parameter.
   *
   * @return the constructor arguments used when creating function instances.
   */
  public Map<String, Object> getArguments() {
    return arguments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfiguredFunctionGroup that = (ConfiguredFunctionGroup) o;
    return Objects.equals(functionGroup, that.functionGroup) &&
        Objects.equals(arguments, that.arguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(functionGroup, arguments);
  }

  @Override
  public String toString() {
    return "ConfiguredFunctionGroup [functionGroup=" + functionGroup + ", arguments=" + arguments + "]";
  }
}
