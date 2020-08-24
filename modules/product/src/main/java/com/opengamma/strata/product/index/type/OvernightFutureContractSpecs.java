/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Commonly traded Overnight future contract specifications.
 */
public final class OvernightFutureContractSpecs {

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<OvernightFutureContractSpec> ENUM_LOOKUP = ExtendedEnum.of(OvernightFutureContractSpec.class);

  //-------------------------------------------------------------------------
  /**
   * The 'GBP-SONIA-3M-IMM-CME' contract.
   * <p>
   * The CME "SON" contract based on quarterly IMM dates.
   */
  public static final OvernightFutureContractSpec GBP_SONIA_3M_IMM_CME =
      OvernightFutureContractSpec.of(StandardOvernightFutureContractSpecs.GBP_SONIA_3M_IMM_CME.getName());

  /**
   * The 'GBP-SONIA-3M-IMM-ICE' contract.
   * <p>
   * The ICE "SO3" contract based on quarterly IMM dates.
   */
  public static final OvernightFutureContractSpec GBP_SONIA_3M_IMM_ICE =
      OvernightFutureContractSpec.of(StandardOvernightFutureContractSpecs.GBP_SONIA_3M_IMM_ICE.getName());

  /**
   * The 'GBP-SONIA-3M-IMM-LCH' contract.
   * <p>
   * The LCH "SON" contract based on quarterly IMM dates.
   * <p>
   * Serial months can be traded for this contract, but no constant is defined here for that at present.
   */
  public static final OvernightFutureContractSpec GBP_SONIA_3M_IMM_LCH =
      OvernightFutureContractSpec.of(StandardOvernightFutureContractSpecs.GBP_SONIA_3M_IMM_LCH.getName());

  /**
   * The 'GBP-SONIA-1M-ICE' contract.
   * <p>
   * The ICE "SOA" contract based on calendar months.
   */
  public static final OvernightFutureContractSpec GBP_SONIA_1M_ICE =
      OvernightFutureContractSpec.of(StandardOvernightFutureContractSpecs.GBP_SONIA_1M_ICE.getName());

  /**
   * The 'GBP-SONIA-1M-IMM-LCH' contract.
   * <p>
   * The LCH "OSN" contract based on monthly IMM dates.
   */
  public static final OvernightFutureContractSpec GBP_SONIA_1M_IMM_LCH =
      OvernightFutureContractSpec.of(StandardOvernightFutureContractSpecs.GBP_SONIA_1M_IMM_LCH.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'USD-SOFR-3M-IMM-CME' contract.
   * <p>
   * The CME "SR3" contract based on quarterly IMM dates.
   */
  public static final OvernightFutureContractSpec USD_SOFR_3M_IMM_CME =
      OvernightFutureContractSpec.of(StandardOvernightFutureContractSpecs.USD_SOFR_3M_IMM_CME.getName());

  /**
   * The 'USD-SOFR-3M-IMM-ICE' contract.
   * <p>
   * The ICE "SF3" contract based on quarterly IMM dates.
   */
  public static final OvernightFutureContractSpec USD_SOFR_3M_IMM_ICE =
      OvernightFutureContractSpec.of(StandardOvernightFutureContractSpecs.USD_SOFR_3M_IMM_ICE.getName());

  /**
   * The 'USD-SOFR-1M-CME' contract.
   * <p>
   * The CME "SR1" contract based on calendar months.
   */
  public static final OvernightFutureContractSpec USD_SOFR_1M_CME =
      OvernightFutureContractSpec.of(StandardOvernightFutureContractSpecs.USD_SOFR_1M_CME.getName());

  /**
   * The 'USD-SOFR-1M-ICE' contract.
   * <p>
   * The ICE "SF1" contract based on calendar months.
   */
  public static final OvernightFutureContractSpec USD_SOFR_1M_ICE =
      OvernightFutureContractSpec.of(StandardOvernightFutureContractSpecs.USD_SOFR_1M_ICE.getName());

  //-------------------------------------------------------------------------
  /**
   * The 'USD-FED-FUND-1M-CME' contract.
   * <p>
   * The CME "ZQ" contract based on calendar months.
   */
  public static final OvernightFutureContractSpec USD_FED_FUND_1M_CME =
      OvernightFutureContractSpec.of(StandardOvernightFutureContractSpecs.USD_FED_FUND_1M_CME.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private OvernightFutureContractSpecs() {
  }

}
