/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.IntFunction;
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
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * A currency-convertible scenario result for multi-currency amounts, holding one amount for each scenario.
 * <p>
 * This contains a list of amounts in a multiple currencies, one amount for each scenario.
 * The calculation runner is able to convert the currency of the values if required.
 * <p>
 * This class uses less memory than an instance based on a list of {@link MultiCurrencyAmount} instances.
 * Internally, it stores the data using a map of currency to {@link DoubleArray}.
 */
@BeanDefinition(builderScope = "private")
public final class MultiCurrencyValuesArray
    implements ScenarioResult<MultiCurrencyAmount>, ScenarioFxConvertible<CurrencyValuesArray>, ImmutableBean {

  /** The currency values, keyed by currency. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableSortedMap<Currency, DoubleArray> values;

  /** The number of values for each currency. */
  private final int size;

  //-------------------------------------------------------------------------
  /**
   * Returns an instance containing the values from the amounts.
   *
   * @param amounts  the amounts, one for each scenario
   * @return an instance containing the values from the list of amounts
   */
  public static MultiCurrencyValuesArray of(MultiCurrencyAmount... amounts) {
    return of(Arrays.asList(amounts));
  }

  /**
   * Returns an instance containing the values from the list of amounts.
   *
   * @param amounts  the amounts, one for each scenario
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
    Map<Currency, DoubleArray> doubleArrayMap = MapStream.of(valueMap).mapValues(v -> DoubleArray.ofUnsafe(v)).toMap();
    return new MultiCurrencyValuesArray(doubleArrayMap);
  }

  /**
   * Obtains an instance using a function to create the entries.
   * <p>
   * The function is passed the scenario index and returns the value for that index.
   * 
   * @param size  the number of elements
   * @param valueFunction  the function used to obtain each value
   * @return an instance initialized using the function
   * @throws IllegalArgumentException is size is zero or less
   */
  public static MultiCurrencyValuesArray of(int size, IntFunction<MultiCurrencyAmount> valueFunction) {
    ArgChecker.notNegativeOrZero(size, "size");
    Map<Currency, double[]> map = new HashMap<>();
    for (int i = 0; i < size; i++) {
      MultiCurrencyAmount mca = valueFunction.apply(i);
      for (CurrencyAmount ca : mca.getAmounts()) {
        double[] array = map.computeIfAbsent(ca.getCurrency(), c -> new double[size]);
        array[i] = ca.getAmount();
      }
    }
    return new MultiCurrencyValuesArray(MapStream.of(map).mapValues(array -> DoubleArray.ofUnsafe(array)).toMap());
  }

  /**
   * Returns an instance containing the values from a map of amounts with the same number of elements in each array.
   *
   * @param values  map of currencies to values
   * @return an instance containing the values from the map
   */
  public static MultiCurrencyValuesArray of(Map<Currency, DoubleArray> values) {
    values.values().stream().reduce((a1, a2) -> checkSize(a1, a2));
    return new MultiCurrencyValuesArray(values);
  }

  /**
   * Checks the size of the arrays are the same and throws an exception if not.
   *
   * @param array1  an array
   * @param array2  an array
   * @return array1
   * @throws IllegalArgumentException if the array sizes are not equal
   */
  private static DoubleArray checkSize(DoubleArray array1, DoubleArray array2) {
    if (array1.size() != array2.size()) {
      throw new IllegalArgumentException(
          Messages.format(
              "Arrays must have the same size but found sizes {} and {}",
              array1.size(),
              array2.size()));
    }
    return array1;
  }

  @ImmutableConstructor
  private MultiCurrencyValuesArray(Map<Currency, DoubleArray> values) {
    this.values = ImmutableSortedMap.copyOf(values);

    if (values.isEmpty()) {
      size = 0;
    } else {
      // All currencies must have the same number of values so we can just take the size of the first
      size = values.values().iterator().next().size();
    }
  }

  //-------------------------------------------------------------------------
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

  //-------------------------------------------------------------------------
  @Override
  public CurrencyValuesArray convertedTo(Currency reportingCurrency, ScenarioFxRateProvider fxRateProvider) {
    if (fxRateProvider.getScenarioCount() != size) {
      throw new IllegalArgumentException(Messages.format(
          "Expected {} FX rates but received {}", size, fxRateProvider.getScenarioCount()));
    }

    double[] singleCurrencyValues = new double[size];
    for (Map.Entry<Currency, DoubleArray> entry : values.entrySet()) {
      Currency currency = entry.getKey();
      DoubleArray currencyValues = entry.getValue();

      for (int i = 0; i < size; i++) {
        double convertedValue = currencyValues.get(i) * fxRateProvider.fxRate(currency, reportingCurrency, i);
        singleCurrencyValues[i] += convertedValue;
      }
    }
    return CurrencyValuesArray.of(reportingCurrency, DoubleArray.ofUnsafe(singleCurrencyValues));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new array containing the values from this array added to the values in the other array.
   * <p>
   * The arrays must have the same size.
   *
   * @param other  another array of multiple currency values.
   * @return a new array containing the values from this array added to the values in the other array
   * @throws IllegalArgumentException if the arrays have different sizes
   */
  public MultiCurrencyValuesArray plus(MultiCurrencyValuesArray other) {
    if (other.size() != size) {
      throw new IllegalArgumentException(
          Messages.format(
              "Sizes must be equal when adding, this size is {}, other size is {}",
              size,
              other.size()));
    }
    Map<Currency, DoubleArray> addedValues = Stream.concat(values.entrySet().stream(), other.values.entrySet().stream())
        .collect(toMap(e -> e.getKey(), e -> e.getValue(), (arr1, arr2) -> arr1.plus(arr2)));
    return MultiCurrencyValuesArray.of(addedValues);
  }

  /**
   * Returns a new array containing the values from this array with the values from the amount added.
   *
   * @param amount  the amount to add
   * @return a new array containing the values from this array added to the values in the other array
   */
  public MultiCurrencyValuesArray plus(MultiCurrencyAmount amount) {
    ImmutableMap.Builder<Currency, DoubleArray> builder = ImmutableMap.builder();

    for (Currency currency : Sets.union(values.keySet(), amount.getCurrencies())) {
      DoubleArray array = values.get(currency);

      if (array == null) {
        builder.put(currency, DoubleArray.filled(size, amount.getAmount(currency).getAmount()));
      } else if (!amount.contains(currency)) {
        builder.put(currency, array);
      } else {
        builder.put(currency, array.plus(amount.getAmount(currency).getAmount()));
      }
    }
    return MultiCurrencyValuesArray.of(builder.build());
  }

  /**
   * Returns a new array containing the values from this array with the values from the other array subtracted.
   * <p>
   * The arrays must have the same size.
   *
   * @param other  another array of multiple currency values.
   * @return a new array containing the values from this array added with the values from the other array subtracted
   * @throws IllegalArgumentException if the arrays have different sizes
   */
  public MultiCurrencyValuesArray minus(MultiCurrencyValuesArray other) {
    if (other.size() != size) {
      throw new IllegalArgumentException(
          Messages.format(
              "Sizes must be equal when subtracting, this size is {}, other size is {}",
              size,
              other.size()));
    }
    ImmutableMap.Builder<Currency, DoubleArray> builder = ImmutableMap.builder();

    for (Currency currency : Sets.union(values.keySet(), other.values.keySet())) {
      DoubleArray array = values.get(currency);
      DoubleArray otherArray = other.values.get(currency);

      if (otherArray == null) {
        builder.put(currency, array);
      } else if (array == null) {
        builder.put(currency, otherArray.multipliedBy(-1));
      } else {
        builder.put(currency, array.minus(otherArray));
      }
    }
    return of(builder.build());
  }

  /**
   * Returns a new array containing the values from this array with the values from the amount subtracted.
   *
   * @param amount  the amount to subtract
   * @return a new array containing the values from this array with the values from the amount subtracted
   */
  public MultiCurrencyValuesArray minus(MultiCurrencyAmount amount) {
    ImmutableMap.Builder<Currency, DoubleArray> builder = ImmutableMap.builder();

    for (Currency currency : Sets.union(values.keySet(), amount.getCurrencies())) {
      DoubleArray array = values.get(currency);

      if (array == null) {
        builder.put(currency, DoubleArray.filled(size, -amount.getAmount(currency).getAmount()));
      } else if (!amount.contains(currency)) {
        builder.put(currency, array);
      } else {
        builder.put(currency, array.minus(amount.getAmount(currency).getAmount()));
      }
    }
    return MultiCurrencyValuesArray.of(builder.build());
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
  public ImmutableSortedMap<Currency, DoubleArray> getValues() {
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
    private final MetaProperty<ImmutableSortedMap<Currency, DoubleArray>> values = DirectMetaProperty.ofImmutable(
        this, "values", MultiCurrencyValuesArray.class, (Class) ImmutableSortedMap.class);
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
    public MetaProperty<ImmutableSortedMap<Currency, DoubleArray>> values() {
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

    private SortedMap<Currency, DoubleArray> values = ImmutableSortedMap.of();

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
          this.values = (SortedMap<Currency, DoubleArray>) newValue;
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
