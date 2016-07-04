/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.collect.Guavate.ensureOnlyOne;

import java.io.Serializable;
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

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * An array of currency amounts with the same currency.
 * <p>
 * This represents an array of {@link CurrencyAmount} in a single currency.
 * Internally, it stores the data using a single {@link Currency} and a {@link DoubleArray}.
 */
@BeanDefinition(builderScope = "private")
public final class CurrencyAmountArray
    implements FxConvertible<CurrencyAmountArray>, ImmutableBean, Serializable {

  /**
   * The currency.
   * All amounts have the same currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The values.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray values;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified currency and array of values.
   *
   * @param currency  the currency of the values
   * @param values  the values
   * @return an instance with the specified currency and values
   */
  public static CurrencyAmountArray of(Currency currency, DoubleArray values) {
    return new CurrencyAmountArray(currency, values);
  }

  /**
   * Obtains an instance from the specified list of amounts.
   * <p>
   * All amounts must have the same currency.
   *
   * @param amounts  the amounts, at least size one
   * @return an instance with the specified amounts
   * @throws IllegalArgumentException if multiple currencies are found
   */
  public static CurrencyAmountArray of(List<CurrencyAmount> amounts) {
    Currency currency = amounts.stream()
        .map(ca -> ca.getCurrency())
        .distinct()
        .reduce(ensureOnlyOne())
        .get();
    double[] values = amounts.stream()
        .mapToDouble(ca -> ca.getAmount())
        .toArray();
    return new CurrencyAmountArray(currency, DoubleArray.ofUnsafe(values));
  }

  /**
   * Obtains an instance using a function to create the entries.
   * <p>
   * The function is passed the index and returns the {@code CurrencyAmount} for that index.
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
  public static CurrencyAmountArray of(int size, IntFunction<CurrencyAmount> valueFunction) {
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
    return new CurrencyAmountArray(currency, DoubleArray.ofUnsafe(array));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the size of the array.
   * 
   * @return the array size
   */
  public int size() {
    return values.size();
  }

  /**
   * Gets the amount at the specified index.
   *
   * @param index  the zero-based index to retrieve
   * @return the amount at the specified index
   */
  public CurrencyAmount get(int index) {
    return CurrencyAmount.of(currency, values.get(index));
  }

  /**
   * Returns a stream of the amounts.
   *
   * @return a stream of the amounts
   */
  public Stream<CurrencyAmount> stream() {
    return values.stream().mapToObj(amount -> CurrencyAmount.of(currency, amount));
  }

  @Override
  public CurrencyAmountArray convertedTo(Currency resultCurrency, FxRateProvider fxRateProvider) {
    if (currency.equals(resultCurrency)) {
      return this;
    }
    double fxRate = fxRateProvider.fxRate(currency, resultCurrency);
    DoubleArray convertedValues = values.multipliedBy(fxRate);
    return new CurrencyAmountArray(resultCurrency, convertedValues);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new array containing the values from this array added to the values in the other array.
   * <p>
   * The amounts are added to the matching element in this array.
   * The currency must be the same as the currency of this array.
   * The arrays must have the same size.
   *
   * @param other  another array of multiple currency values.
   * @return a new array containing the values from this array added to the values in the other array
   * @throws IllegalArgumentException if the arrays have different sizes
   */
  public CurrencyAmountArray plus(CurrencyAmountArray other) {
    if (other.size() != size()) {
      throw new IllegalArgumentException(Messages.format(
          "Sizes must be equal, this size is {}, other size is {}", size(), other.size()));
    }
    if (!other.currency.equals(currency)) {
      throw new IllegalArgumentException(Messages.format(
          "Currencies must be equal, this currency is {}, other currency is {}", currency, other.currency));
    }
    return CurrencyAmountArray.of(currency, values.plus(other.values));
  }

  /**
   * Returns a new array containing the values from this array with the values from the amount added.
   * <p>
   * The amount is added to each element in this array.
   * The currency must be the same as the currency of this array.
   *
   * @param amount  the amount to add
   * @return a new array containing the values from this array added to the values in the other array
   */
  public CurrencyAmountArray plus(CurrencyAmount amount) {
    if (!amount.getCurrency().equals(currency)) {
      throw new IllegalArgumentException(Messages.format(
          "Currencies must be equal, this currency is {}, other currency is {}", currency, amount.getCurrency()));
    }
    return CurrencyAmountArray.of(currency, values.plus(amount.getAmount()));
  }

  /**
   * Returns a new array containing the values from this array with the values from the other array subtracted.
   * <p>
   * The amounts are subtracted from the matching element in this array.
   * The currency must be the same as the currency of this array.
   * The arrays must have the same size.
   *
   * @param other  another array of multiple currency values.
   * @return a new array containing the values from this array added with the values from the other array subtracted
   * @throws IllegalArgumentException if the arrays have different sizes
   */
  public CurrencyAmountArray minus(CurrencyAmountArray other) {
    if (other.size() != size()) {
      throw new IllegalArgumentException(Messages.format(
          "Sizes must be equal, this size is {}, other size is {}", size(), other.size()));
    }
    if (!other.currency.equals(currency)) {
      throw new IllegalArgumentException(Messages.format(
          "Currencies must be equal, this currency is {}, other currency is {}", currency, other.currency));
    }
    return CurrencyAmountArray.of(currency, values.minus(other.values));
  }

  /**
   * Returns a new array containing the values from this array with the values from the amount subtracted.
   * <p>
   * The amount is subtracted from each element in this array.
   * The currency must be the same as the currency of this array.
   *
   * @param amount  the amount to subtract
   * @return a new array containing the values from this array with the values from the amount subtracted
   */
  public CurrencyAmountArray minus(CurrencyAmount amount) {
    if (!amount.getCurrency().equals(currency)) {
      throw new IllegalArgumentException(Messages.format(
          "Currencies must be equal, this currency is {}, other currency is {}", currency, amount.getCurrency()));
    }
    return CurrencyAmountArray.of(currency, values.plus(amount.getAmount()));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CurrencyAmountArray}.
   * @return the meta-bean, not null
   */
  public static CurrencyAmountArray.Meta meta() {
    return CurrencyAmountArray.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CurrencyAmountArray.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private CurrencyAmountArray(
      Currency currency,
      DoubleArray values) {
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(values, "values");
    this.currency = currency;
    this.values = values;
  }

  @Override
  public CurrencyAmountArray.Meta metaBean() {
    return CurrencyAmountArray.Meta.INSTANCE;
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
   * Gets the currency.
   * All amounts have the same currency.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the values.
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
      CurrencyAmountArray other = (CurrencyAmountArray) obj;
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
    buf.append("CurrencyAmountArray{");
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("values").append('=').append(JodaBeanUtils.toString(values));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CurrencyAmountArray}.
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
        this, "currency", CurrencyAmountArray.class, Currency.class);
    /**
     * The meta-property for the {@code values} property.
     */
    private final MetaProperty<DoubleArray> values = DirectMetaProperty.ofImmutable(
        this, "values", CurrencyAmountArray.class, DoubleArray.class);
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
    public BeanBuilder<? extends CurrencyAmountArray> builder() {
      return new CurrencyAmountArray.Builder();
    }

    @Override
    public Class<? extends CurrencyAmountArray> beanType() {
      return CurrencyAmountArray.class;
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
          return ((CurrencyAmountArray) bean).getCurrency();
        case -823812830:  // values
          return ((CurrencyAmountArray) bean).getValues();
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
   * The bean-builder for {@code CurrencyAmountArray}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<CurrencyAmountArray> {

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
    public CurrencyAmountArray build() {
      return new CurrencyAmountArray(
          currency,
          values);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("CurrencyAmountArray.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("values").append('=').append(JodaBeanUtils.toString(values));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
