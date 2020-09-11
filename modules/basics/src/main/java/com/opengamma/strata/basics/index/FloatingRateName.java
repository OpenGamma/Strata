/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.util.Optional;
import java.util.Set;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.collect.Iterables;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;

/**
 * A floating rate index name, such as Libor, Euribor or US Fed Fund.
 * <p>
 * An index represented by this class relates to some form of floating rate.
 * This can include {@link IborIndex} and {@link OvernightIndex} values.
 * <p>
 * This class is designed to match the FpML/ISDA floating rate index concept.
 * The FpML concept provides a single key for floating rates of a variety of
 * types, mixing  Ibor, Overnight, Price and Swap indices.
 * It also sometimes includes a source, such as 'Bloomberg' or 'Reuters'.
 * This class matches the single concept and provided a bridge the more
 * specific index implementations used for pricing.
 * <p>
 * The most common implementations are provided in {@link FloatingRateNames}.
 * <p>
 * The set of supported values, and their mapping to {@code IborIndex}, {@code PriceIndex}
 * and {@code OvernightIndex}, is defined in the {@code FloatingRateName.ini}
 * config file.
 */
public interface FloatingRateName
    extends FloatingRate, Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the floating rate
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FloatingRateName of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the floating rate to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<FloatingRateName> extendedEnum() {
    return FloatingRateNames.ENUM_LOOKUP;
  }

  /**
   * Parses a string, with extended handling of indices.
   * <p>
   * This tries a number of ways to parse the input:
   * <ul>
   * <li>{@link FloatingRateName#of(String)}
   * <li>{@link IborIndex#of(String)}
   * <li>{@link OvernightIndex#of(String)}
   * <li>{@link PriceIndex#of(String)}
   * </ul>
   * Note that if an {@link IborIndex} is parsed, the tenor will be lost.
   * 
   * @param str  the string to parse
   * @return the floating rate
   * @throws IllegalArgumentException if the name is not known
   */
  public static FloatingRateName parse(String str) {
    ArgChecker.notNull(str, "str");
    return tryParse(str).orElseThrow(
        () -> new IllegalArgumentException("Floating rate name not known: " + str));
  }

  /**
   * Tries to parse a string, with extended handling of indices.
   * <p>
   * This tries a number of ways to parse the input:
   * <ul>
   * <li>{@link FloatingRateName#of(String)}
   * <li>{@link IborIndex#of(String)}
   * <li>{@link OvernightIndex#of(String)}
   * <li>{@link PriceIndex#of(String)}
   * </ul>
   * Note that if an {@link IborIndex} is parsed, the tenor will be lost.
   * 
   * @param str  the string to parse
   * @return the floating rate, empty if not found
   */
  public static Optional<FloatingRateName> tryParse(String str) {
    Optional<FloatingRateName> frnOpt = FloatingRateName.extendedEnum().find(str);
    if (frnOpt.isPresent()) {
      return frnOpt;
    }
    return FloatingRateIndex.tryParse(str).map(FloatingRateIndex::getFloatingRateName);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the default Ibor index for a currency.
   * 
   * @param currency  the currency to find the default for
   * @return the floating rate
   * @throws IllegalArgumentException if there is no default for the currency
   */
  public static FloatingRateName defaultIborIndex(Currency currency) {
    return FloatingRateNameIniLookup.INSTANCE.defaultIborIndex(currency);
  }

  /**
   * Gets the default Overnight index for a currency.
   * 
   * @param currency  the currency to find the default for
   * @return the floating rate
   * @throws IllegalArgumentException if there is no default for the currency
   */
  public static FloatingRateName defaultOvernightIndex(Currency currency) {
    return FloatingRateNameIniLookup.INSTANCE.defaultOvernightIndex(currency);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this floating rate, such as 'GBP-LIBOR'.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the external name
   */
  @ToString
  @Override
  public abstract String getName();

  /**
   * Gets the type of the index - Ibor, Overnight or Price.
   * 
   * @return index type - Ibor, Overnight or Price
   */
  public abstract FloatingRateType getType();

  /**
   * Gets the currency of the floating rate.
   * 
   * @return the currency
   * @throws IllegalArgumentException if unable to return an index, which should
   *   only happen if the system is not configured correctly
   */
  @Override
  public default Currency getCurrency() {
    return toFloatingRateIndex().getCurrency();
  }

  /**
   * Gets the active tenors that are applicable for this floating rate.
   * <p>
   * Overnight and Price indices will return an empty set.
   * 
   * @return the available tenors
   */
  public abstract Set<Tenor> getTenors();

  /**
   * Gets a default tenor applicable for this floating rate.
   * <p>
   * This is useful for providing a basic default where errors need to be avoided.
   * The value returned is not intended to be based on market conventions.
   * <p>
   * Ibor floating rates will return 3M, or 13W if that is not available, otherwise
   * the first entry from the set of tenors.
   * Overnight floating rates will return 1D.
   * All other floating rates will return return 1Y.
   * 
   * @return the default tenor
   */
  public default Tenor getDefaultTenor() {
    switch (getType()) {
      case IBOR: {
        Set<Tenor> tenors = getTenors();
        if (tenors.contains(Tenor.TENOR_3M)) {
          return Tenor.TENOR_3M;
        }
        if (tenors.contains(Tenor.TENOR_13W)) {
          return Tenor.TENOR_13W;
        }
        return tenors.iterator().next();
      }
      case OVERNIGHT_AVERAGED:
      case OVERNIGHT_COMPOUNDED:
        return Tenor.TENOR_1D;
      default:
        return Tenor.TENOR_1Y;
    }
  }

  /**
   * Gets the normalized form of the floating rate name.
   * <p>
   * The normalized for is the name that Strata uses for the index.
   * For example, the normalized form of 'GBP-LIBOR-BBA' is 'GBP-LIBOR',
   * and the normalized form of 'EUR-EURIBOR-Reuters' is 'EUR-EURIBOR'.
   * Note that for Ibor indices, the tenor is not present.
   * 
   * @return the normalized name
   */
  public abstract FloatingRateName normalized();

  //-------------------------------------------------------------------------
  /**
   * Returns a floating rate index.
   * <p>
   * Returns a {@link FloatingRateIndex} for this rate name.
   * Only Ibor, Overnight and Price indices are handled.
   * If the rate name is an Ibor rate, the {@linkplain #getDefaultTenor() default tenor} is used.
   * 
   * @return the index
   * @throws IllegalArgumentException if unable to return an index, which should
   *   only happen if the system is not configured correctly
   */
  public default FloatingRateIndex toFloatingRateIndex() {
    // code copied to avoid calling getDefaultTenor() unless necessary
    switch (getType()) {
      case IBOR:
        return toIborIndex(getDefaultTenor());
      case OVERNIGHT_COMPOUNDED:
      case OVERNIGHT_AVERAGED:
        return toOvernightIndex();
      case PRICE:
        return toPriceIndex();
      default:
        throw new IllegalArgumentException("Floating rate index type not known: " + getType());
    }
  }

  /**
   * Returns a floating rate index.
   * <p>
   * Returns a {@link FloatingRateIndex} for this rate name.
   * Only Ibor, Overnight and Price indices are handled.
   * If the rate name is an Ibor rate, the specified tenor is used.
   * 
   * @param iborTenor  the tenor to use if this rate is Ibor
   * @return the index
   * @throws IllegalArgumentException if unable to return an index, which should
   *   only happen if the system is not configured correctly
   */
  public default FloatingRateIndex toFloatingRateIndex(Tenor iborTenor) {
    switch (getType()) {
      case IBOR:
        return toIborIndex(iborTenor);
      case OVERNIGHT_COMPOUNDED:
      case OVERNIGHT_AVERAGED:
        return toOvernightIndex();
      case PRICE:
        return toPriceIndex();
      default:
        throw new IllegalArgumentException("Floating rate index type not known: " + getType());
    }
  }

  /**
   * Checks and returns an Ibor index.
   * <p>
   * If this name represents an Ibor index, then this method returns the matching {@link IborIndex}.
   * If not, an exception is thrown.
   * 
   * @param tenor  the tenor of the index
   * @return the index
   * @throws IllegalStateException if the type is not an Ibor index type
   */
  public abstract IborIndex toIborIndex(Tenor tenor);

  /**
   * Checks and returns the fixing offset associated with the Ibor index.
   * <p>
   * If this name represents an Ibor index, then this method returns the associated fixing offset.
   * If not, an exception is thrown.
   * <p>
   * This method exists primarily to handle DKK CIBOR, where there are two floating rate names but
   * only one underlying index. The CIBOR index itself has a convention where the fixing date is 2 days
   * before the reset date and the effective date is 2 days after the fixing date, matching the name "DKK-CIBOR2-DKNA13".
   * The alternative name, "DKK-CIBOR-DKNA13", has the fixing date equal to the reset date, but with
   * the effective date two days later.
   * 
   * @return the fixing offset applicable to the index
   * @throws IllegalStateException if the type is not an Ibor index type
   */
  public default DaysAdjustment toIborIndexFixingOffset() {
    return toIborIndex(Iterables.getFirst(getTenors(), Tenor.TENOR_3M)).getFixingDateOffset();
  }

  /**
   * Converts to an {@link OvernightIndex}.
   * <p>
   * If this name represents an Overnight index, then this method returns the matching {@link OvernightIndex}.
   * If not, an exception is thrown.
   * 
   * @return the index
   * @throws IllegalStateException if the type is not an Overnight index type
   */
  public abstract OvernightIndex toOvernightIndex();

  /**
   * Converts to an {@link PriceIndex}.
   * <p>
   * If this name represents a price index, then this method returns the matching {@link PriceIndex}.
   * If not, an exception is thrown.
   *
   * @return the index
   * @throws IllegalStateException if the type is not a price index type
   */
  public abstract PriceIndex toPriceIndex();

}
