/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.calc.Measure;

/**
 * Abstract derived calculation function with fields for the target type, measure and required measures.
 * <p>
 * Empty requirements are returned from {@link #requirements}.
 * Subtypes only need to provide an implementation of the {@link #calculate} method.
 *
 * @param <T> the type of calculation target handled by the function
 * @param <R> the type of the measure calculated by the function
 */
public abstract class AbstractDerivedCalculationFunction<T extends CalculationTarget, R>
    implements DerivedCalculationFunction<T, R> {

  /** The target type handled by the function, often a trade. */
  private final Class<T> targetType;

  /** The measure calculated by the function. */
  private final Measure measure;

  /** The measures required as inputs to the calculation. */
  private final Set<Measure> requiredMeasures;

  /**
   * Creates a new function which calculates one measure for targets of one type.
   *
   * @param targetType  the target type handled by the function, often a trade
   * @param measure  the measure calculated by the function
   * @param requiredMeasures  the measures required as inputs to the calculation
   */
  protected AbstractDerivedCalculationFunction(
      Class<T> targetType,
      Measure measure,
      Measure... requiredMeasures) {

    this(targetType, measure, ImmutableSet.copyOf(requiredMeasures));
  }

  /**
   * Creates a new function which calculates one measure for targets of one type.
   *
   * @param targetType  the target type handled by the function, often a trade
   * @param measure  the measure calculated by the function
   * @param requiredMeasures  the measures required as inputs to the calculation
   */
  protected AbstractDerivedCalculationFunction(
      Class<T> targetType,
      Measure measure,
      Set<Measure> requiredMeasures) {

    this.measure = measure;
    this.requiredMeasures = ImmutableSet.copyOf(requiredMeasures);
    this.targetType = targetType;
  }

  @Override
  public Class<T> targetType() {
    return targetType;
  }

  @Override
  public Measure measure() {
    return measure;
  }

  @Override
  public Set<Measure> requiredMeasures() {
    return requiredMeasures;
  }

  @Override
  public FunctionRequirements requirements(T target, CalculationParameters parameters, ReferenceData refData) {
    return FunctionRequirements.empty();
  }

}
