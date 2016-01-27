/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.zipWithIndex;

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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.function.CurrencyConvertible;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A currency-convertible scenario result holding one value for each scenario.
 * <p>
 * This contains a list of values that can be currency converted, one value for each scenario.
 * The calculation runner is able to convert the currency of the values if required.
 * <p>
 * Note that it is recommended to use optimized storage classes if appropriate.
 * Use {@link CurrencyValuesArray} for a list of {@link CurrencyAmount}.
 * Use {@link MultiCurrencyValuesArray} for a list of {@link MultiCurrencyAmount}.
 * 
 * @param <T>  the type of the each convertible
 */
@BeanDefinition(builderScope = "private")
public final class FxConvertibleList<T extends FxConvertible<?>>
    implements CurrencyConvertible<ScenarioResult<?>>, ScenarioResult<T>, ImmutableBean {

  /**
   * The calculated values, one per scenario.
   */
  @PropertyDefinition(validate = "notNull", builderType = "List<? extends T>")
  private final ImmutableList<T> values;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified array of currency-convertible values.
   *
   * @param <T>  the type of FX convertible element
   * @param values  the values, one value for each scenario
   * @return an instance with the specified values
   */
  @SafeVarargs
  public static <T extends FxConvertible<?>> FxConvertibleList<T> of(T... values) {
    return new FxConvertibleList<>(ImmutableList.copyOf(values));
  }

  /**
   * Obtains an instance from the specified list of currency-convertible values.
   *
   * @param <T>  the type of FX convertible element
   * @param values  the values, one value for each scenario
   * @return an instance with the specified values
   */
  public static <T extends FxConvertible<?>> FxConvertibleList<T> of(List<? extends T> values) {
    return new FxConvertibleList<>(values);
  }

  /**
   * Obtains an instance using a function to create the entries.
   * <p>
   * The function is passed the scenario index and returns the value for that index.
   * 
   * @param <T>  the type of FX convertible element
   * @param size  the number of elements
   * @param valueFunction  the function used to obtain each value
   * @return an instance initialized using the function
   * @throws IllegalArgumentException is size is zero or less
   */
  public static <T extends FxConvertible<?>> FxConvertibleList<T> of(int size, IntFunction<T> valueFunction) {
    ArgChecker.notNegativeOrZero(size, "size");
    ImmutableList.Builder<T> builder = ImmutableList.builder();
    for (int i = 0; i < size; i++) {
      builder.add(valueFunction.apply(i));
    }
    return new FxConvertibleList<>(builder.build());
  }

  /**
   * Obtains an instance from the specified list of currency-convertible values.
   * <p>
   * This is a nasty non-public method that hides the casts necessary.
   * All elements in the input list must be pre-checked to ensure that they are {@code FxConvertible}.
   * This code should be a private static method on {@code ScenarioResult} but interfaces cannot have private methods.
   *
   * @param <T>  the input and result type
   * @param values  the values, one value for each scenario, all implementing {@link FxConvertible}
   * @return an instance with the specified values
   */
  @SuppressWarnings("unchecked")
  static <T> ScenarioResult<T> casting(List<T> values) {
    List<FxConvertible<?>> convertibleResults = (List<FxConvertible<?>>) values;
    return (ScenarioResult<T>) FxConvertibleList.of(convertibleResults);
  }

  //-------------------------------------------------------------------------
  @Override
  public ScenarioResult<?> convertedTo(Currency reportingCurrency, CalculationMarketData marketData) {
    List<?> convertedValues = zipWithIndex(values.stream())
        .map(tp -> tp.getFirst().convertedTo(reportingCurrency, ScenarioRateProvider.of(marketData, tp.getSecond())))
        .collect(toImmutableList());

    return DefaultScenarioResult.of(convertedValues);
  }

  @Override
  public int size() {
    return values.size();
  }

  @Override
  public T get(int index) {
    return values.get(index);
  }

  @Override
  public Stream<T> stream() {
    return values.stream();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxConvertibleList}.
   * @return the meta-bean, not null
   */
  @SuppressWarnings("rawtypes")
  public static FxConvertibleList.Meta meta() {
    return FxConvertibleList.Meta.INSTANCE;
  }

  /**
   * The meta-bean for {@code FxConvertibleList}.
   * @param <R>  the bean's generic type
   * @param cls  the bean's generic type
   * @return the meta-bean, not null
   */
  @SuppressWarnings("unchecked")
  public static <R extends FxConvertible<?>> FxConvertibleList.Meta<R> metaFxConvertibleList(Class<R> cls) {
    return FxConvertibleList.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxConvertibleList.Meta.INSTANCE);
  }

  private FxConvertibleList(
      List<? extends T> values) {
    JodaBeanUtils.notNull(values, "values");
    this.values = ImmutableList.copyOf(values);
  }

  @SuppressWarnings("unchecked")
  @Override
  public FxConvertibleList.Meta<T> metaBean() {
    return FxConvertibleList.Meta.INSTANCE;
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
   * Gets the calculated values, one per scenario.
   * @return the value of the property, not null
   */
  public ImmutableList<T> getValues() {
    return values;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FxConvertibleList<?> other = (FxConvertibleList<?>) obj;
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
    buf.append("FxConvertibleList{");
    buf.append("values").append('=').append(JodaBeanUtils.toString(values));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxConvertibleList}.
   * @param <T>  the type
   */
  public static final class Meta<T extends FxConvertible<?>> extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    @SuppressWarnings("rawtypes")
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code values} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<T>> values = DirectMetaProperty.ofImmutable(
        this, "values", FxConvertibleList.class, (Class) ImmutableList.class);
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
    public BeanBuilder<? extends FxConvertibleList<T>> builder() {
      return new FxConvertibleList.Builder<T>();
    }

    @SuppressWarnings({"unchecked", "rawtypes" })
    @Override
    public Class<? extends FxConvertibleList<T>> beanType() {
      return (Class) FxConvertibleList.class;
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
    public MetaProperty<ImmutableList<T>> values() {
      return values;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -823812830:  // values
          return ((FxConvertibleList<?>) bean).getValues();
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
   * The bean-builder for {@code FxConvertibleList}.
   * @param <T>  the type
   */
  private static final class Builder<T extends FxConvertible<?>> extends DirectFieldsBeanBuilder<FxConvertibleList<T>> {

    private List<? extends T> values = ImmutableList.of();

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
    public Builder<T> set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -823812830:  // values
          this.values = (List<? extends T>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder<T> set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder<T> setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder<T> setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder<T> setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public FxConvertibleList<T> build() {
      return new FxConvertibleList<T>(
          values);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("FxConvertibleList.Builder{");
      buf.append("values").append('=').append(JodaBeanUtils.toString(values));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
