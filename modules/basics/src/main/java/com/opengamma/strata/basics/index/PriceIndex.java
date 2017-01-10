/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.location.Country;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;

/**
 * An index of prices.
 * <p>
 * A price index is a normalized average of the prices of goods and/or services.
 * Well-known price indices are published by Governments, such as the Consumer Price Index.
 * The annualized percentage change in the index is a measure of inflation.
 * <p>
 * This interface represents a price index for a specific region.
 * The index is typically published monthly in arrears, however some regions
 * choose quarterly publication.
 * <p>
 * The most common implementations are provided in {@link PriceIndices}.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface PriceIndex
    extends Index, Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static PriceIndex of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the index to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<PriceIndex> extendedEnum() {
    return PriceIndices.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the region that the index is defined for.
   * 
   * @return the region of the index
   */
  public abstract Country getRegion();

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
   * Gets the frequency that the index is published.
   * <p>
   * Most price indices are published monthly, but some are published quarterly.
   * 
   * @return the frequency of publication of the index
   */
  public abstract Frequency getPublicationFrequency();

  /**
   * Gets the floating rate name for this index.
   * 
   * @return the floating rate name
   */
  public abstract FloatingRateName getFloatingRateName();

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

}
