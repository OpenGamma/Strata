/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.util.Optional;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.Named;

/**
 * An index or group of indices used to provide floating rates, typically in interest rate swaps.
 * <p>
 * This provides an abstraction above {@link FloatingRateName} and {@link FloatingRateIndex}.
 * This allows code to work with a specific index, {@link IborIndex}, {@link OvernightIndex} and {@link PriceIndex},
 * or with the index group, represented by {@code FloatingRateName}, using {@code instanceof}
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface FloatingRate
    extends Named {

  /**
   * Parses a string, handling various different formats.
   * <p>
   * This tries a number of ways to parse the input:
   * <ul>
   * <li>{@link IborIndex#of(String)}
   * <li>{@link OvernightIndex#of(String)}
   * <li>{@link PriceIndex#of(String)}
   * <li>{@link FloatingRateName#of(String)}
   * </ul>
   * 
   * @param indexStr  the index string to parse
   * @return the floating rate
   * @throws IllegalArgumentException if the name is not known
   */
  public static FloatingRate parse(String indexStr) {
    ArgChecker.notNull(indexStr, "indexStr");
    return tryParse(indexStr)
        .orElseThrow(() -> new IllegalArgumentException("Floating rate index not known: " + indexStr));
  }

  /**
   * Parses a string, handling various different formats.
   * <p>
   * This tries a number of ways to parse the input:
   * <ul>
   * <li>{@link IborIndex#of(String)}
   * <li>{@link OvernightIndex#of(String)}
   * <li>{@link PriceIndex#of(String)}
   * <li>{@link FloatingRateName#of(String)}
   * </ul>
   * 
   * @param indexStr  the index string to parse
   * @return the floating rate index, empty if not found
   */
  public static Optional<FloatingRate> tryParse(String indexStr) {
    Optional<IborIndex> iborOpt = IborIndex.extendedEnum().find(indexStr);
    if (iborOpt.isPresent()) {
      return iborOpt.map(t -> t);
    }
    Optional<OvernightIndex> overnightOpt = OvernightIndex.extendedEnum().find(indexStr);
    if (overnightOpt.isPresent()) {
      return overnightOpt.map(t -> t);
    }
    Optional<PriceIndex> priceOpt = PriceIndex.extendedEnum().find(indexStr);
    if (priceOpt.isPresent()) {
      return priceOpt.map(t -> t);
    }
    Optional<FloatingRateName> frnOpt = FloatingRateName.extendedEnum().find(indexStr);
    if (frnOpt.isPresent()) {
      return frnOpt.map(t -> t);
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the associated currency.
   * 
   * @return the currency
   */
  public abstract Currency getCurrency();

  /**
   * Gets the associated floating rate name.
   * 
   * @return the floating rate name
   */
  public abstract FloatingRateName getFloatingRateName();

}
