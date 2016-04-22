/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.TypedString;

/**
 * The type that provides meaning to a security attribute.
 * <p>
 * Security attributes, stored on {@link SecurityInfo}, provide the ability to
 * associate arbitrary information with a security in a key-value map.
 * For example, it might be used to provide information about the trading platform.
 * <p>
 * Applications that wish to use security attributes should declare a static
 * constant declaring the {@code SecurityAttributeType} instance, the type parameter
 * and a lowerCamelCase name. For example:
 * <pre>
 *  public static final SecurityAttributeType&lt;String&gt; EXCHANGE = SecurityAttributeType.of("exchange");
 * </pre>
 * 
 * @param <T>  the type of the attribute value
 */
public final class SecurityAttributeType<T>
    extends TypedString<SecurityAttributeType<T>> {

  /**
   * Key used to access the name of the security.
   */
  public static final SecurityAttributeType<String> NAME = SecurityAttributeType.of("name");

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
  public static <T> SecurityAttributeType<T> of(String name) {
    return new SecurityAttributeType<T>(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name
   */
  private SecurityAttributeType(String name) {
    super(name);
  }

}
