/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import java.time.LocalDate;

/**
 * Parameter metadata that specifies a date.
 */
public interface DatedParameterMetadata
    extends ParameterMetadata {

  /**
   * Gets the date associated with the parameter.
   * <p>
   * This is the date that is most closely associated with the parameter.
   * The actual parameter is typically a year fraction based on a day count.
   * 
   * @return the date
   */
  public abstract LocalDate getDate();

}
