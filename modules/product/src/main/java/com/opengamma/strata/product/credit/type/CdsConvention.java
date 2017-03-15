/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import java.time.LocalDate;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.TradeConvention;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.credit.CdsTrade;

/**
 * A market convention for credit default swap trades.
 */
public interface CdsConvention
    extends TradeConvention, Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static CdsConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the convention to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<CdsConvention> extendedEnum() {
    return CdsConventions.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Get the number of days between valuation date and settlement date.
   * <p>
   * It is usually 3 business days for standardised CDS contracts.
   * 
   * @return days adjustment
   */
  public abstract DaysAdjustment getSettlementDateOffset();

  /**
   * Get the currency of the CDS.
   * <p>
   * The amounts of the notional are expressed in terms of this currency.
   * 
   * @return the currency
   */
  public abstract Currency getCurrency();

  //-------------------------------------------------------------------------
  /**
   * Creates a CDS trade based on the trade date and the IMM date logic. 
   * <p>
   * The start date and end date are computed from trade date with the standard semi-annual roll convention.
   * 
   * @param legalEntityId  the legal entity ID
   * @param tradeDate  the trade date
   * @param tenor  the tenor
   * @param buySell  buy or sell
   * @param notional  the notional
   * @param fixedRate  the fixed rate
   * @param refData  the reference data
   * @return the CDS trade
   */
  public default CdsTrade createTrade(
      StandardId legalEntityId,
      LocalDate tradeDate,
      Tenor tenor,
      BuySell buySell,
      double notional,
      double fixedRate,
      ReferenceData refData) {

    LocalDate startDate = CdsImmDateLogic.getPreviousImmDate(tradeDate);
    LocalDate roll = CdsImmDateLogic.getNextSemiAnnualRollDate(tradeDate);
    LocalDate endDate = roll.plus(tenor).minusMonths(3);
    return createTrade(legalEntityId, tradeDate, startDate, endDate, buySell, notional, fixedRate, refData);
  }

  /**
   * Creates a CDS trade based on the trade date, start date and the IMM date logic. 
   * <p>
   * The end date is computed from the start date with the standard semi-annual roll convention.
   * 
   * @param legalEntityId  the legal entity ID
   * @param tradeDate  the trade date
   * @param startDate  the start date
   * @param tenor  the tenor
   * @param buySell  buy or sell
   * @param notional  the notional
   * @param fixedRate  the fixed rate
   * @param refData  the reference data
   * @return the CDS trade
   */
  public default CdsTrade createTrade(
      StandardId legalEntityId,
      LocalDate tradeDate,
      LocalDate startDate,
      Tenor tenor,
      BuySell buySell,
      double notional,
      double fixedRate,
      ReferenceData refData) {

    LocalDate roll = CdsImmDateLogic.getNextSemiAnnualRollDate(startDate);
    LocalDate endDate = roll.plus(tenor).minusMonths(3);
    return createTrade(legalEntityId, tradeDate, startDate, endDate, buySell, notional, fixedRate, refData);
  }

  /**
   * Creates a CDS trade from trade date, start date and end date.
   * <p>
   * The settlement date is computed from the trade date using {@code settlementDateOffset} defined in the convention.
   * 
   * @param legalEntityId  the legal entity ID
   * @param tradeDate  the trade date
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  buy or sell
   * @param notional  the notional
   * @param fixedRate  the fixed rate
   * @param refData  the reference data
   * @return the CDS trade
   */
  public default CdsTrade createTrade(
      StandardId legalEntityId,
      LocalDate tradeDate,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double fixedRate,
      ReferenceData refData) {

    LocalDate settlementDate = getSettlementDateOffset().adjust(tradeDate, refData);
    TradeInfo tradeInfo = TradeInfo.builder()
        .tradeDate(tradeDate)
        .settlementDate(settlementDate)
        .build();
    return toTrade(legalEntityId, tradeInfo, startDate, endDate, buySell, notional, fixedRate);
  }

  /**
   * Creates a CDS trade with {@code TradeInfo}.
   * 
   * @param legalEntityId  the legal entity ID
   * @param tradeInfo  the trade info
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  buy or sell
   * @param notional  the notional
   * @param fixedRate  the fixed rate
   * @return the CDS trade
   */
  public abstract CdsTrade toTrade(
      StandardId legalEntityId,
      TradeInfo tradeInfo,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double fixedRate);

  //-------------------------------------------------------------------------
  /**
   * Creates a CDS trade with upfront fee based on the trade date and the IMM date logic. 
   * <p>
   * The start date and end date are computed from trade date with the standard semi-annual roll convention.
   * 
   * @param legalEntityId  the legal entity ID
   * @param tradeDate  the trade date
   * @param tenor  the tenor
   * @param buySell  buy or sell
   * @param notional  the notional
   * @param fixedRate  the fixed rate
   * @param upFrontFee  the upFront fee
   * @param refData  the reference data
   * @return the CDS trade
   */
  public default CdsTrade createTrade(
      StandardId legalEntityId,
      LocalDate tradeDate,
      Tenor tenor,
      BuySell buySell,
      double notional,
      double fixedRate,
      AdjustablePayment upFrontFee,
      ReferenceData refData) {

    LocalDate startDate = CdsImmDateLogic.getPreviousImmDate(tradeDate);
    LocalDate roll = CdsImmDateLogic.getNextSemiAnnualRollDate(tradeDate);
    LocalDate endDate = roll.plus(tenor).minusMonths(3);
    return createTrade(legalEntityId, tradeDate, startDate, endDate, buySell, notional, fixedRate, upFrontFee, refData);
  }

  /**
   * Creates a CDS trade with upfront fee based on the trade date, start date and the IMM date logic. 
   * <p>
   * The end date is computed from the start date with the standard semi-annual roll convention.
   * 
   * @param legalEntityId  the legal entity ID
   * @param tradeDate  the trade date
   * @param startDate  the start date
   * @param tenor  the tenor
   * @param buySell  buy or sell
   * @param notional  the notional
   * @param fixedRate  the fixed rate
   * @param upFrontFee  the upFront fee
   * @param refData  the reference data
   * @return the CDS trade
   */
  public default CdsTrade createTrade(
      StandardId legalEntityId,
      LocalDate tradeDate,
      LocalDate startDate,
      Tenor tenor,
      BuySell buySell,
      double notional,
      double fixedRate,
      AdjustablePayment upFrontFee,
      ReferenceData refData) {

    LocalDate roll = CdsImmDateLogic.getNextSemiAnnualRollDate(startDate);
    LocalDate endDate = roll.plus(tenor).minusMonths(3);
    return createTrade(legalEntityId, tradeDate, startDate, endDate, buySell, notional, fixedRate, upFrontFee, refData);
  }

  /**
   * Creates a CDS trade with upfront fee from trade date, start date and end date.
   * <p>
   * The settlement date is computed from the trade date using {@code settlementDateOffset} defined in the convention.
   * 
   * @param legalEntityId  the legal entity ID
   * @param tradeDate  the trade date
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  buy or sell
   * @param notional  the notional
   * @param fixedRate  the fixed rate
   * @param upFrontFee  the upFront fee
   * @param refData  the reference data
   * @return the CDS trade
   */
  public default CdsTrade createTrade(
      StandardId legalEntityId,
      LocalDate tradeDate,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double fixedRate,
      AdjustablePayment upFrontFee,
      ReferenceData refData) {

    LocalDate settlementDate = getSettlementDateOffset().adjust(tradeDate, refData);
    TradeInfo tradeInfo = TradeInfo.builder()
        .tradeDate(tradeDate)
        .settlementDate(settlementDate)
        .build();
    return toTrade(legalEntityId, tradeInfo, startDate, endDate, buySell, notional, fixedRate, upFrontFee);
  }

  /**
   * Creates a CDS trade with upfront fee and {@code TradeInfo}.
   * 
   * @param legalEntityId  the legal entity ID
   * @param tradeInfo  the trade info
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  buy or sell
   * @param notional  the notional
   * @param fixedRate  the fixed rate
   * @param upFrontFee  the upFront fee
   * @return the CDS trade
   */
  public abstract CdsTrade toTrade(
      StandardId legalEntityId,
      TradeInfo tradeInfo,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double fixedRate,
      AdjustablePayment upFrontFee);

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this convention.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
