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
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Point sensitivity to a forward rate of an FX rate for a currency pair.
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
   * The currency of the sensitivity.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The base currency that the rate was expressed against.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency baseCurrency;
  /**
   * The fixing date to query the rate for
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate fixingDate;
  /**
   * The value of the sensitivity.
   */
  @PropertyDefinition(overrideGet = true)
  private final double sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code FxIndexSensitivity} from index, base currency, fixing date and sensitivity value. 
   * <p>
   * The sensitivity currency is defaulted to be the counter currency of the currency pair. 
   * 
   * @param index  the index of the FX
   * @param baseCurrency  the base currency 
   * @param fixingDate  the fixing date
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static FxIndexSensitivity of(
      FxIndex index,
      Currency baseCurrency,
      LocalDate fixingDate,
      double sensitivity) {
    boolean inverse = baseCurrency.equals(index.getCurrencyPair().getCounter());
    CurrencyPair pair = inverse ? index.getCurrencyPair().inverse() : index.getCurrencyPair();
    Currency sensiCurrency = pair.getCounter();
    return new FxIndexSensitivity(index, sensiCurrency, baseCurrency, fixingDate, sensitivity);
  }

  /**
   * Obtains a {@code FxIndexSensitivity} from index, sensitivity currency, base currency, 
   * fixing date and sensitivity value. 
   * 
   * @param index  the index of the FX
   * @param currency  the currency of the sensitivity
   * @param baseCurrency  the base currency 
   * @param fixingDate  the fixing date
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static FxIndexSensitivity of(
      FxIndex index,
      Currency currency,
      Currency baseCurrency,
      LocalDate fixingDate,
      double sensitivity) {
    return new FxIndexSensitivity(index, currency, baseCurrency, fixingDate, sensitivity);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(
        index.getCurrencyPair().contains(baseCurrency), "baseCurrency should be one of the currency pair");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency counter to the base currency.
   * 
   * @return the counter currency 
   */
  public Currency getCounterCurrency() {
    boolean inverse = baseCurrency.equals(index.getCurrencyPair().getCounter());
    CurrencyPair pair = inverse ? index.getCurrencyPair().inverse() : index.getCurrencyPair();
    return pair.getCounter();
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new FxIndexSensitivity(index, currency, baseCurrency, fixingDate, sensitivity);
  }

  @Override
  public FxIndexSensitivity withSensitivity(double sensitivity) {
    return new FxIndexSensitivity(index, currency, baseCurrency, fixingDate, sensitivity);
  }

  @Override
  public int compareExcludingSensitivity(PointSensitivity other) {
    if (other instanceof FxIndexSensitivity) {
      FxIndexSensitivity otherFx = (FxIndexSensitivity) other;
      return ComparisonChain.start()
          .compare(index.toString(), otherFx.index.toString())
          .compare(currency, otherFx.currency)
          .compare(baseCurrency, otherFx.baseCurrency)
          .compare(fixingDate, otherFx.fixingDate)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  //-------------------------------------------------------------------------
  @Override
  public FxIndexSensitivity multipliedBy(double factor) {
    return new FxIndexSensitivity(index, currency, baseCurrency, fixingDate, sensitivity * factor);
  }

  @Override
  public FxIndexSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new FxIndexSensitivity(index, currency, baseCurrency, fixingDate, operator.applyAsDouble(sensitivity));
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
      Currency currency,
      Currency baseCurrency,
      LocalDate fixingDate,
      double sensitivity) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(baseCurrency, "baseCurrency");
    JodaBeanUtils.notNull(fixingDate, "fixingDate");
    this.index = index;
    this.currency = currency;
    this.baseCurrency = baseCurrency;
    this.fixingDate = fixingDate;
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
   * Gets the currency of the sensitivity.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the base currency that the rate was expressed against.
   * @return the value of the property, not null
   */
  public Currency getBaseCurrency() {
    return baseCurrency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixing date to query the rate for
   * @return the value of the property, not null
   */
  public LocalDate getFixingDate() {
    return fixingDate;
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
      return JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getCurrency(), other.getCurrency()) &&
          JodaBeanUtils.equal(getBaseCurrency(), other.getBaseCurrency()) &&
          JodaBeanUtils.equal(getFixingDate(), other.getFixingDate()) &&
          JodaBeanUtils.equal(getSensitivity(), other.getSensitivity());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurrency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getBaseCurrency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFixingDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSensitivity());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("FxIndexSensitivity{");
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("currency").append('=').append(getCurrency()).append(',').append(' ');
    buf.append("baseCurrency").append('=').append(getBaseCurrency()).append(',').append(' ');
    buf.append("fixingDate").append('=').append(getFixingDate()).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(getSensitivity()));
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
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", FxIndexSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code baseCurrency} property.
     */
    private final MetaProperty<Currency> baseCurrency = DirectMetaProperty.ofImmutable(
        this, "baseCurrency", FxIndexSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code fixingDate} property.
     */
    private final MetaProperty<LocalDate> fixingDate = DirectMetaProperty.ofImmutable(
        this, "fixingDate", FxIndexSensitivity.class, LocalDate.class);
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
        "currency",
        "baseCurrency",
        "fixingDate",
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
        case 575402001:  // currency
          return currency;
        case -1093862910:  // baseCurrency
          return baseCurrency;
        case 1255202043:  // fixingDate
          return fixingDate;
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
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code baseCurrency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> baseCurrency() {
      return baseCurrency;
    }

    /**
     * The meta-property for the {@code fixingDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> fixingDate() {
      return fixingDate;
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
        case 575402001:  // currency
          return ((FxIndexSensitivity) bean).getCurrency();
        case -1093862910:  // baseCurrency
          return ((FxIndexSensitivity) bean).getBaseCurrency();
        case 1255202043:  // fixingDate
          return ((FxIndexSensitivity) bean).getFixingDate();
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
    private Currency currency;
    private Currency baseCurrency;
    private LocalDate fixingDate;
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
        case 575402001:  // currency
          return currency;
        case -1093862910:  // baseCurrency
          return baseCurrency;
        case 1255202043:  // fixingDate
          return fixingDate;
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
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case -1093862910:  // baseCurrency
          this.baseCurrency = (Currency) newValue;
          break;
        case 1255202043:  // fixingDate
          this.fixingDate = (LocalDate) newValue;
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
          currency,
          baseCurrency,
          fixingDate,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("FxIndexSensitivity.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("baseCurrency").append('=').append(JodaBeanUtils.toString(baseCurrency)).append(',').append(' ');
      buf.append("fixingDate").append('=').append(JodaBeanUtils.toString(fixingDate)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
