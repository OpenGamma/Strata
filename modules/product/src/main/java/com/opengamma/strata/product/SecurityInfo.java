/**
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
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.Messages;

/**
 * Information about a security.
 * <p>
 * This provides common information about a security.
 * This includes the identifier, information about the price and an extensible data map.
 */
@BeanDefinition(builderScope = "private")
public final class SecurityInfo
    implements ImmutableBean, Serializable {

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
   * The additional security information.
   * <p>
   * This stores additional information for the security.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<SecurityInfoType<?>, Object> additionalInfo;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the identifier, tick size and tick value.
   * <p>
   * This creates an instance, building the {@link SecurityPriceInfo} from
   * the tick size and tick value, setting the contract size to 1.
   * <p>
   * A {@code SecurityInfo} also contains a hash map of additional information,
   * keyed by {@link SecurityInfoType}. This hash map may contain anything
   * of interest, and is populated using {@link #withAdditionalInfo(SecurityInfoType, Object)}.
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
   * keyed by {@link SecurityInfoType}. This hash map may contain anything
   * of interest, and is populated using {@link #withAdditionalInfo(SecurityInfoType, Object)}.
   * 
   * @param id  the security identifier
   * @param priceInfo  the information about the price
   * @return the security information
   */
  public static SecurityInfo of(SecurityId id, SecurityPriceInfo priceInfo) {
    return new SecurityInfo(id, priceInfo, ImmutableMap.of());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets additional information about the security.
   * <p>
   * This method obtains the specified additional information.
   * This allows additional information about a security to be obtained if available.
   * <p>
   * If the info is not found, an exception is thrown.
   * 
   * @param <T>  the type of the info
   * @param type  the type to find
   * @return the security information
   * @throws IllegalArgumentException if the information is not found
   */
  public <T> T getAdditionalInfo(SecurityInfoType<T> type) {
    return findAdditionalInfo(type).orElseThrow(() -> new IllegalArgumentException(
        Messages.format("Security info not found for type '{}'", type)));
  }

  /**
   * Finds additional information about the security.
   * <p>
   * This method obtains the specified additional information.
   * This allows additional information about a security to be obtained if available.
   * <p>
   * If the info is not found, optional empty is returned.
   * 
   * @param <T>  the type of the info
   * @param type  the type to find
   * @return the security information
   */
  @SuppressWarnings("unchecked")
  public <T> Optional<T> findAdditionalInfo(SecurityInfoType<T> type) {
    return Optional.ofNullable((T) additionalInfo.get(type));
  }

  /**
   * Returns a copy of this instance with additional information added.
   * <p>
   * This returns a new instance with the specified additional information added.
   * The additional information is added using {@code Map.put(type, value)} semantics.
   * 
   * @param <T> the type of the value
   * @param type  the type providing meaning to the value
   * @param value  the value
   * @return a new instance based on this one with additional information added
   */
  @SuppressWarnings("unchecked")
  public <T> SecurityInfo withAdditionalInfo(SecurityInfoType<T> type, T value) {
    // ImmutableMap.Builder would not provide Map.put semantics
    Map<SecurityInfoType<?>, Object> infoMap = new HashMap<>(additionalInfo);
    infoMap.put(type, value);
    return new SecurityInfo(id, priceInfo, infoMap);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SecurityInfo}.
   * @return the meta-bean, not null
   */
  public static SecurityInfo.Meta meta() {
    return SecurityInfo.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SecurityInfo.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private SecurityInfo(
      SecurityId id,
      SecurityPriceInfo priceInfo,
      Map<SecurityInfoType<?>, Object> additionalInfo) {
    JodaBeanUtils.notNull(id, "id");
    JodaBeanUtils.notNull(priceInfo, "priceInfo");
    JodaBeanUtils.notNull(additionalInfo, "additionalInfo");
    this.id = id;
    this.priceInfo = priceInfo;
    this.additionalInfo = ImmutableMap.copyOf(additionalInfo);
  }

  @Override
  public SecurityInfo.Meta metaBean() {
    return SecurityInfo.Meta.INSTANCE;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
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
   * Gets the additional security information.
   * <p>
   * This stores additional information for the security.
   * @return the value of the property, not null
   */
  public ImmutableMap<SecurityInfoType<?>, Object> getAdditionalInfo() {
    return additionalInfo;
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
          JodaBeanUtils.equal(additionalInfo, other.additionalInfo);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(id);
    hash = hash * 31 + JodaBeanUtils.hashCode(priceInfo);
    hash = hash * 31 + JodaBeanUtils.hashCode(additionalInfo);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("SecurityInfo{");
    buf.append("id").append('=').append(id).append(',').append(' ');
    buf.append("priceInfo").append('=').append(priceInfo).append(',').append(' ');
    buf.append("additionalInfo").append('=').append(JodaBeanUtils.toString(additionalInfo));
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
     * The meta-property for the {@code additionalInfo} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<SecurityInfoType<?>, Object>> additionalInfo = DirectMetaProperty.ofImmutable(
        this, "additionalInfo", SecurityInfo.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "id",
        "priceInfo",
        "additionalInfo");

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
        case -974297739:  // additionalInfo
          return additionalInfo;
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
     * The meta-property for the {@code additionalInfo} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<SecurityInfoType<?>, Object>> additionalInfo() {
      return additionalInfo;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return ((SecurityInfo) bean).getId();
        case -2126070377:  // priceInfo
          return ((SecurityInfo) bean).getPriceInfo();
        case -974297739:  // additionalInfo
          return ((SecurityInfo) bean).getAdditionalInfo();
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
  private static final class Builder extends DirectFieldsBeanBuilder<SecurityInfo> {

    private SecurityId id;
    private SecurityPriceInfo priceInfo;
    private Map<SecurityInfoType<?>, Object> additionalInfo = ImmutableMap.of();

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
        case -974297739:  // additionalInfo
          return additionalInfo;
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
        case -974297739:  // additionalInfo
          this.additionalInfo = (Map<SecurityInfoType<?>, Object>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public SecurityInfo build() {
      return new SecurityInfo(
          id,
          priceInfo,
          additionalInfo);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("SecurityInfo.Builder{");
      buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
      buf.append("priceInfo").append('=').append(JodaBeanUtils.toString(priceInfo)).append(',').append(' ');
      buf.append("additionalInfo").append('=').append(JodaBeanUtils.toString(additionalInfo));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
