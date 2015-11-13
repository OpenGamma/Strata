/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market conventions used to bootstrap an ISDA yield curve
 */
public final class IsdaYieldCurveConventions {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<IsdaYieldCurveConvention> ENUM_LOOKUP = ExtendedEnum.of(IsdaYieldCurveConvention.class);

  /**
   * The 'USD-ISDA' curve.
   */
  public static final IsdaYieldCurveConvention USD_ISDA =
      IsdaYieldCurveConvention.of(StandardIsdaYieldCurveConventions.USD_ISDA.getName());
  /**
   * The 'EUR-ISDA' curve.
   */
  public static final IsdaYieldCurveConvention EUR_ISDA =
      IsdaYieldCurveConvention.of(StandardIsdaYieldCurveConventions.EUR_ISDA.getName());
  /**
   * The 'GBP-ISDA' curve.
   */
  public static final IsdaYieldCurveConvention GBP_ISDA =
      IsdaYieldCurveConvention.of(StandardIsdaYieldCurveConventions.GBP_ISDA.getName());
  /**
   * The 'CHF-ISDA' curve.
   */
  public static final IsdaYieldCurveConvention CHF_ISDA =
      IsdaYieldCurveConvention.of(StandardIsdaYieldCurveConventions.CHF_ISDA.getName());
  /**
   * The 'JPY-ISDA' curve.
   */
  public static final IsdaYieldCurveConvention JPY_ISDA =
      IsdaYieldCurveConvention.of(StandardIsdaYieldCurveConventions.JPY_ISDA.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private IsdaYieldCurveConventions() {
  }

}
