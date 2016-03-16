/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.type.TypedString;

/**
 * The type of additional security information.
 * <p>
 * This extension point allow arbitrary information to be associated with a security.
 * For example, it might be used to provide information about the trading platform.
 * <p>
 * Applications that wish to use security information should declare a static
 * constant declaring the {@code SecurityInfoType} instance, the type parameter
 * and an UpperCamelCase name.
 * 
 * @param <T>  the type associated with the info
 */
public final class SecurityInfoType<T>
    extends TypedString<SecurityInfoType<T>> {

  /**
   * Key used to access the name of the security.
   */
  public static final SecurityInfoType<String> NAME = SecurityInfoType.of("Name");

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
  public static <T> SecurityInfoType<T> of(String name) {
    return new SecurityInfoType<T>(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name
   */
  private SecurityInfoType(String name) {
    super(name);
  }

}
