/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.TypedString;

/**
 * The type that provides meaning to a trade attribute.
 * <p>
 * Trade attributes, stored on {@link TradeInfo}, provide the ability to
 * associate arbitrary information with a trade in a key-value map.
 * For example, it might be used to provide information about the trading platform.
 * <p>
 * Applications that wish to use trade attributes should declare a static
 * constant declaring the {@code TradeAttributeType} instance, the type parameter
 * and a lowerCamelCase name. For example:
 * <pre>
 *  public static final TradeAttributeType&lt;String&gt; DEALER = TradeAttributeType.of("dealer");
 * </pre>
 * 
 * @param <T>  the type of the attribute value
 */
public final class TradeAttributeType<T>
    extends TypedString<TradeAttributeType<T>> {

  /**
   * Key used to access the description of the trade.
   */
  public static final TradeAttributeType<String> DESCRIPTION = TradeAttributeType.of("description");

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * The name may contain any character, but must not be empty.
   *
   * @param <T>  the type associated with the info
   * @param name  the name
   * @return a type instance with the specified name
   */
  @FromString
  public static <T> TradeAttributeType<T> of(String name) {
    return new TradeAttributeType<T>(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name
   */
  private TradeAttributeType(String name) {
    super(name);
  }

}
