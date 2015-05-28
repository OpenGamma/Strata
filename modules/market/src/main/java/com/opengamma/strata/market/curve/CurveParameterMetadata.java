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

}
