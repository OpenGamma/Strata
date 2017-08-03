/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import java.time.LocalDate;
import java.time.Period;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * A market convention for Fixed-Ibor swap trades.
 * <p>
 * This defines the market convention for a Fixed-Ibor single currency swap.
 * This is often known as a <i>vanilla swap</i>.
 * The convention is formed by combining two swap leg conventions in the same currency.
 * <p>
 * To manually create a convention, see {@link ImmutableFixedIborSwapConvention}.
 * To register a specific convention, see {@code FixedIborSwapConvention.ini}.
 */
public interface FixedIborSwapConvention
    extends SingleCurrencySwapConvention, Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FixedIborSwapConvention of(String uniqueName) {
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
  public static ExtendedEnum<FixedIborSwapConvention> extendedEnum() {
    return FixedIborSwapConventions.ENUM_LOOKUP;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the fixed leg.
   * 
   * @return the fixed leg convention
   */
  public abstract FixedRateSwapLegConvention getFixedLeg();

  /**
   * Gets the market convention of the floating leg.
   * 
   * @return the floating leg convention
   */
  public abstract IborRateSwapLegConvention getFloatingLeg();

  //-------------------------------------------------------------------------
  /**
   * Creates a spot-starting trade based on this convention.
   * <p>
   * This returns a trade based on the specified tenor. For example, a tenor
   * of 5 years creates a swap starting on the spot date and maturing 5 years later.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the floating rate is received from the counterparty, with the fixed rate being paid.
   * If selling the swap, the floating rate is paid to the counterparty, with the fixed rate being received.
   * 
   * @param tradeDate  the date of the trade
   * @param tenor  the tenor of the swap
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param fixedRate  the fixed rate, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  @Override
  public default SwapTrade createTrade(
      LocalDate tradeDate,
      Tenor tenor,
      BuySell buySell,
      double notional,
      double fixedRate,
      ReferenceData refData) {

    return SingleCurrencySwapConvention.super.createTrade(tradeDate, tenor, buySell, notional, fixedRate, refData);
  }

  /**
   * Creates a forward-starting trade based on this convention.
   * <p>
   * This returns a trade based on the specified period and tenor. For example, a period of
   * 3 months and a tenor of 5 years creates a swap starting three months after the spot date
   * and maturing 5 years later.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the floating rate is received from the counterparty, with the fixed rate being paid.
   * If selling the swap, the floating rate is paid to the counterparty, with the fixed rate being received.
   * 
   * @param tradeDate  the date of the trade
   * @param periodToStart  the period between the spot date and the start date
   * @param tenor  the tenor of the swap
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param fixedRate  the fixed rate, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  @Override
  public default SwapTrade createTrade(
      LocalDate tradeDate,
      Period periodToStart,
      Tenor tenor,
      BuySell buySell,
      double notional,
      double fixedRate,
      ReferenceData refData) {

    return SingleCurrencySwapConvention.super.createTrade(tradeDate, periodToStart, tenor, buySell, notional, fixedRate, refData);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the floating rate is received from the counterparty, with the fixed rate being paid.
   * If selling the swap, the floating rate is paid to the counterparty, with the fixed rate being received.
   * 
   * @param tradeDate  the date of the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param fixedRate  the fixed rate, typically derived from the market
   * @return the trade
   */
  @Override
  public default SwapTrade toTrade(
      LocalDate tradeDate,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double fixedRate) {

    return SingleCurrencySwapConvention.super.toTrade(tradeDate, startDate, endDate, buySell, notional, fixedRate);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the floating rate is received from the counterparty, with the fixed rate being paid.
   * If selling the swap, the floating rate is paid to the counterparty, with the fixed rate being received.
   * 
   * @param tradeInfo  additional information about the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param fixedRate  the fixed rate, typically derived from the market
   * @return the trade
   */
  @Override
  public abstract SwapTrade toTrade(
      TradeInfo tradeInfo,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double fixedRate);

  //-------------------------------------------------------------------------
  /**
   * Calculates the spot date from the trade date.
   * 
   * @param tradeDate  the trade date
   * @param refData  the reference data, used to resolve the date
   * @return the spot date
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  @Override
  public default LocalDate calculateSpotDateFromTradeDate(LocalDate tradeDate, ReferenceData refData) {
    return getSpotDateOffset().adjust(tradeDate, refData);
  }

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
