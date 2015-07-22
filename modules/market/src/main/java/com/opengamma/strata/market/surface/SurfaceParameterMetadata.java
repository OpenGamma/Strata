/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import java.util.Collections;
import java.util.List;

/**
 * Information about a parameter underlying a surface.
 * <p>
 * Implementations of this interface are used to store metadata about a surface parameter.
 * 
 * @see SurfaceMetadata
 */
public interface SurfaceParameterMetadata {

  /**
   * Gets an empty metadata instance.
   * <p>
   * This is used when there is no metadata for the parameter.
   * 
   * @return the empty instance
   */
  public static SurfaceParameterMetadata empty() {
    return EmptySurfaceParameterMetadata.empty();
  }

  /**
   * Gets a list of empty metadata instances.
   * <p>
   * This is used when there is no metadata for the surface.
   * 
   * @param size  the size of the resulting list
   * @return the empty instance
   */
  public static List<SurfaceParameterMetadata> listOfEmpty(int size) {
    return Collections.nCopies(size, EmptySurfaceParameterMetadata.empty());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the label that describes the parameter.
   * <p>
   * It is intended that the label is relatively short, perhaps 10 characters.
   * However, there is no formal restriction.
   * 
   * @return the description
   */
  public abstract String getLabel();

  /**
   * Returns an object used to identify the parameter so it can be referenced when creating scenarios.
   * <p>
   * A good choice of identifier is one that makes sense to the user and can easily be created as part of a
   * scenario definition. For example, many nodes types are naturally identified by a tenor.
   * <p>
   * The identifier must satisfy the following criteria:
   * <ul>
   *   <li>It must be non-null</li>
   *   <li>It should be unique within a single surface</li>
   *   <li>It should have a sensible implementation of {@code hashCode()} and {@code equals()}.</li>
   * </ul>
   * Otherwise the choice of identifier is free and the system makes no assumptions about it.
   *
   * @return an object used to uniquely identify the parameter within the surface so it can be referenced when
   *   creating scenarios
   */
  public abstract Object getIdentifier();

}
