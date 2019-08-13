/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.Messages;

/**
 * Additional attributes that can be associated with a model object.
 */
public interface Attributes {

  /**
   * Obtains an empty instance.
   * <p>
   * The {@link #withAttribute(AttributeType, Object)} method can be used on
   * the instance to add attributes.
   * 
   * @return the empty instance
   */
  public static Attributes empty() {
    return SimpleAttributes.EMPTY;
  }

  /**
   * Obtains an instance with a single attribute.
   * <p>
   * The {@link #withAttribute(AttributeType, Object)} method can be used on
   * the instance to add more attributes.
   * 
   * @param <T>  the type of the attribute value
   * @param type  the type providing meaning to the value
   * @param value  the value
   * @return the instance
   */
  public static <T> Attributes of(AttributeType<T> type, T value) {
    return new SimpleAttributes(ImmutableMap.of(type, type.toStoredForm(value)));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the attribute types that are available.
   * <p>
   * See {@link AttributeType#captureWildcard()} for a way to capture the wildcard type.
   * <p>
   * The default implementation returns an empty set (backwards compatibility prevents an abstract method for now).
   * 
   * @return the attribute types
   */
  public default ImmutableSet<AttributeType<?>> getAttributeTypes() {
    return ImmutableSet.of(); // TODO: Remove default in Strata v3
  }

  /**
   * Gets the attribute associated with the specified type.
   * <p>
   * This method obtains the specified attribute.
   * This allows an attribute to be obtained if available.
   * <p>
   * If the attribute is not found, an exception is thrown.
   * 
   * @param <T>  the type of the attribute value
   * @param type  the type to find
   * @return the attribute value
   * @throws IllegalArgumentException if the attribute is not found
   */
  public default <T> T getAttribute(AttributeType<T> type) {
    return findAttribute(type).orElseThrow(() -> new IllegalArgumentException(
        Messages.format("Attribute not found for type '{}'", type)));
  }

  /**
   * Determines if an attribute associated with the specified type is present.
   *
   * @param <T>  the type of the attribute value
   * @param type  the type to find
   * @return true if a matching attribute is present
   */
  public default <T> boolean containsAttribute(AttributeType<T> type) {
    return findAttribute(type).isPresent();
  }

  /**
   * Finds the attribute associated with the specified type.
   * <p>
   * This method obtains the specified attribute.
   * This allows an attribute to be obtained if available.
   * <p>
   * If the attribute is not found, optional empty is returned.
   * 
   * @param <T>  the type of the result
   * @param type  the type to find
   * @return the attribute value
   */
  public abstract <T> Optional<T> findAttribute(AttributeType<T> type);

  /**
   * Returns a copy of this instance with the attribute added.
   * <p>
   * This returns a new instance with the specified attribute added.
   * The attribute is added using {@code Map.put(type, value)} semantics.
   * 
   * @param <T>  the type of the attribute value
   * @param type  the type providing meaning to the value
   * @param value  the value
   * @return a new instance based on this one with the attribute added
   */
  public abstract <T> Attributes withAttribute(AttributeType<T> type, T value);

}
