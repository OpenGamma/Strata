/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.analytics;

import java.time.LocalDate;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.basics.date.Tenor;

/**
 * Container with a calibrated yield curve, the raw data used to produce it
 * and the meta data about date -> year fraction conversions.
 */
public interface YieldCurve {

  YieldAndDiscountCurve getCalibratedCurve();

  double getDiscountFactor(LocalDate date);

  double getDiscountFactor(Tenor tenor);

  double getForwardRate(Tenor startTenor, Tenor endTenor);

  // shift methods ... can shift underlying data or built curve

  // access initial market data ?
}
