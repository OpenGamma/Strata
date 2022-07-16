package com.opengamma.strata.product.corporateaction;

/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.Attributes;
import com.opengamma.strata.product.PortfolioItemInfo;
import org.joda.beans.*;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;

@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class CorporateActionInfo
    implements PortfolioItemInfo, ImmutableBean, Serializable {

  private static final long serialVersionUID = 1L;

  @PropertyDefinition(get = "optional", overrideGet = true)
  private final StandardId id; //DPDPDP

  @PropertyDefinition(validate = "notNull")
  private final StandardId corpRefProvider;

  @PropertyDefinition(get = "optional")
  private final StandardId corpRefOfficial;

  @PropertyDefinition(validate = "notNull")
  private final CorporateActionEventType eventType;

  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<AttributeType<?>, Object> attributes;

  //-------------------------------------------------------------------------
  @Override
  public CorporateActionInfo withId(StandardId identifier) {
    return new CorporateActionInfo(identifier, corpRefProvider, corpRefOfficial, eventType, attributes);
  }

  @Override
  public ImmutableSet<AttributeType<?>> getAttributeTypes() {
    return attributes.keySet();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<T> findAttribute(AttributeType<T> type) {
    return Optional.ofNullable(type.fromStoredForm(attributes.get(type)));
  }
  @Override
  @SuppressWarnings("unchecked")
  public <T> CorporateActionInfo withAttribute(AttributeType<T> type, T value) {
    // ImmutableMap.Builder would not provide Map.put semantics
    Map<AttributeType<?>, Object> updatedAttributes = new HashMap<>(attributes);
    if (value == null) {
      updatedAttributes.remove(type);
    } else {
      updatedAttributes.put(type, type.toStoredForm(value));
    }
    return new CorporateActionInfo(id, corpRefProvider, corpRefOfficial, eventType, updatedAttributes);
  }

  @Override
  public CorporateActionInfo withAttributes(Attributes other) {
    CorporateActionInfoBuilder builder = toBuilder();
    for (AttributeType<?> attrType : other.getAttributeTypes()) {
      builder.addAttribute(attrType.captureWildcard(), other.getAttribute(attrType));
    }
    return builder.build();
  }

  @Override
  public CorporateActionInfo combinedWith(PortfolioItemInfo other) {
    CorporateActionInfoBuilder builder = toBuilder();
    other.getId().filter(ignored -> this.id == null).ifPresent(builder::id);
    if (other instanceof CorporateActionInfo) {
      CorporateActionInfo otherInfo = (CorporateActionInfo) other;
    }
    for (AttributeType<?> attrType : other.getAttributeTypes()) {
      if (!attributes.keySet().contains(attrType)) {
        builder.addAttribute(attrType.captureWildcard(), other.getAttribute(attrType));
      }
    }
    return builder.build();
  }

  @Override
  public CorporateActionInfo overrideWith(PortfolioItemInfo other) {
    CorporateActionInfoBuilder builder = toBuilder();
    other.getId().ifPresent(builder::id);
    if (other instanceof CorporateActionInfo) {
      CorporateActionInfo otherInfo = (CorporateActionInfo) other;
    }
    for (AttributeType<?> attrType : other.getAttributeTypes()) {
      builder.addAttribute(attrType.captureWildcard(), other.getAttribute(attrType));
    }
    return builder.build();
  }

  public static CorporateActionInfoBuilder builder() {
    return new CorporateActionInfoBuilder();
  }

  public CorporateActionInfoBuilder toBuilder() {
    return new CorporateActionInfoBuilder(id, corpRefProvider, corpRefOfficial, eventType, attributes);
  }


  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code CorporateActionInfo}.
   * @return the meta-bean, not null
   */
  public static CorporateActionInfo.Meta meta() {
    return CorporateActionInfo.Meta.INSTANCE;
  }

  static {
    MetaBean.register(CorporateActionInfo.Meta.INSTANCE);
  }

  /**
   * Creates an instance.
   * @param id  the value of the property
   * @param corpRefProvider  the value of the property, not null
   * @param corpRefOfficial  the value of the property
   * @param eventType  the value of the property, not null
   * @param attributes  the value of the property, not null
   */
  CorporateActionInfo(
      StandardId id,
      StandardId corpRefProvider,
      StandardId corpRefOfficial,
      CorporateActionEventType eventType,
      Map<AttributeType<?>, Object> attributes) {
    JodaBeanUtils.notNull(corpRefProvider, "corpRefProvider");
    JodaBeanUtils.notNull(eventType, "eventType");
    JodaBeanUtils.notNull(attributes, "attributes");
    this.id = id;
    this.corpRefProvider = corpRefProvider;
    this.corpRefOfficial = corpRefOfficial;
    this.eventType = eventType;
    this.attributes = ImmutableMap.copyOf(attributes);
  }

  @Override
  public CorporateActionInfo.Meta metaBean() {
    return CorporateActionInfo.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the id.
   * @return the optional value of the property, not null
   */
  @Override
  public Optional<StandardId> getId() {
    return Optional.ofNullable(id);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the corpRefProvider.
   * @return the value of the property, not null
   */
  public StandardId getCorpRefProvider() {
    return corpRefProvider;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the corpRefOfficial.
   * @return the optional value of the property, not null
   */
  public Optional<StandardId> getCorpRefOfficial() {
    return Optional.ofNullable(corpRefOfficial);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the eventType.
   * @return the value of the property, not null
   */
  public CorporateActionEventType getEventType() {
    return eventType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the attributes.
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
      CorporateActionInfo other = (CorporateActionInfo) obj;
      return JodaBeanUtils.equal(id, other.id) &&
          JodaBeanUtils.equal(corpRefProvider, other.corpRefProvider) &&
          JodaBeanUtils.equal(corpRefOfficial, other.corpRefOfficial) &&
          JodaBeanUtils.equal(eventType, other.eventType) &&
          JodaBeanUtils.equal(attributes, other.attributes);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(id);
    hash = hash * 31 + JodaBeanUtils.hashCode(corpRefProvider);
    hash = hash * 31 + JodaBeanUtils.hashCode(corpRefOfficial);
    hash = hash * 31 + JodaBeanUtils.hashCode(eventType);
    hash = hash * 31 + JodaBeanUtils.hashCode(attributes);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("CorporateActionInfo{");
    buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
    buf.append("corpRefProvider").append('=').append(JodaBeanUtils.toString(corpRefProvider)).append(',').append(' ');
    buf.append("corpRefOfficial").append('=').append(JodaBeanUtils.toString(corpRefOfficial)).append(',').append(' ');
    buf.append("eventType").append('=').append(JodaBeanUtils.toString(eventType)).append(',').append(' ');
    buf.append("attributes").append('=').append(JodaBeanUtils.toString(attributes));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CorporateActionInfo}.
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
        this, "id", CorporateActionInfo.class, StandardId.class);
    /**
     * The meta-property for the {@code corpRefProvider} property.
     */
    private final MetaProperty<StandardId> corpRefProvider = DirectMetaProperty.ofImmutable(
        this, "corpRefProvider", CorporateActionInfo.class, StandardId.class);
    /**
     * The meta-property for the {@code corpRefOfficial} property.
     */
    private final MetaProperty<StandardId> corpRefOfficial = DirectMetaProperty.ofImmutable(
        this, "corpRefOfficial", CorporateActionInfo.class, StandardId.class);
    /**
     * The meta-property for the {@code eventType} property.
     */
    private final MetaProperty<CorporateActionEventType> eventType = DirectMetaProperty.ofImmutable(
        this, "eventType", CorporateActionInfo.class, CorporateActionEventType.class);
    /**
     * The meta-property for the {@code attributes} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<AttributeType<?>, Object>> attributes = DirectMetaProperty.ofImmutable(
        this, "attributes", CorporateActionInfo.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "id",
        "corpRefProvider",
        "corpRefOfficial",
        "eventType",
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
        case 831736218:  // corpRefProvider
          return corpRefProvider;
        case 1053941396:  // corpRefOfficial
          return corpRefOfficial;
        case 31430900:  // eventType
          return eventType;
        case 405645655:  // attributes
          return attributes;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CorporateActionInfo> builder() {
      return new CorporateActionInfo.Builder();
    }

    @Override
    public Class<? extends CorporateActionInfo> beanType() {
      return CorporateActionInfo.class;
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
     * The meta-property for the {@code corpRefProvider} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> corpRefProvider() {
      return corpRefProvider;
    }

    /**
     * The meta-property for the {@code corpRefOfficial} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> corpRefOfficial() {
      return corpRefOfficial;
    }

    /**
     * The meta-property for the {@code eventType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CorporateActionEventType> eventType() {
      return eventType;
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
          return ((CorporateActionInfo) bean).id;
        case 831736218:  // corpRefProvider
          return ((CorporateActionInfo) bean).getCorpRefProvider();
        case 1053941396:  // corpRefOfficial
          return ((CorporateActionInfo) bean).corpRefOfficial;
        case 31430900:  // eventType
          return ((CorporateActionInfo) bean).getEventType();
        case 405645655:  // attributes
          return ((CorporateActionInfo) bean).getAttributes();
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
   * The bean-builder for {@code CorporateActionInfo}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<CorporateActionInfo> {

    private StandardId id;
    private StandardId corpRefProvider;
    private StandardId corpRefOfficial;
    private CorporateActionEventType eventType;
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
        case 831736218:  // corpRefProvider
          return corpRefProvider;
        case 1053941396:  // corpRefOfficial
          return corpRefOfficial;
        case 31430900:  // eventType
          return eventType;
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
        case 831736218:  // corpRefProvider
          this.corpRefProvider = (StandardId) newValue;
          break;
        case 1053941396:  // corpRefOfficial
          this.corpRefOfficial = (StandardId) newValue;
          break;
        case 31430900:  // eventType
          this.eventType = (CorporateActionEventType) newValue;
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
    public CorporateActionInfo build() {
      return new CorporateActionInfo(
          id,
          corpRefProvider,
          corpRefOfficial,
          eventType,
          attributes);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("CorporateActionInfo.Builder{");
      buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
      buf.append("corpRefProvider").append('=').append(JodaBeanUtils.toString(corpRefProvider)).append(',').append(' ');
      buf.append("corpRefOfficial").append('=').append(JodaBeanUtils.toString(corpRefOfficial)).append(',').append(' ');
      buf.append("eventType").append('=').append(JodaBeanUtils.toString(eventType)).append(',').append(' ');
      buf.append("attributes").append('=').append(JodaBeanUtils.toString(attributes));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}

