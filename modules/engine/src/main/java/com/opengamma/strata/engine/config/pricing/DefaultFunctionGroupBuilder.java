/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config.pricing;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.engine.calculation.function.CalculationSingleFunction;
import com.opengamma.strata.engine.config.FunctionConfig;
import com.opengamma.strata.engine.config.Measure;

/**
 * A mutable builder for building instances of {@link DefaultFunctionGroup}.
 * 
 * @param <T>  the type of the calculation target
 */
public final class DefaultFunctionGroupBuilder<T extends CalculationTarget> {

  /** The type of target handled by the functions in the function group. */
  private final Class<T> targetType;

  /** The name of the function group. */
  private FunctionGroupName name;

  /** Configuration for the functions, keyed by the measure they calculate. */
  private final Map<Measure, FunctionConfig<T>> functionConfig = new HashMap<>();

  /**
   * The constructor arguments used when creating function instances.
   * The argument values are keyed by the name of the corresponding constructor parameter.
   */
  private final Map<String, Object> functionArguments = new HashMap<>();

  // package-private constructor used by DefaultFunctionGroup.builder()
  DefaultFunctionGroupBuilder(Class<T> targetType) {
    this.targetType = ArgChecker.notNull(targetType, "targetType");
  }

  /**
   * Sets the name of the function group.
   *
   * @param name  the name of the function group
   * @return this builder
   */
  public DefaultFunctionGroupBuilder<T> name(String name) {
    ArgChecker.notEmpty(name, "name");
    this.name = FunctionGroupName.of(name);
    return this;
  }

  /**
   * Sets the name of the function group.
   *
   * @param name  the name of the function group
   * @return this builder
   */
  public DefaultFunctionGroupBuilder<T> name(FunctionGroupName name) {
    ArgChecker.notNull(name, "name");
    this.name = name;
    return this;
  }

  /**
   * Adds a function to the function group.
   * <p>
   * Functions added using this method do not have any constructor parameters stored in their configuration.
   * If they require constructor parameters they can be supplied when the function is built.
   *
   * @param measure  the measure calculated by the function
   * @param functionType  the type of the function
   * @return this builder
   */
  public DefaultFunctionGroupBuilder<T> addFunction(
      Measure measure,
      Class<? extends CalculationSingleFunction<T, ?>> functionType) {

    ArgChecker.notNull(measure, "measure");
    ArgChecker.notNull(functionType, "functionType");
    functionConfig.put(measure, FunctionConfig.of(functionType));
    return this;
  }

  /**
   * Adds a function to the function group.
   * <p>
   * Functions added using this method can include constructor parameters in their configuration.
   * If they require additional parameters they can be supplied when the function is built.
   *
   * @param measure  the measure calculated by the function
   * @param functionConfig  the configuration of the function
   * @return this builder
   */
  public DefaultFunctionGroupBuilder<T> addFunction(Measure measure, FunctionConfig<T> functionConfig) {
    ArgChecker.notNull(measure, "measure");
    ArgChecker.notNull(functionConfig, "functionConfig");
    this.functionConfig.put(measure, functionConfig);
    return this;
  }

  /**
   * Returns a function group built from the data in this builder.
   *
   * @return a function group built from the data in this builder
   */
  public DefaultFunctionGroup<T> build() {
    return new DefaultFunctionGroup<>(name, targetType, functionConfig, functionArguments);
  }
}
