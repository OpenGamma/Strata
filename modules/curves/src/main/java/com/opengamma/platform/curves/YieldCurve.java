/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.curves;

import java.time.LocalDate;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.basics.date.Tenor;

/**
 * Container with a calibrated yield curve, the raw data used to produce it
 * and the meta data about date -> year fraction conversions.
 */
public interface YieldCurve {

  /**
   * Get the analytics calibrated curve.
   *
   * @return the calibrated curve
   */
  YieldAndDiscountCurve getCalibratedCurve();

  /**
   * Gets the discount factor from the calibrated curve for a
   * particular date.
   *
   * @param date  the date to get discount factor for
   * @return the discount factor for the date
   */
  double getDiscountFactor(LocalDate date);

  /**
   * Gets the discount factor from the calibrated curve for a
   * particular tenor.
   *
   * @param tenor  the tenor to get discount factor for
   * @return the discount factor for the tenor
   */
  double getDiscountFactor(Tenor tenor);

  /**
   * Get the forward rate from the calibrated curve between two tenors.
   *
   * @param startTenor  the start tenor
   * @param endTenor  the end tenor
   * @return the forward rate
   */
  // TODO - validate if it makes any sense for start to be after end
  double getForwardRate(Tenor startTenor, Tenor endTenor);

  // TODO - shift methods ... can shift underlying data or built curve

  // TODO - access the market data which was used to construct the curve
}
