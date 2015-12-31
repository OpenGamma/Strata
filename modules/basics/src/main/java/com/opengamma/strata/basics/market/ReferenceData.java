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
 * Provides access to reference data, such as securities and time-series.
 * <p>
 * Reference data is looked up using implementations of {@link ReferenceDataId}.
 * The result is an instance of {@link TypedReferenceData} that allows multiple pieces
 * of reference data to be associated with the same identifier, based on the data type.
 * <p>
 * The standard implementation is {@link ImmutableReferenceData}.
 */
public interface ReferenceData {

  /**
   * Obtains an instance from a map of typed reference data.
   * <p>
   * Each identifier in the map refers to an instance of {@link TypedReferenceData}.
   * This allows multiple values to be connected to the same identifier, accessed by type.
   *
   * @param values  the reference data values
   * @return reference data containing the values
   */
  public static ReferenceData of(Map<? extends ReferenceDataId, TypedReferenceData> values) {
    return ImmutableReferenceData.of(values);
  }

  /**
   * Obtains an instance containing no reference data.
   *
   * @return empty reference data
   */
  public static ReferenceData empty() {
    return ImmutableReferenceData.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this typed reference data contains a value for the specified identifier and type.
   *
   * @param identifier  the reference data identifier to find
   * @param type  the reference data type to find
   * @return true if a value is contained for the identifier and type
   */
  public default boolean containsValue(ReferenceDataId identifier, Class<?> type) {
    return findValue(identifier, type).isPresent();
  }

  /**
   * Gets the reference data value for the specified identifier and type.
   * <p>
   * The result will be a single piece of reference data of the specified type.
   *
   * @param <T>  the type of the reference data
   * @param identifier  the reference data identifier to find
   * @param type  the reference data type to find
   * @return the reference data value
   * @throws IllegalArgumentException if no value is found
   */
  public default <T> T getValue(ReferenceDataId identifier, Class<T> type) {
    return findValue(identifier, type)
        .orElseThrow(() -> new IllegalArgumentException(Messages.format(
            "No reference data found for identifier '{}' and type '{}'", identifier, type.getClass().getSimpleName())));
  }

  /**
   * Finds the reference data value for the specified identifier and type.
   * <p>
   * The result will be a single piece of reference data of the specified type.
   * An empty optional is returned if the type is not found.
   *
   * @param <T>  the type of the reference data
   * @param identifier  the reference data identifier to find
   * @param type  the reference data type to find
   * @return the reference data value, empty if not found
   */
  public default <T> Optional<T> findValue(ReferenceDataId identifier, Class<T> type) {
    return findTyped(identifier).flatMap(t -> t.findValue(type));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the typed reference data for the specified identifier.
   * <p>
   * The result will be an instance of {@link TypedReferenceData} that can be queried for the value.
   *
   * @param identifier  the reference data identifier to find
   * @return the typed reference data
   * @throws IllegalArgumentException if no value is found
   */
  public default TypedReferenceData getTyped(ReferenceDataId identifier) {
    return findTyped(identifier)
        .orElseThrow(() -> new IllegalArgumentException(Messages.format(
            "No reference data found for identifier '{}'", identifier)));
  }

  /**
   * Finds the typed reference data for the specified identifier.
   * <p>
   * The result will be a single piece of reference data of the specified type.
   * An empty optional is returned if the type is not found.
   *
   * @param identifier  the reference data identifier to find
   * @return the typed reference data, empty if not found
   */
  public abstract Optional<TypedReferenceData> findTyped(ReferenceDataId identifier);

  /**
   * Gets the available identifiers.
   *
   * @return the available identifiers
   */
  public abstract Set<ReferenceDataId> identifiers();

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
  public abstract ReferenceData combinedWith(ReferenceData other);

}
