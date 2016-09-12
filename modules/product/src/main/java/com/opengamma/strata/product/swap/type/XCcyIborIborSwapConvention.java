/**
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
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.TradeConvention;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * A market convention for cross-currency Ibor-Ibor swap trades without FX reset.
 * <p>
 * This defines the market convention for a cross-currency Ibor-Ibor swap.
 * The convention is formed by combining two swap leg conventions in different currencies.
 * <p>
 * The market price is for the difference (spread) between the values of the two legs.
 * This convention has two legs, the "spread leg" and the "flat leg". The spread will be
 * added to the "spread leg".
 * <p>
 * For example, a 'EUR/USD' basis swap has 'EUR-EURIBOR-3M' as the spread leg and 'USD-LIBOR-3M' as the flat leg.
 * <p>
 * To manually create a convention, see {@link ImmutableXCcyIborIborSwapConvention}.
 * To register a specific convention, see {@code XCcyIborIborSwapConvention.ini}.
 */
public interface XCcyIborIborSwapConvention
    extends TradeConvention, Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static XCcyIborIborSwapConvention of(String uniqueName) {
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
  public static ExtendedEnum<XCcyIborIborSwapConvention> extendedEnum() {
    return XCcyIborIborSwapConventions.ENUM_LOOKUP;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the floating leg that has the spread applied.
   * <p>
   * The spread is the market price of the instrument.
   * It is added to the observed interest rate.
   * 
   * @return the spread leg convention
   */
  public abstract IborRateSwapLegConvention getSpreadLeg();

  /**
   * Gets the market convention of the floating leg that does not have the spread applied.
   * 
   * @return the flat leg convention
   */
  public abstract IborRateSwapLegConvention getFlatLeg();

  /**
   * Gets the offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date to find the start date.
   * A typical value is "plus 2 business days".
   * 
   * @return the spot date offset, not null
   */
  public abstract DaysAdjustment getSpotDateOffset();

  /**
   * Gets the currency pair of the convention.
   * 
   * @return the currency pair
   */
  public default CurrencyPair getCurrencyPair() {
    return CurrencyPair.of(getSpreadLeg().getCurrency(), getFlatLeg().getCurrency());
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a spot-starting trade based on this convention.
   * <p>
   * This returns a trade based on the specified tenor. For example, a tenor
   * of 5 years creates a swap starting on the spot date and maturing 5 years later.
   * <p>
   * The notionals are unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the rate of the flat leg is received, with the rate of the spread leg being paid.
   * If selling the swap, the opposite occurs.
   * 
   * @param tradeDate  the date of the trade
   * @param tenor  the tenor of the swap
   * @param buySell  the buy/sell flag
   * @param notionalSpreadLeg  the notional amount for the spread leg
   * @param notionalFlatLeg  the notional amount for the flat leg
   * @param spread  the spread, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public default SwapTrade createTrade(
      LocalDate tradeDate,
      Tenor tenor,
      BuySell buySell,
      double notionalSpreadLeg,
      double notionalFlatLeg,
      double spread,
      ReferenceData refData) {

    return createTrade(tradeDate, Period.ZERO, tenor, buySell, notionalSpreadLeg, notionalFlatLeg, spread, refData);
  }

  /**
   * Creates a forward-starting trade based on this convention.
   * <p>
   * This returns a trade based on the specified period and tenor. For example, a period of
   * 3 months and a tenor of 5 years creates a swap starting three months after the spot date
   * and maturing 5 years later.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the rate of the flat leg is received, with the rate of the spread leg being paid.
   * If selling the swap, the opposite occurs.
   * 
   * @param tradeDate  the date of the trade
   * @param periodToStart  the period between the spot date and the start date
   * @param tenor  the tenor of the swap
   * @param buySell  the buy/sell flag
   * @param notionalSpreadLeg  the notional amount for the spread leg
   * @param notionalFlatLeg  the notional amount for the flat leg
   * @param spread  the spread, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public default SwapTrade createTrade(
      LocalDate tradeDate,
      Period periodToStart,
      Tenor tenor,
      BuySell buySell,
      double notionalSpreadLeg,
      double notionalFlatLeg,
      double spread,
      ReferenceData refData) {

    LocalDate spotValue = calculateSpotDateFromTradeDate(tradeDate, refData);
    LocalDate startDate = spotValue.plus(periodToStart);
    LocalDate endDate = startDate.plus(tenor.getPeriod());
    return toTrade(tradeDate, startDate, endDate, buySell, notionalSpreadLeg, notionalFlatLeg, spread);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the rate of the flat leg is received, with the rate of the spread leg being paid.
   * If selling the swap, the opposite occurs.
   * 
   * @param tradeDate  the date of the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notionalSpreadLeg  the notional amount for the spread leg
   * @param notionalFlatLeg  the notional amount for the flat leg
   * @param spread  the spread, typically derived from the market
   * @return the trade
   */
  public default SwapTrade toTrade(
      LocalDate tradeDate,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notionalSpreadLeg,
      double notionalFlatLeg,
      double spread) {

    TradeInfo tradeInfo = TradeInfo.of(tradeDate);
    return toTrade(tradeInfo, startDate, endDate, buySell, notionalSpreadLeg, notionalFlatLeg, spread);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the rate of the flat leg is received, with the rate of the spread leg being paid.
   * If selling the swap, the opposite occurs.
   * 
   * @param tradeInfo  additional information about the trade.
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notionalSpreadLeg  the notional amount for the spread leg
   * @param notionalFlatLeg  the notional amount for the flat leg
   * @param spread  the spread, typically derived from the market
   * @return the trade
   */
  public abstract SwapTrade toTrade(
      TradeInfo tradeInfo,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notionalSpreadLeg,
      double notionalFlatLeg,
      double spread);

  //-------------------------------------------------------------------------
  /**
   * Calculates the spot date from the trade date.
   * 
   * @param tradeDate  the trade date
   * @param refData  the reference data, used to resolve the date
   * @return the spot date
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
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
