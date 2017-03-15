/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeTemplate;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * A template for creating an Ibor Future trade.
 */
public interface IborFutureTemplate
    extends TradeTemplate {

  /**
   * Obtains a template based on the specified convention using a relative definition of time.
   * <p>
   * The specific future is defined by two date-related inputs, the minimum period and the 1-based future number.
   * For example, the 2nd future of the series where the 1st future is at least 1 week after the value date
   * would be represented by a minimum period of 1 week and future number 2.
   * 
   * @param minimumPeriod  the minimum period between the base date and the first future
   * @param sequenceNumber  the 1-based index of the future after the minimum period, must be 1 or greater
   * @param convention  the future convention
   * @return the template
   */
  public static IborFutureTemplate of(Period minimumPeriod, int sequenceNumber, IborFutureConvention convention) {
    return RelativeIborFutureTemplate.of(minimumPeriod, sequenceNumber, convention);
  }

  /**
   * Obtains a template based on the specified convention using an absolute definition of time.
   * <p>
   * The future is selected from a sequence of futures based on a year-month.
   * In most cases, the date of the future will be in the same month as the specified month,
   * but this is not guaranteed.
   * 
   * @param yearMonth  the year-month to use to select the future
   * @param convention  the future convention
   * @return the template
   */
  public static IborFutureTemplate of(YearMonth yearMonth, IborFutureConvention convention) {
    return AbsoluteIborFutureTemplate.of(yearMonth, convention);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying index.
   * 
   * @return the index
   */
  public abstract IborIndex getIndex();

  /**
   * Creates a trade based on this template.
   * <p>
   * This returns a trade based on the specified date.
   * The notional is unsigned, with the quantity determining the direction of the trade.
   * 
   * @param tradeDate  the date of the trade
   * @param securityId  the identifier of the security
   * @param quantity  the number of contracts traded, positive if buying, negative if selling
   * @param notional  the notional amount of one future contract
   * @param price  the trade price
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public abstract IborFutureTrade createTrade(
      LocalDate tradeDate,
      SecurityId securityId,
      double quantity,
      double notional,
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

  //-------------------------------------------------------------------------
  /**
   * Calculates the approximate maturity from the trade date.
   * <p>
   * This returns a year fraction that estimates the time to maturity.
   * For example, this might take the number of months between the trade date
   * and the date of the end of the future and divide it by 12.
   * 
   * @param tradeDate  the trade date
   * @return the approximate time to maturity
   */
  public abstract double approximateMaturity(LocalDate tradeDate);

}
