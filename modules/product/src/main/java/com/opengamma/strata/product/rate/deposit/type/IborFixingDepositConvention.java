/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.deposit.type;

import java.time.LocalDate;
import java.time.Period;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.TradeConvention;
import com.opengamma.strata.product.rate.deposit.IborFixingDepositTrade;

/**
 * A convention for Ibor fixing deposit trades.
 * <p>
 * This defines the convention for an Ibor fixing deposit against a particular index.
 * In most cases, the index contains sufficient information to fully define the convention.
 * As such, the convention is set to be created on the fly based on the index.
 * <p>
 * To manually create a convention, see {@link ImmutableIborFixingDepositConvention}.
 * To register a specific convention, see {@code IborFixingDepositConvention.ini}.
 */
public interface IborFixingDepositConvention
    extends TradeConvention, Named {

  /**
   * Obtains a convention from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static IborFixingDepositConvention of(String uniqueName) {
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
  public static IborFixingDepositConvention of(IborIndex index) {
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
  public static ExtendedEnum<IborFixingDepositConvention> extendedEnum() {
    return IborFixingDepositConventions.ENUM_LOOKUP;
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

  //-------------------------------------------------------------------------
  /**
   * Creates a template based on this convention.
   * <p>
   * This returns a template based on this convention.
   * The period from the start date to the end date will be the tenor of the index.
   * 
   * @return the template
   */
  public default IborFixingDepositTemplate toTemplate() {
    return toTemplate(getIndex().getTenor().getPeriod());
  }

  /**
   * Creates a template based on this convention, specifying the period from start to end.
   * <p>
   * This returns a template based on this convention.
   * The period from the start date to the end date is specified.
   * 
   * @param depositPeriod  the period from the start date to the end date
   * @return the template
   */
  public default IborFixingDepositTemplate toTemplate(Period depositPeriod) {
    return IborFixingDepositTemplate.of(depositPeriod, this);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified deposit period.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the Ibor fixing deposit, the floating rate is paid to the counterparty, with the fixed rate being received.
   * If selling the Ibor fixing deposit, the floating rate is received from the counterparty, with the fixed rate being paid.
   * 
   * @param tradeDate  the date of the trade
   * @param depositPeriod  the period between the start date and the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @return the trade
   */
  public abstract IborFixingDepositTrade toTrade(
      LocalDate tradeDate,
      Period depositPeriod,
      BuySell buySell,
      double notional,
      double fixedRate);

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the Ibor fixing deposit, the floating rate is paid to the counterparty, with the fixed rate being received.
   * If selling the Ibor fixing deposit, the floating rate is received from the counterparty, with the fixed rate being paid.
   * 
   * @param tradeDate  the date of the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @return the trade
   */
  public abstract IborFixingDepositTrade toTrade(
      LocalDate tradeDate,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double fixedRate);

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
