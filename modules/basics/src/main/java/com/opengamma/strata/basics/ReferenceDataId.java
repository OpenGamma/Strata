/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

/**
 * An identifier for a unique item of reference data.
 * <p>
 * Reference data is obtained from an instance of {@link ReferenceData} using this identifier.
 *
 * @param <T>  the type of the reference data this identifier refers to
 */
public interface ReferenceDataId<T> {

  /**
   * Gets the type of data this identifier refers to.
   *
   * @return the type of the reference data this identifier refers to
   */
  public abstract Class<T> getReferenceDataType();

  /**
   * Low-level method to query the reference data value associated with this identifier,
   * returning null if not found.
   * <p>
   * This is a low-level method that obtains the reference data value, returning null instead of an error.
   * Applications should use {@link ReferenceData#getValue(ReferenceDataId)} in preference to this method.
   *
   * @param refData  the reference data to lookup the value in
   * @return the reference data value, null if not found
   */
  public default T queryValueOrNull(ReferenceData refData) {
    return refData.queryValueOrNull(this);
  }

}
