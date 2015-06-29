/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Constants for standard CDS market conventions.
 * <p>
 * See ISDA CDS documentation for more details.
 */
public final class CdsConventions {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<CdsConvention> ENUM_LOOKUP = ExtendedEnum.of(CdsConvention.class);

  /**
   * The North American USD CDS convention.
   */
  public static final CdsConvention NORTH_AMERICAN_USD =
      CdsConvention.of(StandardCdsConventions.NORTH_AMERICAN_USD.getName());
  /**
   * The European EUR CDS convention.
   */
  public static final CdsConvention EUROPEAN_EUR =
      CdsConvention.of(StandardCdsConventions.EUROPEAN_EUR.getName());
  /**
   * The European GBP CDS convention.
   */
  public static final CdsConvention EUROPEAN_GBP =
      CdsConvention.of(StandardCdsConventions.EUROPEAN_GBP.getName());
  /**
   * The European CHF CDS convention.
   */
  public static final CdsConvention EUROPEAN_CHF =
      CdsConvention.of(StandardCdsConventions.EUROPEAN_CHF.getName());
  /**
   * The European USD CDS convention.
   */
  public static final CdsConvention EUROPEAN_USD =
      CdsConvention.of(StandardCdsConventions.EUROPEAN_USD.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private CdsConventions() {
  }

}
