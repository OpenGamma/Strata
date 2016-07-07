/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static java.util.stream.Collector.Characteristics.UNORDERED;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Collector;
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
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmountArray;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * A currency-convertible scenario array for multi-currency amounts, holding one amount for each scenario.
 * <p>
 * This contains a list of amounts in a multiple currencies, one amount for each scenario.
 * The calculation runner is able to convert the currency of the values if required.
 * <p>
 * This class uses less memory than an instance based on a list of {@link MultiCurrencyAmount} instances.
 * Internally, it stores the data using a map of currency to {@link DoubleArray}.
 */
@BeanDefinition(builderScope = "private")
public final class MultiCurrencyScenarioArray
    implements ScenarioArray<MultiCurrencyAmount>, ScenarioFxConvertible<CurrencyScenarioArray>, ImmutableBean {

  /**
   * The multi-currency amounts, one per scenario.
   */
  @PropertyDefinition(validate = "notNull")
  private final MultiCurrencyAmountArray amounts;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified currency and array of values.
   *
   * @param amounts  the amounts, one for each scenario
   * @return an instance with the specified currency and values
   */
  public static MultiCurrencyScenarioArray of(MultiCurrencyAmountArray amounts) {
    return new MultiCurrencyScenarioArray(amounts);
  }

  /**
   * Returns an instance containing the values from the amounts.
   *
   * @param amounts  the amounts, one for each scenario
   * @return an instance containing the values from the list of amounts
   */
  public static MultiCurrencyScenarioArray of(MultiCurrencyAmount... amounts) {
    return of(Arrays.asList(amounts));
  }

  /**
   * Returns an instance containing the values from the list of amounts.
   *
   * @param amounts  the amounts, one for each scenario
   * @return an instance containing the values from the list of amounts
   */
  public static MultiCurrencyScenarioArray of(List<MultiCurrencyAmount> amounts) {
    return new MultiCurrencyScenarioArray(MultiCurrencyAmountArray.of(amounts));
  }

  /**
   * Obtains an instance using a function to create the entries.
   * <p>
   * The function is passed the scenario index and returns the value for that index.
   * 
   * @param size  the number of elements
   * @param amountFunction  the function used to obtain each amount
   * @return an instance initialized using the function
   * @throws IllegalArgumentException is size is zero or less
   */
  public static MultiCurrencyScenarioArray of(int size, IntFunction<MultiCurrencyAmount> amountFunction) {
    return new MultiCurrencyScenarioArray(MultiCurrencyAmountArray.of(size, amountFunction));
  }

  /**
   * Returns an instance containing the values from a map of amounts with the same number of elements in each array.
   *
   * @param values  map of currencies to values
   * @return an instance containing the values from the map
   */
  public static MultiCurrencyScenarioArray of(Map<Currency, DoubleArray> values) {
    return new MultiCurrencyScenarioArray(MultiCurrencyAmountArray.of(values));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the set of currencies for which this object contains values.
   *
   * @return the set of currencies for which this object contains values
   */
  public Set<Currency> getCurrencies() {
    return amounts.getCurrencies();
  }

  /**
   * Returns the values for the specified currency, throws an exception if there are no values for the currency.
   *
   * @param currency  the currency for which values are required
   * @return the values for the specified currency, throws an exception if there are none
   * @throws IllegalArgumentException if there are no values for the currency
   */
  public DoubleArray getValues(Currency currency) {
    DoubleArray currencyValues = amounts.getValues(currency);
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
  public int getScenarioCount() {
    return amounts.size();
  }

  /**
   * Returns a {@link MultiCurrencyAmount} at the specified index.
   * <p>
   * This method is not very efficient for large sizes as a new object must be created at each index.
   * Consider using {@link #getValues(Currency)} instead.
   *
   * @param index  the index that should be returned
   * @return a multi currency amount containing the currency values at the specified index
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  @Override
  public MultiCurrencyAmount get(int index) {
    List<CurrencyAmount> currencyAmounts = amounts.getCurrencies().stream()
        .map(ccy -> CurrencyAmount.of(ccy, amounts.getValues(ccy).get(index)))
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
    return amounts.stream();
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyScenarioArray convertedTo(Currency reportingCurrency, ScenarioFxRateProvider fxRateProvider) {
    int size = getScenarioCount();
    if (fxRateProvider.getScenarioCount() != size) {
      throw new IllegalArgumentException(Messages.format(
          "Expected {} FX rates but received {}", size, fxRateProvider.getScenarioCount()));
    }

    double[] singleCurrencyValues = new double[size];
    for (Map.Entry<Currency, DoubleArray> entry : amounts.getValues().entrySet()) {
      Currency currency = entry.getKey();
      DoubleArray currencyValues = entry.getValue();

      for (int i = 0; i < size; i++) {
        double convertedValue = currencyValues.get(i) * fxRateProvider.fxRate(currency, reportingCurrency, i);
        singleCurrencyValues[i] += convertedValue;
      }
    }
    return CurrencyScenarioArray.of(reportingCurrency, DoubleArray.ofUnsafe(singleCurrencyValues));
  }

  /**
   * Returns a multi currency scenario array representing the total of the input arrays.
   * <p>
   * If the input contains the same currency more than once, the amounts are added together.
   *
   * @param arrays  the amount arrays
   * @return the total amounts
   */
  public static MultiCurrencyScenarioArray total(Iterable<CurrencyScenarioArray> arrays) {
    return Guavate.stream(arrays).collect(toMultiCurrencyScenarioArray());
  }

  /**
   * Returns a collector which creates a multi currency scenario array by combining a stream of
   * currency scenario arrays.
   * <p>
   * The arrays in the stream must all have the same length.
   *
   * @return the collector
   */
  public static Collector<CurrencyScenarioArray, ?, MultiCurrencyScenarioArray> toMultiCurrencyScenarioArray() {
    return Collector.<CurrencyScenarioArray, Map<Currency, CurrencyAmountArray>, MultiCurrencyScenarioArray>of(
        // accumulate into a map
        HashMap::new,
        (map, ca) -> map.merge(ca.getCurrency(), ca.getAmounts(), CurrencyAmountArray::plus),
        // combine two maps
        (map1, map2) -> {
          map2.values().forEach((ca2) -> map1.merge(ca2.getCurrency(), ca2, CurrencyAmountArray::plus));
          return map1;
        },
        // convert to MultiCurrencyScenarioArray
        map -> {
          Map<Currency, DoubleArray> currencyArrayMap = MapStream.of(map).mapValues(caa -> caa.getValues()).toMap();
          return MultiCurrencyScenarioArray.of(currencyArrayMap);
        },
        UNORDERED);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code MultiCurrencyScenarioArray}.
   * @return the meta-bean, not null
   */
  public static MultiCurrencyScenarioArray.Meta meta() {
    return MultiCurrencyScenarioArray.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(MultiCurrencyScenarioArray.Meta.INSTANCE);
  }

  private MultiCurrencyScenarioArray(
      MultiCurrencyAmountArray amounts) {
    JodaBeanUtils.notNull(amounts, "amounts");
    this.amounts = amounts;
  }

  @Override
  public MultiCurrencyScenarioArray.Meta metaBean() {
    return MultiCurrencyScenarioArray.Meta.INSTANCE;
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
   * Gets the multi-currency amounts, one per scenario.
   * @return the value of the property, not null
   */
  public MultiCurrencyAmountArray getAmounts() {
    return amounts;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      MultiCurrencyScenarioArray other = (MultiCurrencyScenarioArray) obj;
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
    buf.append("MultiCurrencyScenarioArray{");
    buf.append("amounts").append('=').append(JodaBeanUtils.toString(amounts));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code MultiCurrencyScenarioArray}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code amounts} property.
     */
    private final MetaProperty<MultiCurrencyAmountArray> amounts = DirectMetaProperty.ofImmutable(
        this, "amounts", MultiCurrencyScenarioArray.class, MultiCurrencyAmountArray.class);
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
    public BeanBuilder<? extends MultiCurrencyScenarioArray> builder() {
      return new MultiCurrencyScenarioArray.Builder();
    }

    @Override
    public Class<? extends MultiCurrencyScenarioArray> beanType() {
      return MultiCurrencyScenarioArray.class;
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
    public MetaProperty<MultiCurrencyAmountArray> amounts() {
      return amounts;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -879772901:  // amounts
          return ((MultiCurrencyScenarioArray) bean).getAmounts();
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
   * The bean-builder for {@code MultiCurrencyScenarioArray}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<MultiCurrencyScenarioArray> {

    private MultiCurrencyAmountArray amounts;

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
          this.amounts = (MultiCurrencyAmountArray) newValue;
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
    public MultiCurrencyScenarioArray build() {
      return new MultiCurrencyScenarioArray(
          amounts);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("MultiCurrencyScenarioArray.Builder{");
      buf.append("amounts").append('=').append(JodaBeanUtils.toString(amounts));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
