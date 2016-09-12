/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
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

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.collect.Messages;

/**
 * The reporting currency.
 * <p>
 * This is used to specify the currency that the result should be reporting in.
 * The currency specified may be explicit, using {@link #of(Currency)}, or implicit
 * using {@link #NATURAL}. The "natural" currency of a target is obtained from
 * {@link CalculationFunction#naturalCurrency(CalculationTarget, ReferenceData)}.
 * <p>
 * If the result is not associated with a currency, such as for "par rate", then the
 * reporting currency will effectively be ignored.
 */
@BeanDefinition(builderScope = "private")
public final class ReportingCurrency
    implements ImmutableBean, Serializable {

  /**
   * An instance requesting the "natural" currency of the target.
   * <p>
   * When converting calculation results, conversion will occur to the "natural" currency of the target.
   * The "natural" currency of a target is obtained
   * from {@link CalculationFunction#naturalCurrency(CalculationTarget, ReferenceData)}.
   */
  public static final ReportingCurrency NATURAL = new ReportingCurrency(ReportingCurrencyType.NATURAL, null);
  /**
   * An instance requesting no currency conversion.
   * <p>
   * Calculation results are normally converted to a single currency.
   * If this reporting currency is used, then no currency conversion will be performed.
   */
  public static final ReportingCurrency NONE = new ReportingCurrency(ReportingCurrencyType.NONE, null);

  /**
   * The type of reporting currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ReportingCurrencyType type;
  /**
   * The reporting currency.
   * <p>
   * This property will be set only if the type is 'Specific'.
   */
  @PropertyDefinition(get = "field")
  private final Currency currency;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance requesting the specified currency.
   * <p>
   * When converting calculation results, conversion will occur to the specified currency.
   * This returns an instance with the type {@link ReportingCurrencyType#SPECIFIC}.
   *
   * @param currency  the specific currency
   * @return a reporting currency instance requesting the specified currency
   */
  public static ReportingCurrency of(Currency currency) {
    return new ReportingCurrency(ReportingCurrencyType.SPECIFIC, currency);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the type is 'Specific'.
   * <p>
   * When converting calculation results, conversion will occur to the specific currency
   * returned by {@link #getCurrency()}.
   * 
   * @return true if the type is 'Specific'
   */
  public boolean isSpecific() {
    return (type == ReportingCurrencyType.SPECIFIC);
  }

  /**
   * Checks if the type is 'Natural'.
   * <p>
   * When converting calculation results, conversion will occur to the "natural" currency of the target.
   * The "natural" currency of a target is obtained
   * from {@link CalculationFunction#naturalCurrency(CalculationTarget, ReferenceData)}.
   * 
   * @return true if the type is 'Natural'
   */
  public boolean isNatural() {
    return (type == ReportingCurrencyType.NATURAL);
  }

  /**
   * Checks if the type is 'None'.
   * <p>
   * Calculation results are normally converted to a single currency.
   * If this returns true than no currency conversion will be performed.
   * 
   * @return true if the type is 'None'
   */
  public boolean isNone() {
    return (type == ReportingCurrencyType.NONE);
  }

  /**
   * Gets the currency if the type is 'Specific'.
   * <p>
   * If the type is 'Specific', this returns the currency.
   * Otherwise, this throws an exception.
   * As such, the type must be checked using #is
   * 
   * @return the currency, only available if the type is 'Specific'
   * @throws IllegalStateException if called on a failure result
   */
  public Currency getCurrency() {
    if (!isSpecific()) {
      throw new IllegalStateException(Messages.format("No currency available for type '{}'", type));
    }
    return currency;
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return type + (currency != null ? ":" + currency.toString() : "");
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ReportingCurrency}.
   * @return the meta-bean, not null
   */
  public static ReportingCurrency.Meta meta() {
    return ReportingCurrency.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ReportingCurrency.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ReportingCurrency(
      ReportingCurrencyType type,
      Currency currency) {
    JodaBeanUtils.notNull(type, "type");
    this.type = type;
    this.currency = currency;
  }

  @Override
  public ReportingCurrency.Meta metaBean() {
    return ReportingCurrency.Meta.INSTANCE;
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
   * Gets the type of reporting currency.
   * @return the value of the property, not null
   */
  public ReportingCurrencyType getType() {
    return type;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ReportingCurrency other = (ReportingCurrency) obj;
      return JodaBeanUtils.equal(type, other.type) &&
          JodaBeanUtils.equal(currency, other.currency);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(type);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ReportingCurrency}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code type} property.
     */
    private final MetaProperty<ReportingCurrencyType> type = DirectMetaProperty.ofImmutable(
        this, "type", ReportingCurrency.class, ReportingCurrencyType.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", ReportingCurrency.class, Currency.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "type",
        "currency");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          return type;
        case 575402001:  // currency
          return currency;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ReportingCurrency> builder() {
      return new ReportingCurrency.Builder();
    }

    @Override
    public Class<? extends ReportingCurrency> beanType() {
      return ReportingCurrency.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code type} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ReportingCurrencyType> type() {
      return type;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          return ((ReportingCurrency) bean).getType();
        case 575402001:  // currency
          return ((ReportingCurrency) bean).currency;
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
   * The bean-builder for {@code ReportingCurrency}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ReportingCurrency> {

    private ReportingCurrencyType type;
    private Currency currency;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          return type;
        case 575402001:  // currency
          return currency;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3575610:  // type
          this.type = (ReportingCurrencyType) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
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
    public ReportingCurrency build() {
      return new ReportingCurrency(
          type,
          currency);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ReportingCurrency.Builder{");
      buf.append("type").append('=').append(JodaBeanUtils.toString(type)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
