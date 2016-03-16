/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

/**
 * A additional piece of security information.
 * <p>
 * An instance of this class can be obtained by called {@link SecurityInfoType#value(Object)}.
 * 
 * @param <T> the type of the value
 */
public final class SecurityInfoValue<T> {

  /**
   * The type providing meaning to the value.
   */
  private final SecurityInfoType<T> type;
  /**
   * The value.
   */
  private final T value;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the identifier.
   * 
   * @param <T> the type of the value
   * @param type  the type providing meaning to the value
   * @param value  the value
   * @return the information value
   */
  public static <T> SecurityInfoValue<T> of(SecurityInfoType<T> type, T value) {
    return new SecurityInfoValue<T>(type, value);
  }

  // creates an instance
  private SecurityInfoValue(SecurityInfoType<T> type, T value) {
    this.type = type;
    this.value = value;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type providing meaning to the value.
   * 
   * @return the type
   */
  public SecurityInfoType<T> getType() {
    return type;
  }

  /**
   * Gets the value.
   * 
   * @return the value
   */
  public T getValue() {
    return value;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof SecurityInfoValue<?>) {
      SecurityInfoValue<?> other = (SecurityInfoValue<?>) obj;
      return type.equals(other.type) && value.equals(other.value);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return type.hashCode() ^ value.hashCode();
  }

  @Override
  public String toString() {
    return type + ":" + value;
  }

}
