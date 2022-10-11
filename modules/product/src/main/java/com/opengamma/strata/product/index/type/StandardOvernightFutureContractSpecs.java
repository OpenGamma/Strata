/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DateSequences.MONTHLY_1ST;
import static com.opengamma.strata.basics.date.DateSequences.MONTHLY_IMM;
import static com.opengamma.strata.basics.date.DateSequences.QUARTERLY_IMM;
import static com.opengamma.strata.basics.date.DateSequences.QUARTERLY_IMM_6_SERIAL;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.CHZU;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.OvernightIndices.CHF_SARON;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_ESTR;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_SOFR;
import static com.opengamma.strata.product.swap.OvernightAccrualMethod.AVERAGED_DAILY;
import static com.opengamma.strata.product.swap.OvernightAccrualMethod.COMPOUNDED;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;

/**
 * Commonly traded Overnight future contract specifications.
 */
final class StandardOvernightFutureContractSpecs {

  /**
   * The 'CHF_SARON_3M_IMM_ICE' convention.
   * <p>
   * https://www.theice.com/products/72270612/Three-Month-Saron-Index-Futures-Contract
   * <p>
   * Contract code "SA3"
   * 16 delivery months are available for trading
   */
  public static final OvernightFutureContractSpec CHF_SARON_3M_IMM_ICE =
      ImmutableOvernightFutureContractSpec.builder()
          .name("CHF-SARON-3M-IMM-ICE")
          .index(CHF_SARON)
          .dateSequence(QUARTERLY_IMM_6_SERIAL)
          .accrualMethod(COMPOUNDED)
          .lastTradeDateAdjustment(DaysAdjustment.ofBusinessDays(1, CHZU, BusinessDayAdjustment.of(PRECEDING, CHZU)))
          .notional(1_000_000d)
          .build();

  /**
   * The 'CHF_SARON_3M_IMM_EUREX' convention.
   * <p>
   * https://www.eurex.com/ex-en/markets/int/mon/saron-futures/saron/3M-SARON-Futures-1405958
   * <p>
   * Contract code "FSR3"
   * 12 delivery months are available for trading
   */
  public static final OvernightFutureContractSpec CHF_SARON_3M_IMM_EUREX =
      ImmutableOvernightFutureContractSpec.builder()
          .name("CHF-SARON-3M-IMM-EUREX")
          .index(CHF_SARON)
          .dateSequence(QUARTERLY_IMM)
          .accrualMethod(COMPOUNDED)
          .lastTradeDateAdjustment(DaysAdjustment.ofBusinessDays(1, CHZU, BusinessDayAdjustment.of(PRECEDING, CHZU)))
          .notional(1_000_000d)
          .build();

  /**
   * The 'EUR_ESTR_1M_IMM_ICE' convention.
   * <p>
   * https://www.theice.com/products/37650328/One-Month-ESTR-Index-Futures
   * <p>
   * Contract code "EON"
   * Maximum of 24 delivery months available for trading
   */
  public static final OvernightFutureContractSpec EUR_ESTR_1M_IMM_ICE =
      ImmutableOvernightFutureContractSpec.builder()
          .name("EUR-ESTR-1M-IMM-ICE")
          .index(EUR_ESTR)
          .dateSequence(MONTHLY_IMM)
          .accrualMethod(COMPOUNDED)
          .lastTradeDateAdjustment(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, EUTA)))
          .notional(1_000_000d)
          .build();

  /**
   * The 'GBP-SONIA-3M-IMM-CME' convention.
   * https://www.cmegroup.com/trading/interest-rates/stir/quarterly-imm-sonia_contract_specifications.html
   */
  public static final OvernightFutureContractSpec GBP_SONIA_3M_IMM_CME =
      ImmutableOvernightFutureContractSpec.builder()
          .name("GBP-SONIA-3M-IMM-CME")
          .index(GBP_SONIA)
          .dateSequence(QUARTERLY_IMM)
          .accrualMethod(COMPOUNDED)
          .lastTradeDateAdjustment(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, GBLO)))
          .notional(1_000_000d)
          .build();

  /**
   * The 'GBP-SONIA-3M-IMM-ICE' convention.
   * <p>
   * https://www.theice.com/products/68361266/Three-Month-Sonia-Index-Futures
   * <p>
   * Contract code "SO3"
   */
  public static final OvernightFutureContractSpec GBP_SONIA_3M_IMM_ICE =
      ImmutableOvernightFutureContractSpec.builder()
          .name("GBP-SONIA-3M-IMM-ICE")
          .index(GBP_SONIA)
          .dateSequence(QUARTERLY_IMM)
          .accrualMethod(COMPOUNDED)
          .notional(1_000_000d)
          .build();

  /**
   * The 'GBP-SONIA-3M-IMM-LCH' convention.
   * https://docs.londonstockexchange.com/sites/default/files/documents/CurveGlobal%2520SONIA%2520Futures%2520Contract%2520Specs%2520-%2520JULY%25202019.pdf
   */
  public static final OvernightFutureContractSpec GBP_SONIA_3M_IMM_LCH =
      ImmutableOvernightFutureContractSpec.builder()
          .name("GBP-SONIA-3M-IMM-LCH")
          .index(GBP_SONIA)
          .dateSequence(QUARTERLY_IMM_6_SERIAL)
          .accrualMethod(COMPOUNDED)
          .lastTradeDateAdjustment(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, GBLO)))
          .notional(500_000d)
          .build();

  /**
   * The 'GBP-SONIA-1M-ICE' convention.
   * https://www.theice.com/products/66380299/One-Month-SONIA-Index-Futures
   */
  public static final OvernightFutureContractSpec GBP_SONIA_1M_ICE =
      ImmutableOvernightFutureContractSpec.builder()
          .name("GBP-SONIA-1M-ICE")
          .index(GBP_SONIA)
          .dateSequence(MONTHLY_1ST)
          .accrualMethod(AVERAGED_DAILY)
          .notional(3_000_000d)
          .build();

  /**
   * The 'GBP-SONIA-1M-LCH' convention.
   * https://docs.londonstockexchange.com/sites/default/files/documents/CurveGlobal%2520SONIA%2520Futures%2520Contract%2520Specs%2520-%2520JULY%25202019.pdf
   */
  public static final OvernightFutureContractSpec GBP_SONIA_1M_IMM_LCH =
      ImmutableOvernightFutureContractSpec.builder()
          .name("GBP-SONIA-1M-IMM-LCH")
          .index(GBP_SONIA)
          .dateSequence(MONTHLY_IMM)
          .accrualMethod(COMPOUNDED)
          .lastTradeDateAdjustment(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, GBLO)))
          .notional(1_500_000d)
          .build();

  //-------------------------------------------------------------------------
  /**
   * The 'USD-SOFR-3M-IMM-CME' convention.
   * <p>
   * https://www.cmegroup.com/trading/interest-rates/stir/three-month-sofr_contract_specifications.html
   * <p>
   * Contract code "SR1"
   */
  public static final OvernightFutureContractSpec USD_SOFR_1M_IMM_CME =
      ImmutableOvernightFutureContractSpec.builder()
          .name("USD-SOFR-1M-IMM-CME")
          .index(USD_SOFR)
          .dateSequence(MONTHLY_IMM)
          .accrualMethod(COMPOUNDED)
          .notional(5_000_000d)
          .build();

  /**
   * The 'USD-SOFR-3M-IMM-CME' convention.
   * <p>
   * https://www.cmegroup.com/trading/interest-rates/stir/three-month-sofr_contract_specifications.html
   * <p>
   * Contract code "SR3"
   */
  public static final OvernightFutureContractSpec USD_SOFR_3M_IMM_CME =
      ImmutableOvernightFutureContractSpec.builder()
          .name("USD-SOFR-3M-IMM-CME")
          .index(USD_SOFR)
          .dateSequence(QUARTERLY_IMM)
          .accrualMethod(COMPOUNDED)
          .notional(1_000_000d)
          .build();

  /**
   * The 'USD-SOFR-3M-IMM-ICE' convention.
   * https://www.theice.com/products/70005442/ICE-Futures-Europe-Three-Month-SOFR-Index-Future
   */
  public static final OvernightFutureContractSpec USD_SOFR_3M_IMM_ICE =
      ImmutableOvernightFutureContractSpec.builder()
          .name("USD-SOFR-3M-IMM-ICE")
          .index(USD_SOFR)
          .dateSequence(QUARTERLY_IMM)
          .accrualMethod(COMPOUNDED)
          .notional(4_000_000d)
          .build();

  /**
   * The 'USD-SOFR-1M-CME' convention.
   * https://www.cmegroup.com/trading/interest-rates/stir/one-month-sofr_contract_specifications.html
   */
  public static final OvernightFutureContractSpec USD_SOFR_1M_CME =
      ImmutableOvernightFutureContractSpec.builder()
          .name("USD-SOFR-1M-CME")
          .index(USD_SOFR)
          .dateSequence(MONTHLY_1ST)
          .accrualMethod(AVERAGED_DAILY)
          .notional(5_000_000d)
          .build();

  /**
   * The 'USD-SOFR-1M-ICE' convention.
   * https://www.theice.com/products/70005423/ICE-Futures-Europe-One-Month-SOFR-Index-Future
   */
  public static final OvernightFutureContractSpec USD_SOFR_1M_ICE =
      ImmutableOvernightFutureContractSpec.builder()
          .name("USD-SOFR-1M-ICE")
          .index(USD_SOFR)
          .dateSequence(MONTHLY_1ST)
          .accrualMethod(AVERAGED_DAILY)
          .notional(12_000_000d)
          .build();

  //-------------------------------------------------------------------------
  /**
   * The 'USD-FED-FUND-1M-CME' convention.
   * https://www.cmegroup.com/trading/interest-rates/stir/30-day-federal-fund_contract_specifications.html
   * <p>
   * Contract code "ZQ"
   */
  public static final OvernightFutureContractSpec USD_FED_FUND_1M_CME =
      ImmutableOvernightFutureContractSpec.builder()
          .name("USD-FED-FUND-1M-CME")
          .index(USD_FED_FUND)
          .dateSequence(MONTHLY_1ST)
          .accrualMethod(AVERAGED_DAILY)
          .notional(5_000_000d)
          .build();

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardOvernightFutureContractSpecs() {
  }

}
