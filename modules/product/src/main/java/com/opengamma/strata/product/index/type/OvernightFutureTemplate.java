/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeTemplate;
import com.opengamma.strata.product.index.OvernightFutureTrade;

/**
 * A template for creating an Overnight Future trade.
 */
public interface OvernightFutureTemplate
    extends TradeTemplate {

  /**
   * Obtains a template based on the specified contract specification using a relative definition of time.
   * <p>
   * The specific future is defined by two date-related inputs, the minimum period and the 1-based future number.
   * For example, the 2nd future of the series where the 1st future is at least 1 week after the value date
   * would be represented by a minimum period of 1 week and future number 2.
   * 
   * @param minimumPeriod  the minimum period between the base date and the first future
   * @param sequenceNumber  the 1-based index of the future after the minimum period, must be 1 or greater
   * @param contractSpec  the contract specification
   * @return the template
   */
  public static OvernightFutureTemplate of(Period minimumPeriod, int sequenceNumber, OvernightFutureContractSpec contractSpec) {
    return RelativeOvernightFutureTemplate.of(minimumPeriod, sequenceNumber, contractSpec);
  }

  /**
   * Obtains a template based on the specified convention using an absolute definition of time.
   * <p>
   * The future is selected from a sequence of futures based on a year-month.
   * In most cases, the date of the future will be in the same month as the specified month,
   * but this is not guaranteed.
   * 
   * @param yearMonth  the year-month to use to select the future
   * @param contractSpec  the contract specification
   * @return the template
   */
  public static OvernightFutureTemplate of(YearMonth yearMonth, OvernightFutureContractSpec contractSpec) {
    return AbsoluteOvernightFutureTemplate.of(yearMonth, contractSpec);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying index.
   * 
   * @return the index
   */
  public abstract OvernightIndex getIndex();

  /**
   * Gets the contract specification of the Overnight future.
   * 
   * @return the convention
   */
  public abstract OvernightFutureContractSpec getContractSpec();

  /**
   * Creates a trade based on this template.
   * <p>
   * This returns a trade based on the specified date.
   * 
   * @param tradeDate  the date of the trade
   * @param securityId  the identifier of the security
   * @param quantity  the number of contracts traded, positive if buying, negative if selling
   * @param price  the trade price
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public abstract OvernightFutureTrade createTrade(
      LocalDate tradeDate,
      SecurityId securityId,
      double quantity,
      double price,
      ReferenceData refData);

  /**
   * Calculates the reference date of the trade.
   * 
   * @param tradeDate  the date of the trade
   * @param refData  the reference data, used to resolve the date
   * @return the future reference date
   */
  public abstract LocalDate calculateReferenceDateFromTradeDate(LocalDate tradeDate, ReferenceData refData);

}
