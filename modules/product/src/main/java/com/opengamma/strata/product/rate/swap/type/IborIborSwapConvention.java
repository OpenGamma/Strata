/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.swap.type;

import java.time.LocalDate;
import java.time.Period;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.TradeConvention;
import com.opengamma.strata.product.rate.swap.SwapTrade;

/**
 * A market convention for Ibor-Ibor swap trades.
 * <p>
 * This defines the market convention for a Ibor-Ibor single currency swap.
 * The convention is formed by combining two swap leg conventions in the same currency.
 * <p>
 * The market price is for the difference (spread) between the values of the two legs.
 * This convention has two legs, the "spread leg" and the "flat leg". The spread will be
 * added to the "spread leg", which is typically the leg with the shorter underlying tenor.
 * The payment frequency is typically determined by the longer underlying tenor, with
 * compounding applied.
 * <p>
 * For example, a 'USD 3s1s' basis swap has 'USD-LIBOR-1M' as the spread leg and 'USD-LIBOR-3M'
 * as the flat leg. Payment is every 3 months, with the one month leg compounded.
 * <p>
 * To manually create a convention, see {@link ImmutableIborIborSwapConvention}.
 * To register a specific convention, see {@code IborIborSwapConvention.ini}.
 */
public interface IborIborSwapConvention
    extends TradeConvention, Named {

  /**
   * Obtains a convention from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static IborIborSwapConvention of(String uniqueName) {
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
  public static ExtendedEnum<IborIborSwapConvention> extendedEnum() {
    return IborIborSwapConventions.ENUM_LOOKUP;
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

  //-------------------------------------------------------------------------
  /**
   * Creates a spot-starting trade based on this convention.
   * <p>
   * This returns a trade based on the specified tenor. For example, a tenor
   * of 5 years creates a swap starting on the spot date and maturing 5 years later.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the rate of the flat leg is received from the counterparty,
   * with the rate of the spread leg being paid. If selling the swap, the opposite occurs.
   * 
   * @param tradeDate  the date of the trade
   * @param tenor  the tenor of the swap
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param spread  the spread, typically derived from the market
   * @return the trade
   */
  public default SwapTrade toTrade(
      LocalDate tradeDate,
      Tenor tenor,
      BuySell buySell,
      double notional,
      double spread) {

    return toTrade(tradeDate, Period.ZERO, tenor, buySell, notional, spread);
  }

  /**
   * Creates a forward-starting trade based on this convention.
   * <p>
   * This returns a trade based on the specified period and tenor. For example, a period of
   * 3 months and a tenor of 5 years creates a swap starting three months after the spot date
   * and maturing 5 years later.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the rate of the flat leg is received from the counterparty,
   * with the rate of the spread leg being paid. If selling the swap, the opposite occurs.
   * 
   * @param tradeDate  the date of the trade
   * @param periodToStart  the period between the spot date and the start date
   * @param tenor  the tenor of the swap
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param spread  the spread, typically derived from the market
   * @return the trade
   */
  public default SwapTrade toTrade(
      LocalDate tradeDate,
      Period periodToStart,
      Tenor tenor,
      BuySell buySell,
      double notional,
      double spread) {

    LocalDate spotValue = calculateSpotDateFromTradeDate(tradeDate);
    LocalDate startDate = spotValue.plus(periodToStart);
    LocalDate endDate = startDate.plus(tenor.getPeriod());
    return toTrade(tradeDate, startDate, endDate, buySell, notional, spread);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the rate of the flat leg is received from the counterparty,
   * with the rate of the spread leg being paid. If selling the swap, the opposite occurs.
   * 
   * @param tradeDate  the date of the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param spread  the spread, typically derived from the market
   * @return the trade
   */
  public abstract SwapTrade toTrade(
      LocalDate tradeDate,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double spread);

  //-------------------------------------------------------------------------
  /**
   * Calculates the spot date from the trade date.
   * 
   * @param tradeDate  the trade date
   * @return the spot date
   */
  public abstract LocalDate calculateSpotDateFromTradeDate(LocalDate tradeDate);

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
