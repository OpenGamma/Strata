/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

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

import com.google.common.collect.ComparisonChain;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.market.model.SabrParameterType;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Sensitivity of a caplet/floorlet to SABR model parameters.
 * <p>
 * Holds the sensitivity vales to grid points of the SABR parameters.
 */
@BeanDefinition(builderScope = "private")
public final class IborCapletFloorletSabrSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The name of the volatilities.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborCapletFloorletVolatilitiesName volatilitiesName;
  /**
   * The time to expiry of the option as a year fraction.
   */
  @PropertyDefinition(validate = "notNull")
  private final double expiry;
  /**
  * The type of the sensitivity.
  */
  @PropertyDefinition
  private final SabrParameterType sensitivityType;
  /**
  * The currency of the sensitivity.
  */
  @PropertyDefinition(overrideGet = true)
  private final Currency currency;
  /**
   * The value of the sensitivity.
   */
  @PropertyDefinition(overrideGet = true)
  private final double sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified elements.
   * 
   * @param volatilitiesName  the name of the volatilities
   * @param expiry  the time to expiry of the option as a year fraction
   * @param sensitivityType  the type of the sensitivity
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the sensitivity object
   */
  public static IborCapletFloorletSabrSensitivity of(
      IborCapletFloorletVolatilitiesName volatilitiesName,
      double expiry,
      SabrParameterType sensitivityType,
      Currency sensitivityCurrency,
      double sensitivity) {

    return new IborCapletFloorletSabrSensitivity(
        volatilitiesName,
        expiry,
        sensitivityType,
        sensitivityCurrency,
        sensitivity);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborCapletFloorletSabrSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new IborCapletFloorletSabrSensitivity(volatilitiesName, expiry, sensitivityType, currency, sensitivity);
  }

  @Override
  public IborCapletFloorletSabrSensitivity withSensitivity(double sensitivity) {
    return new IborCapletFloorletSabrSensitivity(volatilitiesName, expiry, sensitivityType, currency, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof IborCapletFloorletSabrSensitivity) {
      IborCapletFloorletSabrSensitivity otherSwpt = (IborCapletFloorletSabrSensitivity) other;
      return ComparisonChain.start()
          .compare(volatilitiesName, otherSwpt.volatilitiesName)
          .compare(currency, otherSwpt.currency)
          .compare(expiry, otherSwpt.expiry)
          .compare(sensitivityType, otherSwpt.sensitivityType)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public IborCapletFloorletSabrSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (IborCapletFloorletSabrSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborCapletFloorletSabrSensitivity multipliedBy(double factor) {
    return new IborCapletFloorletSabrSensitivity(volatilitiesName, expiry, sensitivityType, currency, sensitivity * factor);
  }

  @Override
  public IborCapletFloorletSabrSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new IborCapletFloorletSabrSensitivity(
        volatilitiesName, expiry, sensitivityType, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public IborCapletFloorletSabrSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public IborCapletFloorletSabrSensitivity cloned() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborCapletFloorletSabrSensitivity}.
   * @return the meta-bean, not null
   */
  public static IborCapletFloorletSabrSensitivity.Meta meta() {
    return IborCapletFloorletSabrSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborCapletFloorletSabrSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private IborCapletFloorletSabrSensitivity(
      IborCapletFloorletVolatilitiesName volatilitiesName,
      double expiry,
      SabrParameterType sensitivityType,
      Currency currency,
      double sensitivity) {
    JodaBeanUtils.notNull(volatilitiesName, "volatilitiesName");
    JodaBeanUtils.notNull(expiry, "expiry");
    this.volatilitiesName = volatilitiesName;
    this.expiry = expiry;
    this.sensitivityType = sensitivityType;
    this.currency = currency;
    this.sensitivity = sensitivity;
  }

  @Override
  public IborCapletFloorletSabrSensitivity.Meta metaBean() {
    return IborCapletFloorletSabrSensitivity.Meta.INSTANCE;
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
   * Gets the name of the volatilities.
   * @return the value of the property, not null
   */
  public IborCapletFloorletVolatilitiesName getVolatilitiesName() {
    return volatilitiesName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time to expiry of the option as a year fraction.
   * @return the value of the property, not null
   */
  public double getExpiry() {
    return expiry;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type of the sensitivity.
   * @return the value of the property
   */
  public SabrParameterType getSensitivityType() {
    return sensitivityType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the sensitivity.
   * @return the value of the property
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value of the sensitivity.
   * @return the value of the property
   */
  @Override
  public double getSensitivity() {
    return sensitivity;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IborCapletFloorletSabrSensitivity other = (IborCapletFloorletSabrSensitivity) obj;
      return JodaBeanUtils.equal(volatilitiesName, other.volatilitiesName) &&
          JodaBeanUtils.equal(expiry, other.expiry) &&
          JodaBeanUtils.equal(sensitivityType, other.sensitivityType) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(volatilitiesName);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiry);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivityType);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("IborCapletFloorletSabrSensitivity{");
    buf.append("volatilitiesName").append('=').append(volatilitiesName).append(',').append(' ');
    buf.append("expiry").append('=').append(expiry).append(',').append(' ');
    buf.append("sensitivityType").append('=').append(sensitivityType).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborCapletFloorletSabrSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code volatilitiesName} property.
     */
    private final MetaProperty<IborCapletFloorletVolatilitiesName> volatilitiesName = DirectMetaProperty.ofImmutable(
        this, "volatilitiesName", IborCapletFloorletSabrSensitivity.class, IborCapletFloorletVolatilitiesName.class);
    /**
     * The meta-property for the {@code expiry} property.
     */
    private final MetaProperty<Double> expiry = DirectMetaProperty.ofImmutable(
        this, "expiry", IborCapletFloorletSabrSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code sensitivityType} property.
     */
    private final MetaProperty<SabrParameterType> sensitivityType = DirectMetaProperty.ofImmutable(
        this, "sensitivityType", IborCapletFloorletSabrSensitivity.class, SabrParameterType.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", IborCapletFloorletSabrSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", IborCapletFloorletSabrSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "volatilitiesName",
        "expiry",
        "sensitivityType",
        "currency",
        "sensitivity");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 2100884654:  // volatilitiesName
          return volatilitiesName;
        case -1289159373:  // expiry
          return expiry;
        case 1598929529:  // sensitivityType
          return sensitivityType;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends IborCapletFloorletSabrSensitivity> builder() {
      return new IborCapletFloorletSabrSensitivity.Builder();
    }

    @Override
    public Class<? extends IborCapletFloorletSabrSensitivity> beanType() {
      return IborCapletFloorletSabrSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code volatilitiesName} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborCapletFloorletVolatilitiesName> volatilitiesName() {
      return volatilitiesName;
    }

    /**
     * The meta-property for the {@code expiry} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> expiry() {
      return expiry;
    }

    /**
     * The meta-property for the {@code sensitivityType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SabrParameterType> sensitivityType() {
      return sensitivityType;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code sensitivity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> sensitivity() {
      return sensitivity;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 2100884654:  // volatilitiesName
          return ((IborCapletFloorletSabrSensitivity) bean).getVolatilitiesName();
        case -1289159373:  // expiry
          return ((IborCapletFloorletSabrSensitivity) bean).getExpiry();
        case 1598929529:  // sensitivityType
          return ((IborCapletFloorletSabrSensitivity) bean).getSensitivityType();
        case 575402001:  // currency
          return ((IborCapletFloorletSabrSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((IborCapletFloorletSabrSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code IborCapletFloorletSabrSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<IborCapletFloorletSabrSensitivity> {

    private IborCapletFloorletVolatilitiesName volatilitiesName;
    private double expiry;
    private SabrParameterType sensitivityType;
    private Currency currency;
    private double sensitivity;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 2100884654:  // volatilitiesName
          return volatilitiesName;
        case -1289159373:  // expiry
          return expiry;
        case 1598929529:  // sensitivityType
          return sensitivityType;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 2100884654:  // volatilitiesName
          this.volatilitiesName = (IborCapletFloorletVolatilitiesName) newValue;
          break;
        case -1289159373:  // expiry
          this.expiry = (Double) newValue;
          break;
        case 1598929529:  // sensitivityType
          this.sensitivityType = (SabrParameterType) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 564403871:  // sensitivity
          this.sensitivity = (Double) newValue;
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
    public IborCapletFloorletSabrSensitivity build() {
      return new IborCapletFloorletSabrSensitivity(
          volatilitiesName,
          expiry,
          sensitivityType,
          currency,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("IborCapletFloorletSabrSensitivity.Builder{");
      buf.append("volatilitiesName").append('=').append(JodaBeanUtils.toString(volatilitiesName)).append(',').append(' ');
      buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
      buf.append("sensitivityType").append('=').append(JodaBeanUtils.toString(sensitivityType)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
