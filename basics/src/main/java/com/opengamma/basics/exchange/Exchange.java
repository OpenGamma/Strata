/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.exchange;

import java.time.ZoneId;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.basics.location.Country;
import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.named.ExtendedEnum;
import com.opengamma.collect.named.Named;

/**
 * A financial exchange, providing a marketplace for trading financial instruments.
 * <p>
 * Many financial products are traded on a specific exchange.
 * Implementations of this interface define these exchanges.
 * <p>
 * The most common implementations are provided in {@link Exchanges}.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface Exchange
    extends Named {

  /**
   * Obtains a {@code RateIndex} from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the rate index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static Exchange of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of {@code RateIndex} to be lookup up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<Exchange> extendedEnum() {
    return Exchanges.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the country of the exchange.
   * 
   * @return the country of the exchange
   */
  public abstract Country getCountry();

  /**
   * Gets the time-zone of the exchange.
   * 
   * @return the time-zone of the exchange
   */
  public abstract ZoneId getTimeZone();

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
  public String getName();

}
