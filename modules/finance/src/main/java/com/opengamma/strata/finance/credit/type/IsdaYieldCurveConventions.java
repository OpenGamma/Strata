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

public final class IsdaYieldCurveConventions {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  static final ExtendedEnum<IsdaYieldCurveConvention> ENUM_LOOKUP = ExtendedEnum.of(IsdaYieldCurveConvention.class);

  public static final IsdaYieldCurveConvention NORTH_AMERICAN_USD =
      IsdaYieldCurveConvention.of(StandardIsdaYieldCurveConventions.NORTH_AMERICAN_USD.getName());

  /**
   * Restricted constructor.
   */
  private IsdaYieldCurveConventions() {}
}
