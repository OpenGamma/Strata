/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

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
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Point sensitivity to the zero rate curve.
 * <p>
 * Holds the sensitivity to the zero rate curve at a specific date.
 */
@BeanDefinition(builderScope = "private")
public final class ZeroRateSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The currency of the curve for which the sensitivity is computed.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency curveCurrency;
  /**
   * The time that was queried, expressed as a year fraction.
   */
  @PropertyDefinition
  private final double yearFraction;
  /**
   * The currency of the sensitivity.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The value of the sensitivity.
   */
  @PropertyDefinition(overrideGet = true)
  private final double sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the curve currency, date and value.
   * <p>
   * The currency representing the curve is used also for the sensitivity currency.
   * 
   * @param currency  the currency of the curve and sensitivity
   * @param yearFraction  the year fraction that was looked up on the curve
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static ZeroRateSensitivity of(Currency currency, double yearFraction, double sensitivity) {
    return new ZeroRateSensitivity(currency, yearFraction, currency, sensitivity);
  }

  /**
   * Obtains an instance from the curve currency, date, sensitivity currency and value.
   * <p>
   * The currency representing the curve is used also for the sensitivity currency.
   * 
   * @param curveCurrency  the currency of the curve
   * @param yearFraction  the year fraction that was looked up on the curve
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static ZeroRateSensitivity of(
      Currency curveCurrency,
      double yearFraction,
      Currency sensitivityCurrency,
      double sensitivity) {
    return new ZeroRateSensitivity(curveCurrency, yearFraction, sensitivityCurrency, sensitivity);
  }

  //-------------------------------------------------------------------------
  @Override
  public ZeroRateSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new ZeroRateSensitivity(curveCurrency, yearFraction, currency, sensitivity);
  }

  @Override
  public ZeroRateSensitivity withSensitivity(double sensitivity) {
    return new ZeroRateSensitivity(curveCurrency, yearFraction, currency, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof ZeroRateSensitivity) {
      ZeroRateSensitivity otherZero = (ZeroRateSensitivity) other;
      return ComparisonChain.start()
          .compare(curveCurrency, otherZero.curveCurrency)
          .compare(currency, otherZero.currency)
          .compare(yearFraction, otherZero.yearFraction)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public ZeroRateSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (ZeroRateSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public ZeroRateSensitivity multipliedBy(double factor) {
    return new ZeroRateSensitivity(curveCurrency, yearFraction, currency, sensitivity * factor);
  }

  @Override
  public ZeroRateSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new ZeroRateSensitivity(curveCurrency, yearFraction, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public ZeroRateSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public ZeroRateSensitivity cloned() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ZeroRateSensitivity}.
   * @return the meta-bean, not null
   */
  public static ZeroRateSensitivity.Meta meta() {
    return ZeroRateSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ZeroRateSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ZeroRateSensitivity(
      Currency curveCurrency,
      double yearFraction,
      Currency currency,
      double sensitivity) {
    JodaBeanUtils.notNull(curveCurrency, "curveCurrency");
    JodaBeanUtils.notNull(currency, "currency");
    this.curveCurrency = curveCurrency;
    this.yearFraction = yearFraction;
    this.currency = currency;
    this.sensitivity = sensitivity;
  }

  @Override
  public ZeroRateSensitivity.Meta metaBean() {
    return ZeroRateSensitivity.Meta.INSTANCE;
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
   * Gets the currency of the curve for which the sensitivity is computed.
   * @return the value of the property, not null
   */
  public Currency getCurveCurrency() {
    return curveCurrency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time that was queried, expressed as a year fraction.
   * @return the value of the property
   */
  public double getYearFraction() {
    return yearFraction;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the sensitivity.
   * @return the value of the property, not null
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
      ZeroRateSensitivity other = (ZeroRateSensitivity) obj;
      return JodaBeanUtils.equal(curveCurrency, other.curveCurrency) &&
          JodaBeanUtils.equal(yearFraction, other.yearFraction) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(curveCurrency);
    hash = hash * 31 + JodaBeanUtils.hashCode(yearFraction);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("ZeroRateSensitivity{");
    buf.append("curveCurrency").append('=').append(curveCurrency).append(',').append(' ');
    buf.append("yearFraction").append('=').append(yearFraction).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ZeroRateSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code curveCurrency} property.
     */
    private final MetaProperty<Currency> curveCurrency = DirectMetaProperty.ofImmutable(
        this, "curveCurrency", ZeroRateSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", ZeroRateSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", ZeroRateSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", ZeroRateSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "curveCurrency",
        "yearFraction",
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
        case 1303639584:  // curveCurrency
          return curveCurrency;
        case -1731780257:  // yearFraction
          return yearFraction;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ZeroRateSensitivity> builder() {
      return new ZeroRateSensitivity.Builder();
    }

    @Override
    public Class<? extends ZeroRateSensitivity> beanType() {
      return ZeroRateSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code curveCurrency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> curveCurrency() {
      return curveCurrency;
    }

    /**
     * The meta-property for the {@code yearFraction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> yearFraction() {
      return yearFraction;
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
        case 1303639584:  // curveCurrency
          return ((ZeroRateSensitivity) bean).getCurveCurrency();
        case -1731780257:  // yearFraction
          return ((ZeroRateSensitivity) bean).getYearFraction();
        case 575402001:  // currency
          return ((ZeroRateSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((ZeroRateSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code ZeroRateSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ZeroRateSensitivity> {

    private Currency curveCurrency;
    private double yearFraction;
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
        case 1303639584:  // curveCurrency
          return curveCurrency;
        case -1731780257:  // yearFraction
          return yearFraction;
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
        case 1303639584:  // curveCurrency
          this.curveCurrency = (Currency) newValue;
          break;
        case -1731780257:  // yearFraction
          this.yearFraction = (Double) newValue;
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
    public ZeroRateSensitivity build() {
      return new ZeroRateSensitivity(
          curveCurrency,
          yearFraction,
          currency,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("ZeroRateSensitivity.Builder{");
      buf.append("curveCurrency").append('=').append(JodaBeanUtils.toString(curveCurrency)).append(',').append(' ');
      buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
