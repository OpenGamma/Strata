/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Additional information about a portfolio item.
 * <p>
 * This allows additional information to be associated with an item.
 * It is kept in a separate object as the information is generally optional for pricing.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface PortfolioItemInfo extends Attributes {

  /**
   * Obtains an empty info instance.
   * <p>
   * The resulting instance implements this interface and is useful for classes that
   * extend {@link PortfolioItem} but are not trades or positions.
   * The returned instance can be customized using {@code with} methods.
   * 
   * @return the empty info instance
   */
  public static PortfolioItemInfo empty() {
    return ItemInfo.empty();
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
  public static <T> PortfolioItemInfo of(AttributeType<T> type, T value) {
    return new ItemInfo(null, ImmutableMap.of(type, type.toStoredForm(value)));
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * 
   * @return the builder
   */
  public static PortfolioItemInfoBuilder<PortfolioItemInfo> builder() {
    return new PortfolioItemInfoBuilder<PortfolioItemInfo>() {
      private StandardId id;
      private final Map<AttributeType<?>, Object> attributes = new HashMap<>();

      @Override
      public PortfolioItemInfoBuilder<PortfolioItemInfo> id(StandardId id) {
        this.id = id;
        return this;
      }

      @Override
      public <V> PortfolioItemInfoBuilder<PortfolioItemInfo> addAttribute(
          AttributeType<V> attributeType,
          V attributeValue) {

        ArgChecker.notNull(attributeType, "attributeType");
        ArgChecker.notNull(attributeValue, "attributeValue");
        attributes.put(attributeType, attributeType.toStoredForm(attributeValue));
        return this;
      }

      @Override
      public PortfolioItemInfo build() {
        return new ItemInfo(id, attributes);
      }
    };
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the primary identifier for the portfolio item, optional.
   * <p>
   * The identifier is used to identify the portfolio item.
   * It will typically be an identifier in an external data system.
   * <p>
   * A portfolio item may have multiple active identifiers. Any identifier may be chosen here.
   * Certain uses of the identifier, such as storage in a database, require that the
   * identifier does not change over time, and this should be considered best practice.
   * 
   * @return the identifier, optional
   */
  public abstract Optional<StandardId> getId();

  /**
   * Returns a copy of this instance with the identifier changed.
   * <p>
   * This returns a new instance with the identifier changed.
   * If the specified identifier is null, the existing identifier will be removed.
   * If the specified identifier is non-null, it will become the identifier of the resulting info.
   * 
   * @param identifier  the identifier to set
   * @return a new instance based on this one with the identifier set
   */
  public abstract PortfolioItemInfo withId(StandardId identifier);

  @Override
  public abstract ImmutableSet<AttributeType<?>> getAttributeTypes();

  @Override
  public abstract <T> PortfolioItemInfo withAttribute(AttributeType<T> type, T value);

  @Override
  public default PortfolioItemInfo withAttributes(Attributes other) {
    return (PortfolioItemInfo) Attributes.super.withAttributes(other);
  }

  /**
   * Combines this info with another.
   * <p>
   * If there is a conflict, data from this instance takes precedence.
   * If the other instance is not of the same type, data may be lost.
   * 
   * @param other  the other instance
   * @return the combined instance
   */
  public default PortfolioItemInfo combinedWith(PortfolioItemInfo other) {
    PortfolioItemInfo combinedInfo = this;
    if (!combinedInfo.getId().isPresent() && other.getId().isPresent()) {
      combinedInfo = combinedInfo.withId(other.getId().get());
    }
    for (AttributeType<?> attrType : other.getAttributeTypes()) {
      if (!combinedInfo.getAttributeTypes().contains(attrType)) {
        combinedInfo = combinedInfo.withAttribute(attrType.captureWildcard(), other.getAttribute(attrType));
      }
    }
    return combinedInfo;
  }

  /**
   * Overrides attributes of this info with another.
   * <p>
   * If there is a conflict, data from the other instance takes precedence.
   * If the other instance is not of the same type, data may be lost.
   * 
   * @param other  the other instance
   * @return the combined instance
   */
  public default PortfolioItemInfo overrideWith(PortfolioItemInfo other) {
    PortfolioItemInfo combinedInfo = this;
    if (other.getId().isPresent()) {
      combinedInfo = combinedInfo.withId(other.getId().get());
    }
    for (AttributeType<?> attrType : other.getAttributeTypes()) {
      combinedInfo = combinedInfo.withAttribute(attrType.captureWildcard(), other.getAttribute(attrType));
    }
    return combinedInfo;
  }

}
