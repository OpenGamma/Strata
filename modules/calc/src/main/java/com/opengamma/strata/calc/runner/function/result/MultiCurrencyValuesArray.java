/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.joda.beans.Bean;
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.UnmodifiableIterator;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.calc.runner.function.CalculationMultiFunction;
import com.opengamma.strata.calc.runner.function.CalculationSingleFunction;
import com.opengamma.strata.calc.runner.function.CurrencyConvertible;
import com.opengamma.strata.collect.Messages;

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
@BeanDefinition
public final class MultiCurrencyValuesArray
    implements CurrencyConvertible<CurrencyValuesArray>, ScenarioResult<MultiCurrencyAmount>, ImmutableBean {

  /** The currency values, keyed by currency. */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<Currency, double[]> values;

  /** The number of values for each currency. */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final int size;

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
        double[] currencyValues = currencyValuesArray(valueMap, currencyAmount.getCurrency(), size);
        currencyValues[i] = currencyAmount.getAmount();
      }
    }
    return new MultiCurrencyValuesArray(valueMap, size);
  }

  /**
   * Returns an array from the map keyed by the specified currency. If there is no entry for the currency
   * an array is created with the specified size and inserted into the map.
   *
   * @param mutableMap  the map of currency to values, new values are inserted if needed
   * @param currency  the currency whose array is required
   * @param size  the size of the currency arrays
   * @return the array for the specified currency, created and inserted into the map if necessary
   */
  private static double[] currencyValuesArray(HashMap<Currency, double[]> mutableMap, Currency currency, int size) {
    double[] currencyValues = mutableMap.get(currency);

    if (currencyValues != null) {
      return currencyValues;
    }
    double[] newArray = new double[size];
    mutableMap.put(currency, newArray);
    return newArray;
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
  public double[] getValues(Currency currency) {
    return getValuesUnsafe(currency).clone();
  }

  /**
   * Returns the values for the specified currency without copying the underlying array,
   * throws an exception if there are no values for the currency.
   * <p>
   * This method returns the underlying array without copying for efficiency so the caller
   * <strong>must not</strong> mutate it. Doing so would violate the immutability of this class.
   *
   * @param currency  the currency for which values are required
   * @return the values for the specified currency, throws an exception if there are none
   * @throws IllegalArgumentException if there are no values for the currency
   */
  public double[] getValuesUnsafe(Currency currency) {
    double[] currencyValues = values.get(currency);

    if (currencyValues == null) {
      throw new IllegalArgumentException("No values available for " + currency);
    }
    return currencyValues;
  }

  /**
   * Returns the values for all currencies without copying the underlying arrays.
   * <p>
   * This method returns the underlying arrays without copying for efficiency so the caller
   * <strong>must not</strong> mutate them. Doing so would violate the immutability of this class.
   *
   * @return the values keyed by currency
   */
  public Map<Currency, double[]> getValuesUnsafe() {
    return values;
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
   * Prefer {@link #getValues(Currency)} and {@link #getValuesUnsafe(Currency)}
   *
   * @param index  the index of the result that should be returned
   * @return a multi currency amount containing the currency values at the specified index
   */
  @Override
  public MultiCurrencyAmount get(int index) {
    List<CurrencyAmount> currencyAmounts = values.keySet().stream()
        .map(ccy -> CurrencyAmount.of(ccy, values.get(ccy)[index]))
        .collect(toList());
    return MultiCurrencyAmount.of(currencyAmounts);
  }

  /**
   * Returns a stream of {@link MultiCurrencyAmount} instances containing the values from this object.
   * <p>
   * This method is not very efficient for large sizes as a new object must be created for each value.
   * Prefer {@link #getValues(Currency)} and {@link #getValuesUnsafe(Currency)}
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

    for (Map.Entry<Currency, double[]> entry : values.entrySet()) {
      Currency currency = entry.getKey();
      double[] currencyValues = entry.getValue();
      MarketDataBox<FxRate> rates;
      rates = reportingCurrency.equals(currency) ?
          MarketDataBox.ofSingleValue(FxRate.of(currency, currency, 1)) : // TODO Remove if #613 is fixed
          marketData.getValue(FxRateKey.of(currency, reportingCurrency));
      checkNumberOfRates(rates.getScenarioCount());

      for (int i = 0; i < size; i++) {
        double convertedValue = rates.getValue(i).convert(currencyValues[i], currency, reportingCurrency);
        singleCurrencyValues[i] += convertedValue;
      }
    }
    return CurrencyValuesArray.ofUnsafe(reportingCurrency, singleCurrencyValues);
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

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    MultiCurrencyValuesArray other = (MultiCurrencyValuesArray) obj;

    if (values.size() != other.values.size()) {
      return false;
    }
    for (Currency currency : values.keySet()) {
      double[] currencyValues = values.get(currency);
      double[] otherCurrencyValues = other.values.get(currency);

      if (otherCurrencyValues == null) {
        return false;
      }
      if (!Arrays.equals(currencyValues, otherCurrencyValues)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();

    // This is necessary to ensure Arrays.hashCode is used for the value arrays
    // If the hash code of the map were used it would use the identity hash code of the arrays
    for (Map.Entry<Currency, double[]> entry : values.entrySet()) {
      hash = hash * 31 + JodaBeanUtils.hashCode(entry.getKey());
      hash = hash * 31 + JodaBeanUtils.hashCode(entry.getValue());
    }
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("MultiCurrencyValuesArray{");
    buf.append("values").append('=').append('{');

    for (UnmodifiableIterator<Map.Entry<Currency, double[]>> it = values.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<Currency, double[]> entry = it.next();
      buf.append(entry.getKey()).append('=').append(Arrays.toString(entry.getValue()));

      if (it.hasNext()) {
        buf.append(',').append(' ');
      }
    }
    buf.append('}').append(',').append(' ');
    buf.append("size").append('=').append(JodaBeanUtils.toString(size));
    buf.append('}');
    return buf.toString();
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

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static MultiCurrencyValuesArray.Builder builder() {
    return new MultiCurrencyValuesArray.Builder();
  }

  private MultiCurrencyValuesArray(
      Map<Currency, double[]> values,
      int size) {
    JodaBeanUtils.notNull(values, "values");
    JodaBeanUtils.notNull(size, "size");
    this.values = ImmutableMap.copyOf(values);
    this.size = size;
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
  private ImmutableMap<Currency, double[]> getValues() {
    return values;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of values for each currency.
   * @return the value of the property, not null
   */
  private int getSize() {
    return size;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
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
    private final MetaProperty<ImmutableMap<Currency, double[]>> values = DirectMetaProperty.ofImmutable(
        this, "values", MultiCurrencyValuesArray.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code size} property.
     */
    private final MetaProperty<Integer> size = DirectMetaProperty.ofImmutable(
        this, "size", MultiCurrencyValuesArray.class, Integer.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "values",
        "size");

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
        case 3530753:  // size
          return size;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public MultiCurrencyValuesArray.Builder builder() {
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
    public MetaProperty<ImmutableMap<Currency, double[]>> values() {
      return values;
    }

    /**
     * The meta-property for the {@code size} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> size() {
      return size;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -823812830:  // values
          return ((MultiCurrencyValuesArray) bean).getValues();
        case 3530753:  // size
          return ((MultiCurrencyValuesArray) bean).getSize();
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
  public static final class Builder extends DirectFieldsBeanBuilder<MultiCurrencyValuesArray> {

    private Map<Currency, double[]> values = ImmutableMap.of();
    private int size;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(MultiCurrencyValuesArray beanToCopy) {
      this.values = beanToCopy.getValues();
      this.size = beanToCopy.getSize();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -823812830:  // values
          return values;
        case 3530753:  // size
          return size;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -823812830:  // values
          this.values = (Map<Currency, double[]>) newValue;
          break;
        case 3530753:  // size
          this.size = (Integer) newValue;
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
          values,
          size);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the currency values, keyed by currency.
     * @param values  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder values(Map<Currency, double[]> values) {
      JodaBeanUtils.notNull(values, "values");
      this.values = values;
      return this;
    }

    /**
     * Sets the number of values for each currency.
     * @param size  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder size(int size) {
      JodaBeanUtils.notNull(size, "size");
      this.size = size;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("MultiCurrencyValuesArray.Builder{");
      buf.append("values").append('=').append(JodaBeanUtils.toString(values)).append(',').append(' ');
      buf.append("size").append('=').append(JodaBeanUtils.toString(size));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
