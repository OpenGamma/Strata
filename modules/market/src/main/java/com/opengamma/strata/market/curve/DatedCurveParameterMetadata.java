/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.time.LocalDate;

/**
 * Curve parameter metadata that specifies a date.
 */
public interface DatedCurveParameterMetadata
    extends CurveParameterMetadata {

  /**
   * Gets the date of the curve node.
   * <p>
   * This is the date that the node on the curve is defined as.
   * There is not necessarily a direct relationship with a date from an underlying instrument.
   * It may be the effective date or the maturity date but equally it may not.
   * 
   * @return the date
   */
  public abstract LocalDate getDate();

}
