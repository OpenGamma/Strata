/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

/**
 * An object that can be expanded for pricing.
 * <p>
 * Interface marking those objects that can be expanded for pricing.
 * <p>
 * The expanded form of an object has certain conversions performed ready for pricing.
 * In this form, the holiday calendar rules have been applied, with dates being adjusted to valid business days.
 * If the holiday calendar changes, then the expanded form may no longer be accurate.
 * Care must be taken when placing the expanded form in a cache or persistence layer.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 * 
 * @param <T>  the type of the expanded form
 */
public interface Expandable<T> {

  /**
   * Expands this Object.
   * <p>
   * This converts the object implementing this interface to the equivalent expanded form.
   * Conversion must not be lossy.
   * 
   * @return the expanded form of this object
   * @throws RuntimeException if unable to expand due to an invalid definition
   */
  public abstract T expand();

}
