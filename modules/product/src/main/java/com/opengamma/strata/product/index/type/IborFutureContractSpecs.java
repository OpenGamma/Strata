/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Market standard Ibor future conventions.
 */
public final class IborFutureContractSpecs {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<IborFutureContractSpec> ENUM_LOOKUP = ExtendedEnum.of(IborFutureContractSpec.class);

  //-------------------------------------------------------------------------
  /**
   * The 'GBP-LIBOR-3M-IMM-ICE' contract.
   * <p>
   * The ICE "L" contract based on quarterly IMM dates, also known as "FSS".
   */
  public static final IborFutureContractSpec GBP_LIBOR_3M_IMM_ICE =
      IborFutureContractSpec.of(StandardIborFutureContractSpecs.GBP_LIBOR_3M_IMM_ICE.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'EUR-EURIBOR-3M-IMM-ICE' contract.
   * <p>
   * The ICE "I" contract based on quarterly IMM dates, also known as "FEI".
   */
  public static final IborFutureContractSpec EUR_EURIBOR_3M_IMM_ICE =
      IborFutureContractSpec.of(StandardIborFutureContractSpecs.EUR_EURIBOR_3M_IMM_ICE.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'USD-LIBOR-3M-IMM-CME' contract.
   * <p>
   * The CME "ED" contract based on quarterly IMM dates.
   */
  public static final IborFutureContractSpec USD_LIBOR_3M_IMM_CME =
      IborFutureContractSpec.of(StandardIborFutureContractSpecs.USD_LIBOR_3M_IMM_CME.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private IborFutureContractSpecs() {
  }

}
