/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.util.Collections;
import java.util.List;

import org.joda.beans.ImmutableBean;

/**
 * Information about a parameter underlying a curve.
 * <p>
 * Implementations of this interface are used to store metadata about a curve parameter.
 * 
 * @see CurveMetadata
 */
public interface CurveParameterMetadata
    extends ImmutableBean {

  /**
   * Gets an empty metadata instance.
   * <p>
   * This is used when there is no metadata for the parameter.
   * 
   * @return the empty instance
   */
  public static CurveParameterMetadata empty() {
    return EmptyCurveParameterMetadata.empty();
  }

  /**
   * Gets a list of empty metadata instances.
   * <p>
   * This is used when there is no metadata for the curve.
   * 
   * @param size  the size of the resulting list
   * @return the empty instance
   */
  public static List<CurveParameterMetadata> listOfEmpty(int size) {
    return Collections.nCopies(size, EmptyCurveParameterMetadata.empty());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets a description of the parameter.
   * <p>
   * It is intended that the description is relatively short, perhaps 10 characters.
   * 
   * @return the description
   */
  public abstract String getDescription();

  /**
   * Returns an object used to identify the parameter so it can be referenced when creating scenarios.
   * <p>
   * A good choice of identifier is one that makes sense to the user and can easily be created as part of a
   * scenario definition. For example, many nodes types are naturally identified by a tenor.
   * <p>
   * The identifier must satisfy the following criteria:
   * <ul>
   *   <li>It must be non-null</li>
   *   <li>It should be unique within a single curve</li>
   *   <li>It should have a sensible implementation of {@code hashCode()} and {@code equals()}.</li>
   * </ul>
   * Otherwise the choice of identifier is free and the system makes no assumptions about it.
   *
   * @return an object used to uniquely identify the parameter within the curve so it can be referenced when
   *   creating scenarios
   */
  public abstract Object getIdentifier();
}
