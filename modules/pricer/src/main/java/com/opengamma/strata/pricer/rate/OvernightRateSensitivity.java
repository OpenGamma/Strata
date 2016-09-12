/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

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
import org.joda.beans.ImmutableValidator;
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
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Point sensitivity to a rate from an Overnight index curve.
 * <p>
 * Holds the sensitivity to the {@link OvernightIndex} curve for a fixing period.
 * <p>
 * This class handles the common case where the rate for a period is approximated
 * instead of computing the individual rate for each date in the period by storing
 * the end date of the fixing period.
 */
@BeanDefinition(builderScope = "private")
public final class OvernightRateSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The Overnight rate observation.
   * <p>
   * This includes the index and fixing date.
   */
  @PropertyDefinition(validate = "notNull")
  private final OvernightIndexObservation observation;
  /**
   * The end date of the period.
   * This must be after the fixing date.
   * It may be the maturity date implied by the fixing date, but it may also be later.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
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
   * Obtains an instance from the observation and sensitivity value.
   * <p>
   * The currency is defaulted from the index.
   * The end date will be the maturity date of the observation.
   * 
   * @param observation  the rate observation, including the fixing date
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static OvernightRateSensitivity of(OvernightIndexObservation observation, double sensitivity) {
    return of(observation, observation.getCurrency(), sensitivity);
  }

  /**
   * Obtains an instance from the observation and sensitivity value,
   * specifying the currency of the value.
   * <p>
   * The end date will be the maturity date of the observation.
   * 
   * @param observation  the rate observation, including the fixing date
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static OvernightRateSensitivity of(
      OvernightIndexObservation observation,
      Currency sensitivityCurrency,
      double sensitivity) {

    return new OvernightRateSensitivity(observation, observation.getMaturityDate(), sensitivityCurrency, sensitivity);
  }

  /**
   * Obtains an instance for a period observation of the index from the observation
   * and sensitivity value.
   * <p>
   * The currency is defaulted from the index.
   * 
   * @param observation  the rate observation, including the fixing date
   * @param endDate  the end date of the period
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static OvernightRateSensitivity ofPeriod(
      OvernightIndexObservation observation,
      LocalDate endDate,
      double sensitivity) {

    return ofPeriod(observation, endDate, observation.getCurrency(), sensitivity);
  }

  /**
   * Obtains an instance for a period observation of the index from the observation
   * and sensitivity value, specifying the currency of the value.
   * 
   * @param observation  the rate observation, including the fixing date
   * @param endDate  the end date of the period
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static OvernightRateSensitivity ofPeriod(
      OvernightIndexObservation observation,
      LocalDate endDate,
      Currency sensitivityCurrency,
      double sensitivity) {

    return new OvernightRateSensitivity(observation, endDate, sensitivityCurrency, sensitivity);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(observation.getFixingDate(), endDate, "fixingDate", "endDate");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the Overnight index that the sensitivity refers to.
   * 
   * @return the Overnight index
   */
  public OvernightIndex getIndex() {
    return observation.getIndex();
  }

  //-------------------------------------------------------------------------
  @Override
  public OvernightRateSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new OvernightRateSensitivity(observation, endDate, currency, sensitivity);
  }

  @Override
  public OvernightRateSensitivity withSensitivity(double sensitivity) {
    return new OvernightRateSensitivity(observation, endDate, currency, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof OvernightRateSensitivity) {
      OvernightRateSensitivity otherOn = (OvernightRateSensitivity) other;
      return ComparisonChain.start()
          .compare(getIndex().toString(), otherOn.getIndex().toString())
          .compare(currency, otherOn.currency)
          .compare(observation.getFixingDate(), otherOn.observation.getFixingDate())
          .compare(endDate, otherOn.endDate)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public OvernightRateSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (OvernightRateSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public OvernightRateSensitivity multipliedBy(double factor) {
    return new OvernightRateSensitivity(observation, endDate, currency, sensitivity * factor);
  }

  @Override
  public OvernightRateSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new OvernightRateSensitivity(observation, endDate, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public OvernightRateSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public OvernightRateSensitivity cloned() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code OvernightRateSensitivity}.
   * @return the meta-bean, not null
   */
  public static OvernightRateSensitivity.Meta meta() {
    return OvernightRateSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(OvernightRateSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private OvernightRateSensitivity(
      OvernightIndexObservation observation,
      LocalDate endDate,
      Currency currency,
      double sensitivity) {
    JodaBeanUtils.notNull(observation, "observation");
    JodaBeanUtils.notNull(endDate, "endDate");
    JodaBeanUtils.notNull(currency, "currency");
    this.observation = observation;
    this.endDate = endDate;
    this.currency = currency;
    this.sensitivity = sensitivity;
    validate();
  }

  @Override
  public OvernightRateSensitivity.Meta metaBean() {
    return OvernightRateSensitivity.Meta.INSTANCE;
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
   * Gets the Overnight rate observation.
   * <p>
   * This includes the index and fixing date.
   * @return the value of the property, not null
   */
  public OvernightIndexObservation getObservation() {
    return observation;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date of the period.
   * This must be after the fixing date.
   * It may be the maturity date implied by the fixing date, but it may also be later.
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
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
      OvernightRateSensitivity other = (OvernightRateSensitivity) obj;
      return JodaBeanUtils.equal(observation, other.observation) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(observation);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("OvernightRateSensitivity{");
    buf.append("observation").append('=').append(observation).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code OvernightRateSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code observation} property.
     */
    private final MetaProperty<OvernightIndexObservation> observation = DirectMetaProperty.ofImmutable(
        this, "observation", OvernightRateSensitivity.class, OvernightIndexObservation.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", OvernightRateSensitivity.class, LocalDate.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", OvernightRateSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", OvernightRateSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "observation",
        "endDate",
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
        case 122345516:  // observation
          return observation;
        case -1607727319:  // endDate
          return endDate;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends OvernightRateSensitivity> builder() {
      return new OvernightRateSensitivity.Builder();
    }

    @Override
    public Class<? extends OvernightRateSensitivity> beanType() {
      return OvernightRateSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code observation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<OvernightIndexObservation> observation() {
      return observation;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> endDate() {
      return endDate;
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
        case 122345516:  // observation
          return ((OvernightRateSensitivity) bean).getObservation();
        case -1607727319:  // endDate
          return ((OvernightRateSensitivity) bean).getEndDate();
        case 575402001:  // currency
          return ((OvernightRateSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((OvernightRateSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code OvernightRateSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<OvernightRateSensitivity> {

    private OvernightIndexObservation observation;
    private LocalDate endDate;
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
        case 122345516:  // observation
          return observation;
        case -1607727319:  // endDate
          return endDate;
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
        case 122345516:  // observation
          this.observation = (OvernightIndexObservation) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
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
    public OvernightRateSensitivity build() {
      return new OvernightRateSensitivity(
          observation,
          endDate,
          currency,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("OvernightRateSensitivity.Builder{");
      buf.append("observation").append('=').append(JodaBeanUtils.toString(observation)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
