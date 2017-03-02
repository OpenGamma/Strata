/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx.type;

import java.time.LocalDate;
import java.time.Period;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.TradeConvention;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.fx.FxSwapTrade;

/**
 * A market convention for FX Swap trades.
 * <p>
 * This defines the market convention for a FX swap based on a particular currency pair.
 * <p>
 * To manually create a convention, see {@link ImmutableFxSwapConvention}.
 * To register a specific convention, see {@code FxSwapConvention.ini}.
 */
public interface FxSwapConvention
    extends TradeConvention, Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the convention
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FxSwapConvention of(String uniqueName) {
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
  public static ExtendedEnum<FxSwapConvention> extendedEnum() {
    return FxSwapConventions.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency pair of the convention.
   * 
   * @return the currency pair
   */
  public abstract CurrencyPair getCurrencyPair();

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
   * For example, a '3M x 6M' FX swap has a period from spot to the start date of 3 months and
   * a period from spot to the end date of 6 months
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the FX Swap, the amount in the first currency of the pair is received in the near leg and paid in the 
   * far leg, while the second currency is paid in the near leg and received in the far leg.
   * 
   * @param tradeDate  the date of the trade
   * @param periodToNear  the period between the spot date and the near date
   * @param periodToFar  the period between the spot date and the far date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the first currency of the currency pair
   * @param nearFxRate  the FX rate for the near leg
   * @param farLegForwardPoints  the FX points to be added to the FX rate at the far leg
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public default FxSwapTrade createTrade(
      LocalDate tradeDate,
      Period periodToNear,
      Period periodToFar,
      BuySell buySell,
      double notional,
      double nearFxRate,
      double farLegForwardPoints,
      ReferenceData refData) {

    LocalDate spotValue = calculateSpotDateFromTradeDate(tradeDate, refData);
    LocalDate startDate = spotValue.plus(periodToNear);
    LocalDate endDate = spotValue.plus(periodToFar);
    return toTrade(tradeDate, startDate, endDate, buySell, notional, nearFxRate, farLegForwardPoints);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the FX Swap, the amount in the first currency of the pair is received in the near leg and paid in the 
   * far leg, while the second currency is paid in the near leg and received in the far leg.
   * 
   * @param tradeDate  the date of the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param nearFxRate  the FX rate for the near leg
   * @param farLegForwardPoints  the FX points to be added to the FX rate at the far leg
   * @return the trade
   */
  public default FxSwapTrade toTrade(
      LocalDate tradeDate,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double nearFxRate,
      double farLegForwardPoints) {

    TradeInfo tradeInfo = TradeInfo.of(tradeDate);
    return toTrade(tradeInfo, startDate, endDate, buySell, notional, nearFxRate, farLegForwardPoints);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the FX Swap, the amount in the first currency of the pair is received in the near leg and paid in the 
   * far leg, while the second currency is paid in the near leg and received in the far leg.
   * 
   * @param tradeInfo  additional information about the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param nearFxRate  the FX rate for the near leg
   * @param farLegForwardPoints  the FX points to be added to the FX rate at the far leg
   * @return the trade
   */
  public abstract FxSwapTrade toTrade(
      TradeInfo tradeInfo,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double nearFxRate,
      double farLegForwardPoints);

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
