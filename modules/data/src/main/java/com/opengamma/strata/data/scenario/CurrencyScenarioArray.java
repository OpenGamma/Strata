/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static com.opengamma.strata.collect.Guavate.ensureOnlyOne;

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
import com.opengamma.strata.collect.ArgChecker;
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
   * The currency of the values.
   * All values have the same currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The calculated values, one per scenario.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray values;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified currency and array of values.
   *
   * @param currency  the currency of the values
   * @param values  the values, one for each scenario
   * @return an instance with the specified currency and values
   */
  public static CurrencyScenarioArray of(Currency currency, DoubleArray values) {
    return new CurrencyScenarioArray(currency, values);
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
    Currency currency = amounts.stream()
        .map(ca -> ca.getCurrency())
        .distinct()
        .reduce(ensureOnlyOne())
        .get();
    double[] values = amounts.stream()
        .mapToDouble(ca -> ca.getAmount())
        .toArray();
    return new CurrencyScenarioArray(currency, DoubleArray.ofUnsafe(values));
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
   * @param valueFunction  the function used to obtain each value
   * @return an instance initialized using the function
   * @throws IllegalArgumentException is size is zero or less
   */
  public static CurrencyScenarioArray of(int size, IntFunction<CurrencyAmount> valueFunction) {
    ArgChecker.notNegativeOrZero(size, "size");
    double[] array = new double[size];
    CurrencyAmount ca0 = valueFunction.apply(0);
    Currency currency = ca0.getCurrency();
    array[0] = ca0.getAmount();
    for (int i = 1; i < size; i++) {
      CurrencyAmount ca = valueFunction.apply(i);
      if (!ca.getCurrency().equals(currency)) {
        throw new IllegalArgumentException(Messages.format("Currencies differ: {} and {}", currency, ca.getCurrency()));
      }
      array[i] = ca.getAmount();
    }
    return new CurrencyScenarioArray(currency, DoubleArray.ofUnsafe(array));
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyScenarioArray convertedTo(Currency reportingCurrency, ScenarioFxRateProvider fxRateProvider) {
    if (currency.equals(reportingCurrency)) {
      return this;
    }
    if (fxRateProvider.getScenarioCount() != values.size()) {
      throw new IllegalArgumentException(Messages.format(
          "Expected {} FX rates but received {}", values.size(), fxRateProvider.getScenarioCount()));
    }
    DoubleArray convertedValues =
        values.mapWithIndex((i, v) -> v * fxRateProvider.fxRate(currency, reportingCurrency, i));
    return new CurrencyScenarioArray(reportingCurrency, convertedValues);
  }

  @Override
  public int getScenarioCount() {
    return values.size();
  }

  @Override
  public CurrencyAmount get(int index) {
    return CurrencyAmount.of(currency, values.get(index));
  }

  @Override
  public Stream<CurrencyAmount> stream() {
    return values.stream().mapToObj(amount -> CurrencyAmount.of(currency, amount));
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
      Currency currency,
      DoubleArray values) {
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(values, "values");
    this.currency = currency;
    this.values = values;
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
   * Gets the currency of the values.
   * All values have the same currency.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the calculated values, one per scenario.
   * @return the value of the property, not null
   */
  public DoubleArray getValues() {
    return values;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CurrencyScenarioArray other = (CurrencyScenarioArray) obj;
      return JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(values, other.values);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(values);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("CurrencyScenarioArray{");
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("values").append('=').append(JodaBeanUtils.toString(values));
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
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", CurrencyScenarioArray.class, Currency.class);
    /**
     * The meta-property for the {@code values} property.
     */
    private final MetaProperty<DoubleArray> values = DirectMetaProperty.ofImmutable(
        this, "values", CurrencyScenarioArray.class, DoubleArray.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currency",
        "values");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case -823812830:  // values
          return values;
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
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code values} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> values() {
      return values;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return ((CurrencyScenarioArray) bean).getCurrency();
        case -823812830:  // values
          return ((CurrencyScenarioArray) bean).getValues();
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

    private Currency currency;
    private DoubleArray values;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case -823812830:  // values
          return values;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case -823812830:  // values
          this.values = (DoubleArray) newValue;
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
          currency,
          values);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("CurrencyScenarioArray.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("values").append('=').append(JodaBeanUtils.toString(values));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
