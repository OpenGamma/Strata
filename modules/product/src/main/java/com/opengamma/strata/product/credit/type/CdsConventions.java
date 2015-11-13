/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

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
   * The 'USD-NorthAmerican' CDS convention.
   */
  public static final CdsConvention USD_NORTH_AMERICAN =
      CdsConvention.of(StandardCdsConventions.USD_NORTH_AMERICAN.getName());
  /**
   * The 'EUR-European' CDS convention.
   */
  public static final CdsConvention EUR_EUROPEAN =
      CdsConvention.of(StandardCdsConventions.EUR_EUROPEAN.getName());
  /**
   * The 'GBP-European' CDS convention.
   */
  public static final CdsConvention GBP_EUROPEAN =
      CdsConvention.of(StandardCdsConventions.GBP_EUROPEAN.getName());
  /**
   * The 'CHF-European' CDS convention.
   */
  public static final CdsConvention CHF_EUROPEAN =
      CdsConvention.of(StandardCdsConventions.CHF_EUROPEAN.getName());
  /**
   * The 'USD-European' CDS convention.
   */
  public static final CdsConvention USD_EUROPEAN =
      CdsConvention.of(StandardCdsConventions.USD_EUROPEAN.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private CdsConventions() {
  }

}
