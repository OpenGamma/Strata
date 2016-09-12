/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.TypedString;

/**
 * The type that provides meaning to a position attribute.
 * <p>
 * Position attributes, stored on {@link PositionInfo}, provide the ability to
 * associate arbitrary information with a position in a key-value map.
 * For example, it might be used to provide information about the trading platform.
 * <p>
 * Applications that wish to use position attributes should declare a static
 * constant declaring the {@code PositionAttributeType} instance, the type parameter
 * and a lowerCamelCase name. For example:
 * <pre>
 *  public static final PositionAttributeType&lt;String&gt; DIVISION = PositionAttributeType.of("division");
 * </pre>
 * 
 * @param <T>  the type of the attribute value
 */
public final class PositionAttributeType<T>
    extends TypedString<PositionAttributeType<T>> {

  /**
   * Key used to access the description of the position.
   */
  public static final PositionAttributeType<String> DESCRIPTION = PositionAttributeType.of("description");

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
  public static <T> PositionAttributeType<T> of(String name) {
    return new PositionAttributeType<T>(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name
   */
  private PositionAttributeType(String name) {
    super(name);
  }

}
