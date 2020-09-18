/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import java.time.LocalDate;
import java.time.YearMonth;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.date.SequenceDate;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.index.OvernightFuturePosition;
import com.opengamma.strata.product.index.OvernightFutureTrade;

/**
 * A contract specification for exchange traded Overnight Futures.
 * <p>
 * The contract specification defines how the future is traded.
 * A specific future is created by specifying the year-month.
 * <p>
 * For commonly traded contract specifications, see {@link OvernightFutureContractSpecs}.
 * To manually create a contract specification, see {@link ImmutableOvernightFutureContractSpec}.
 * To register a specific contract specification, see {@code OvernightFutureContractSpec.ini}.
 */
public interface OvernightFutureContractSpec
    extends Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static OvernightFutureContractSpec of(String uniqueName) {
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
  public static ExtendedEnum<OvernightFutureContractSpec> extendedEnum() {
    return OvernightFutureContractSpecs.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the Overnight index.
   * <p>
   * The floating rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-SONIA'.
   * 
   * @return the index
   */
  public abstract OvernightIndex getIndex();

  /**
   * Gets the notional.
   * <p>
   * The notional is a positive number in the index currency.
   * 
   * @return the notional
   */
  public abstract double getNotional();

  //-------------------------------------------------------------------------
  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the instructions in the {@link SequenceDate}.
   * The sequence date points at the expiry of the future, which is how they are referred to in the market.
   * 
   * @param tradeDate  the trade date
   * @param securityId  the identifier of the security
   * @param sequenceDate  the date to be used from the sequence identifying the expiry of the future
   * @param quantity  the number of contracts traded, positive if buying, negative if selling
   * @param price  the trade price of the future
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public abstract OvernightFutureTrade createTrade(
      LocalDate tradeDate,
      SecurityId securityId,
      SequenceDate sequenceDate,
      double quantity,
      double price,
      ReferenceData refData);

  //-------------------------------------------------------------------------
  /**
   * Creates a position based on this convention.
   *
   * @param securityId  the identifier of the security
   * @param expiry  the expiry year month
   * @param quantity  the number of contracts traded, positive if buying, negative if selling
   * @param refData  the reference data, used to resolve the trade dates
   * @return the position
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public abstract OvernightFuturePosition createPosition(
      SecurityId securityId,
      YearMonth expiry,
      double quantity,
      ReferenceData refData);

  //-------------------------------------------------------------------------
  /**
   * Calculates the reference date from the trade date.
   * <p>
   * This determines the date from the {@link SequenceDate}.
   * 
   * @param tradeDate  the trade date
   * @param sequenceDate  the date to be used from the sequence 
   * @param refData  the reference data, used to resolve the date
   * @return the reference date of the contract, also known as the start date
   */
  public abstract LocalDate calculateReferenceDate(
      LocalDate tradeDate,
      SequenceDate sequenceDate,
      ReferenceData refData);

  //-------------------------------------------------------------------------
  /**
   * Calculates the last fixing date from the trade date.
   * 
   * @param referenceDate  the reference date of the contract, also known as the start date
   * @param refData  the reference data, used to resolve the date
   * @return the last fixing date
   */
  public abstract LocalDate calculateLastFixingDate(LocalDate referenceDate, ReferenceData refData);

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
