/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.analytics;

import java.time.LocalDate;
import java.util.Map;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.basics.date.Tenor;

public interface CurveCalibrator {

  //---------------------------------
  // Simple curve builds using zero-coupon rates, so there is no conversion work to be done

  YieldCurve buildYieldCurve(Map<Tenor, Double> zeroCouponRates, LocalDate valuationDate);


  //---------------------------------
  // Curve builds using rates derived from instruments and market rates




  // build curves based on underlying curve e.g. build me a forward curve using this discounting curve


  // build multicurve bundles


  // build multicurve bundles using fixed multicurve bundles as additional input

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

  // Represents the instruments used to build a curve
  public enum InstrumentType {
    CASH, SWAP, FUTURE, FRA
  }

}
