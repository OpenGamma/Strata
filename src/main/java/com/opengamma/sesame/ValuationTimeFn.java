/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZonedDateTime;

/**
 * Function providing the injected valuation time.
 */
public interface ValuationTimeFn {

  /**
   * Gets the valuation time as a local date.
   * 
   * @return the valuation date, not null
   */
  LocalDate getDate();

  /**
   * Gets the valuation time as full zoned date-time.
   * 
   * @return the valuation time, not null
   */
  ZonedDateTime getTime();

}
