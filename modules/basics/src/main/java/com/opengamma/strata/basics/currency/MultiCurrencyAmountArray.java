/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static java.util.stream.Collector.Characteristics.UNORDERED;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.IntFunction;
import java.util.stream.Collector;
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
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * An array of multi-currency amounts.
 * <p>
 * This represents an array of {@link MultiCurrencyAmount}.
 * Internally, it stores the data using a map of {@link Currency} to {@link DoubleArray},
 * which uses less memory than a {@code List<MultiCurrencyAmount>}.
 */
@BeanDefinition(builderScope = "private")
public final class MultiCurrencyAmountArray
    implements FxConvertible<CurrencyAmountArray>, ImmutableBean, Serializable {

  /**
   * The size of this array.
   */
  @PropertyDefinition(validate = "notNegative")
  private final int size;

  /**
   * The currency values, keyed by currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableSortedMap<Currency, DoubleArray> values;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified multi-currency amounts.
   *
   * @param amounts  the amounts
   * @return an instance with the specified amounts
   */
  public static MultiCurrencyAmountArray of(MultiCurrencyAmount... amounts) {
    return of(Arrays.asList(amounts));
  }

  /**
   * Obtains an instance from the specified multi-currency amounts.
   *
   * @param amounts  the amounts
   * @return an instance with the specified amounts
   */
  public static MultiCurrencyAmountArray of(List<MultiCurrencyAmount> amounts) {
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
    return new MultiCurrencyAmountArray(size, doubleArrayMap);
  }

  /**
   * Obtains an instance using a function to create the entries.
   * <p>
   * The function is passed the index and returns the {@code MultiCurrencyAmount} for that index.
   * 
   * @param size  the number of elements, at least size one
   * @param valueFunction  the function used to obtain each value
   * @return an instance initialized using the function
   * @throws IllegalArgumentException is size is zero or less
   */
  public static MultiCurrencyAmountArray of(int size, IntFunction<MultiCurrencyAmount> valueFunction) {
    Map<Currency, double[]> map = new HashMap<>();
    for (int i = 0; i < size; i++) {
      MultiCurrencyAmount mca = valueFunction.apply(i);
      for (CurrencyAmount ca : mca.getAmounts()) {
        double[] array = map.computeIfAbsent(ca.getCurrency(), c -> new double[size]);
        array[i] = ca.getAmount();
      }
    }
    return new MultiCurrencyAmountArray(size, MapStream.of(map).mapValues(array -> DoubleArray.ofUnsafe(array)).toMap());
  }

  /**
   * Obtains an instance from a map of amounts.
   * <p>
   * Each currency is associated with an array of amounts.
   * All the arrays must have the same number of elements.
   * <p>
   * If the map is empty the returned array will have a size of zero. To create an empty array
   * with a non-zero size use one of the other {@code of} methods.
   *
   * @param values  map of currencies to values
   * @return an instance containing the values from the map
   */
  public static MultiCurrencyAmountArray of(Map<Currency, DoubleArray> values) {
    values.values().stream().reduce((a1, a2) -> checkSize(a1, a2));
    // All of the values must have the same size so use the size of the first
    int size = values.isEmpty() ? 0 : values.values().iterator().next().size();
    return new MultiCurrencyAmountArray(size, values);
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
  private MultiCurrencyAmountArray(int size, Map<Currency, DoubleArray> values) {
    this.values = ImmutableSortedMap.copyOf(values);
    this.size = size;
  }

  // validate when deserializing
  private Object readResolve() {
    values.values().stream().reduce((a1, a2) -> checkSize(a1, a2));
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the set of currencies for which this object contains values.
   *
   * @return the set of currencies for which this object contains values
   */
  public Set<Currency> getCurrencies() {
    return values.keySet();
  }

  /**
   * Gets the values for the specified currency, throws an exception if there are no values for the currency.
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
   * Gets the size of the array.
   * 
   * @return the array size
   */
  public int size() {
    return size;
  }

  /**
   * Gets the amount at the specified index.
   *
   * @param index  the zero-based index to retrieve
   * @return the amount at the specified index
   */
  public MultiCurrencyAmount get(int index) {
    List<CurrencyAmount> currencyAmounts = values.keySet().stream()
        .map(ccy -> CurrencyAmount.of(ccy, values.get(ccy).get(index)))
        .collect(toList());
    return MultiCurrencyAmount.of(currencyAmounts);
  }

  /**
   * Returns a stream of the amounts.
   *
   * @return a stream of the amounts
   */
  public Stream<MultiCurrencyAmount> stream() {
    return IntStream.range(0, size).mapToObj(this::get);
  }

  @Override
  public CurrencyAmountArray convertedTo(Currency resultCurrency, FxRateProvider fxRateProvider) {
    double[] singleCurrencyValues = new double[size];
    for (Map.Entry<Currency, DoubleArray> entry : values.entrySet()) {
      Currency currency = entry.getKey();
      DoubleArray currencyValues = entry.getValue();
      for (int i = 0; i < size; i++) {
        singleCurrencyValues[i] += currencyValues.get(i) * fxRateProvider.fxRate(currency, resultCurrency);
      }
    }
    return CurrencyAmountArray.of(resultCurrency, DoubleArray.ofUnsafe(singleCurrencyValues));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new array containing the values from this array added to the values in the other array.
   * <p>
   * The amounts are added to the matching element in this array.
   * The arrays must have the same size.
   *
   * @param other  another array of multiple currency values.
   * @return a new array containing the values from this array added to the values in the other array
   * @throws IllegalArgumentException if the arrays have different sizes
   */
  public MultiCurrencyAmountArray plus(MultiCurrencyAmountArray other) {
    if (other.size() != size) {
      throw new IllegalArgumentException(Messages.format(
          "Sizes must be equal, this size is {}, other size is {}", size, other.size()));
    }
    Map<Currency, DoubleArray> addedValues = Stream.concat(values.entrySet().stream(), other.values.entrySet().stream())
        .collect(toMap(e -> e.getKey(), e -> e.getValue(), (arr1, arr2) -> arr1.plus(arr2)));
    return MultiCurrencyAmountArray.of(addedValues);
  }

  /**
   * Returns a new array containing the values from this array with the values from the amount added.
   * <p>
   * The amount is added to each element in this array.
   *
   * @param amount  the amount to add
   * @return a new array containing the values from this array added to the values in the other array
   */
  public MultiCurrencyAmountArray plus(MultiCurrencyAmount amount) {
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
    return MultiCurrencyAmountArray.of(builder.build());
  }

  /**
   * Returns a new array containing the values from this array with the values from the other array subtracted.
   * <p>
   * The amounts are subtracted from the matching element in this array.
   * The arrays must have the same size.
   *
   * @param other  another array of multiple currency values.
   * @return a new array containing the values from this array added with the values from the other array subtracted
   * @throws IllegalArgumentException if the arrays have different sizes
   */
  public MultiCurrencyAmountArray minus(MultiCurrencyAmountArray other) {
    if (other.size() != size) {
      throw new IllegalArgumentException(Messages.format(
          "Sizes must be equal, this size is {}, other size is {}", size, other.size()));
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
   * <p>
   * The amount is subtracted from each element in this array.
   *
   * @param amount  the amount to subtract
   * @return a new array containing the values from this array with the values from the amount subtracted
   */
  public MultiCurrencyAmountArray minus(MultiCurrencyAmount amount) {
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
    return MultiCurrencyAmountArray.of(builder.build());
  }

  /**
   * Returns a multi currency amount array representing the total of the input arrays.
   * <p>
   * If the input contains the same currency more than once, the amounts are added together.
   *
   * @param arrays  the amount arrays
   * @return the total amounts
   */
  public static MultiCurrencyAmountArray total(Iterable<CurrencyAmountArray> arrays) {
    return Guavate.stream(arrays).collect(toMultiCurrencyAmountArray());
  }

  /**
   * Returns a collector which creates a multi currency amount array by combining a stream of
   * currency amount arrays.
   * <p>
   * The arrays in the stream must all have the same length.
   *
   * @return the collector
   */
  public static Collector<CurrencyAmountArray, ?, MultiCurrencyAmountArray> toMultiCurrencyAmountArray() {
    return Collector.<CurrencyAmountArray, Map<Currency, CurrencyAmountArray>, MultiCurrencyAmountArray>of(
        // accumulate into a map
        HashMap::new,
        (map, ca) -> map.merge(ca.getCurrency(), ca, CurrencyAmountArray::plus),
        // combine two maps
        (map1, map2) -> {
          map2.values().forEach((ca2) -> map1.merge(ca2.getCurrency(), ca2, CurrencyAmountArray::plus));
          return map1;
        },
        // convert to MultiCurrencyAmountArray
        map -> {
          Map<Currency, DoubleArray> currencyArrayMap = MapStream.of(map).mapValues(caa -> caa.getValues()).toMap();
          return MultiCurrencyAmountArray.of(currencyArrayMap);
        },
        UNORDERED);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code MultiCurrencyAmountArray}.
   * @return the meta-bean, not null
   */
  public static MultiCurrencyAmountArray.Meta meta() {
    return MultiCurrencyAmountArray.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(MultiCurrencyAmountArray.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public MultiCurrencyAmountArray.Meta metaBean() {
    return MultiCurrencyAmountArray.Meta.INSTANCE;
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
   * Gets the size of this array.
   * @return the value of the property
   */
  public int getSize() {
    return size;
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
      MultiCurrencyAmountArray other = (MultiCurrencyAmountArray) obj;
      return (size == other.size) &&
          JodaBeanUtils.equal(values, other.values);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(size);
    hash = hash * 31 + JodaBeanUtils.hashCode(values);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("MultiCurrencyAmountArray{");
    buf.append("size").append('=').append(size).append(',').append(' ');
    buf.append("values").append('=').append(JodaBeanUtils.toString(values));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code MultiCurrencyAmountArray}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code size} property.
     */
    private final MetaProperty<Integer> size = DirectMetaProperty.ofImmutable(
        this, "size", MultiCurrencyAmountArray.class, Integer.TYPE);
    /**
     * The meta-property for the {@code values} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableSortedMap<Currency, DoubleArray>> values = DirectMetaProperty.ofImmutable(
        this, "values", MultiCurrencyAmountArray.class, (Class) ImmutableSortedMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "size",
        "values");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3530753:  // size
          return size;
        case -823812830:  // values
          return values;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends MultiCurrencyAmountArray> builder() {
      return new MultiCurrencyAmountArray.Builder();
    }

    @Override
    public Class<? extends MultiCurrencyAmountArray> beanType() {
      return MultiCurrencyAmountArray.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code size} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> size() {
      return size;
    }

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
        case 3530753:  // size
          return ((MultiCurrencyAmountArray) bean).getSize();
        case -823812830:  // values
          return ((MultiCurrencyAmountArray) bean).getValues();
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
   * The bean-builder for {@code MultiCurrencyAmountArray}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<MultiCurrencyAmountArray> {

    private int size;
    private SortedMap<Currency, DoubleArray> values = ImmutableSortedMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3530753:  // size
          return size;
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
        case 3530753:  // size
          this.size = (Integer) newValue;
          break;
        case -823812830:  // values
          this.values = (SortedMap<Currency, DoubleArray>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public MultiCurrencyAmountArray build() {
      return new MultiCurrencyAmountArray(
          size,
          values);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("MultiCurrencyAmountArray.Builder{");
      buf.append("size").append('=').append(JodaBeanUtils.toString(size)).append(',').append(' ');
      buf.append("values").append('=').append(JodaBeanUtils.toString(values));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
