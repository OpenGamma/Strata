/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array;

import java.util.stream.Stream;

/**
 * Base interface for all array types.
 * <p>
 * This provides an abstraction over data structures that represent an array accessed by index.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 * 
 * @param <T>  the type of the element, the boxed type if primitive
 */
public interface Array<T> {

  /**
   * Gets the size of the array.
   * <p>
   * This is the total number of elements in the array.
   * 
   * @return the size of the array
   */
  public abstract int size();

  /**
   * Checks if this array is empty.
   * 
   * @return true if empty
   */
  public default boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Gets the element at the specified index in this array.
   * <p>
   * The index must be valid, between zero and {@code size()}.
   * 
   * @param index  the zero-based index to retrieve
   * @return the element at the index
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public abstract T get(int index);

  /**
   * Returns a stream over the elements.
   * <p>
   * The stream returns an ordered view of the values in the array.
   *
   * @return a stream over the elements in the array
   */
  public abstract Stream<T> stream();

}
