/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard Ibor future conventions.
 */
public final class IborFutureConventions {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<IborFutureConvention> ENUM_LOOKUP = ExtendedEnum.of(IborFutureConvention.class);

  //-------------------------------------------------------------------------
  /**
   * The 'GBP-LIBOR-3M-Quarterly-IMM' convention.
   * <p>
   * The 'GBP-LIBOR-3M' index based on quarterly IMM dates.
   */
  public static final IborFutureConvention GBP_LIBOR_3M_QUARTERLY_IMM =
      IborFutureConvention.of(StandardIborFutureConventions.GBP_LIBOR_3M_QUARTERLY_IMM.getName());

  /**
   * The 'GBP-LIBOR-3M-Monthly-IMM' convention.
   * <p>
   * The 'GBP-LIBOR-3M' index based on monthly IMM dates.
   */
  public static final IborFutureConvention GBP_LIBOR_3M_MONTHLY_IMM =
      IborFutureConvention.of(StandardIborFutureConventions.GBP_LIBOR_3M_MONTHLY_IMM.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'EUR-EURIBOR-3M-Quarterly-IMM' convention.
   * <p>
   * The 'EUR-EURIBOR-3M' index based on quarterly IMM dates.
   */
  public static final IborFutureConvention EUR_EURIBOR_3M_QUARTERLY_IMM =
      IborFutureConvention.of(StandardIborFutureConventions.EUR_EURIBOR_3M_QUARTERLY_IMM.getName());

  /**
   * The 'EUR-EURIBOR-3M-Monthly-IMM' convention.
   * <p>
   * The 'EUR-EURIBOR-3M' index based on monthly IMM dates.
   */
  public static final IborFutureConvention EUR_EURIBOR_3M_MONTHLY_IMM =
      IborFutureConvention.of(StandardIborFutureConventions.EUR_EURIBOR_3M_MONTHLY_IMM.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'USD-LIBOR-3M-Quarterly-IMM' convention.
   * <p>
   * The 'USD-LIBOR-3M' index based on quarterly IMM dates.
   */
  public static final IborFutureConvention USD_LIBOR_3M_QUARTERLY_IMM =
      IborFutureConvention.of(StandardIborFutureConventions.USD_LIBOR_3M_QUARTERLY_IMM.getName());

  /**
   * The 'USD-LIBOR-3M-Monthly-IMM' convention.
   * <p>
   * The 'USD-LIBOR-3M' index based on monthly IMM dates.
   */
  public static final IborFutureConvention USD_LIBOR_3M_MONTHLY_IMM =
      IborFutureConvention.of(StandardIborFutureConventions.USD_LIBOR_3M_MONTHLY_IMM.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private IborFutureConventions() {
  }

}
