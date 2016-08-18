/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit.cds;

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
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Point sensitivity to the zero hazard rate curve.
 * <p>
 * Holds the sensitivity to the zero hazard rate curve at a specific date.
 */
@BeanDefinition(builderScope = "private")
public final class CreditCurveZeroRateSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The currency of the curve for which the sensitivity is computed.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency curveCurrency;
  /**
   * The legal entity identifier.
   * <p>
   * This identifier is used for the reference legal entity of a credit derivative.
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId legalEntityId;
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
   * Obtains an instance.
   * 
   * @param currency  the currency of the curve and sensitivity
   * @param legalEntityId  the legal entity identifier
   * @param yearFraction  the year fraction that was looked up on the curve
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static CreditCurveZeroRateSensitivity of(
      Currency currency,
      double yearFraction,
      StandardId legalEntityId,
      double sensitivity) {

    return of(currency, legalEntityId, yearFraction, currency, sensitivity);
  }

  /**
   * Obtains an instance with sensitivity currency specified.
   * 
   * @param curveCurrency  the currency of the curve
   * @param legalEntityId  the legal entity identifier
   * @param yearFraction  the year fraction that was looked up on the curve
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static CreditCurveZeroRateSensitivity of(
      Currency curveCurrency,
      StandardId legalEntityId,
      double yearFraction,
      Currency sensitivityCurrency,
      double sensitivity) {

    return new CreditCurveZeroRateSensitivity(curveCurrency, legalEntityId, yearFraction, sensitivityCurrency, sensitivity);
  }

  /**
   * Obtains an instance from {@code ZeroRateSensitivity} and {@code StandardId}.
   * 
   * @param zeroRateSensitivity  the zero rate sensitivity
   * @param legalEntityId  the legal entity identifier
   * @return the point sensitivity object
   */
  public static CreditCurveZeroRateSensitivity of(
      ZeroRateSensitivity zeroRateSensitivity,
      StandardId legalEntityId) {

    return of(
        zeroRateSensitivity.getCurveCurrency(),
        legalEntityId,
        zeroRateSensitivity.getYearFraction(),
        zeroRateSensitivity.getCurrency(),
        zeroRateSensitivity.getSensitivity());
  }

  //-------------------------------------------------------------------------
  @Override
  public CreditCurveZeroRateSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new CreditCurveZeroRateSensitivity(curveCurrency, legalEntityId, yearFraction, currency, sensitivity);
  }

  @Override
  public CreditCurveZeroRateSensitivity withSensitivity(double sensitivity) {
    return new CreditCurveZeroRateSensitivity(curveCurrency, legalEntityId, yearFraction, currency, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof CreditCurveZeroRateSensitivity) {
      CreditCurveZeroRateSensitivity otherZero = (CreditCurveZeroRateSensitivity) other;
      return ComparisonChain.start()
          .compare(curveCurrency, otherZero.curveCurrency)
          .compare(legalEntityId, otherZero.legalEntityId)
          .compare(currency, otherZero.currency)
          .compare(yearFraction, otherZero.yearFraction)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public CreditCurveZeroRateSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (CreditCurveZeroRateSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public CreditCurveZeroRateSensitivity multipliedBy(double factor) {
    return new CreditCurveZeroRateSensitivity(curveCurrency, legalEntityId, yearFraction, currency, sensitivity * factor);
  }

  @Override
  public CreditCurveZeroRateSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new CreditCurveZeroRateSensitivity(curveCurrency, legalEntityId, yearFraction, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public CreditCurveZeroRateSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public CreditCurveZeroRateSensitivity cloned() {
    return this;
  }

  /**
   * Obtains the underlying {@code ZeroRateSensitivity}. 
   * <p>
   * This creates the zero rate sensitivity object by omitting the legal entity identifier.
   * 
   * @return the point sensitivity object
   */
  public ZeroRateSensitivity createZeroRateSensitivity() {
    return ZeroRateSensitivity.of(curveCurrency, yearFraction, currency, sensitivity);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CreditCurveZeroRateSensitivity}.
   * @return the meta-bean, not null
   */
  public static CreditCurveZeroRateSensitivity.Meta meta() {
    return CreditCurveZeroRateSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CreditCurveZeroRateSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private CreditCurveZeroRateSensitivity(
      Currency curveCurrency,
      StandardId legalEntityId,
      double yearFraction,
      Currency currency,
      double sensitivity) {
    JodaBeanUtils.notNull(curveCurrency, "curveCurrency");
    JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
    JodaBeanUtils.notNull(currency, "currency");
    this.curveCurrency = curveCurrency;
    this.legalEntityId = legalEntityId;
    this.yearFraction = yearFraction;
    this.currency = currency;
    this.sensitivity = sensitivity;
  }

  @Override
  public CreditCurveZeroRateSensitivity.Meta metaBean() {
    return CreditCurveZeroRateSensitivity.Meta.INSTANCE;
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
   * Gets the legal entity identifier.
   * <p>
   * This identifier is used for the reference legal entity of a credit derivative.
   * @return the value of the property, not null
   */
  public StandardId getLegalEntityId() {
    return legalEntityId;
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
      CreditCurveZeroRateSensitivity other = (CreditCurveZeroRateSensitivity) obj;
      return JodaBeanUtils.equal(curveCurrency, other.curveCurrency) &&
          JodaBeanUtils.equal(legalEntityId, other.legalEntityId) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(yearFraction);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("CreditCurveZeroRateSensitivity{");
    buf.append("curveCurrency").append('=').append(curveCurrency).append(',').append(' ');
    buf.append("legalEntityId").append('=').append(legalEntityId).append(',').append(' ');
    buf.append("yearFraction").append('=').append(yearFraction).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CreditCurveZeroRateSensitivity}.
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
        this, "curveCurrency", CreditCurveZeroRateSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code legalEntityId} property.
     */
    private final MetaProperty<StandardId> legalEntityId = DirectMetaProperty.ofImmutable(
        this, "legalEntityId", CreditCurveZeroRateSensitivity.class, StandardId.class);
    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", CreditCurveZeroRateSensitivity.class, Double.TYPE);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", CreditCurveZeroRateSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", CreditCurveZeroRateSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "curveCurrency",
        "legalEntityId",
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
        case 866287159:  // legalEntityId
          return legalEntityId;
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
    public BeanBuilder<? extends CreditCurveZeroRateSensitivity> builder() {
      return new CreditCurveZeroRateSensitivity.Builder();
    }

    @Override
    public Class<? extends CreditCurveZeroRateSensitivity> beanType() {
      return CreditCurveZeroRateSensitivity.class;
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
     * The meta-property for the {@code legalEntityId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> legalEntityId() {
      return legalEntityId;
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
          return ((CreditCurveZeroRateSensitivity) bean).getCurveCurrency();
        case 866287159:  // legalEntityId
          return ((CreditCurveZeroRateSensitivity) bean).getLegalEntityId();
        case -1731780257:  // yearFraction
          return ((CreditCurveZeroRateSensitivity) bean).getYearFraction();
        case 575402001:  // currency
          return ((CreditCurveZeroRateSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((CreditCurveZeroRateSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code CreditCurveZeroRateSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<CreditCurveZeroRateSensitivity> {

    private Currency curveCurrency;
    private StandardId legalEntityId;
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
        case 866287159:  // legalEntityId
          return legalEntityId;
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
        case 866287159:  // legalEntityId
          this.legalEntityId = (StandardId) newValue;
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
    public CreditCurveZeroRateSensitivity build() {
      return new CreditCurveZeroRateSensitivity(
          curveCurrency,
          legalEntityId,
          yearFraction,
          currency,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("CreditCurveZeroRateSensitivity.Builder{");
      buf.append("curveCurrency").append('=').append(JodaBeanUtils.toString(curveCurrency)).append(',').append(' ');
      buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
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
