/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.engine.calculation.function.CalculationSingleFunction;

/**
 * Mutable builder for building instances of {@link FunctionConfig}.
 * 
 * @param <T>  the type of the calculation target
 */
public final class FunctionConfigBuilder<T extends CalculationTarget> {

  // TODO FunctionMetadata instead of function type - includes type and set of calculated measures
  /** The type of the function. */
  private final Class<? extends CalculationSingleFunction<T, ?>> functionType;

  /** The arguments to the function constructor. */
  private final Map<String, Object> arguments = new HashMap<>();

  // package-private constructor so it's visible from FunctionConfig.builder()
  FunctionConfigBuilder(Class<? extends CalculationSingleFunction<T, ?>> functionType) {
    this.functionType = ArgChecker.notNull(functionType, "functionType");
  }

  /**
   * Adds a constructor argument used when creating function instances.
   *
   * @param name  the name of the constructor parameter
   * @param value  the value of the constructor argument
   * @return this builder
   */
  public FunctionConfigBuilder<T> addArgument(String name, Object value) {
    ArgChecker.notEmpty(name, "name");
    ArgChecker.notNull(value, "value");
    arguments.put(name, value);
    return this;
  }

  /**
   * Adds constructor arguments used when creating function instances.
   *
   * @param arguments  a map of constructor parameter name to constructor argument value
   * @return this builder
   */
  public FunctionConfigBuilder<T> addArguments(Map<String, Object> arguments) {
    ArgChecker.notNull(arguments, "arguments");
    this.arguments.putAll(arguments);
    return this;
  }

  /**
   * Returns an instance of {@code FunctionConfig} built from the data in this builder.
   *
   * @return an instance of {@code FunctionConfig} built from the data in this builder
   */
  public FunctionConfig<T> build() {
    return new FunctionConfig<>(functionType, arguments);
  }
}
