/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra.type;

import java.time.LocalDate;
import java.time.Period;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.TradeConvention;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.fra.FraTrade;

/**
 * A market convention for forward rate agreement (FRA) trades.
 * <p>
 * This defines the market convention for a FRA against a particular index.
 * In most cases, the index contains sufficient information to fully define the convention.
 * As such, the convention is set to be created on the fly based on the index.
 * <p>
 * To manually create a convention, see {@link ImmutableFraConvention}.
 * To register a specific convention, see {@code FraConvention.ini}.
 */
public interface FraConvention
    extends TradeConvention, Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FraConvention of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Obtains a convention based on the specified index.
   * <p>
   * This uses the index name to find the matching convention.
   * By default, this will always return a convention, however configuration may be added
   * to restrict the conventions that are registered.
   * 
   * @param index  the index, from which the index name is used to find the matching convention
   * @return the convention
   * @throws IllegalArgumentException if no convention is registered for the index
   */
  public static FraConvention of(IborIndex index) {
    ArgChecker.notNull(index, "index");
    return extendedEnum().lookup(index.getName());
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the convention to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<FraConvention> extendedEnum() {
    return FraConventions.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the Ibor index.
   * <p>
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   * 
   * @return the index
   */
  public abstract IborIndex getIndex();

  /**
   * Gets the offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date to find the start date.
   * A typical value is "plus 2 business days".
   * 
   * @return the spot date offset, not null
   */
  public abstract DaysAdjustment getSpotDateOffset();

  //-------------------------------------------------------------------------
  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified periods.
   * For example, a '2 x 5' FRA has a period to the start date of 2 months and
   * a period to the end date of 5 months
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the FRA, the floating rate is received from the counterparty, with the fixed rate being paid.
   * If selling the FRA, the floating rate is paid to the counterparty, with the fixed rate being received.
   * 
   * @param tradeDate  the date of the trade
   * @param periodToStart  the period between the spot date and the start date
   * @param periodToEnd  the period between the spot date and the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public abstract FraTrade createTrade(
      LocalDate tradeDate,
      Period periodToStart,
      Period periodToEnd,
      BuySell buySell,
      double notional,
      double fixedRate,
      ReferenceData refData);

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the FRA, the floating rate is received from the counterparty, with the fixed rate being paid.
   * If selling the FRA, the floating rate is paid to the counterparty, with the fixed rate being received.
   * 
   * @param tradeDate  the date of the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param paymentDate  the payment date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @return the trade
   */
  public default FraTrade toTrade(
      LocalDate tradeDate,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate paymentDate,
      BuySell buySell,
      double notional,
      double fixedRate) {

    TradeInfo tradeInfo = TradeInfo.of(tradeDate);
    return toTrade(tradeInfo, startDate, endDate, paymentDate, buySell, notional, fixedRate);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the FRA, the floating rate is received from the counterparty, with the fixed rate being paid.
   * If selling the FRA, the floating rate is paid to the counterparty, with the fixed rate being received.
   * 
   * @param tradeInfo  additional information about the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param paymentDate  the payment date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @return the trade
   */
  public abstract FraTrade toTrade(
      TradeInfo tradeInfo,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate paymentDate,
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
