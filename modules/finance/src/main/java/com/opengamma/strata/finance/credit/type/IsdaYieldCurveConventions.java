/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.finance.credit.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market conventions used to bootstrap an ISDA yield curve
 */
public final class IsdaYieldCurveConventions {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  static final ExtendedEnum<IsdaYieldCurveConvention> ENUM_LOOKUP = ExtendedEnum.of(IsdaYieldCurveConvention.class);

  public static final IsdaYieldCurveConvention ISDA_USD =
      IsdaYieldCurveConvention.of(StandardIsdaYieldCurveConventions.ISDA_USD.getName());

  public static final IsdaYieldCurveConvention ISDA_EUR =
      IsdaYieldCurveConvention.of(StandardIsdaYieldCurveConventions.ISDA_EUR.getName());

  public static final IsdaYieldCurveConvention ISDA_GBP =
      IsdaYieldCurveConvention.of(StandardIsdaYieldCurveConventions.ISDA_GBP.getName());

  public static final IsdaYieldCurveConvention ISDA_CHF =
      IsdaYieldCurveConvention.of(StandardIsdaYieldCurveConventions.ISDA_CHF.getName());

  public static final IsdaYieldCurveConvention ISDA_JPY =
      IsdaYieldCurveConvention.of(StandardIsdaYieldCurveConventions.ISDA_JPY.getName());

  /**
   * Restricted constructor.
   */
  private IsdaYieldCurveConventions() {
  }
}
