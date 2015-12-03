/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.function.CalculationMultiFunction;
import com.opengamma.strata.calc.runner.function.CalculationSingleFunction;
import com.opengamma.strata.calc.runner.function.CurrencyConvertible;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Arrays of currency values in multiple currencies representing the result of the same calculation
 * performed for multiple scenarios.
 * <p>
 * For a large number of values it is more efficient to use this class than a list of individual
 * {@link MultiCurrencyAmount} instances. Internally this class uses a single map and primitive double
 * arrays for greater efficiency that storing the amounts using {@link MultiCurrencyAmount}.
 * <p>
 * This class is intended to be used as the return value from the {@code execute} method of
 * implementations of {@link CalculationSingleFunction} and {@link CalculationMultiFunction}.
 * <p>
 * Instances of this class will be automatically converted to the reporting currency by the calculation engine.
 */
@BeanDefinition(builderScope = "private")
public final class MultiCurrencyValuesArray
    implements CurrencyConvertible<CurrencyValuesArray>, ScenarioResult<MultiCurrencyAmount>, ImmutableBean {

  /** The currency values, keyed by currency. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Currency, DoubleArray> values;

  /** The number of values for each currency. */
  private final int size;

  /**
   * Returns an instance containing the values from the amounts.
   *
   * @param amounts  the amounts containing the currency amounts
   * @return an instance containing the values from the list of amounts
   */
  public static MultiCurrencyValuesArray of(MultiCurrencyAmount... amounts) {
    return of(Arrays.asList(amounts));
  }

  /**
   * Returns an instance containing the values from the list of amounts.
   *
   * @param amounts  the amounts containing the currency amounts
   * @return an instance containing the values from the list of amounts
   */
  public static MultiCurrencyValuesArray of(List<MultiCurrencyAmount> amounts) {
    int size = amounts.size();
    HashMap<Currency, double[]> valueMap = new HashMap<>();

    for (int i = 0; i < size; i++) {
      MultiCurrencyAmount multiCurrencyAmount = amounts.get(i);

      for (CurrencyAmount currencyAmount : multiCurrencyAmount.getAmounts()) {
        double[] currencyValues = valueMap.computeIfAbsent(currencyAmount.getCurrency(), ccy -> new double[size]);
        currencyValues[i] = currencyAmount.getAmount();
      }
    }
    Map<Currency, DoubleArray> doubleArrayMap =
        valueMap.entrySet().stream().collect(toMap(e -> e.getKey(), e -> DoubleArray.ofUnsafe(e.getValue())));

    return new MultiCurrencyValuesArray(doubleArrayMap);
  }

  @ImmutableConstructor
  private MultiCurrencyValuesArray(Map<Currency, DoubleArray> values) {
    this.values = ImmutableMap.copyOf(values);

    if (values.isEmpty()) {
      size = 0;
    } else {
      // All currencies must have the same number of values so we can just take the size of the first
      size = values.values().iterator().next().size();
    }
  }

  /**
   * Returns the set of currencies for which this object contains values.
   *
   * @return the set of currencies for which this object contains values
   */
  public Set<Currency> getCurrencies() {
    return values.keySet();
  }

  /**
   * Returns the values for the specified currency, throws an exception if there are no values for the currency.
   *
   * @param currency  the currency for which values are required
   * @return the values for the specified currency, throws an exception if there are none
   * @throws IllegalArgumentException if there are no values for the currency
   */
  public DoubleArray getValues(Currency currency) {
    DoubleArray currencyValues = values.get(currency);

    if (currencyValues == null) {
      throw new IllegalArgumentException("No values available for " + currency);
    }
    return currencyValues;
  }

  /**
   * Returns the number of currency values for each currency.
   *
   * @return the number of currency values for each currency
   */
  @Override
  public int size() {
    return size;
  }

  /**
   * Returns a {@link MultiCurrencyAmount} at the specified index.
   * <p>
   * This method is not very efficient for large sizes as a new object must be created at each index.
   * Consider using {@link #getValues(Currency)} instead.
   *
   * @param index  the index of the result that should be returned
   * @return a multi currency amount containing the currency values at the specified index
   */
  @Override
  public MultiCurrencyAmount get(int index) {
    List<CurrencyAmount> currencyAmounts = values.keySet().stream()
        .map(ccy -> CurrencyAmount.of(ccy, values.get(ccy).get(index)))
        .collect(toList());
    return MultiCurrencyAmount.of(currencyAmounts);
  }

  /**
   * Returns a stream of {@link MultiCurrencyAmount} instances containing the values from this object.
   * <p>
   * This method is not very efficient for large sizes as a new object must be created for each value.
   * Consider using {@link #getValues(Currency)} instead.
   *
   * @return a stream of multi currency amounts containing the currency values from this object
   */
  @Override
  public Stream<MultiCurrencyAmount> stream() {
    return IntStream.range(0, size).mapToObj(this::get);
  }

  @Override
  public CurrencyValuesArray convertedTo(Currency reportingCurrency, CalculationMarketData marketData) {
    double[] singleCurrencyValues = new double[size];

    for (Map.Entry<Currency, DoubleArray> entry : values.entrySet()) {
      Currency currency = entry.getKey();
      DoubleArray currencyValues = entry.getValue();
      MarketDataBox<FxRate> rates;
      rates = reportingCurrency.equals(currency) ?
          MarketDataBox.ofSingleValue(FxRate.of(currency, currency, 1)) : // TODO Remove if #613 is fixed
          marketData.getValue(FxRateKey.of(currency, reportingCurrency));
      checkNumberOfRates(rates.getScenarioCount());

      for (int i = 0; i < size; i++) {
        double convertedValue = rates.getValue(i).convert(currencyValues.get(i), currency, reportingCurrency);
        singleCurrencyValues[i] += convertedValue;
      }
    }
    return CurrencyValuesArray.of(reportingCurrency, DoubleArray.ofUnsafe(singleCurrencyValues));
  }

  private void checkNumberOfRates(int rateCount) {
    if (rateCount != 1 && size != rateCount) {
      throw new IllegalArgumentException(
          Messages.format(
              "Number of rates ({}) must be 1 or the same as the number of values ({})",
              rateCount,
              size));
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code MultiCurrencyValuesArray}.
   * @return the meta-bean, not null
   */
  public static MultiCurrencyValuesArray.Meta meta() {
    return MultiCurrencyValuesArray.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(MultiCurrencyValuesArray.Meta.INSTANCE);
  }

  @Override
  public MultiCurrencyValuesArray.Meta metaBean() {
    return MultiCurrencyValuesArray.Meta.INSTANCE;
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
   * Gets the currency values, keyed by currency.
   * @return the value of the property, not null
   */
  public ImmutableMap<Currency, DoubleArray> getValues() {
    return values;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      MultiCurrencyValuesArray other = (MultiCurrencyValuesArray) obj;
      return JodaBeanUtils.equal(values, other.values);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(values);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("MultiCurrencyValuesArray{");
    buf.append("values").append('=').append(JodaBeanUtils.toString(values));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code MultiCurrencyValuesArray}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code values} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Currency, DoubleArray>> values = DirectMetaProperty.ofImmutable(
        this, "values", MultiCurrencyValuesArray.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "values");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -823812830:  // values
          return values;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends MultiCurrencyValuesArray> builder() {
      return new MultiCurrencyValuesArray.Builder();
    }

    @Override
    public Class<? extends MultiCurrencyValuesArray> beanType() {
      return MultiCurrencyValuesArray.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code values} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Currency, DoubleArray>> values() {
      return values;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -823812830:  // values
          return ((MultiCurrencyValuesArray) bean).getValues();
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
   * The bean-builder for {@code MultiCurrencyValuesArray}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<MultiCurrencyValuesArray> {

    private Map<Currency, DoubleArray> values = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -823812830:  // values
          return values;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -823812830:  // values
          this.values = (Map<Currency, DoubleArray>) newValue;
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
    public MultiCurrencyValuesArray build() {
      return new MultiCurrencyValuesArray(
          values);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("MultiCurrencyValuesArray.Builder{");
      buf.append("values").append('=').append(JodaBeanUtils.toString(values));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
