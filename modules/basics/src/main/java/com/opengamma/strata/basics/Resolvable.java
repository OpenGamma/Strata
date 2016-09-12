/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

/**
 * An object that can be resolved against reference data.
 * <p>
 * Interface marking those objects that can be resolved using {@link ReferenceData}.
 * Implementations of this interface will use {@linkplain ReferenceDataId identifiers}
 * to refer to key concepts, such as holiday calendars and securities.
 * <p>
 * When the {@code resolve(ReferenceData)} method is called, the identifiers are resolved.
 * The resolving process will take each identifier, look it up using the {@code ReferenceData},
 * and return a new "resolved" instance.
 * Typically the result is of a type that is optimized for pricing.
 * <p>
 * Resolved objects may be bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 * 
 * @param <T>  the type of the resolved result
 */
public interface Resolvable<T> {

  /**
   * Resolves this object using the specified reference data.
   * <p>
   * This converts the object implementing this interface to the equivalent resolved form.
   * All {@link ReferenceDataId} identifiers in this instance will be resolved.
   * The resolved form will typically be a type that is optimized for pricing.
   * <p>
   * Resolved objects may be bound to data that changes over time, such as holiday calendars.
   * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
   * Care must be taken when placing the resolved form in a cache or persistence layer.
   * 
   * @param refData  the reference data to use when resolving
   * @return the resolved instance
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   * @throws RuntimeException if unable to resolve due to an invalid definition
   */
  public abstract T resolve(ReferenceData refData);

}
