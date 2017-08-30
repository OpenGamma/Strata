/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.util.Optional;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An index used to provide floating rates, typically in interest rate swaps.
 * <p>
 * See {@link IborIndex}, {@link OvernightIndex} and  {@link PriceIndex} for more details.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface FloatingRateIndex
    extends Index {

  /**
   * Parses a string, handling different types of index.
   * <p>
   * This tries a number of ways to parse the input:
   * <ul>
   * <li>{@link IborIndex#of(String)}
   * <li>{@link OvernightIndex#of(String)}
   * <li>{@link PriceIndex#of(String)}
   * <li>{@link FloatingRateName#of(String)}
   * </ul>
   * If {@code FloatingRateName} is used to match an Ibor index, then a tenor is needed
   * to return an index. The tenor from {@link FloatingRateName#getDefaultTenor()} will be used.
   * 
   * @param indexStr  the index string to parse
   * @return the floating rate
   * @throws IllegalArgumentException if the name is not known
   */
  public static FloatingRateIndex parse(String indexStr) {
    return parse(indexStr, null);
  }

  /**
   * Parses a string, handling different types of index, optionally specifying a tenor for Ibor.
   * <p>
   * This tries a number of ways to parse the input:
   * <ul>
   * <li>{@link IborIndex#of(String)}
   * <li>{@link OvernightIndex#of(String)}
   * <li>{@link PriceIndex#of(String)}
   * <li>{@link FloatingRateName#of(String)}
   * </ul>
   * If {@code FloatingRateName} is used to match an Ibor index, then a tenor is needed
   * to return an index. The tenor can optionally be supplied. If needed and missing,
   * the result of {@link FloatingRateName#getDefaultTenor()} will be used.
   * 
   * @param indexStr  the index string to parse
   * @param defaultIborTenor  the tenor to use for Ibor if matched as a {@code FloatingRateName}, may be null
   * @return the floating rate
   * @throws IllegalArgumentException if the name is not known
   */
  public static FloatingRateIndex parse(String indexStr, Tenor defaultIborTenor) {
    ArgChecker.notNull(indexStr, "indexStr");
    Optional<IborIndex> iborOpt = IborIndex.extendedEnum().find(indexStr);
    if (iborOpt.isPresent()) {
      return iborOpt.get();
    }
    Optional<OvernightIndex> overnightOpt = OvernightIndex.extendedEnum().find(indexStr);
    if (overnightOpt.isPresent()) {
      return overnightOpt.get();
    }
    Optional<PriceIndex> priceOpt = PriceIndex.extendedEnum().find(indexStr);
    if (priceOpt.isPresent()) {
      return priceOpt.get();
    }
    Optional<FloatingRateName> frnOpt = FloatingRateName.extendedEnum().find(indexStr);
    if (frnOpt.isPresent()) {
      FloatingRateName frn = frnOpt.get();
      return frn.toFloatingRateIndex(defaultIborTenor != null ? defaultIborTenor : frn.getDefaultTenor());
    }
    throw new IllegalArgumentException("Floating rate index not known: " + indexStr);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency of the index.
   * 
   * @return the currency of the index
   */
  public abstract Currency getCurrency();

  /**
   * Gets whether the index is active.
   * <p>
   * Over time some indices become inactive and are no longer produced.
   * If this occurs, this method will return false.
   * 
   * @return true if the index is active, false if inactive
   */
  public abstract boolean isActive();

  /**
   * Gets the day count convention of the index.
   * 
   * @return the day count convention
   */
  public abstract DayCount getDayCount();

  /**
   * Gets the floating rate name for this index.
   * <p>
   * For an Ibor index, the {@link FloatingRateName} does not include the tenor.
   * It can be used to find the other tenors available for this index.
   * 
   * @return the floating rate name
   */
  public abstract FloatingRateName getFloatingRateName();

  /**
   * Gets the default day count convention for the associated fixed leg.
   * <p>
   * A rate index is often paid against a fixed leg, such as in a vanilla Swap.
   * The day count convention of the fixed leg often differs from that of the index,
   * and the default is value is available here.
   * 
   * @return the day count convention
   */
  public default DayCount getDefaultFixedLegDayCount() {
    return getDayCount();
  }

}
