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

}
