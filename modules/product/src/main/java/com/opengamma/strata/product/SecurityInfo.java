/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Information about a security.
 * <p>
 * This provides common information about a security.
 * This includes the identifier, information about the price and an extensible data map.
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class SecurityInfo
    implements Attributes, ImmutableBean, Serializable {

  /**
   * The security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * It is the key used to lookup the security in {@link ReferenceData}.
   * <p>
   * A real-world security will typically have multiple identifiers.
   * The only restriction placed on the identifier is that it is sufficiently
   * unique for the reference data lookup. As such, it is acceptable to use
   * an identifier from a well-known global or vendor symbology.
   */
  @PropertyDefinition(validate = "notNull")
  private final SecurityId id;
  /**
   * The information about the security price.
   * <p>
   * This provides information about the security price.
   * This can be used to convert the price into a monetary value.
   */
  @PropertyDefinition(validate = "notNull")
  private final SecurityPriceInfo priceInfo;
  /**
   * The security attributes.
   * <p>
   * Security attributes provide the ability to associate arbitrary information in a key-value map.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<AttributeType<?>, Object> attributes;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the identifier, tick size and tick value.
   * <p>
   * This creates an instance, building the {@link SecurityPriceInfo} from
   * the tick size and tick value, setting the contract size to 1.
   * <p>
   * A {@code SecurityInfo} also contains a hash map of additional information,
   * keyed by {@link AttributeType}. This hash map may contain anything
   * of interest, and is populated using {@link #withAttribute(AttributeType, Object)}.
   * 
   * @param id  the security identifier
   * @param tickSize  the size of each tick, not negative or zero
   * @param tickValue  the value of each tick
   * @return the security information
   */
  public static SecurityInfo of(SecurityId id, double tickSize, CurrencyAmount tickValue) {
    return new SecurityInfo(id, SecurityPriceInfo.of(tickSize, tickValue), ImmutableMap.of());
  }

  /**
   * Obtains an instance from the identifier and pricing info.
   * <p>
   * A {@code SecurityInfo} also contains a hash map of additional information,
   * keyed by {@link AttributeType}. This hash map may contain anything
   * of interest, and is populated using {@link #withAttribute(AttributeType, Object)}.
   * 
   * @param id  the security identifier
   * @param priceInfo  the information about the price
   * @return the security information
   */
  public static SecurityInfo of(SecurityId id, SecurityPriceInfo priceInfo) {
    return new SecurityInfo(id, priceInfo, ImmutableMap.of());
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * 
   * @return the builder, not null
   */
  public static SecurityInfoBuilder builder() {
    return new SecurityInfoBuilder();
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<AttributeType<?>> getAttributeTypes() {
    return attributes.keySet();
  }

  @Override
  public <T> Optional<T> findAttribute(AttributeType<T> type) {
    return Optional.ofNullable(type.fromStoredForm(attributes.get(type)));
  }

  @Override
  public <T> SecurityInfo withAttribute(AttributeType<T> type, T value) {
    // ImmutableMap.Builder would not provide Map.put semantics
    Map<AttributeType<?>, Object> updatedAttributes = new HashMap<>(attributes);
    if (value == null) {
      updatedAttributes.remove(type);
    } else {
      updatedAttributes.put(type, type.toStoredForm(value));
    }
    return new SecurityInfo(id, priceInfo, updatedAttributes);
  }

  @Override
  public SecurityInfo withAttributes(Attributes other) {
    SecurityInfoBuilder builder = toBuilder();
    for (AttributeType<?> attrType : other.getAttributeTypes()) {
      builder.addAttribute(attrType.captureWildcard(), other.getAttribute(attrType));
    }
    return builder.build();
  }

  /**
   * Returns a builder populated with the values of this instance.
   * 
   * @return a builder populated with the values of this instance
   */
  public SecurityInfoBuilder toBuilder() {
    return new SecurityInfoBuilder(id, priceInfo, attributes);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SecurityInfo}.
   * @return the meta-bean, not null
   */
  public static SecurityInfo.Meta meta() {
    return SecurityInfo.Meta.INSTANCE;
  }

  static {
    MetaBean.register(SecurityInfo.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param id  the value of the property, not null
   * @param priceInfo  the value of the property, not null
   * @param attributes  the value of the property, not null
   */
  SecurityInfo(
      SecurityId id,
      SecurityPriceInfo priceInfo,
      Map<AttributeType<?>, Object> attributes) {
    JodaBeanUtils.notNull(id, "id");
    JodaBeanUtils.notNull(priceInfo, "priceInfo");
    JodaBeanUtils.notNull(attributes, "attributes");
    this.id = id;
    this.priceInfo = priceInfo;
    this.attributes = ImmutableMap.copyOf(attributes);
  }

  @Override
  public SecurityInfo.Meta metaBean() {
    return SecurityInfo.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * It is the key used to lookup the security in {@link ReferenceData}.
   * <p>
   * A real-world security will typically have multiple identifiers.
   * The only restriction placed on the identifier is that it is sufficiently
   * unique for the reference data lookup. As such, it is acceptable to use
   * an identifier from a well-known global or vendor symbology.
   * @return the value of the property, not null
   */
  public SecurityId getId() {
    return id;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the information about the security price.
   * <p>
   * This provides information about the security price.
   * This can be used to convert the price into a monetary value.
   * @return the value of the property, not null
   */
  public SecurityPriceInfo getPriceInfo() {
    return priceInfo;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the security attributes.
   * <p>
   * Security attributes provide the ability to associate arbitrary information in a key-value map.
   * @return the value of the property, not null
   */
  public ImmutableMap<AttributeType<?>, Object> getAttributes() {
    return attributes;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SecurityInfo other = (SecurityInfo) obj;
      return JodaBeanUtils.equal(id, other.id) &&
          JodaBeanUtils.equal(priceInfo, other.priceInfo) &&
          JodaBeanUtils.equal(attributes, other.attributes);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(id);
    hash = hash * 31 + JodaBeanUtils.hashCode(priceInfo);
    hash = hash * 31 + JodaBeanUtils.hashCode(attributes);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("SecurityInfo{");
    buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
    buf.append("priceInfo").append('=').append(JodaBeanUtils.toString(priceInfo)).append(',').append(' ');
    buf.append("attributes").append('=').append(JodaBeanUtils.toString(attributes));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SecurityInfo}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code id} property.
     */
    private final MetaProperty<SecurityId> id = DirectMetaProperty.ofImmutable(
        this, "id", SecurityInfo.class, SecurityId.class);
    /**
     * The meta-property for the {@code priceInfo} property.
     */
    private final MetaProperty<SecurityPriceInfo> priceInfo = DirectMetaProperty.ofImmutable(
        this, "priceInfo", SecurityInfo.class, SecurityPriceInfo.class);
    /**
     * The meta-property for the {@code attributes} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<AttributeType<?>, Object>> attributes = DirectMetaProperty.ofImmutable(
        this, "attributes", SecurityInfo.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "id",
        "priceInfo",
        "attributes");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return id;
        case -2126070377:  // priceInfo
          return priceInfo;
        case 405645655:  // attributes
          return attributes;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SecurityInfo> builder() {
      return new SecurityInfo.Builder();
    }

    @Override
    public Class<? extends SecurityInfo> beanType() {
      return SecurityInfo.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code id} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SecurityId> id() {
      return id;
    }

    /**
     * The meta-property for the {@code priceInfo} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SecurityPriceInfo> priceInfo() {
      return priceInfo;
    }

    /**
     * The meta-property for the {@code attributes} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<AttributeType<?>, Object>> attributes() {
      return attributes;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return ((SecurityInfo) bean).getId();
        case -2126070377:  // priceInfo
          return ((SecurityInfo) bean).getPriceInfo();
        case 405645655:  // attributes
          return ((SecurityInfo) bean).getAttributes();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code SecurityInfo}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<SecurityInfo> {

    private SecurityId id;
    private SecurityPriceInfo priceInfo;
    private Map<AttributeType<?>, Object> attributes = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return id;
        case -2126070377:  // priceInfo
          return priceInfo;
        case 405645655:  // attributes
          return attributes;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          this.id = (SecurityId) newValue;
          break;
        case -2126070377:  // priceInfo
          this.priceInfo = (SecurityPriceInfo) newValue;
          break;
        case 405645655:  // attributes
          this.attributes = (Map<AttributeType<?>, Object>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public SecurityInfo build() {
      return new SecurityInfo(
          id,
          priceInfo,
          attributes);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("SecurityInfo.Builder{");
      buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
      buf.append("priceInfo").append('=').append(JodaBeanUtils.toString(priceInfo)).append(',').append(' ');
      buf.append("attributes").append('=').append(JodaBeanUtils.toString(attributes));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
