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
 * Standard CDS market conventions
 * <p>
 * See cdsmodel.com for details
 */
public final class CdsConventions {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  static final ExtendedEnum<CdsConvention> ENUM_LOOKUP = ExtendedEnum.of(CdsConvention.class);

  public static final CdsConvention NORTH_AMERICAN_USD =
      CdsConvention.of(StandardCdsConventions.NORTH_AMERICAN_USD.getName());

  public static final CdsConvention EUROPEAN_GBP =
      CdsConvention.of(StandardCdsConventions.EUROPEAN_GBP.getName());

  public static final CdsConvention EUROPEAN_CHF =
      CdsConvention.of(StandardCdsConventions.EUROPEAN_CHF.getName());

  public static final CdsConvention EUROPEAN_USD =
      CdsConvention.of(StandardCdsConventions.EUROPEAN_USD.getName());

  /**
   * Restricted constructor.
   */
  private CdsConventions() {
  }
}
