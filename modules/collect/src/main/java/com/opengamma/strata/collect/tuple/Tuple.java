/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.tuple;

import java.util.List;

/**
 * Base interface for all tuple types.
 * <p>
 * An ordered list of elements of a fixed size, where each element can have a different type.
 * <p>
 * All implementations must be final, immutable and thread-safe.
 */
public interface Tuple {

  /**
   * Gets the number of elements held by this tuple.
   * <p>
   * Each tuple type has a fixed size, returned by this method.
   * For example, {@link Pair} returns 2.
   * 
   * @return the size of the tuple
   */
  public abstract int size();

  /**
   * Gets the elements from this tuple as a list.
   * <p>
   * The list contains each element in the tuple in order.
   * 
   * @return the elements as a list
   */
  public abstract List<Object> elements();

}
