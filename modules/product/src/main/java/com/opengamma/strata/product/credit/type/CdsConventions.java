/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Standardized credit default swap conventions.
 */
public final class CdsConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<CdsConvention> ENUM_LOOKUP = ExtendedEnum.of(CdsConvention.class);

  /**
   * USD-dominated standardized credit default swap.
   */
  public static final CdsConvention USD_STANDARD = CdsConvention.of(StandardCdsConventions.USD_STANDARD.getName());

  /**
   * EUR-dominated standardized credit default swap.
   * <p>
   * The payment dates are calculated with 'EUTA'.
   */
  public static final CdsConvention EUR_STANDARD = CdsConvention.of(StandardCdsConventions.EUR_STANDARD.getName());

  /**
   * EUR-dominated standardized credit default swap.
   * <p>
   * The payment dates are calculated with 'EUTA' and 'GBLO'.
   */
  public static final CdsConvention EUR_GB_STANDARD = CdsConvention.of(StandardCdsConventions.EUR_GB_STANDARD.getName());

  /**
   * GBP-dominated standardized credit default swap.
   * <p>
   * The payment dates are calculated with 'GBLO'.
   */
  public static final CdsConvention GBP_STANDARD = CdsConvention.of(StandardCdsConventions.GBP_STANDARD.getName());

  /**
   * GBP-dominated standardized credit default swap.
   * <p>
   * The payment dates are calculated with 'GBLO' and 'USNY'.
   */
  public static final CdsConvention GBP_US_STANDARD = CdsConvention.of(StandardCdsConventions.GBP_US_STANDARD.getName());

  /**
   * JPY-dominated standardized credit default swap.
   * <p>
   * The payment dates are calculated with 'JPTO'.
   */
  public static final CdsConvention JPY_STANDARD = CdsConvention.of(StandardCdsConventions.JPY_STANDARD.getName());

  /**
   * JPY-dominated standardized credit default swap.
   * <p>
   * The payment dates are calculated with 'JPTO', 'USNY' and 'GBLO'.
   */
  public static final CdsConvention JPY_US_GB_STANDARD = CdsConvention.of(StandardCdsConventions.JPY_US_GB_STANDARD.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private CdsConventions() {
  }

}
