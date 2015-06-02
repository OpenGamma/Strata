/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.io.Serializable;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.ObjectDoublePair;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivities;
import com.opengamma.strata.market.sensitivity.IndexCurrencySensitivityKey;
import com.opengamma.strata.market.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.market.sensitivity.NameCurrencySensitivityKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.SensitivityKey;
import com.opengamma.strata.market.value.PriceIndexValues;

/**
 * A provider of price indexes. 
 * <p>
 * This wraps an immutable map of price index curves, retrieves the historic or forward rate
 * of price index and computes the basic curve sensitivity for the forward rate. 
 * <p>
 * This is intended to be used as an element of additionalData in {@link ImmutableRatesProvider}.
 */
@BeanDefinition
public final class PriceIndexProvider
    implements ImmutableBean, Serializable {

  /**
   * An empty instance.
   */
  private static final PriceIndexProvider EMPTY = new PriceIndexProvider(ImmutableMap.of());

  /**
   * The map containing the price index values. 
   * The map links price indexes to their price index values.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<PriceIndex, PriceIndexValues> priceIndexValues;

  //-------------------------------------------------------------------------
  /**
   * An empty provider instance.
   * 
   * @return the empty instance
   */
  public static PriceIndexProvider empty() {
    return EMPTY;
  }

  /**
   * Obtains the price index provider from an index and its price index curve.
   * 
   * @param index  the price index
   * @param curve  the curve
   * @return the PriceIndexProvider instance
   */
  public static PriceIndexProvider of(PriceIndex index, PriceIndexValues curve) {
    return new PriceIndexProvider(ImmutableMap.of(index, curve));
  }

  /**
   * Obtains the price index provider from a map.
   * 
   * @param map  the map of PriceIndex and PriceIndexValues
   * @return the PriceIndexProvider instance
   */
  public static PriceIndexProvider of(Map<PriceIndex, PriceIndexValues> map) {
    return new PriceIndexProvider(map);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the values for an Price index.
   * <p>
   * The value of the Price index, such as 'US-CPI-U', varies over time.
   * This returns an object that can provide historic and forward values for the specified index.
   * 
   * @param index  the index to find values for
   * @return the values for the specified index
   * @throws IllegalArgumentException if the values are not available
   */
  public PriceIndexValues priceIndexValues(PriceIndex index) {
    PriceIndexValues values = priceIndexValues.get(index);
    if (values == null) {
      throw new IllegalArgumentException("Unable to find index: " + index);
    }
    return values;
  }

  //-------------------------------------------------------------------------
  //TODO discuss appropriate location of parameterSensitivity method.
  /**
   * Computes the parameter sensitivity.
   * <p>
   * This computes the {@link CurveParameterSensitivities} associated with the {@link PointSensitivities}.
   * This corresponds to the projection of the point sensitivity to the curve internal parameters representation.
   * 
   * @param pointSensitivities the point sensitivity
   * @return  the sensitivity to the curve parameters
   */
  public CurveParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {

    Map<SensitivityKey, double[]> mutableMap = new HashMap<>();
    // group by currency
    ListMultimap<IndexCurrencySensitivityKey, ObjectDoublePair<YearMonth>> grouped = ArrayListMultimap.create();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof InflationRateSensitivity) {
        InflationRateSensitivity pt = (InflationRateSensitivity) point;
        PriceIndex index = pt.getIndex();
        YearMonth referenceMonth = pt.getReferenceMonth();
        double sensitivityValue = pt.getSensitivity();
        IndexCurrencySensitivityKey key = IndexCurrencySensitivityKey.of(index, pt.getCurrency());
        grouped.put(key, ObjectDoublePair.of(referenceMonth, sensitivityValue));
      }
    }
    // calculate per currency
    for (IndexCurrencySensitivityKey key : grouped.keySet()) {
      PriceIndex index = (PriceIndex) key.getIndex();
      PriceIndexValues values = priceIndexValues.get(index);
      SensitivityKey keyParam = NameCurrencySensitivityKey.of(values.getCurveName(), key.getCurrency());
      double[] sensiParam = parameterSensitivityIndex(values, grouped.get(key));
      mutableMap.merge(keyParam, sensiParam, PriceIndexProvider::combineArrays);
    }
    return CurveParameterSensitivities.of(mutableMap);
  }

  // DoublesPair should contain a pair of reference time and point sensitivity value
  private double[] parameterSensitivityIndex(PriceIndexValues values, List<ObjectDoublePair<YearMonth>> pointSensitivity) {
    int nbParameters = values.getParameterCount();
    double[] result = new double[nbParameters];
    for (ObjectDoublePair<YearMonth> timeAndS : pointSensitivity) {
      double[] unitSens = values.unitParameterSensitivity(timeAndS.getFirst());
      double forwardBar = timeAndS.getSecond();
      for (int i = 0; i < nbParameters; i++) {
        result[i] += unitSens[i] * forwardBar;
      }
    }
    return result;
  }

  // add two arrays - copy form ImmutableRatesProvider
  private static double[] combineArrays(double[] a, double[] b) {
    ArgChecker.isTrue(a.length == b.length, "Sensitivity arrays must have same length");
    double[] result = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = a[i] + b[i];
    }
    return result;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code PriceIndexProvider}.
   * @return the meta-bean, not null
   */
  public static PriceIndexProvider.Meta meta() {
    return PriceIndexProvider.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(PriceIndexProvider.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static PriceIndexProvider.Builder builder() {
    return new PriceIndexProvider.Builder();
  }

  private PriceIndexProvider(
      Map<PriceIndex, PriceIndexValues> priceIndexValues) {
    JodaBeanUtils.notNull(priceIndexValues, "priceIndexValues");
    this.priceIndexValues = ImmutableMap.copyOf(priceIndexValues);
  }

  @Override
  public PriceIndexProvider.Meta metaBean() {
    return PriceIndexProvider.Meta.INSTANCE;
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
   * Gets the map containing the price index values.
   * The map links price indexes to their price index values.
   * @return the value of the property, not null
   */
  public ImmutableMap<PriceIndex, PriceIndexValues> getPriceIndexValues() {
    return priceIndexValues;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      PriceIndexProvider other = (PriceIndexProvider) obj;
      return JodaBeanUtils.equal(getPriceIndexValues(), other.getPriceIndexValues());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getPriceIndexValues());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("PriceIndexProvider{");
    buf.append("priceIndexValues").append('=').append(JodaBeanUtils.toString(getPriceIndexValues()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PriceIndexProvider}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code priceIndexValues} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<PriceIndex, PriceIndexValues>> priceIndexValues = DirectMetaProperty.ofImmutable(
        this, "priceIndexValues", PriceIndexProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "priceIndexValues");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1422773131:  // priceIndexValues
          return priceIndexValues;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public PriceIndexProvider.Builder builder() {
      return new PriceIndexProvider.Builder();
    }

    @Override
    public Class<? extends PriceIndexProvider> beanType() {
      return PriceIndexProvider.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code priceIndexValues} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<PriceIndex, PriceIndexValues>> priceIndexValues() {
      return priceIndexValues;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1422773131:  // priceIndexValues
          return ((PriceIndexProvider) bean).getPriceIndexValues();
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
   * The bean-builder for {@code PriceIndexProvider}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<PriceIndexProvider> {

    private Map<PriceIndex, PriceIndexValues> priceIndexValues = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(PriceIndexProvider beanToCopy) {
      this.priceIndexValues = beanToCopy.getPriceIndexValues();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1422773131:  // priceIndexValues
          return priceIndexValues;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1422773131:  // priceIndexValues
          this.priceIndexValues = (Map<PriceIndex, PriceIndexValues>) newValue;
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
    public PriceIndexProvider build() {
      return new PriceIndexProvider(
          priceIndexValues);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code priceIndexValues} property in the builder.
     * @param priceIndexValues  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder priceIndexValues(Map<PriceIndex, PriceIndexValues> priceIndexValues) {
      JodaBeanUtils.notNull(priceIndexValues, "priceIndexValues");
      this.priceIndexValues = priceIndexValues;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("PriceIndexProvider.Builder{");
      buf.append("priceIndexValues").append('=').append(JodaBeanUtils.toString(priceIndexValues));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
