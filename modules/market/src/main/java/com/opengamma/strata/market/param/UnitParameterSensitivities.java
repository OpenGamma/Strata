/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

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
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.surface.Surface;

/**
 * Unit parameter sensitivity for parameterized market data, such as curves.
 * <p>
 * Parameter sensitivity is the sensitivity of a value to the parameters of
 * {@linkplain ParameterizedData parameterized market data} objects that are used to determine the value.
 * Common {@code ParameterizedData} implementations include {@link Curve} and {@link Surface}.
 * <p>
 * For example, par rate sensitivity to an underlying curve would be expressed using this
 * class as there is no associated currency.
 * <p>
 * The sensitivity is expressed as a single entry for piece of parameterized market data.
 * The sensitivity has no associated currency.
 * The order of the list has no specific meaning.
 * <p>
 * One way of viewing this class is as a {@code Map} from a specific sensitivity key to
 * {@code DoubleArray} sensitivity values. However, instead of being structured as a {@code Map},
 * the data is structured as a {@code List}, with the key and value in each entry.
 */
@BeanDefinition(builderScope = "private")
public final class UnitParameterSensitivities
    implements ImmutableBean, Serializable {

  /**
   * An empty instance.
   */
  private static final UnitParameterSensitivities EMPTY = new UnitParameterSensitivities(ImmutableList.of());

  /**
   * The parameter sensitivities.
   * <p>
   * Each entry includes details of the {@link ParameterizedData} it relates to.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<UnitParameterSensitivity> sensitivities;

  //-------------------------------------------------------------------------
  /**
   * An empty sensitivity instance.
   * 
   * @return the empty instance
   */
  public static UnitParameterSensitivities empty() {
    return EMPTY;
  }

  /**
   * Obtains an instance from a single sensitivity entry.
   * 
   * @param sensitivity  the sensitivity entry
   * @return the sensitivities instance
   */
  public static UnitParameterSensitivities of(UnitParameterSensitivity sensitivity) {
    return new UnitParameterSensitivities(ImmutableList.of(sensitivity));
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
  public static UnitParameterSensitivities of(UnitParameterSensitivity... sensitivities) {
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
  public static UnitParameterSensitivities of(List<? extends UnitParameterSensitivity> sensitivities) {
    List<UnitParameterSensitivity> mutable = new ArrayList<>();
    for (UnitParameterSensitivity otherSens : sensitivities) {
      insert(mutable, otherSens);
    }
    return new UnitParameterSensitivities(ImmutableList.copyOf(mutable));
  }

  // used when not pre-sorted
  @ImmutableConstructor
  private UnitParameterSensitivities(List<? extends UnitParameterSensitivity> sensitivities) {
    if (sensitivities.size() < 2) {
      this.sensitivities = ImmutableList.copyOf(sensitivities);
    } else {
      List<UnitParameterSensitivity> mutable = new ArrayList<>(sensitivities);
      mutable.sort(UnitParameterSensitivity::compareKey);
      this.sensitivities = ImmutableList.copyOf(mutable);
    }
  }

  // used when pre-sorted
  private UnitParameterSensitivities(ImmutableList<UnitParameterSensitivity> sensitivities) {
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
   * Gets a single sensitivity instance by name.
   * 
   * @param name  the curve name to find
   * @return the matching sensitivity
   * @throws IllegalArgumentException if the name and currency do not match an entry
   */
  public UnitParameterSensitivity getSensitivity(MarketDataName<?> name) {
    return findSensitivity(name)
        .orElseThrow(() -> new IllegalArgumentException(Messages.format(
            "Unable to find sensitivity: {}", name)));
  }

  /**
   * Finds a single sensitivity instance by name.
   * <p>
   * If the sensitivity is not found, optional empty is returned.
   * 
   * @param name  the curve name to find
   * @return the matching sensitivity
   */
  public Optional<UnitParameterSensitivity> findSensitivity(MarketDataName<?> name) {
    return sensitivities.stream()
        .filter(sens -> sens.getMarketDataName().equals(name))
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
  public UnitParameterSensitivities combinedWith(UnitParameterSensitivity other) {
    List<UnitParameterSensitivity> mutable = new ArrayList<>(sensitivities);
    insert(mutable, other);
    return new UnitParameterSensitivities(ImmutableList.copyOf(mutable));
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
  public UnitParameterSensitivities combinedWith(UnitParameterSensitivities other) {
    List<UnitParameterSensitivity> mutable = new ArrayList<>(sensitivities);
    for (UnitParameterSensitivity otherSens : other.sensitivities) {
      insert(mutable, otherSens);
    }
    return new UnitParameterSensitivities(ImmutableList.copyOf(mutable));
  }

  // inserts a sensitivity into the mutable list in the right location
  // merges the entry with an existing entry if the key matches
  private static void insert(List<UnitParameterSensitivity> mutable, UnitParameterSensitivity addition) {
    int index = Collections.binarySearch(
        mutable, addition, UnitParameterSensitivity::compareKey);
    if (index >= 0) {
      UnitParameterSensitivity base = mutable.get(index);
      DoubleArray combined = base.getSensitivity().plus(addition.getSensitivity());
      mutable.set(index, base.withSensitivity(combined));
    } else {
      int insertionPoint = -(index + 1);
      mutable.add(insertionPoint, addition);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this sensitivity to a monetary value, multiplying by the specified factor.
   * <p>
   * The result will consist of the entries based on the entries of this instance.
   * Each entry in the result will be in the specified currency and multiplied by the specified amount.
   * 
   * @param currency  the currency of the amount
   * @param amount  the amount to multiply by
   * @return the resulting sensitivity object
   */
  public CurrencyParameterSensitivities multipliedBy(Currency currency, double amount) {
    return sensitivities.stream()
        .map(s -> s.multipliedBy(currency, amount))
        .collect(
            Collectors.collectingAndThen(
                Guavate.toImmutableList(),
                CurrencyParameterSensitivities::of));
  }

  /**
   * Returns an instance with the sensitivity values multiplied by the specified factor.
   * <p>
   * The result will consist of the same entries, but with each sensitivity value multiplied.
   * This instance is immutable and unaffected by this method.
   * 
   * @param factor  the multiplicative factor
   * @return an instance based on this one, with each sensitivity multiplied by the factor
   */
  public UnitParameterSensitivities multipliedBy(double factor) {
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
  public UnitParameterSensitivities mapSensitivities(DoubleUnaryOperator operator) {
    return sensitivities.stream()
        .map(s -> s.mapSensitivity(operator))
        .collect(
            Collectors.collectingAndThen(
                Guavate.toImmutableList(),
                UnitParameterSensitivities::new));
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
  public UnitParameterSensitivities split() {
    if (!sensitivities.stream().anyMatch(s -> s.getParameterSplit().isPresent())) {
      return this;
    }
    return of(sensitivities.stream()
        .flatMap(s -> s.split().stream())
        .collect(toImmutableList()));
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
  public boolean equalWithTolerance(UnitParameterSensitivities other, double tolerance) {
    List<UnitParameterSensitivity> mutable = new ArrayList<>(other.sensitivities);
    // for each sensitivity in this instance, find matching in other instance
    for (UnitParameterSensitivity sens1 : sensitivities) {
      // list is already sorted so binary search is safe
      int index = Collections.binarySearch(mutable, sens1, UnitParameterSensitivity::compareKey);
      if (index >= 0) {
        // matched, so must be equal
        UnitParameterSensitivity sens2 = mutable.get(index);
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
    for (UnitParameterSensitivity sens2 : mutable) {
      if (!sens2.getSensitivity().equalZeroWithTolerance(tolerance)) {
        return false;
      }
    }
    return true;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code UnitParameterSensitivities}.
   * @return the meta-bean, not null
   */
  public static UnitParameterSensitivities.Meta meta() {
    return UnitParameterSensitivities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(UnitParameterSensitivities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public UnitParameterSensitivities.Meta metaBean() {
    return UnitParameterSensitivities.Meta.INSTANCE;
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
  public ImmutableList<UnitParameterSensitivity> getSensitivities() {
    return sensitivities;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      UnitParameterSensitivities other = (UnitParameterSensitivities) obj;
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
    buf.append("UnitParameterSensitivities{");
    buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(sensitivities));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code UnitParameterSensitivities}.
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
    private final MetaProperty<ImmutableList<UnitParameterSensitivity>> sensitivities = DirectMetaProperty.ofImmutable(
        this, "sensitivities", UnitParameterSensitivities.class, (Class) ImmutableList.class);
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
    public BeanBuilder<? extends UnitParameterSensitivities> builder() {
      return new UnitParameterSensitivities.Builder();
    }

    @Override
    public Class<? extends UnitParameterSensitivities> beanType() {
      return UnitParameterSensitivities.class;
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
    public MetaProperty<ImmutableList<UnitParameterSensitivity>> sensitivities() {
      return sensitivities;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          return ((UnitParameterSensitivities) bean).getSensitivities();
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
   * The bean-builder for {@code UnitParameterSensitivities}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<UnitParameterSensitivities> {

    private List<UnitParameterSensitivity> sensitivities = ImmutableList.of();

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
          this.sensitivities = (List<UnitParameterSensitivity>) newValue;
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
    public UnitParameterSensitivities build() {
      return new UnitParameterSensitivities(
          sensitivities);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("UnitParameterSensitivities.Builder{");
      buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(sensitivities));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
