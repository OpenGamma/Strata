/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import java.util.ArrayList;
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
import com.opengamma.strata.market.surface.SurfaceName;

/**
 * Currency-based parameter sensitivity for a collection of surfaces.
 * <p>
 * Currency-based parameter sensitivity is the sensitivity of a currency-based value to the parameters
 * of a surface used to determine the value.
 * <p>
 * This class represents sensitivity to a collection of surfaces.
 * The sensitivity is expressed as a single entry for each surface.
 * The order of the list has no specific meaning.
 */
@BeanDefinition(builderScope = "private")
public final class SurfaceCurrencyParameterSensitivities
    implements FxConvertible<SurfaceCurrencyParameterSensitivities>, ImmutableBean {

  /**
  * An empty instance.
  */
  private static final SurfaceCurrencyParameterSensitivities EMPTY = new SurfaceCurrencyParameterSensitivities(ImmutableList.of());

  /**
  * The parameter sensitivities.
  * <p>
  * Each entry includes details of the surface it relates to.
  */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<SurfaceCurrencyParameterSensitivity> sensitivities;

  //-------------------------------------------------------------------------
  /**
   * An empty sensitivity instance.
   * 
   * @return the empty instance
   */
  public static SurfaceCurrencyParameterSensitivities empty() {
    return EMPTY;
  }

  /**
   * Obtains an instance from a multiple sensitivity entries.
   *
   * @param sensitivities  the sensitivities
   * @return the sensitivities instance
   */
  public static SurfaceCurrencyParameterSensitivities of(SurfaceCurrencyParameterSensitivity... sensitivities) {
    return new SurfaceCurrencyParameterSensitivities(ImmutableList.copyOf(sensitivities));
  }

  /**
   * Obtains an instance from a list of sensitivity entries.
   * 
   * @param sensitivities  the list of sensitivity entries
   * @return the sensitivities instance
   */
  public static SurfaceCurrencyParameterSensitivities of(
      List<? extends SurfaceCurrencyParameterSensitivity> sensitivities) {
    List<SurfaceCurrencyParameterSensitivity> mutable = new ArrayList<>();
    for (SurfaceCurrencyParameterSensitivity otherSens : sensitivities) {
      insert(mutable, otherSens);
    }
    return new SurfaceCurrencyParameterSensitivities(ImmutableList.copyOf(mutable));
  }

  // where not pre-sorted
  @ImmutableConstructor
  private SurfaceCurrencyParameterSensitivities(List<? extends SurfaceCurrencyParameterSensitivity> sensitivities) {
    if (sensitivities.size() < 2) {
      this.sensitivities = ImmutableList.copyOf(sensitivities);
    } else {
      List<SurfaceCurrencyParameterSensitivity> mutable = new ArrayList<>(sensitivities);
      mutable.sort(SurfaceCurrencyParameterSensitivity::compareExcludingSensitivity);
      this.sensitivities = ImmutableList.copyOf(mutable);
    }
  }

  // used when pre-sorted
  private SurfaceCurrencyParameterSensitivities(ImmutableList<SurfaceCurrencyParameterSensitivity> sensitivities) {
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
   * @param name  the surface name to find
   * @param currency  the currency to find
   * @return the matching sensitivity
   * @throws IllegalArgumentException if the name and currency do not match an entry
   */
  public SurfaceCurrencyParameterSensitivity getSensitivity(SurfaceName name, Currency currency) {
    return findSensitivity(name, currency)
        .orElseThrow(() -> new IllegalArgumentException(Messages.format(
            "Unable to find sensitivity: {} for {}", name, currency)));
  }

  /**
   * Finds a single sensitivity instance by name and currency.
   * <p>
   * If the sensitivity is not found, optional empty is returned.
   * 
   * @param name  the surface name to find
   * @param currency  the currency to find
   * @return the matching sensitivity
   */
  public Optional<SurfaceCurrencyParameterSensitivity> findSensitivity(SurfaceName name, Currency currency) {
    return sensitivities.stream()
        .filter(sens -> sens.getSurfaceName().equals(name) && sens.getCurrency().equals(currency))
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
  public SurfaceCurrencyParameterSensitivities combinedWith(SurfaceCurrencyParameterSensitivity other) {
    List<SurfaceCurrencyParameterSensitivity> mutable = new ArrayList<>(sensitivities);
    insert(mutable, other);
    return new SurfaceCurrencyParameterSensitivities(ImmutableList.copyOf(mutable));
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
  public SurfaceCurrencyParameterSensitivities combinedWith(SurfaceCurrencyParameterSensitivities other) {
    List<SurfaceCurrencyParameterSensitivity> mutable = new ArrayList<>(sensitivities);
    for (SurfaceCurrencyParameterSensitivity otherSens : other.sensitivities) {
      insert(mutable, otherSens);
    }
    return new SurfaceCurrencyParameterSensitivities(ImmutableList.copyOf(mutable));
  }

  // inserts a sensitivity into the mutable list in the right location
  // merges the entry with an existing entry if the key matches
  private static void insert(List<SurfaceCurrencyParameterSensitivity> mutable,
      SurfaceCurrencyParameterSensitivity addition) {
    int index = Collections.binarySearch(
        mutable, addition, SurfaceCurrencyParameterSensitivity::compareExcludingSensitivity);
    if (index >= 0) {
      SurfaceCurrencyParameterSensitivity base = mutable.get(index);
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
  public SurfaceCurrencyParameterSensitivities convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    List<SurfaceCurrencyParameterSensitivity> mutable = new ArrayList<>();
    for (SurfaceCurrencyParameterSensitivity sens : sensitivities) {
      insert(mutable, sens.convertedTo(resultCurrency, rateProvider));
    }
    return new SurfaceCurrencyParameterSensitivities(ImmutableList.copyOf(mutable));
  }

  /**
   * Totals all the sensitivity values.
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
    SurfaceCurrencyParameterSensitivities converted = convertedTo(resultCurrency, rateProvider);
    double total = converted.sensitivities.stream()
        .mapToDouble(s -> s.getSensitivity().total())
        .sum();
    return CurrencyAmount.of(resultCurrency, total);
  }

  /**
   * Totals all the sensitivity values.
   * <p>
   * The result is the total of all values, in whatever currency they are defined.
   * 
   * @return the total sensitivity
   */
  public MultiCurrencyAmount total() {
    return sensitivities.stream()
        .map(SurfaceCurrencyParameterSensitivity::total)
        .collect(MultiCurrencyAmount.collector());
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
  public SurfaceCurrencyParameterSensitivities multipliedBy(double factor) {
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
  public SurfaceCurrencyParameterSensitivities mapSensitivities(DoubleUnaryOperator operator) {
    return sensitivities.stream()
        .map(s -> s.mapSensitivity(operator))
        .collect(
            Collectors.collectingAndThen(
                Guavate.toImmutableList(),
                SurfaceCurrencyParameterSensitivities::new));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SurfaceCurrencyParameterSensitivities}.
   * @return the meta-bean, not null
   */
  public static SurfaceCurrencyParameterSensitivities.Meta meta() {
    return SurfaceCurrencyParameterSensitivities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SurfaceCurrencyParameterSensitivities.Meta.INSTANCE);
  }

  @Override
  public SurfaceCurrencyParameterSensitivities.Meta metaBean() {
    return SurfaceCurrencyParameterSensitivities.Meta.INSTANCE;
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
   * Each entry includes details of the surface it relates to.
   * @return the value of the property, not null
   */
  public ImmutableList<SurfaceCurrencyParameterSensitivity> getSensitivities() {
    return sensitivities;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SurfaceCurrencyParameterSensitivities other = (SurfaceCurrencyParameterSensitivities) obj;
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
    buf.append("SurfaceCurrencyParameterSensitivities{");
    buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(sensitivities));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SurfaceCurrencyParameterSensitivities}.
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
    private final MetaProperty<ImmutableList<SurfaceCurrencyParameterSensitivity>> sensitivities = DirectMetaProperty.ofImmutable(
        this, "sensitivities", SurfaceCurrencyParameterSensitivities.class, (Class) ImmutableList.class);
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
    public BeanBuilder<? extends SurfaceCurrencyParameterSensitivities> builder() {
      return new SurfaceCurrencyParameterSensitivities.Builder();
    }

    @Override
    public Class<? extends SurfaceCurrencyParameterSensitivities> beanType() {
      return SurfaceCurrencyParameterSensitivities.class;
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
    public MetaProperty<ImmutableList<SurfaceCurrencyParameterSensitivity>> sensitivities() {
      return sensitivities;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          return ((SurfaceCurrencyParameterSensitivities) bean).getSensitivities();
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
   * The bean-builder for {@code SurfaceCurrencyParameterSensitivities}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<SurfaceCurrencyParameterSensitivities> {

    private List<SurfaceCurrencyParameterSensitivity> sensitivities = ImmutableList.of();

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
          this.sensitivities = (List<SurfaceCurrencyParameterSensitivity>) newValue;
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
    public SurfaceCurrencyParameterSensitivities build() {
      return new SurfaceCurrencyParameterSensitivities(
          sensitivities);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("SurfaceCurrencyParameterSensitivities.Builder{");
      buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(sensitivities));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
