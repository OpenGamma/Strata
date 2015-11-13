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
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.collect.Messages;

/**
 * Point sensitivity to a forward rate of an FX rate for an FX index.
 * <p>
 * Holds the sensitivity to the {@link FxIndex} curve at a fixing date.
 */
@BeanDefinition(builderScope = "private")
public final class FxIndexSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The index of the FX for which the sensitivity is computed.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxIndex index;
  /**
   * The reference currency.
   * <p>
   * This is the base currency of the FX conversion that occurs using the index.
   * The reference currency must be one of the two currencies of the index.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency referenceCurrency;
  /**
   * The fixing date to query the rate for.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate fixingDate;
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
   * Obtains a {@code FxIndexSensitivity} from index, reference currency, fixing date and sensitivity value.
   * <p>
   * The sensitivity currency is defaulted to be the index currency that is not the reference currency.
   * 
   * @param index  the index of the FX
   * @param referenceCurrency  the reference currency
   * @param fixingDate  the fixing date
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static FxIndexSensitivity of(
      FxIndex index,
      Currency referenceCurrency,
      LocalDate fixingDate,
      double sensitivity) {
    boolean inverse = referenceCurrency.equals(index.getCurrencyPair().getCounter());
    CurrencyPair pair = inverse ? index.getCurrencyPair().inverse() : index.getCurrencyPair();
    Currency sensiCurrency = pair.getCounter();
    return new FxIndexSensitivity(index, referenceCurrency, fixingDate, sensiCurrency, sensitivity);
  }

  /**
   * Obtains a {@code FxIndexSensitivity} from index, reference currency, fixing date,
   * sensitivity currency and sensitivity value.
   * 
   * @param index  the index of the FX
   * @param referenceCurrency  the reference currency
   * @param fixingDate  the fixing date
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static FxIndexSensitivity of(
      FxIndex index,
      Currency referenceCurrency,
      LocalDate fixingDate,
      Currency sensitivityCurrency,
      double sensitivity) {
    return new FxIndexSensitivity(index, referenceCurrency, fixingDate, sensitivityCurrency, sensitivity);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    if (!index.getCurrencyPair().contains(referenceCurrency)) {
      throw new IllegalArgumentException(
          Messages.format("Reference currency {} must be one of those in the FxIndex {}", referenceCurrency, index));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency counter to the reference currency.
   * <p>
   * The index contains two currencies. One is the reference currency.
   * This method returns the other.
   * 
   * @return the counter currency
   */
  public Currency getReferenceCounterCurrency() {
    boolean inverse = referenceCurrency.equals(index.getCurrencyPair().getBase());
    return inverse ? index.getCurrencyPair().getCounter() : index.getCurrencyPair().getBase();
  }

  /**
   * Converts this sensitivity to an {@code FxForwardSensitivity}.
   * <p>
   * The time series, fixing date and FX index are lost by this conversion.
   * Instead, maturity date and currency pair are contained in {@link FxForwardSensitivity}.
   * 
   * @return the FX forward sensitivity
   */
  public FxForwardSensitivity toFxForwardSensitivity() {
    LocalDate maturityDate = index.calculateMaturityFromFixing(fixingDate);
    return FxForwardSensitivity.of(index.getCurrencyPair(), referenceCurrency, maturityDate, currency, sensitivity);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new FxIndexSensitivity(index, referenceCurrency, fixingDate, currency, sensitivity);
  }

  @Override
  public FxIndexSensitivity withSensitivity(double sensitivity) {
    return new FxIndexSensitivity(index, referenceCurrency, fixingDate, currency, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof FxIndexSensitivity) {
      FxIndexSensitivity otherFx = (FxIndexSensitivity) other;
      return ComparisonChain.start()
          .compare(index.toString(), otherFx.index.toString())
          .compare(currency, otherFx.currency)
          .compare(referenceCurrency, otherFx.referenceCurrency)
          .compare(fixingDate, otherFx.fixingDate)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public FxIndexSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (FxIndexSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexSensitivity multipliedBy(double factor) {
    return new FxIndexSensitivity(index, referenceCurrency, fixingDate, currency, sensitivity * factor);
  }

  @Override
  public FxIndexSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new FxIndexSensitivity(index, referenceCurrency, fixingDate, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public FxIndexSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public FxIndexSensitivity cloned() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxIndexSensitivity}.
   * @return the meta-bean, not null
   */
  public static FxIndexSensitivity.Meta meta() {
    return FxIndexSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxIndexSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private FxIndexSensitivity(
      FxIndex index,
      Currency referenceCurrency,
      LocalDate fixingDate,
      Currency currency,
      double sensitivity) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(referenceCurrency, "referenceCurrency");
    JodaBeanUtils.notNull(fixingDate, "fixingDate");
    JodaBeanUtils.notNull(currency, "currency");
    this.index = index;
    this.referenceCurrency = referenceCurrency;
    this.fixingDate = fixingDate;
    this.currency = currency;
    this.sensitivity = sensitivity;
    validate();
  }

  @Override
  public FxIndexSensitivity.Meta metaBean() {
    return FxIndexSensitivity.Meta.INSTANCE;
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
   * Gets the index of the FX for which the sensitivity is computed.
   * @return the value of the property, not null
   */
  public FxIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reference currency.
   * <p>
   * This is the base currency of the FX conversion that occurs using the index.
   * The reference currency must be one of the two currencies of the index.
   * @return the value of the property, not null
   */
  public Currency getReferenceCurrency() {
    return referenceCurrency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixing date to query the rate for.
   * @return the value of the property, not null
   */
  public LocalDate getFixingDate() {
    return fixingDate;
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
      FxIndexSensitivity other = (FxIndexSensitivity) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(referenceCurrency, other.referenceCurrency) &&
          JodaBeanUtils.equal(fixingDate, other.fixingDate) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(referenceCurrency);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixingDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("FxIndexSensitivity{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("referenceCurrency").append('=').append(referenceCurrency).append(',').append(' ');
    buf.append("fixingDate").append('=').append(fixingDate).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxIndexSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<FxIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", FxIndexSensitivity.class, FxIndex.class);
    /**
     * The meta-property for the {@code referenceCurrency} property.
     */
    private final MetaProperty<Currency> referenceCurrency = DirectMetaProperty.ofImmutable(
        this, "referenceCurrency", FxIndexSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code fixingDate} property.
     */
    private final MetaProperty<LocalDate> fixingDate = DirectMetaProperty.ofImmutable(
        this, "fixingDate", FxIndexSensitivity.class, LocalDate.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", FxIndexSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", FxIndexSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "referenceCurrency",
        "fixingDate",
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
        case 100346066:  // index
          return index;
        case 727652476:  // referenceCurrency
          return referenceCurrency;
        case 1255202043:  // fixingDate
          return fixingDate;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FxIndexSensitivity> builder() {
      return new FxIndexSensitivity.Builder();
    }

    @Override
    public Class<? extends FxIndexSensitivity> beanType() {
      return FxIndexSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code referenceCurrency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> referenceCurrency() {
      return referenceCurrency;
    }

    /**
     * The meta-property for the {@code fixingDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> fixingDate() {
      return fixingDate;
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
        case 100346066:  // index
          return ((FxIndexSensitivity) bean).getIndex();
        case 727652476:  // referenceCurrency
          return ((FxIndexSensitivity) bean).getReferenceCurrency();
        case 1255202043:  // fixingDate
          return ((FxIndexSensitivity) bean).getFixingDate();
        case 575402001:  // currency
          return ((FxIndexSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((FxIndexSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code FxIndexSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<FxIndexSensitivity> {

    private FxIndex index;
    private Currency referenceCurrency;
    private LocalDate fixingDate;
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
        case 100346066:  // index
          return index;
        case 727652476:  // referenceCurrency
          return referenceCurrency;
        case 1255202043:  // fixingDate
          return fixingDate;
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
        case 100346066:  // index
          this.index = (FxIndex) newValue;
          break;
        case 727652476:  // referenceCurrency
          this.referenceCurrency = (Currency) newValue;
          break;
        case 1255202043:  // fixingDate
          this.fixingDate = (LocalDate) newValue;
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
    public FxIndexSensitivity build() {
      return new FxIndexSensitivity(
          index,
          referenceCurrency,
          fixingDate,
          currency,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("FxIndexSensitivity.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("referenceCurrency").append('=').append(JodaBeanUtils.toString(referenceCurrency)).append(',').append(' ');
      buf.append("fixingDate").append('=').append(JodaBeanUtils.toString(fixingDate)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
