/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.DerivedProperty;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A position in a security, where the security is embedded ready for mark-to-market pricing.
 * <p>
 * This represents a position in a security, defined by long and short quantity.
 * The security is embedded directly, however the underlying product model is not available.
 * The security may be of any kind, including equities, bonds and exchange traded derivatives (ETD).
 * <p>
 * The net quantity of the position is stored using two fields - {@code longQuantity} and {@code shortQuantity}.
 * These two fields must not be negative.
 * In many cases, only a long quantity or short quantity will be present with the other set to zero.
 * However it is also possible for both to be non-zero, allowing long and short positions to be treated separately.
 * The net quantity is available via {@link #getQuantity()}.
 */
@BeanDefinition(constructorScope = "package")
public final class GenericSecurityPosition
    implements Position, SecurityQuantity, ImmutableBean, Serializable {

  /**
   * The additional position information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the position.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final PositionInfo info;
  /**
   * The underlying security.
   */
  @PropertyDefinition(validate = "notNull")
  private final GenericSecurity security;
  /**
   * The long quantity of the security.
   * <p>
   * This is the quantity of the underlying security that is held.
   * The quantity cannot be negative, as that would imply short selling.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double longQuantity;
  /**
   * The short quantity of the security.
   * <p>
   * This is the quantity of the underlying security that has been short sold.
   * The quantity cannot be negative, as that would imply the position is long.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double shortQuantity;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the security and net quantity.
   * <p>
   * The net quantity is the long quantity minus the short quantity, which may be negative.
   * If the quantity is positive it is treated as a long quantity.
   * Otherwise it is treated as a short quantity.
   *
   * @param security  the underlying security
   * @param netQuantity  the net quantity of the underlying security
   * @return the position
   */
  public static GenericSecurityPosition ofNet(GenericSecurity security, double netQuantity) {
    return ofNet(PositionInfo.empty(), security, netQuantity);
  }

  /**
   * Obtains an instance from position information, security and net quantity.
   * <p>
   * The net quantity is the long quantity minus the short quantity, which may be negative.
   *
   * @param positionInfo  the position information
   * @param security  the underlying security
   * @param netQuantity  the net quantity of the underlying security
   * @return the position
   */
  public static GenericSecurityPosition ofNet(PositionInfo positionInfo, GenericSecurity security, double netQuantity) {
    double longQuantity = netQuantity >= 0 ? netQuantity : 0;
    double shortQuantity = netQuantity >= 0 ? 0 : -netQuantity;
    return new GenericSecurityPosition(positionInfo, security, longQuantity, shortQuantity);
  }

  /**
   * Obtains an instance from the security, long quantity and short quantity.
   * <p>
   * The long quantity and short quantity must be zero or positive, not negative.
   *
   * @param security  the underlying security
   * @param longQuantity  the long quantity of the underlying security
   * @param shortQuantity  the short quantity of the underlying security
   * @return the position
   */
  public static GenericSecurityPosition ofLongShort(GenericSecurity security, double longQuantity, double shortQuantity) {
    return ofLongShort(PositionInfo.empty(), security, longQuantity, shortQuantity);
  }

  /**
   * Obtains an instance from position information, security, long quantity and short quantity.
   *
   * @param positionInfo  the position information
   * @param security  the underlying security
   * @param longQuantity  the long quantity of the underlying security
   * @param shortQuantity  the short quantity of the underlying security
   * @return the position
   */
  public static GenericSecurityPosition ofLongShort(
      PositionInfo positionInfo,
      GenericSecurity security,
      double longQuantity,
      double shortQuantity) {

    return new GenericSecurityPosition(positionInfo, security, longQuantity, shortQuantity);
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.info = PositionInfo.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   *
   * @return the security identifier
   */
  @Override
  public SecurityId getSecurityId() {
    return security.getSecurityId();
  }

  /**
   * Gets the currency of the trade.
   * <p>
   * This is the currency of the security.
   *
   * @return the trading currency
   */
  public Currency getCurrency() {
    return security.getCurrency();
  }

  /**
   * Gets the net quantity of the security.
   * <p>
   * This returns the <i>net</i> quantity of the underlying security.
   * The result is positive if the net position is <i>long</i> and negative
   * if the net position is <i>short</i>.
   * <p>
   * This is calculated by subtracting the short quantity from the long quantity.
   *
   * @return the net quantity of the underlying security
   */
  @Override
  @DerivedProperty
  public double getQuantity() {
    return longQuantity - shortQuantity;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code GenericSecurityPosition}.
   * @return the meta-bean, not null
   */
  public static GenericSecurityPosition.Meta meta() {
    return GenericSecurityPosition.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(GenericSecurityPosition.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static GenericSecurityPosition.Builder builder() {
    return new GenericSecurityPosition.Builder();
  }

  /**
   * Creates an instance.
   * @param info  the value of the property, not null
   * @param security  the value of the property, not null
   * @param longQuantity  the value of the property
   * @param shortQuantity  the value of the property
   */
  GenericSecurityPosition(
      PositionInfo info,
      GenericSecurity security,
      double longQuantity,
      double shortQuantity) {
    JodaBeanUtils.notNull(info, "info");
    JodaBeanUtils.notNull(security, "security");
    ArgChecker.notNegative(longQuantity, "longQuantity");
    ArgChecker.notNegative(shortQuantity, "shortQuantity");
    this.info = info;
    this.security = security;
    this.longQuantity = longQuantity;
    this.shortQuantity = shortQuantity;
  }

  @Override
  public GenericSecurityPosition.Meta metaBean() {
    return GenericSecurityPosition.Meta.INSTANCE;
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
   * Gets the additional position information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the position.
   * @return the value of the property, not null
   */
  @Override
  public PositionInfo getInfo() {
    return info;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying security.
   * @return the value of the property, not null
   */
  public GenericSecurity getSecurity() {
    return security;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the long quantity of the security.
   * <p>
   * This is the quantity of the underlying security that is held.
   * The quantity cannot be negative, as that would imply short selling.
   * @return the value of the property
   */
  public double getLongQuantity() {
    return longQuantity;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the short quantity of the security.
   * <p>
   * This is the quantity of the underlying security that has been short sold.
   * The quantity cannot be negative, as that would imply the position is long.
   * @return the value of the property
   */
  public double getShortQuantity() {
    return shortQuantity;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      GenericSecurityPosition other = (GenericSecurityPosition) obj;
      return JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(security, other.security) &&
          JodaBeanUtils.equal(longQuantity, other.longQuantity) &&
          JodaBeanUtils.equal(shortQuantity, other.shortQuantity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(security);
    hash = hash * 31 + JodaBeanUtils.hashCode(longQuantity);
    hash = hash * 31 + JodaBeanUtils.hashCode(shortQuantity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("GenericSecurityPosition{");
    buf.append("info").append('=').append(info).append(',').append(' ');
    buf.append("security").append('=').append(security).append(',').append(' ');
    buf.append("longQuantity").append('=').append(longQuantity).append(',').append(' ');
    buf.append("shortQuantity").append('=').append(JodaBeanUtils.toString(shortQuantity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code GenericSecurityPosition}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code info} property.
     */
    private final MetaProperty<PositionInfo> info = DirectMetaProperty.ofImmutable(
        this, "info", GenericSecurityPosition.class, PositionInfo.class);
    /**
     * The meta-property for the {@code security} property.
     */
    private final MetaProperty<GenericSecurity> security = DirectMetaProperty.ofImmutable(
        this, "security", GenericSecurityPosition.class, GenericSecurity.class);
    /**
     * The meta-property for the {@code longQuantity} property.
     */
    private final MetaProperty<Double> longQuantity = DirectMetaProperty.ofImmutable(
        this, "longQuantity", GenericSecurityPosition.class, Double.TYPE);
    /**
     * The meta-property for the {@code shortQuantity} property.
     */
    private final MetaProperty<Double> shortQuantity = DirectMetaProperty.ofImmutable(
        this, "shortQuantity", GenericSecurityPosition.class, Double.TYPE);
    /**
     * The meta-property for the {@code quantity} property.
     */
    private final MetaProperty<Double> quantity = DirectMetaProperty.ofDerived(
        this, "quantity", GenericSecurityPosition.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "info",
        "security",
        "longQuantity",
        "shortQuantity",
        "quantity");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case 949122880:  // security
          return security;
        case 611668775:  // longQuantity
          return longQuantity;
        case -2094395097:  // shortQuantity
          return shortQuantity;
        case -1285004149:  // quantity
          return quantity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public GenericSecurityPosition.Builder builder() {
      return new GenericSecurityPosition.Builder();
    }

    @Override
    public Class<? extends GenericSecurityPosition> beanType() {
      return GenericSecurityPosition.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code info} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PositionInfo> info() {
      return info;
    }

    /**
     * The meta-property for the {@code security} property.
     * @return the meta-property, not null
     */
    public MetaProperty<GenericSecurity> security() {
      return security;
    }

    /**
     * The meta-property for the {@code longQuantity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> longQuantity() {
      return longQuantity;
    }

    /**
     * The meta-property for the {@code shortQuantity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> shortQuantity() {
      return shortQuantity;
    }

    /**
     * The meta-property for the {@code quantity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> quantity() {
      return quantity;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return ((GenericSecurityPosition) bean).getInfo();
        case 949122880:  // security
          return ((GenericSecurityPosition) bean).getSecurity();
        case 611668775:  // longQuantity
          return ((GenericSecurityPosition) bean).getLongQuantity();
        case -2094395097:  // shortQuantity
          return ((GenericSecurityPosition) bean).getShortQuantity();
        case -1285004149:  // quantity
          return ((GenericSecurityPosition) bean).getQuantity();
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
   * The bean-builder for {@code GenericSecurityPosition}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<GenericSecurityPosition> {

    private PositionInfo info;
    private GenericSecurity security;
    private double longQuantity;
    private double shortQuantity;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(GenericSecurityPosition beanToCopy) {
      this.info = beanToCopy.getInfo();
      this.security = beanToCopy.getSecurity();
      this.longQuantity = beanToCopy.getLongQuantity();
      this.shortQuantity = beanToCopy.getShortQuantity();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case 949122880:  // security
          return security;
        case 611668775:  // longQuantity
          return longQuantity;
        case -2094395097:  // shortQuantity
          return shortQuantity;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          this.info = (PositionInfo) newValue;
          break;
        case 949122880:  // security
          this.security = (GenericSecurity) newValue;
          break;
        case 611668775:  // longQuantity
          this.longQuantity = (Double) newValue;
          break;
        case -2094395097:  // shortQuantity
          this.shortQuantity = (Double) newValue;
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

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    /**
     * @deprecated Loop in application code
     */
    @Override
    @Deprecated
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public GenericSecurityPosition build() {
      return new GenericSecurityPosition(
          info,
          security,
          longQuantity,
          shortQuantity);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the additional position information, defaulted to an empty instance.
     * <p>
     * This allows additional information to be attached to the position.
     * @param info  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder info(PositionInfo info) {
      JodaBeanUtils.notNull(info, "info");
      this.info = info;
      return this;
    }

    /**
     * Sets the underlying security.
     * @param security  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder security(GenericSecurity security) {
      JodaBeanUtils.notNull(security, "security");
      this.security = security;
      return this;
    }

    /**
     * Sets the long quantity of the security.
     * <p>
     * This is the quantity of the underlying security that is held.
     * The quantity cannot be negative, as that would imply short selling.
     * @param longQuantity  the new value
     * @return this, for chaining, not null
     */
    public Builder longQuantity(double longQuantity) {
      ArgChecker.notNegative(longQuantity, "longQuantity");
      this.longQuantity = longQuantity;
      return this;
    }

    /**
     * Sets the short quantity of the security.
     * <p>
     * This is the quantity of the underlying security that has been short sold.
     * The quantity cannot be negative, as that would imply the position is long.
     * @param shortQuantity  the new value
     * @return this, for chaining, not null
     */
    public Builder shortQuantity(double shortQuantity) {
      ArgChecker.notNegative(shortQuantity, "shortQuantity");
      this.shortQuantity = shortQuantity;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("GenericSecurityPosition.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("security").append('=').append(JodaBeanUtils.toString(security)).append(',').append(' ');
      buf.append("longQuantity").append('=').append(JodaBeanUtils.toString(longQuantity)).append(',').append(' ');
      buf.append("shortQuantity").append('=').append(JodaBeanUtils.toString(shortQuantity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
