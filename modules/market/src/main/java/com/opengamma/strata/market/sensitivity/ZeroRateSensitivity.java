/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import java.io.Serializable;
import java.time.LocalDate;
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
   * The currency of the sensitivity.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The date that was looked up on the curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate date;
  /**
   * The value of the sensitivity.
   */
  @PropertyDefinition(overrideGet = true)
  private final double sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code ZeroRateSensitivity} from the curve currency, date and value.
   * <p>
   * The currency representing the curve is used also for the sensitivity currency.
   * 
   * @param currency  the currency of the curve and sensitivity
   * @param date  the date that was looked up on the curve
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static ZeroRateSensitivity of(Currency currency, LocalDate date, double sensitivity) {
    return new ZeroRateSensitivity(currency, currency, date, sensitivity);
  }

  /**
   * Obtains a {@code ZeroRateSensitivity} from the curve currency, date, sensitivity currency and value.
   * <p>
   * The currency representing the curve is used also for the sensitivity currency.
   * 
   * @param curveCurrency  the currency of the curve
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param date  the date that was looked up on the curve
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static ZeroRateSensitivity of(
      Currency curveCurrency,
      Currency sensitivityCurrency,
      LocalDate date,
      double sensitivity) {
    return new ZeroRateSensitivity(curveCurrency, sensitivityCurrency, date, sensitivity);
  }

  //-------------------------------------------------------------------------
  @Override
  public ZeroRateSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new ZeroRateSensitivity(curveCurrency, currency, date, sensitivity);
  }

  @Override
  public ZeroRateSensitivity withSensitivity(double sensitivity) {
    return new ZeroRateSensitivity(curveCurrency, currency, date, sensitivity);
  }

  @Override
  public int compareExcludingSensitivity(PointSensitivity other) {
    if (other instanceof ZeroRateSensitivity) {
      ZeroRateSensitivity otherZero = (ZeroRateSensitivity) other;
      return ComparisonChain.start()
          .compare(curveCurrency, otherZero.curveCurrency)
          .compare(currency, otherZero.currency)
          .compare(date, otherZero.date)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  //-------------------------------------------------------------------------
  @Override
  public ZeroRateSensitivity multipliedBy(double factor) {
    return new ZeroRateSensitivity(curveCurrency, currency, date, sensitivity * factor);
  }

  @Override
  public ZeroRateSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new ZeroRateSensitivity(curveCurrency, currency, date, operator.applyAsDouble(sensitivity));
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
      Currency currency,
      LocalDate date,
      double sensitivity) {
    JodaBeanUtils.notNull(curveCurrency, "curveCurrency");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(date, "date");
    this.curveCurrency = curveCurrency;
    this.currency = currency;
    this.date = date;
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
   * Gets the currency of the sensitivity.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that was looked up on the curve.
   * @return the value of the property, not null
   */
  public LocalDate getDate() {
    return date;
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
      return JodaBeanUtils.equal(getCurveCurrency(), other.getCurveCurrency()) &&
          JodaBeanUtils.equal(getCurrency(), other.getCurrency()) &&
          JodaBeanUtils.equal(getDate(), other.getDate()) &&
          JodaBeanUtils.equal(getSensitivity(), other.getSensitivity());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurveCurrency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurrency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSensitivity());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("ZeroRateSensitivity{");
    buf.append("curveCurrency").append('=').append(getCurveCurrency()).append(',').append(' ');
    buf.append("currency").append('=').append(getCurrency()).append(',').append(' ');
    buf.append("date").append('=').append(getDate()).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(getSensitivity()));
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
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", ZeroRateSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code date} property.
     */
    private final MetaProperty<LocalDate> date = DirectMetaProperty.ofImmutable(
        this, "date", ZeroRateSensitivity.class, LocalDate.class);
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
        "currency",
        "date",
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
        case 575402001:  // currency
          return currency;
        case 3076014:  // date
          return date;
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
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code date} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> date() {
      return date;
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
        case 575402001:  // currency
          return ((ZeroRateSensitivity) bean).getCurrency();
        case 3076014:  // date
          return ((ZeroRateSensitivity) bean).getDate();
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
    private Currency currency;
    private LocalDate date;
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
        case 575402001:  // currency
          return currency;
        case 3076014:  // date
          return date;
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
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 3076014:  // date
          this.date = (LocalDate) newValue;
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
          currency,
          date,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("ZeroRateSensitivity.Builder{");
      buf.append("curveCurrency").append('=').append(JodaBeanUtils.toString(curveCurrency)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("date").append('=').append(JodaBeanUtils.toString(date)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
