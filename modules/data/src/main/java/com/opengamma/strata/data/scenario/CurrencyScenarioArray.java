/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Stream;

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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyAmountArray;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * A currency-convertible scenario array for a single currency, holding one amount for each scenario.
 * <p>
 * This contains a list of amounts in a single currency, one amount for each scenario.
 * The calculation runner is able to convert the currency of the values if required.
 * <p>
 * This class uses less memory than an instance based on a list of {@link CurrencyAmount} instances.
 * Internally, it stores the data using a single currency and a {@link DoubleArray}.
 */
@BeanDefinition(builderScope = "private")
public final class CurrencyScenarioArray
    implements ScenarioArray<CurrencyAmount>, ScenarioFxConvertible<CurrencyScenarioArray>, ImmutableBean {

  /**
   * The currency amounts, one per scenario.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmountArray amounts;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified currency and array of values.
   *
   * @param amounts  the amounts, one for each scenario
   * @return an instance with the specified currency and values
   */
  public static CurrencyScenarioArray of(CurrencyAmountArray amounts) {
    return new CurrencyScenarioArray(amounts);
  }

  /**
   * Obtains an instance from the specified currency and array of values.
   *
   * @param currency  the currency of the values
   * @param values  the values, one for each scenario
   * @return an instance with the specified currency and values
   */
  public static CurrencyScenarioArray of(Currency currency, DoubleArray values) {
    return new CurrencyScenarioArray(CurrencyAmountArray.of(currency, values));
  }

  /**
   * Obtains an instance from the specified list of amounts.
   * <p>
   * All amounts must have the same currency.
   *
   * @param amounts  the amounts, one for each scenario
   * @return an instance with the specified amounts
   * @throws IllegalArgumentException if multiple currencies are found
   */
  public static CurrencyScenarioArray of(List<CurrencyAmount> amounts) {
    return new CurrencyScenarioArray(CurrencyAmountArray.of(amounts));
  }

  /**
   * Obtains an instance using a function to create the entries.
   * <p>
   * The function is passed the scenario index and returns the {@code CurrencyAmount} for that index.
   * <p>
   * In some cases it may be possible to specify the currency with a function providing a {@code double}.
   * To do this, use {@link DoubleArray#of(int, java.util.function.IntToDoubleFunction)} and
   * then call {@link #of(Currency, DoubleArray)}.
   * 
   * @param size  the number of elements, at least size one
   * @param amountFunction  the function used to obtain each amount
   * @return an instance initialized using the function
   * @throws IllegalArgumentException is size is zero or less
   */
  public static CurrencyScenarioArray of(int size, IntFunction<CurrencyAmount> amountFunction) {
    return new CurrencyScenarioArray(CurrencyAmountArray.of(size, amountFunction));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency.
   * 
   * @return the currency
   */
  public Currency getCurrency() {
    return amounts.getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public int getScenarioCount() {
    return amounts.size();
  }

  @Override
  public CurrencyAmount get(int index) {
    return amounts.get(index);
  }

  @Override
  public Stream<CurrencyAmount> stream() {
    return amounts.stream();
  }

  @Override
  public CurrencyScenarioArray convertedTo(Currency reportingCurrency, ScenarioFxRateProvider fxRateProvider) {
    if (getCurrency().equals(reportingCurrency)) {
      return this;
    }
    if (fxRateProvider.getScenarioCount() != amounts.size()) {
      throw new IllegalArgumentException(Messages.format(
          "Expected {} FX rates but received {}", amounts.size(), fxRateProvider.getScenarioCount()));
    }
    DoubleArray convertedValues =
        amounts.getValues().mapWithIndex((i, v) -> v * fxRateProvider.fxRate(getCurrency(), reportingCurrency, i));
    return of(reportingCurrency, convertedValues);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CurrencyScenarioArray}.
   * @return the meta-bean, not null
   */
  public static CurrencyScenarioArray.Meta meta() {
    return CurrencyScenarioArray.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CurrencyScenarioArray.Meta.INSTANCE);
  }

  private CurrencyScenarioArray(
      CurrencyAmountArray amounts) {
    JodaBeanUtils.notNull(amounts, "amounts");
    this.amounts = amounts;
  }

  @Override
  public CurrencyScenarioArray.Meta metaBean() {
    return CurrencyScenarioArray.Meta.INSTANCE;
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
   * Gets the currency amounts, one per scenario.
   * @return the value of the property, not null
   */
  public CurrencyAmountArray getAmounts() {
    return amounts;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CurrencyScenarioArray other = (CurrencyScenarioArray) obj;
      return JodaBeanUtils.equal(amounts, other.amounts);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(amounts);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("CurrencyScenarioArray{");
    buf.append("amounts").append('=').append(JodaBeanUtils.toString(amounts));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CurrencyScenarioArray}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code amounts} property.
     */
    private final MetaProperty<CurrencyAmountArray> amounts = DirectMetaProperty.ofImmutable(
        this, "amounts", CurrencyScenarioArray.class, CurrencyAmountArray.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "amounts");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -879772901:  // amounts
          return amounts;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CurrencyScenarioArray> builder() {
      return new CurrencyScenarioArray.Builder();
    }

    @Override
    public Class<? extends CurrencyScenarioArray> beanType() {
      return CurrencyScenarioArray.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code amounts} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmountArray> amounts() {
      return amounts;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -879772901:  // amounts
          return ((CurrencyScenarioArray) bean).getAmounts();
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
   * The bean-builder for {@code CurrencyScenarioArray}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<CurrencyScenarioArray> {

    private CurrencyAmountArray amounts;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -879772901:  // amounts
          return amounts;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -879772901:  // amounts
          this.amounts = (CurrencyAmountArray) newValue;
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
    public CurrencyScenarioArray build() {
      return new CurrencyScenarioArray(
          amounts);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("CurrencyScenarioArray.Builder{");
      buf.append("amounts").append('=').append(JodaBeanUtils.toString(amounts));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
