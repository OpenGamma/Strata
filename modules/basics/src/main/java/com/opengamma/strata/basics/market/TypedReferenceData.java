/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.opengamma.strata.collect.Messages;

/**
 * Provides typed access to reference data, such as securities and time-series.
 * <p>
 * Reference data is looked up in {@link ReferenceData} using implementations of {@link ReferenceDataId}.
 * The result is an instance of this interface that allows multiple pieces of reference
 * data to be associated with the same identifier, based on the data type.
 * <p>
 * The standard implementation is {@link ImmutableTypedReferenceData}.
 */
public interface TypedReferenceData {

  /**
   * Obtains an instance from a map of values keyed by type.
   *
   * @param values  the reference data values
   * @return typed reference data containing the values
   */
  public static TypedReferenceData of(Map<Class<?>, ?> values) {
    return ImmutableTypedReferenceData.of(values);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this typed reference data contains a value for the specified type.
   *
   * @param type  the reference data type to find
   * @return true if a value is contained for the type
   */
  public default boolean containsValue(Class<?> type) {
    return findValue(type).isPresent();
  }

  /**
   * Gets the reference data value for the specified type.
   * <p>
   * The result will be a single piece of reference data of the specified type.
   *
   * @param <T>  the type of the reference data
   * @param type  the reference data type to find
   * @return the reference data value
   * @throws IllegalArgumentException if no value is found
   */
  public default <T> T getValue(Class<T> type) {
    return findValue(type)
        .orElseThrow(() -> new IllegalArgumentException(Messages.format(
            "No reference data found for type '{}'", type.getClass().getSimpleName())));
  }

  /**
   * Finds the reference data value for the specified type.
   * <p>
   * The result will be a single piece of reference data of the specified type.
   * An empty optional is returned if the type is not found.
   *
   * @param <T>  the type of the reference data
   * @param type  the reference data type to find
   * @return the reference data value, empty if not found
   */
  public abstract <T> Optional<T> findValue(Class<T> type);

  /**
   * Gets the available types.
   *
   * @return the available types
   */
  public abstract Set<Class<?>> types();

  //-------------------------------------------------------------------------
  /**
   * Combines this reference data with another.
   * <p>
   * The result combines both sets reference data.
   * If there is a conflict, an exception is thrown.
   * 
   * @param other  the other reference data
   * @return the combined reference data
   * @throws IllegalArgumentException if a conflict is found
   */
  public abstract TypedReferenceData combinedWith(TypedReferenceData other);

}
