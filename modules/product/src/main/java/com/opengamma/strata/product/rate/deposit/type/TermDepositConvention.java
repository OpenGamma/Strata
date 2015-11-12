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
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.TradeConvention;
import com.opengamma.strata.product.rate.deposit.TermDepositTrade;

/**
 * A market convention for term deposit trades.
 * <p>
 * This defines the market convention for a term deposit.
 * <p>
 * To manually create a convention, see {@link ImmutableTermDepositConvention}.
 * To register a specific convention, see {@code TermDepositConvention.ini}.
 */
public interface TermDepositConvention
    extends TradeConvention, Named {

  /**
   * Obtains a convention from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static TermDepositConvention of(String uniqueName) {
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
  public static ExtendedEnum<TermDepositConvention> extendedEnum() {
    return TermDepositConventions.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the primary currency.
   * <p>
   * This is the currency of the term deposit and the currency that payment is made in.
   * 
   * @return the currency
   */
  public abstract Currency getCurrency();

  //-------------------------------------------------------------------------
  /**
   * Creates a template based on this convention, specifying the period from start to end.
   * <p>
   * This returns a template based on this convention.
   * The period from the start date to the end date is specified.
   * 
   * @param depositPeriod  the period from the start date to the end date
   * @return the template
   */
  public default TermDepositTemplate toTemplate(Period depositPeriod) {
    return TermDepositTemplate.of(depositPeriod, this);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified deposit period.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the term deposit, the principal is paid at the start date and the
   * principal plus interest is received at the end date.
   * If selling the term deposit, the principal is received at the start date and the
   * principal plus interest is paid at the end date.
   * 
   * @param tradeDate  the date of the trade
   * @param depositPeriod  the period between the start date and the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param rate  the fixed rate, typically derived from the market
   * @return the trade
   */
  public abstract TermDepositTrade toTrade(
      LocalDate tradeDate,
      Period depositPeriod,
      BuySell buySell,
      double notional,
      double rate);

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the term deposit, the principal is paid at the start date and the
   * principal plus interest is received at the end date.
   * If selling the term deposit, the principal is received at the start date and the
   * principal plus interest is paid at the end date.
   * 
   * @param tradeDate  the date of the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param rate  the fixed rate, typically derived from the market
   * @return the trade
   */
  public abstract TermDepositTrade toTrade(
      LocalDate tradeDate,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double rate);

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
