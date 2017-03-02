/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeConvention;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * A market convention for Ibor Future trades.
 * <p>
 * This defines the market convention for a future against a particular index.
 * <p>
 * To manually create a convention, see {@link ImmutableIborFutureConvention}.
 * To register a specific convention, see {@code IborFutureConvention.ini}.
 */
public interface IborFutureConvention
    extends TradeConvention, Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static IborFutureConvention of(String uniqueName) {
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
  public static ExtendedEnum<IborFutureConvention> extendedEnum() {
    return IborFutureConventions.ENUM_LOOKUP;
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
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified minimum period and sequence number.
   * 
   * @param tradeDate  the trade date
   * @param securityId  the identifier of the security
   * @param minimumPeriod  minimum period between the value date and the first future
   * @param sequenceNumber  the 1-based sequence number of the futures
   * @param quantity  the number of contracts traded, positive if buying, negative if selling
   * @param notional  the notional amount of one future contract
   * @param price  the trade price of the future
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public abstract IborFutureTrade createTrade(
      LocalDate tradeDate,
      SecurityId securityId,
      Period minimumPeriod,
      int sequenceNumber,
      double quantity,
      double notional,
      double price,
      ReferenceData refData);

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified year-month.
   * 
   * @param tradeDate  the trade date
   * @param securityId  the identifier of the security
   * @param yearMonth  the year-month that the future is defined to be for
   * @param quantity  the number of contracts traded, positive if buying, negative if selling
   * @param notional  the notional amount of one future contract
   * @param price  the trade price of the future
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public abstract IborFutureTrade createTrade(
      LocalDate tradeDate,
      SecurityId securityId,
      YearMonth yearMonth,
      double quantity,
      double notional,
      double price,
      ReferenceData refData);

  //-------------------------------------------------------------------------
  /**
   * Calculates the reference date from the trade date.
   * <p>
   * This determines the date from the specified minimum period and sequence number.
   * 
   * @param tradeDate  the trade date
   * @param minimumPeriod  minimum period between the trade date and the first future
   * @param sequenceNumber  the 1-based sequence number of the futures
   * @param refData  the reference data, used to resolve the date
   * @return the future reference date
   */
  public abstract LocalDate calculateReferenceDateFromTradeDate(
      LocalDate tradeDate,
      Period minimumPeriod,
      int sequenceNumber,
      ReferenceData refData);

  /**
   * Calculates the reference date from the trade date.
   * <p>
   * This determines the date from the specified year-month.
   * 
   * @param tradeDate  the trade date
   * @param yearMonth  the year-month that the future is defined to be for
   * @param refData  the reference data, used to resolve the date
   * @return the future reference date
   */
  public abstract LocalDate calculateReferenceDateFromTradeDate(
      LocalDate tradeDate,
      YearMonth yearMonth,
      ReferenceData refData);

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
