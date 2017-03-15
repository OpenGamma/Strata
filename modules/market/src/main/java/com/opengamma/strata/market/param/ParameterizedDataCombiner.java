/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Helper that can be used to combine two or more underlying instances of {@code ParameterizedData}.
 * <p>
 * This is used by implementations of {@link ParameterizedData} that are based on more
 * than one underlying {@code ParameterizedData} instance.
 * <p>
 * This helper should be created in the constructor of the combined instance.
 * In each of the five {@code ParameterizedData} methods of the combined instance,
 * this helper should be invoked. See {@code DiscountFxForwardRates} for sample usage.
 */
public final class ParameterizedDataCombiner {

  /**
   * The underlying instances.
   */
  private final ParameterizedData[] underlyings;
  /**
   * The lookup array.
   */
  private final int[] lookup;
  /**
   * The count of parameters.
   */
  private final int paramCount;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that can combine the specified underlying instances.
   * 
   * @param instances  the underlying instances to combine
   * @return the combiner
   */
  public static ParameterizedDataCombiner of(ParameterizedData... instances) {
    return new ParameterizedDataCombiner(instances);
  }

  /**
   * Obtains an instance that can combine the specified underlying instances.
   * 
   * @param instances  the underlying instances to combine
   * @return the combiner
   */
  public static ParameterizedDataCombiner of(List<? extends ParameterizedData> instances) {
    return new ParameterizedDataCombiner((ParameterizedData[]) instances.toArray(new ParameterizedData[0]));
  }

  //------------------------------------------------------------------------- 
  // creates an instance
  private ParameterizedDataCombiner(ParameterizedData[] underlyings) {
    ArgChecker.notEmpty(underlyings, "underlyings");
    int size = underlyings.length;
    this.underlyings = underlyings;
    int[] lookup = new int[size];
    for (int i = 1; i < size; i++) {
      lookup[i] = lookup[i - 1] + underlyings[i - 1].getParameterCount();
    }
    this.lookup = lookup;
    this.paramCount = lookup[size - 1] + underlyings[size - 1].getParameterCount();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the number of parameters.
   * <p>
   * This returns the total parameter count of all the instances.
   * 
   * @return the number of parameters
   */
  public int getParameterCount() {
    return paramCount;
  }

  /**
   * Gets the value of the parameter at the specified index.
   * <p>
   * This gets the parameter from the correct instance.
   * 
   * @param parameterIndex  the zero-based index of the parameter to get
   * @return the value of the parameter
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public double getParameter(int parameterIndex) {
    int underlyingIndex = findUnderlyingIndex(parameterIndex);
    int adjustment = lookup[underlyingIndex];
    return underlyings[underlyingIndex].getParameter(parameterIndex - adjustment);
  }

  /**
   * Gets the metadata of the parameter at the specified index.
   * <p>
   * This gets the parameter metadata from the correct instance.
   * 
   * @param parameterIndex  the zero-based index of the parameter to get
   * @return the metadata of the parameter
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    int underlyingIndex = findUnderlyingIndex(parameterIndex);
    int adjustment = lookup[underlyingIndex];
    return underlyings[underlyingIndex].getParameterMetadata(parameterIndex - adjustment);
  }

  //-------------------------------------------------------------------------
  /**
   * Updates a parameter on the specified underlying.
   * <p>
   * This should be invoked once for each of the underlying instances.
   * It is intended to be used to pass the result of each invocation to the
   * constructor of the combined instance.
   * <p>
   * If the parameter index applies to the underlying, it is updated.
   * If the parameter index does not apply to the underlying, no error occurs.
   * 
   * @param <R>  the type of the underlying
   * @param underlyingIndex  the index of the underlying instance
   * @param underlyingType  the type of the parameterized data at the specified index
   * @param parameterIndex  the zero-based index of the parameter to change
   * @param newValue  the new value for the specified parameter
   * @return a parameterized data instance based on this with the specified parameter altered
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public <R extends ParameterizedData> R underlyingWithParameter(
      int underlyingIndex,
      Class<R> underlyingType,
      int parameterIndex,
      double newValue) {

    ParameterizedData perturbed = underlyings[underlyingIndex];
    if (findUnderlyingIndex(parameterIndex) == underlyingIndex) {
      int adjustment = lookup[underlyingIndex];
      perturbed = perturbed.withParameter(parameterIndex - adjustment, newValue);
    }
    return underlyingType.cast(perturbed);
  }

  /**
   * Applies a perturbation to the specified underlying.
   * <p>
   * This should be invoked once for each of the underlying instances.
   * It is intended to be used to pass the result of each invocation to the
   * constructor of the combined instance.
   * 
   * @param <R>  the type of the underlying
   * @param underlyingIndex  the index of the underlying instance
   * @param underlyingType  the type of the parameterized data at the specified index
   * @param perturbation  the perturbation to apply
   * @return a parameterized data instance based on this with the specified perturbation applied
   */
  public <R extends ParameterizedData> R underlyingWithPerturbation(
      int underlyingIndex,
      Class<R> underlyingType,
      ParameterPerturbation perturbation) {

    ParameterizedData underlying = underlyings[underlyingIndex];
    // perturb using a derived perturbation that adjusts the index
    int adjustment = lookup[underlyingIndex];
    ParameterizedData perturbed = underlying.withPerturbation(
        (idx, value, meta) -> perturbation.perturbParameter(idx + adjustment, value, meta));
    return underlyingType.cast(perturbed);
  }

  //-------------------------------------------------------------------------
  /**
   * Updates a parameter on the specified list of underlying instances.
   * <p>
   * The correct underlying is identified and updated, with the list returned.
   * 
   * @param <R>  the type of the underlying
   * @param underlyingType  the type of the parameterized data at the specified index
   * @param parameterIndex  the zero-based index of the parameter to change
   * @param newValue  the new value for the specified parameter
   * @return a parameterized data instance based on this with the specified parameter altered
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public <R extends ParameterizedData> List<R> withParameter(
      Class<R> underlyingType,
      int parameterIndex,
      double newValue) {

    int underlyingIndex = findUnderlyingIndex(parameterIndex);
    ImmutableList.Builder<R> builder = ImmutableList.builder();
    for (int i = 0; i < underlyings.length; i++) {
      ParameterizedData underlying = underlyings[i];
      if (i == underlyingIndex) {
        int adjustment = lookup[underlyingIndex];
        ParameterizedData perturbed = underlying.withParameter(parameterIndex - adjustment, newValue);
        builder.add(underlyingType.cast(perturbed));
      } else {
        builder.add(underlyingType.cast(underlying));
      }
    }
    return builder.build();
  }

  /**
   * Applies a perturbation to each underlying.
   * <p>
   * The updated list of underlying instances is returned.
   * 
   * @param <R>  the type of the underlying
   * @param underlyingType  the type of the parameterized data at the specified index
   * @param perturbation  the perturbation to apply
   * @return a parameterized data instance based on this with the specified parameter altered
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public <R extends ParameterizedData> List<R> withPerturbation(
      Class<R> underlyingType,
      ParameterPerturbation perturbation) {

    ImmutableList.Builder<R> builder = ImmutableList.builder();
    for (int i = 0; i < underlyings.length; i++) {
      builder.add(underlyingWithPerturbation(i, underlyingType, perturbation));
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // convert parameter index to underlying index
  private int findUnderlyingIndex(int parameterIndex) {
    Preconditions.checkElementIndex(parameterIndex, paramCount);
    for (int i = 1; i < lookup.length; i++) {
      if (parameterIndex < lookup[i]) {
        return i - 1;
      }
    }
    return lookup.length - 1;
  }

}
