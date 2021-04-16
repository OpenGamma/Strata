/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.util.Optional;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

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
    extends Index, FloatingRate {

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
    return tryParse(indexStr, defaultIborTenor)
        .orElseThrow(() -> new IllegalArgumentException("Floating rate index not known: " + indexStr));
  }

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
   * @return the floating rate index, empty if not found
   */
  public static Optional<FloatingRateIndex> tryParse(String indexStr) {
    return tryParse(indexStr, null);
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
   * @return the floating rate index, empty if not found
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static Optional<FloatingRateIndex> tryParse(String indexStr, Tenor defaultIborTenor) {
    // using a block lambda as code is too complex for a ternary and private interface methods are not available
    return FloatingRate.tryParse(indexStr)
        .map(fr -> {
          if (fr instanceof FloatingRateName) {
            FloatingRateName frName = (FloatingRateName) fr;
            return frName.toFloatingRateIndex(defaultIborTenor != null ? defaultIborTenor : frName.getDefaultTenor());
          } else {
            return (FloatingRateIndex) fr;
          }
        });
  }

  /**
   * Obtains an instance from the specified unique name.
   * <p>
   * This parses names from {@link IborIndex}, {@link OvernightIndex} and {@link PriceIndex}.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FloatingRateIndex of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return Indices.FLOATING_RATE_INDEX_LOOKUP.lookup(uniqueName);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this index.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

  /**
   * Gets the currency of the index.
   * 
   * @return the currency of the index
   */
  @Override
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
  @Override
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
