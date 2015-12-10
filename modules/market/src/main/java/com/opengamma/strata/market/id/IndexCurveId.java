/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.id;

import com.opengamma.strata.basics.index.Index;

/**
 * Market data ID identifying a forward curve for an index.
 */
public interface IndexCurveId extends CurveId {

  /**
   * Returns the index of the curve.
   *
   * @return the index of the curve
   */
  public abstract Index getIndex();
}
