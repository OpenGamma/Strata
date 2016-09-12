/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import java.util.Collections;
import java.util.List;

import org.joda.beans.ImmutableBean;

/**
 * Information about a single parameter.
 * <p>
 * Implementations of this interface are used to store metadata about a parameter.
 * Parameters are an abstraction over curves, surfaces and other types of data.
 */
public interface ParameterMetadata
    extends ImmutableBean {

  /**
   * Gets an empty metadata instance.
   * <p>
   * This is used when the actual metadata is not known.
   * 
   * @return the empty instance
   */
  public static ParameterMetadata empty() {
    return EmptyParameterMetadata.empty();
  }

  /**
   * Gets a list of empty metadata instances.
   * <p>
   * This is used when there the actual metadata is not known.
   * 
   * @param size  the size of the resulting list
   * @return the empty instance
   */
  public static List<ParameterMetadata> listOfEmpty(int size) {
    return Collections.nCopies(size, EmptyParameterMetadata.empty());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the label that describes the parameter.
   * <p>
   * It is intended that the label is relatively short, however there is no formal restriction.
   * 
   * @return the label
   */
  public abstract String getLabel();

  /**
   * Returns an object used to identify the parameter.
   * <p>
   * A good choice of identifier is one that makes sense to the user and can easily be created as part of a
   * scenario definition. For example, many nodes types are naturally identified by a tenor.
   * <p>
   * The identifier must satisfy the following criteria:
   * <ul>
   *   <li>It must be non-null</li>
   *   <li>It should be unique within a single data set</li>
   *   <li>It should have a sensible implementation of {@code hashCode()} and {@code equals()}.</li>
   * </ul>
   * Otherwise the choice of identifier is free and the system makes no assumptions about it.
   *
   * @return an object used to uniquely identify the parameter within the data
   */
  public abstract Object getIdentifier();

}
