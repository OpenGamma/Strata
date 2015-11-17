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
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.market.value.LegalEntityGroup;

/**
 * Point sensitivity to the issuer curve.
 * <p>
 * Holds the sensitivity to the issuer curve at a specific date.
 */
@BeanDefinition(builderScope = "private")
public final class IssuerCurveZeroRateSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The currency of the curve for which the sensitivity is computed.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency curveCurrency;
  /**
   * The date that was looked up on the curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate date;
  /**
   * The currency of the sensitivity.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The legal entity group. 
   * <p>
   * The group defines the legal entity that the discount factors are for.
   */
  @PropertyDefinition(validate = "notNull")
  private final LegalEntityGroup legalEntityGroup;
  /**
   * The value of the sensitivity.
   */
  @PropertyDefinition(overrideGet = true)
  private final double sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code IssuerCurveZeroRateSensitivity} from the curve currency, date, legal entity group and value.
   * <p>
   * The currency representing the curve is used also for the sensitivity currency.
   * 
   * @param currency  the currency of the curve and sensitivity
   * @param date  the date that was looked up on the curve
   * @param legalEntityGroup  the legal entity group
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static IssuerCurveZeroRateSensitivity of(
      Currency currency,
      LocalDate date,
      LegalEntityGroup legalEntityGroup,
      double sensitivity) {
    return of(currency, date, currency, legalEntityGroup, sensitivity);
  }

  /**
   * Obtains a {@code IssuerCurveZeroRateSensitivity} from zero rate sensitivity and legal entity group. 
   * 
   * @param zeroRateSensitivity  the zero rate sensitivity
   * @param legalEntityGroup  the legal entity group
   * @return the point sensitivity object
   */
  public static IssuerCurveZeroRateSensitivity of(
      ZeroRateSensitivity zeroRateSensitivity,
      LegalEntityGroup legalEntityGroup) {
    return of(
        zeroRateSensitivity.getCurveCurrency(),
        zeroRateSensitivity.getDate(),
        zeroRateSensitivity.getCurrency(),
        legalEntityGroup,
        zeroRateSensitivity.getSensitivity());
  }

  /**
   * Obtains a {@code IssuerCurveZeroRateSensitivity} from the curve currency, date, sensitivity currency,
   * legal entity group and value.
   * 
   * @param curveCurrency  the currency of the curve
   * @param date  the date that was looked up on the curve
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param legalEntityGroup  the legal entity group
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static IssuerCurveZeroRateSensitivity of(
      Currency curveCurrency,
      LocalDate date,
      Currency sensitivityCurrency,
      LegalEntityGroup legalEntityGroup,
      double sensitivity) {
    return new IssuerCurveZeroRateSensitivity(curveCurrency, date, sensitivityCurrency, legalEntityGroup, sensitivity);
  }

  //-------------------------------------------------------------------------
  @Override
  public IssuerCurveZeroRateSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new IssuerCurveZeroRateSensitivity(curveCurrency, date, currency, legalEntityGroup, sensitivity);
  }

  @Override
  public IssuerCurveZeroRateSensitivity withSensitivity(double sensitivity) {
    return new IssuerCurveZeroRateSensitivity(curveCurrency, date, currency, legalEntityGroup, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof IssuerCurveZeroRateSensitivity) {
      IssuerCurveZeroRateSensitivity otherZero = (IssuerCurveZeroRateSensitivity) other;
      return ComparisonChain.start()
          .compare(curveCurrency, otherZero.curveCurrency)
          .compare(currency, otherZero.currency)
          .compare(date, otherZero.date)
          .compare(legalEntityGroup, otherZero.legalEntityGroup)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public IssuerCurveZeroRateSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (IssuerCurveZeroRateSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public IssuerCurveZeroRateSensitivity multipliedBy(double factor) {
    return new IssuerCurveZeroRateSensitivity(curveCurrency, date, currency, legalEntityGroup, sensitivity * factor);
  }

  @Override
  public IssuerCurveZeroRateSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new IssuerCurveZeroRateSensitivity(
        curveCurrency, date, currency, legalEntityGroup, operator.applyAsDouble(sensitivity));
  }

  @Override
  public IssuerCurveZeroRateSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public IssuerCurveZeroRateSensitivity cloned() {
    return this;
  }

  /**
   * Obtains the underlying {@code ZeroRateSensitivity}. 
   * <p>
   * This creates the zero rate sensitivity object by omitting the legal entity group. 
   * 
   * @return the point sensitivity object
   */
  public ZeroRateSensitivity createZeroRateSensitivity() {
    return ZeroRateSensitivity.of(curveCurrency, date, currency, sensitivity);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IssuerCurveZeroRateSensitivity}.
   * @return the meta-bean, not null
   */
  public static IssuerCurveZeroRateSensitivity.Meta meta() {
    return IssuerCurveZeroRateSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IssuerCurveZeroRateSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private IssuerCurveZeroRateSensitivity(
      Currency curveCurrency,
      LocalDate date,
      Currency currency,
      LegalEntityGroup legalEntityGroup,
      double sensitivity) {
    JodaBeanUtils.notNull(curveCurrency, "curveCurrency");
    JodaBeanUtils.notNull(date, "date");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(legalEntityGroup, "legalEntityGroup");
    this.curveCurrency = curveCurrency;
    this.date = date;
    this.currency = currency;
    this.legalEntityGroup = legalEntityGroup;
    this.sensitivity = sensitivity;
  }

  @Override
  public IssuerCurveZeroRateSensitivity.Meta metaBean() {
    return IssuerCurveZeroRateSensitivity.Meta.INSTANCE;
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
   * Gets the date that was looked up on the curve.
   * @return the value of the property, not null
   */
  public LocalDate getDate() {
    return date;
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
   * Gets the legal entity group.
   * <p>
   * The group defines the legal entity that the discount factors are for.
   * @return the value of the property, not null
   */
  public LegalEntityGroup getLegalEntityGroup() {
    return legalEntityGroup;
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
      IssuerCurveZeroRateSensitivity other = (IssuerCurveZeroRateSensitivity) obj;
      return JodaBeanUtils.equal(curveCurrency, other.curveCurrency) &&
          JodaBeanUtils.equal(date, other.date) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(legalEntityGroup, other.legalEntityGroup) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(curveCurrency);
    hash = hash * 31 + JodaBeanUtils.hashCode(date);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityGroup);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("IssuerCurveZeroRateSensitivity{");
    buf.append("curveCurrency").append('=').append(curveCurrency).append(',').append(' ');
    buf.append("date").append('=').append(date).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("legalEntityGroup").append('=').append(legalEntityGroup).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IssuerCurveZeroRateSensitivity}.
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
        this, "curveCurrency", IssuerCurveZeroRateSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code date} property.
     */
    private final MetaProperty<LocalDate> date = DirectMetaProperty.ofImmutable(
        this, "date", IssuerCurveZeroRateSensitivity.class, LocalDate.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", IssuerCurveZeroRateSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code legalEntityGroup} property.
     */
    private final MetaProperty<LegalEntityGroup> legalEntityGroup = DirectMetaProperty.ofImmutable(
        this, "legalEntityGroup", IssuerCurveZeroRateSensitivity.class, LegalEntityGroup.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", IssuerCurveZeroRateSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "curveCurrency",
        "date",
        "currency",
        "legalEntityGroup",
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
        case 3076014:  // date
          return date;
        case 575402001:  // currency
          return currency;
        case -899047453:  // legalEntityGroup
          return legalEntityGroup;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends IssuerCurveZeroRateSensitivity> builder() {
      return new IssuerCurveZeroRateSensitivity.Builder();
    }

    @Override
    public Class<? extends IssuerCurveZeroRateSensitivity> beanType() {
      return IssuerCurveZeroRateSensitivity.class;
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
     * The meta-property for the {@code date} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> date() {
      return date;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code legalEntityGroup} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LegalEntityGroup> legalEntityGroup() {
      return legalEntityGroup;
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
          return ((IssuerCurveZeroRateSensitivity) bean).getCurveCurrency();
        case 3076014:  // date
          return ((IssuerCurveZeroRateSensitivity) bean).getDate();
        case 575402001:  // currency
          return ((IssuerCurveZeroRateSensitivity) bean).getCurrency();
        case -899047453:  // legalEntityGroup
          return ((IssuerCurveZeroRateSensitivity) bean).getLegalEntityGroup();
        case 564403871:  // sensitivity
          return ((IssuerCurveZeroRateSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code IssuerCurveZeroRateSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<IssuerCurveZeroRateSensitivity> {

    private Currency curveCurrency;
    private LocalDate date;
    private Currency currency;
    private LegalEntityGroup legalEntityGroup;
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
        case 3076014:  // date
          return date;
        case 575402001:  // currency
          return currency;
        case -899047453:  // legalEntityGroup
          return legalEntityGroup;
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
        case 3076014:  // date
          this.date = (LocalDate) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case -899047453:  // legalEntityGroup
          this.legalEntityGroup = (LegalEntityGroup) newValue;
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
    public IssuerCurveZeroRateSensitivity build() {
      return new IssuerCurveZeroRateSensitivity(
          curveCurrency,
          date,
          currency,
          legalEntityGroup,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("IssuerCurveZeroRateSensitivity.Builder{");
      buf.append("curveCurrency").append('=').append(JodaBeanUtils.toString(curveCurrency)).append(',').append(' ');
      buf.append("date").append('=').append(JodaBeanUtils.toString(date)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("legalEntityGroup").append('=').append(JodaBeanUtils.toString(legalEntityGroup)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
