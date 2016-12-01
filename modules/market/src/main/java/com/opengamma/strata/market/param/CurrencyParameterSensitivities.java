/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.basics.currency.MultiCurrencyAmount.toMultiCurrencyAmount;
import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;

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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.surface.Surface;

/**
 * Currency-based parameter sensitivity for parameterized market data, such as curves.
 * <p>
 * Parameter sensitivity is the sensitivity of a value to the parameters of
 * {@linkplain ParameterizedData parameterized market data} objects that are used to determine the value.
 * Common {@code ParameterizedData} implementations include {@link Curve} and {@link Surface}.
 * <p>
 * For example, the parameter sensitivity for present value on a FRA might contain
 * two entries, one for the Ibor forward curve and one for the discount curve.
 * Each entry identifies the curve that was queried and the resulting sensitivity values,
 * one for each node on the curve.
 * <p>
 * The sensitivity is expressed as a single entry for piece of parameterized market data.
 * The sensitivity represents a monetary value in the specified currency.
 * The order of the list has no specific meaning.
 * <p>
 * One way of viewing this class is as a {@code Map} from a specific sensitivity key to
 * {@code DoubleArray} sensitivity values. However, instead of being structured as a {@code Map},
 * the data is structured as a {@code List}, with the key and value in each entry.
 */
@BeanDefinition(builderScope = "private")
public final class CurrencyParameterSensitivities
    implements FxConvertible<CurrencyParameterSensitivities>, ImmutableBean, Serializable {

  /**
   * An empty instance.
   */
  private static final CurrencyParameterSensitivities EMPTY = new CurrencyParameterSensitivities(ImmutableList.of());

  /**
   * The parameter sensitivities.
   * <p>
   * Each entry includes details of the {@link ParameterizedData} it relates to.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<CurrencyParameterSensitivity> sensitivities;

  //-------------------------------------------------------------------------
  /**
   * An empty sensitivity instance.
   * 
   * @return the empty instance
   */
  public static CurrencyParameterSensitivities empty() {
    return EMPTY;
  }

  /**
   * Obtains an instance from a single sensitivity entry.
   * 
   * @param sensitivity  the sensitivity entry
   * @return the sensitivities instance
   */
  public static CurrencyParameterSensitivities of(CurrencyParameterSensitivity sensitivity) {
    return new CurrencyParameterSensitivities(ImmutableList.of(sensitivity));
  }

  /**
   * Obtains an instance from an array of sensitivity entries.
   * <p>
   * The order of sensitivities is typically unimportant, however it is retained
   * and exposed in {@link #equals(Object)}.
   *
   * @param sensitivities  the sensitivities
   * @return the sensitivities instance
   */
  public static CurrencyParameterSensitivities of(CurrencyParameterSensitivity... sensitivities) {
    return of(Arrays.asList(sensitivities));
  }

  /**
   * Obtains an instance from a list of sensitivity entries.
   * <p>
   * The order of sensitivities is typically unimportant, however it is retained
   * and exposed in {@link #equals(Object)}.
   * 
   * @param sensitivities  the list of sensitivity entries
   * @return the sensitivities instance
   */
  public static CurrencyParameterSensitivities of(List<? extends CurrencyParameterSensitivity> sensitivities) {
    List<CurrencyParameterSensitivity> mutable = new ArrayList<>();
    for (CurrencyParameterSensitivity otherSens : sensitivities) {
      insert(mutable, otherSens);
    }
    return new CurrencyParameterSensitivities(ImmutableList.copyOf(mutable));
  }

  // used when not pre-sorted
  @ImmutableConstructor
  private CurrencyParameterSensitivities(List<? extends CurrencyParameterSensitivity> sensitivities) {
    if (sensitivities.size() < 2) {
      this.sensitivities = ImmutableList.copyOf(sensitivities);
    } else {
      List<CurrencyParameterSensitivity> mutable = new ArrayList<>(sensitivities);
      mutable.sort(CurrencyParameterSensitivity::compareKey);
      this.sensitivities = ImmutableList.copyOf(mutable);
    }
  }

  // used when pre-sorted
  private CurrencyParameterSensitivities(ImmutableList<CurrencyParameterSensitivity> sensitivities) {
    this.sensitivities = sensitivities;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of sensitivity entries.
   * 
   * @return the size of the internal list of point sensitivities
   */
  public int size() {
    return sensitivities.size();
  }

  /**
   * Gets a single sensitivity instance by name and currency.
   * 
   * @param name  the curve name to find
   * @param currency  the currency to find
   * @return the matching sensitivity
   * @throws IllegalArgumentException if the name and currency do not match an entry
   */
  public CurrencyParameterSensitivity getSensitivity(MarketDataName<?> name, Currency currency) {
    return findSensitivity(name, currency)
        .orElseThrow(() -> new IllegalArgumentException(Messages.format(
            "Unable to find sensitivity: {} for {}", name, currency)));
  }

  /**
   * Finds a single sensitivity instance by name and currency.
   * <p>
   * If the sensitivity is not found, optional empty is returned.
   * 
   * @param name  the curve name to find
   * @param currency  the currency to find
   * @return the matching sensitivity
   */
  public Optional<CurrencyParameterSensitivity> findSensitivity(MarketDataName<?> name, Currency currency) {
    return sensitivities.stream()
        .filter(sens -> sens.getMarketDataName().equals(name) && sens.getCurrency().equals(currency))
        .findFirst();
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this parameter sensitivities with another instance.
   * <p>
   * This returns a new sensitivity instance with the specified sensitivity added.
   * This instance is immutable and unaffected by this method.
   * The result may contain duplicate parameter sensitivities.
   * 
   * @param other  the other parameter sensitivity
   * @return an instance based on this one, with the other instance added
   */
  public CurrencyParameterSensitivities combinedWith(CurrencyParameterSensitivity other) {
    List<CurrencyParameterSensitivity> mutable = new ArrayList<>(sensitivities);
    insert(mutable, other);
    return new CurrencyParameterSensitivities(ImmutableList.copyOf(mutable));
  }

  /**
   * Combines this parameter sensitivities with another instance.
   * <p>
   * This returns a new sensitivity instance with a combined list of parameter sensitivities.
   * This instance is immutable and unaffected by this method.
   * The result may contain duplicate parameter sensitivities.
   * 
   * @param other  the other parameter sensitivities
   * @return an instance based on this one, with the other instance added
   */
  public CurrencyParameterSensitivities combinedWith(CurrencyParameterSensitivities other) {
    List<CurrencyParameterSensitivity> mutable = new ArrayList<>(sensitivities);
    for (CurrencyParameterSensitivity otherSens : other.sensitivities) {
      insert(mutable, otherSens);
    }
    return new CurrencyParameterSensitivities(ImmutableList.copyOf(mutable));
  }

  // inserts a sensitivity into the mutable list in the right location
  // merges the entry with an existing entry if the key matches
  private static void insert(List<CurrencyParameterSensitivity> mutable, CurrencyParameterSensitivity addition) {
    int index = Collections.binarySearch(
        mutable, addition, CurrencyParameterSensitivity::compareKey);
    if (index >= 0) {
      CurrencyParameterSensitivity base = mutable.get(index);
      DoubleArray combined = base.getSensitivity().plus(addition.getSensitivity());
      mutable.set(index, base.withSensitivity(combined));
    } else {
      int insertionPoint = -(index + 1);
      mutable.add(insertionPoint, addition);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Converts the sensitivities in this instance to an equivalent in the specified currency.
   * <p>
   * Any FX conversion that is required will use rates from the provider.
   * 
   * @param resultCurrency  the currency of the result
   * @param rateProvider  the provider of FX rates
   * @return the sensitivity object expressed in terms of the result currency
   * @throws RuntimeException if no FX rate could be found
   */
  @Override
  public CurrencyParameterSensitivities convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    List<CurrencyParameterSensitivity> mutable = new ArrayList<>();
    for (CurrencyParameterSensitivity sens : sensitivities) {
      insert(mutable, sens.convertedTo(resultCurrency, rateProvider));
    }
    return new CurrencyParameterSensitivities(ImmutableList.copyOf(mutable));
  }

  //-------------------------------------------------------------------------
  /**
   * Splits this sensitivity instance.
   * <p>
   * This examines each individual sensitivity to see if it can be {@link CurrencyParameterSensitivity#split() split}.
   * If any can be split, the result will contain the combination of the split sensitivities.
   * 
   * @return this sensitivity, with any combined sensitivities split
   */
  public CurrencyParameterSensitivities split() {
    if (!sensitivities.stream().anyMatch(s -> s.getParameterSplit().isPresent())) {
      return this;
    }
    return of(sensitivities.stream()
        .flatMap(s -> s.split().stream())
        .collect(toImmutableList()));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the total of the sensitivity values.
   * <p>
   * The result is the total of all values, as converted to the specified currency.
   * Any FX conversion that is required will use rates from the provider.
   * 
   * @param resultCurrency  the currency of the result
   * @param rateProvider  the provider of FX rates
   * @return the total sensitivity
   * @throws RuntimeException if no FX rate could be found
   */
  public CurrencyAmount total(Currency resultCurrency, FxRateProvider rateProvider) {
    CurrencyParameterSensitivities converted = convertedTo(resultCurrency, rateProvider);
    double total = converted.sensitivities.stream()
        .mapToDouble(s -> s.getSensitivity().sum())
        .sum();
    return CurrencyAmount.of(resultCurrency, total);
  }

  /**
   * Returns the total of the sensitivity values.
   * <p>
   * The result is the total of all values, in whatever currency they are defined.
   * 
   * @return the total sensitivity
   */
  public MultiCurrencyAmount total() {
    return sensitivities.stream()
        .map(CurrencyParameterSensitivity::total)
        .collect(toMultiCurrencyAmount());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the sensitivity values multiplied by the specified factor.
   * <p>
   * The result will consist of the same entries, but with each sensitivity value multiplied.
   * This instance is immutable and unaffected by this method.
   * 
   * @param factor  the multiplicative factor
   * @return an instance based on this one, with each sensitivity multiplied by the factor
   */
  public CurrencyParameterSensitivities multipliedBy(double factor) {
    return mapSensitivities(s -> s * factor);
  }

  /**
   * Returns an instance with the specified operation applied to the sensitivity values.
   * <p>
   * The result will consist of the same entries, but with the operator applied to each sensitivity value.
   * This instance is immutable and unaffected by this method.
   * <p>
   * This is used to apply a mathematical operation to the sensitivity values.
   * For example, the operator could multiply the sensitivities by a constant, or take the inverse.
   * <pre>
   *   inverse = base.mapSensitivities(value -> 1 / value);
   * </pre>
   *
   * @param operator  the operator to be applied to the sensitivities
   * @return an instance based on this one, with the operator applied to the sensitivity values
   */
  public CurrencyParameterSensitivities mapSensitivities(DoubleUnaryOperator operator) {
    return sensitivities.stream()
        .map(s -> s.mapSensitivity(operator))
        .collect(
            Collectors.collectingAndThen(
                Guavate.toImmutableList(),
                CurrencyParameterSensitivities::new));
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this sensitivity equals another within the specified tolerance.
   * <p>
   * This returns true if the two instances have the same keys, with arrays of the
   * same length, where the {@code double} values are equal within the specified tolerance.
   * 
   * @param other  the other sensitivity
   * @param tolerance  the tolerance
   * @return true if equal up to the tolerance
   */
  public boolean equalWithTolerance(CurrencyParameterSensitivities other, double tolerance) {
    List<CurrencyParameterSensitivity> mutable = new ArrayList<>(other.sensitivities);
    // for each sensitivity in this instance, find matching in other instance
    for (CurrencyParameterSensitivity sens1 : sensitivities) {
      // list is already sorted so binary search is safe
      int index = Collections.binarySearch(mutable, sens1, CurrencyParameterSensitivity::compareKey);
      if (index >= 0) {
        // matched, so must be equal
        CurrencyParameterSensitivity sens2 = mutable.get(index);
        if (!sens1.getSensitivity().equalWithTolerance(sens2.getSensitivity(), tolerance)) {
          return false;
        }
        mutable.remove(index);
      } else {
        // did not match, so must be zero
        if (!sens1.getSensitivity().equalZeroWithTolerance(tolerance)) {
          return false;
        }
      }
    }
    // all that remain from other instance must be zero
    for (CurrencyParameterSensitivity sens2 : mutable) {
      if (!sens2.getSensitivity().equalZeroWithTolerance(tolerance)) {
        return false;
      }
    }
    return true;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CurrencyParameterSensitivities}.
   * @return the meta-bean, not null
   */
  public static CurrencyParameterSensitivities.Meta meta() {
    return CurrencyParameterSensitivities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CurrencyParameterSensitivities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public CurrencyParameterSensitivities.Meta metaBean() {
    return CurrencyParameterSensitivities.Meta.INSTANCE;
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
   * Gets the parameter sensitivities.
   * <p>
   * Each entry includes details of the {@link ParameterizedData} it relates to.
   * @return the value of the property, not null
   */
  public ImmutableList<CurrencyParameterSensitivity> getSensitivities() {
    return sensitivities;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CurrencyParameterSensitivities other = (CurrencyParameterSensitivities) obj;
      return JodaBeanUtils.equal(sensitivities, other.sensitivities);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivities);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("CurrencyParameterSensitivities{");
    buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(sensitivities));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CurrencyParameterSensitivities}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code sensitivities} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<CurrencyParameterSensitivity>> sensitivities = DirectMetaProperty.ofImmutable(
        this, "sensitivities", CurrencyParameterSensitivities.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "sensitivities");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          return sensitivities;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CurrencyParameterSensitivities> builder() {
      return new CurrencyParameterSensitivities.Builder();
    }

    @Override
    public Class<? extends CurrencyParameterSensitivities> beanType() {
      return CurrencyParameterSensitivities.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code sensitivities} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<CurrencyParameterSensitivity>> sensitivities() {
      return sensitivities;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          return ((CurrencyParameterSensitivities) bean).getSensitivities();
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
   * The bean-builder for {@code CurrencyParameterSensitivities}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<CurrencyParameterSensitivities> {

    private List<CurrencyParameterSensitivity> sensitivities = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          return sensitivities;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          this.sensitivities = (List<CurrencyParameterSensitivity>) newValue;
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
    public CurrencyParameterSensitivities build() {
      return new CurrencyParameterSensitivities(
          sensitivities);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("CurrencyParameterSensitivities.Builder{");
      buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(sensitivities));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
