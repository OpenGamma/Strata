/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.basics.currency.MultiCurrencyAmount.toMultiCurrencyAmount;

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
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.Curve;

/**
 * The second order parameter sensitivity for parameterized market data.
 * <p>
 * Parameter sensitivity is the sensitivity of a value to the parameters of
 * {@linkplain ParameterizedData parameterized market data} objects that are used to determine the value.
 * The main application is the parameter sensitivities for curves. Thus {@code ParameterizedData} is typically {@link Curve}.
 * <p>
 * The sensitivity is expressed as a single entry of second order sensitivities for piece of parameterized market data.
 * The cross-gamma between different {@code ParameterizedData} is not represented.
 * The sensitivity represents a monetary value in the specified currency.
 * The order of the list has no specific meaning.
 * <p>
 * One way of viewing this class is as a {@code Map} from a specific sensitivity key to
 * {@code DoubleMatrix} sensitivity values. However, instead of being structured as a {@code Map},
 * the data is structured as a {@code List}, with the key and value in each entry.
 */
@BeanDefinition(builderScope = "private")
public final class CrossGammaParameterSensitivities
    implements FxConvertible<CrossGammaParameterSensitivities>, ImmutableBean, Serializable {

  /**
   * An empty instance.
   */
  private static final CrossGammaParameterSensitivities EMPTY = new CrossGammaParameterSensitivities(ImmutableList.of());

  /**
   * The parameter sensitivities.
   * <p>
   * Each entry includes details of the {@link ParameterizedData} it relates to.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<CrossGammaParameterSensitivity> sensitivities;

  //-------------------------------------------------------------------------
  /**
   * An empty sensitivity instance.
   * 
   * @return the empty instance
   */
  public static CrossGammaParameterSensitivities empty() {
    return EMPTY;
  }

  /**
   * Obtains an instance from a single sensitivity entry.
   * 
   * @param sensitivity  the sensitivity entry
   * @return the sensitivities instance
   */
  public static CrossGammaParameterSensitivities of(CrossGammaParameterSensitivity sensitivity) {
    return new CrossGammaParameterSensitivities(ImmutableList.of(sensitivity));
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
  public static CrossGammaParameterSensitivities of(CrossGammaParameterSensitivity... sensitivities) {
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
  public static CrossGammaParameterSensitivities of(List<? extends CrossGammaParameterSensitivity> sensitivities) {
    List<CrossGammaParameterSensitivity> mutable = new ArrayList<>();
    for (CrossGammaParameterSensitivity otherSens : sensitivities) {
      insert(mutable, otherSens);
    }
    return new CrossGammaParameterSensitivities(ImmutableList.copyOf(mutable));
  }

  // used when not pre-sorted
  @ImmutableConstructor
  private CrossGammaParameterSensitivities(List<? extends CrossGammaParameterSensitivity> sensitivities) {
    if (sensitivities.size() < 2) {
      this.sensitivities = ImmutableList.copyOf(sensitivities);
    } else {
      List<CrossGammaParameterSensitivity> mutable = new ArrayList<>(sensitivities);
      mutable.sort(CrossGammaParameterSensitivity::compareKey);
      this.sensitivities = ImmutableList.copyOf(mutable);
    }
  }

  // used when pre-sorted
  private CrossGammaParameterSensitivities(ImmutableList<CrossGammaParameterSensitivity> sensitivities) {
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
  public CrossGammaParameterSensitivity getSensitivity(MarketDataName<?> name, Currency currency) {
    return findSensitivity(name, currency)
        .orElseThrow(() -> new IllegalArgumentException(Messages.format(
            "Unable to find sensitivity: {} for {}", name, currency)));
  }

  /**
   * Gets a single sensitivity instance by names and currency.
   * <p>
   * This returns the sensitivity of the market data ({@code nameFirst}) delta to another market data ({@code nameSecond}).
   * The result is sensitive to the order of {@code nameFirst} and {@code nameSecond}.
   * 
   * @param nameFirst  the name
   * @param nameSecond  the name
   * @param currency  the currency
   * @return the matching sensitivity
   * @throws IllegalArgumentException if the name and currency do not match an entry
   */
  public CrossGammaParameterSensitivity getSensitivity(
      MarketDataName<?> nameFirst,
      MarketDataName<?> nameSecond,
      Currency currency) {

    CrossGammaParameterSensitivity sensi = findSensitivity(nameFirst, currency)
        .orElseThrow(() -> new IllegalArgumentException(Messages.format(
            "Unable to find sensitivity: {} for {}", nameFirst, currency)));
    return sensi.getSensitivity(nameSecond);
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
  public Optional<CrossGammaParameterSensitivity> findSensitivity(MarketDataName<?> name, Currency currency) {
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
  public CrossGammaParameterSensitivities combinedWith(CrossGammaParameterSensitivity other) {
    List<CrossGammaParameterSensitivity> mutable = new ArrayList<>(sensitivities);
    insert(mutable, other);
    return new CrossGammaParameterSensitivities(ImmutableList.copyOf(mutable));
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
  public CrossGammaParameterSensitivities combinedWith(CrossGammaParameterSensitivities other) {
    List<CrossGammaParameterSensitivity> mutable = new ArrayList<>(sensitivities);
    for (CrossGammaParameterSensitivity otherSens : other.sensitivities) {
      insert(mutable, otherSens);
    }
    return new CrossGammaParameterSensitivities(ImmutableList.copyOf(mutable));
  }

  // inserts a sensitivity into the mutable list in the right location
  // merges the entry with an existing entry if the key matches
  private static void insert(List<CrossGammaParameterSensitivity> mutable, CrossGammaParameterSensitivity addition) {
    int index = Collections.binarySearch(
        mutable, addition, CrossGammaParameterSensitivity::compareKey);
    if (index >= 0) {
      CrossGammaParameterSensitivity base = mutable.get(index);
      DoubleMatrix combined = base.getSensitivity().plus(addition.getSensitivity());
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
  public CrossGammaParameterSensitivities convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    List<CrossGammaParameterSensitivity> mutable = new ArrayList<>();
    for (CrossGammaParameterSensitivity sens : sensitivities) {
      insert(mutable, sens.convertedTo(resultCurrency, rateProvider));
    }
    return new CrossGammaParameterSensitivities(ImmutableList.copyOf(mutable));
  }

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
    CrossGammaParameterSensitivities converted = convertedTo(resultCurrency, rateProvider);
    double total = converted.sensitivities.stream()
        .mapToDouble(s -> s.getSensitivity().total())
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
        .map(CrossGammaParameterSensitivity::total)
        .collect(toMultiCurrencyAmount());
  }

  /**
   * Returns the diagonal part of the sensitivity values.
   * 
   * @return the diagonal part
   */
  public CurrencyParameterSensitivities diagonal() {
    return CurrencyParameterSensitivities.of(sensitivities.stream().map(s -> s.diagonal()).collect(Collectors.toList()));
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
  public CrossGammaParameterSensitivities multipliedBy(double factor) {
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
  public CrossGammaParameterSensitivities mapSensitivities(DoubleUnaryOperator operator) {
    return sensitivities.stream()
        .map(s -> s.mapSensitivity(operator))
        .collect(
            Collectors.collectingAndThen(
                Guavate.toImmutableList(),
                CrossGammaParameterSensitivities::new));
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
  public boolean equalWithTolerance(CrossGammaParameterSensitivities other, double tolerance) {
    List<CrossGammaParameterSensitivity> mutable = new ArrayList<>(other.sensitivities);
    // for each sensitivity in this instance, find matching in other instance
    for (CrossGammaParameterSensitivity sens1 : sensitivities) {
      // list is already sorted so binary search is safe
      int index = Collections.binarySearch(mutable, sens1, CrossGammaParameterSensitivity::compareKey);
      if (index >= 0) {
        // matched, so must be equal
        CrossGammaParameterSensitivity sens2 = mutable.get(index);
        if (!equalWithTolerance(sens1.getSensitivity(), sens2.getSensitivity(), tolerance)) {
          return false;
        }
        mutable.remove(index);
      } else {
        // did not match, so must be zero
        if (!equalZeroWithTolerance(sens1.getSensitivity(), tolerance)) {
          return false;
        }
      }
    }
    // all that remain from other instance must be zero
    for (CrossGammaParameterSensitivity sens2 : mutable) {
      if (!equalZeroWithTolerance(sens2.getSensitivity(), tolerance)) {
        return false;
      }
    }
    return true;
  }

  private boolean equalWithTolerance(DoubleMatrix sens1, DoubleMatrix sens2, double tolerance) {
    int colCount = sens1.columnCount();
    if (colCount != sens2.columnCount()) {
      return false;
    }
    for (int i = 0; i < colCount; ++i) {
      if (!sens1.column(i).equalWithTolerance(sens2.column(i), tolerance)) {
        return false;
      }
    }
    return true;
  }

  private boolean equalZeroWithTolerance(DoubleMatrix sens, double tolerance) {
    int colCount = sens.columnCount();
    for (int i = 0; i < colCount; ++i) {
      if (!DoubleArrayMath.fuzzyEqualsZero(sens.column(i).toArray(), tolerance)) {
        return false;
      }
    }
    return true;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CrossGammaParameterSensitivities}.
   * @return the meta-bean, not null
   */
  public static CrossGammaParameterSensitivities.Meta meta() {
    return CrossGammaParameterSensitivities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CrossGammaParameterSensitivities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public CrossGammaParameterSensitivities.Meta metaBean() {
    return CrossGammaParameterSensitivities.Meta.INSTANCE;
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
  public ImmutableList<CrossGammaParameterSensitivity> getSensitivities() {
    return sensitivities;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CrossGammaParameterSensitivities other = (CrossGammaParameterSensitivities) obj;
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
    buf.append("CrossGammaParameterSensitivities{");
    buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(sensitivities));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CrossGammaParameterSensitivities}.
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
    private final MetaProperty<ImmutableList<CrossGammaParameterSensitivity>> sensitivities = DirectMetaProperty.ofImmutable(
        this, "sensitivities", CrossGammaParameterSensitivities.class, (Class) ImmutableList.class);
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
    public BeanBuilder<? extends CrossGammaParameterSensitivities> builder() {
      return new CrossGammaParameterSensitivities.Builder();
    }

    @Override
    public Class<? extends CrossGammaParameterSensitivities> beanType() {
      return CrossGammaParameterSensitivities.class;
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
    public MetaProperty<ImmutableList<CrossGammaParameterSensitivity>> sensitivities() {
      return sensitivities;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          return ((CrossGammaParameterSensitivities) bean).getSensitivities();
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
   * The bean-builder for {@code CrossGammaParameterSensitivities}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<CrossGammaParameterSensitivities> {

    private List<CrossGammaParameterSensitivity> sensitivities = ImmutableList.of();

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
          this.sensitivities = (List<CrossGammaParameterSensitivity>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public CrossGammaParameterSensitivities build() {
      return new CrossGammaParameterSensitivities(
          sensitivities);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("CrossGammaParameterSensitivities.Builder{");
      buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(sensitivities));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
