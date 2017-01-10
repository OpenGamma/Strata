/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.util.Set;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.currency.Currency;
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
    extends Named {

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
   * Gets the name that uniquely identifies this index.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * It will be the external name, typically from FpML, such as 'GBP-LIBOR-BBA'.
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
   * Gets the active tenors that are applicable for this floating rate.
   * <p>
   * Overnight and Price indices will return an empty set.
   * 
   * @return the available tenors
   */
  public abstract Set<Tenor> getTenors();

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
