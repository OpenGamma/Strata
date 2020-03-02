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
import com.opengamma.strata.basics.StandardId;

/**
 * Additional information about a position.
 * <p>
 * This allows additional information about a position to be associated.
 * It is kept in a separate object as the information is optional for pricing.
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class PositionInfo
    implements PortfolioItemInfo, ImmutableBean, Serializable {

  /**
   * An empty instance of {@code PositionInfo}.
   */
  private static final PositionInfo EMPTY = new PositionInfo(null, ImmutableMap.of());

  /**
   * The primary identifier for the position, optional.
   * <p>
   * The identifier is used to identify the position.
   * It will typically be an identifier in an external data system.
   * <p>
   * A position may have multiple active identifiers. Any identifier may be chosen here.
   * Certain uses of the identifier, such as storage in a database, require that the
   * identifier does not change over time, and this should be considered best practice.
   */
  @PropertyDefinition(get = "optional", overrideGet = true)
  private final StandardId id;
  /**
   * The position attributes.
   * <p>
   * Position attributes provide the ability to associate arbitrary information in a key-value map.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<AttributeType<?>, Object> attributes;

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty instance, with no identifier or attributes.
   * 
   * @return the empty instance
   */
  public static PositionInfo empty() {
    return EMPTY;
  }

  /**
   * Obtains an instance with the specified position identifier.
   * 
   * @param positionId  the position identifier
   * @return the position information
   */
  public static PositionInfo of(StandardId positionId) {
    return new PositionInfo(positionId, ImmutableMap.of());
  }

  /**
   * Obtains an instance based on the supplied info.
   *
   * @param info  the base info
   * @return the position information
   */
  public static PositionInfo from(PortfolioItemInfo info) {
    if (info instanceof PositionInfo) {
      return ((PositionInfo) info);
    }
    return empty().combinedWith(info);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * 
   * @return the builder
   */
  public static PositionInfoBuilder builder() {
    return new PositionInfoBuilder();
  }

  //-------------------------------------------------------------------------
  @Override
  public PositionInfo withId(StandardId identifier) {
    return new PositionInfo(identifier, attributes);
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
  public <T> PositionInfo withAttribute(AttributeType<T> type, T value) {
    // ImmutableMap.Builder would not provide Map.put semantics
    Map<AttributeType<?>, Object> updatedAttributes = new HashMap<>(attributes);
    if (value == null) {
      updatedAttributes.remove(type);
    } else {
      updatedAttributes.put(type, type.toStoredForm(value));
    }
    return new PositionInfo(id, updatedAttributes);
  }

  @Override
  public PositionInfo withAttributes(Attributes other) {
    PositionInfoBuilder builder = toBuilder();
    for (AttributeType<?> attrType : other.getAttributeTypes()) {
      builder.addAttribute(attrType.captureWildcard(), other.getAttribute(attrType));
    }
    return builder.build();
  }

  @Override
  public PositionInfo combinedWith(PortfolioItemInfo other) {
    PositionInfoBuilder builder = toBuilder();
    other.getId().filter(ignored -> this.id == null).ifPresent(builder::id);
    for (AttributeType<?> attrType : other.getAttributeTypes()) {
      if (!attributes.keySet().contains(attrType)) {
        builder.addAttribute(attrType.captureWildcard(), other.getAttribute(attrType));
      }
    }
    return builder.build();
  }

  @Override
  public PositionInfo overrideWith(PortfolioItemInfo other) {
    PositionInfoBuilder builder = toBuilder();
    other.getId().ifPresent(builder::id);
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
  public PositionInfoBuilder toBuilder() {
    return new PositionInfoBuilder(id, attributes);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code PositionInfo}.
   * @return the meta-bean, not null
   */
  public static PositionInfo.Meta meta() {
    return PositionInfo.Meta.INSTANCE;
  }

  static {
    MetaBean.register(PositionInfo.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param id  the value of the property
   * @param attributes  the value of the property, not null
   */
  PositionInfo(
      StandardId id,
      Map<AttributeType<?>, Object> attributes) {
    JodaBeanUtils.notNull(attributes, "attributes");
    this.id = id;
    this.attributes = ImmutableMap.copyOf(attributes);
  }

  @Override
  public PositionInfo.Meta metaBean() {
    return PositionInfo.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the primary identifier for the position, optional.
   * <p>
   * The identifier is used to identify the position.
   * It will typically be an identifier in an external data system.
   * <p>
   * A position may have multiple active identifiers. Any identifier may be chosen here.
   * Certain uses of the identifier, such as storage in a database, require that the
   * identifier does not change over time, and this should be considered best practice.
   * @return the optional value of the property, not null
   */
  @Override
  public Optional<StandardId> getId() {
    return Optional.ofNullable(id);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the position attributes.
   * <p>
   * Position attributes provide the ability to associate arbitrary information in a key-value map.
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
      PositionInfo other = (PositionInfo) obj;
      return JodaBeanUtils.equal(id, other.id) &&
          JodaBeanUtils.equal(attributes, other.attributes);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(id);
    hash = hash * 31 + JodaBeanUtils.hashCode(attributes);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("PositionInfo{");
    buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
    buf.append("attributes").append('=').append(JodaBeanUtils.toString(attributes));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PositionInfo}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code id} property.
     */
    private final MetaProperty<StandardId> id = DirectMetaProperty.ofImmutable(
        this, "id", PositionInfo.class, StandardId.class);
    /**
     * The meta-property for the {@code attributes} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<AttributeType<?>, Object>> attributes = DirectMetaProperty.ofImmutable(
        this, "attributes", PositionInfo.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "id",
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
        case 405645655:  // attributes
          return attributes;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends PositionInfo> builder() {
      return new PositionInfo.Builder();
    }

    @Override
    public Class<? extends PositionInfo> beanType() {
      return PositionInfo.class;
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
    public MetaProperty<StandardId> id() {
      return id;
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
          return ((PositionInfo) bean).id;
        case 405645655:  // attributes
          return ((PositionInfo) bean).getAttributes();
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
   * The bean-builder for {@code PositionInfo}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<PositionInfo> {

    private StandardId id;
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
          this.id = (StandardId) newValue;
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
    public PositionInfo build() {
      return new PositionInfo(
          id,
          attributes);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("PositionInfo.Builder{");
      buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
      buf.append("attributes").append('=').append(JodaBeanUtils.toString(attributes));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
