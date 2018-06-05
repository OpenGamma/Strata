/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.TypedString;

/**
 * The type that provides meaning to an attribute.
 * <p>
 * Attributes provide the ability to associate arbitrary information with the trade model in a key-value map.
 * For example, it might be used to provide information about the trading platform.
 * <p>
 * Applications that wish to use attributes should declare a static constant declaring the
 * {@code AttributeType} instance, the type parameter and a lowerCamelCase name. For example:
 * <pre>
 *  public static final AttributeType&lt;String&gt; DEALER = AttributeType.of("dealer");
 * </pre>
 * 
 * @param <T>  the type of the attribute value
 */
public final class AttributeType<T>
    extends TypedString<AttributeType<T>> {

  /**
   * Key used to access the description.
   */
  public static final AttributeType<String> DESCRIPTION = AttributeType.of("description");
  /**
   * Key used to access the name.
   */
  public static final AttributeType<String> NAME = AttributeType.of("name");

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
  public static <T> AttributeType<T> of(String name) {
    return new AttributeType<T>(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name
   */
  private AttributeType(String name) {
    super(name);
  }

}
