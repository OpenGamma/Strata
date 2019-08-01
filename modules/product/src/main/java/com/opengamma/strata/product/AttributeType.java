/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.joda.convert.FromString;
import org.joda.convert.StringConvert;
import org.joda.convert.ToString;
import org.joda.convert.TypedStringConverter;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.common.CcpId;

/**
 * The type that provides meaning to an attribute.
 * <p>
 * Attributes provide the ability to associate arbitrary information with the trade model in a key-value map.
 * For example, it might be used to provide information about the trading platform.
 * <p>
 * Applications that wish to use attributes should declare a static constant declaring the
 * {@code AttributeType} instance, the type parameter and a lowerCamelCase name. For example:
 * <pre>
 *  public static final AttributeType&lt;String&gt; DEALER = AttributeType.registerInstance("dealer", DealerId.class);
 * </pre>
 * <p>
 * The purpose of registering the type is to enable {@link #toStoredForm} and {@link #fromStoredForm}}.
 * If the type can be converted using Joda-Convert's basic converter, then the data will be stored as strings.
 * This allows the sender and receiver of a message to use a different Java type for the value.
 * 
 * @param <T>  the type of the attribute value
 */
public final class AttributeType<T>
    implements Named, Comparable<AttributeType<T>>, Serializable {

  /**
   * Cache of instances.
   */
  private static final ConcurrentMap<String, AttributeType<?>> INSTANCES = new ConcurrentHashMap<>();
  /**
   * Key used to access the description.
   */
  public static final AttributeType<String> DESCRIPTION = AttributeType.registerInstance("description", String.class);
  /**
   * Key used to access the name.
   */
  public static final AttributeType<String> NAME = AttributeType.registerInstance("name", String.class);
  /**
   * Key used to access the CCP.
   */
  public static final AttributeType<CcpId> CCP = AttributeType.registerInstance("ccp", CcpId.class);

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The name.
   */
  private final String name;
  /**
   * The type.
   */
  private final transient Class<T> type;
  /**
   * The converter to the stored form.
   */
  private final transient Function<T, Object> toStoredFormConverter;
  /**
   * The converter from the stored form.
   */
  private final transient Function<Object, T> fromStoredFormConverter;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name, which should be pre-registered.
   * <p>
   * The name may contain any character, but must not be empty.
   *
   * @param <T>  the type associated with the info
   * @param name  the name
   * @return an instance with the specified name
   * @throws IllegalArgumentException if the instance is not registered
   */
  @SuppressWarnings("unchecked")
  @FromString
  public static <T> AttributeType<T> of(String name) {
    ArgChecker.notEmpty(name, "name");
    return (AttributeType<T>) INSTANCES.computeIfAbsent(name, key -> new AttributeType<T>(key));
  }

  /**
   * Registers an instance for the specified name and type.
   * <p>
   * The name may contain any character, but must not be empty.
   * <p>
   * Aliases can be specified if desired.
   * This is intended to handle the situation where an attribute type is renamed.
   * The old name is the alias, whereas the new name is the main name.
   *
   * @param <T>  the type associated with the info
   * @param name  the name
   * @param type  the type of the value
   * @param aliases  the aliases to register under
   * @return a type instance with the specified name
   */
  public static <T> AttributeType<T> registerInstance(String name, Class<T> type, String... aliases) {
    ArgChecker.notEmpty(name, "name");
    ArgChecker.notNull(type, "type");
    ArgChecker.notNull(aliases, "aliases");
    AttributeType<T> instance = new AttributeType<>(name, type);
    INSTANCES.put(name, instance);
    for (String alias : aliases) {
      ArgChecker.notNull(alias, "alias");
      INSTANCES.put(alias, instance);
    }
    return instance;
  }

  //-------------------------------------------------------------------------
  // creates an instance
  @SuppressWarnings("unchecked")
  private AttributeType(String name) {
    this.name = ArgChecker.notEmpty(name, "name");
    this.type = null;
    this.toStoredFormConverter = value -> value;
    this.fromStoredFormConverter = value -> (T) value;
  }

  // creates an instance
  @SuppressWarnings("unchecked")
  private AttributeType(String name, Class<T> type) {
    this.name = ArgChecker.notEmpty(name, "name");
    this.type = ArgChecker.notNull(type, "type");
    if (StringConvert.INSTANCE.isConvertible(type)) {
      TypedStringConverter<T> converter = StringConvert.INSTANCE.findTypedConverter(type);
      this.toStoredFormConverter = value -> converter.convertToString(value);
      this.fromStoredFormConverter =
          value -> value instanceof String ?
              converter.convertFromString(type, (String) value) :
              converter.convertFromString(type, StringConvert.INSTANCE.convertToString(value));
    } else {
      this.toStoredFormConverter = value -> value;
      this.fromStoredFormConverter = value -> type.cast(value);
    }
  }

  // resolve after deserialization
  private Object readResolve() {
    ArgChecker.notEmpty(name, "name");
    return of(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the name.
   * 
   * @return the name
   */
  @Override
  public String getName() {
    return name;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the normalized form of the attribute type.
   * <p>
   * This handles situations where the {@code AttributeType} instance was created before
   * the constant was registered.
   * 
   * @return the normalized form
   */
  public AttributeType<T> normalized() {
    return type == null ? of(name) : this;
  }

  /**
   * Converts the value to the stored form.
   * <p>
   * This method is not for general use. Only implementations of {@link Attributes} should use it.
   * 
   * @param value  the value, may be null
   * @return the stored form of the value, may be null
   */
  public Object toStoredForm(T value) {
    if (value == null) {
      return null;
    }
    return normalized().toStoredFormConverter.apply(value);
  }

  /**
   * Converts from the stored form.
   * <p>
   * This method is not for general use. Only implementations of {@link Attributes} should use it.
   * 
   * @param storedValue  the stored value, may be null
   * @return the converted value, may be null
   */
  public T fromStoredForm(Object storedValue) {
    if (storedValue == null) {
      return null;
    }
    return normalized().fromStoredFormConverter.apply(storedValue);
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this type to another.
   * <p>
   * Instances are compared in alphabetical order based on the name.
   * 
   * @param other  the object to compare to
   * @return the comparison
   */
  @Override
  public final int compareTo(AttributeType<T> other) {
    return name.compareTo(other.toString());
  }

  /**
   * Checks if this type equals another.
   * <p>
   * Instances are compared based on the name.
   * 
   * @param obj  the object to compare to, null returns false
   * @return true if equal
   */
  @Override
  public final boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == getClass()) {
      AttributeType<?> other = (AttributeType<?>) obj;
      return name.equals(other.name);
    }
    return false;
  }

  /**
   * Returns a suitable hash code.
   * 
   * @return a suitable hash code
   */
  @Override
  public final int hashCode() {
    return name.hashCode() ^ getClass().hashCode();
  }

  /**
   * Returns the name.
   * 
   * @return the string form, not empty
   */
  @Override
  @ToString
  public final String toString() {
    return name;
  }

}
