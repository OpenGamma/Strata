/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.io.Serializable;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
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
import com.opengamma.analytics.financial.model.interestrate.curve.PriceIndexCurve;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.market.sensitivity.IndexCurrencySensitivityKey;
import com.opengamma.strata.market.sensitivity.InflationRateSensitivity;
import com.opengamma.strata.market.sensitivity.NameCurrencySensitivityKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SensitivityKey;

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
   * The map containing the price index curves. 
   * The map links price indexes to their price index curves.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<PriceIndex, PriceIndexCurve> priceIndexCurves;

  //TODO old PriceIndexCurve should be replaced by new PriceIndexCurve

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
  public static PriceIndexProvider of(PriceIndex index, PriceIndexCurve curve) {
    return new PriceIndexProvider(ImmutableMap.of(index, curve));
  }

  /**
   * Obtains the price index provider from a map.
   * 
   * @param map  the map of PriceIndex and PriceIndexCurve
   * @return the PriceIndexProvider instance
   */
  public static PriceIndexProvider of(Map<PriceIndex, PriceIndexCurve> map) {
    return new PriceIndexProvider(map);
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this price index provider with an additional price index curve.
   * <p>
   * This returns a new price index provider instance with the additional curve. 
   * The result contains all of the price indexes plus the specified price index. 
   * If the additional price index is present in this instance, an exception is thrown. 
   * 
   * @param index  the price index
   * @param curve  the price index curve
   * @return a price index provider instance with this and the additional curve
   */
  public PriceIndexProvider combinedWith(PriceIndex index, PriceIndexCurve curve) {
    Map<PriceIndex, PriceIndexCurve> combined = new LinkedHashMap<>(priceIndexCurves);
    ArgChecker.isFalse(combined.containsKey(index), "Index curve for {} is present", index);
    combined.put(index, curve);
    return new PriceIndexProvider(combined);
  }

  /**
   * Combines this price index provider with another price index provider instance.
   * <p>
   * This returns a new price index provider instance with the other price index provider. The result contains 
   * all of the price indexes in this instance plus the price indexes in the other price index provider. 
   * If a price index in the additional price index provider is present in this instance, an exception is thrown. 
   * 
   * @param other  the other price index provider
   * @return a price index provider instance with this and the other price index provider
   */
  public PriceIndexProvider combinedWith(PriceIndexProvider other) {
    if (other.priceIndexCurves.isEmpty()) {
      return this;
    }
    if (priceIndexCurves.isEmpty()) {
      return other;
    }
    Map<PriceIndex, PriceIndexCurve> combined = new LinkedHashMap<>(priceIndexCurves);
    for (Entry<PriceIndex, PriceIndexCurve> entry : other.priceIndexCurves.entrySet()) {
      ArgChecker.isFalse(combined.containsKey(entry.getKey()), "Index curve for {} is present", entry.getKey());
      combined.put(entry.getKey(), entry.getValue());
    }
    return new PriceIndexProvider(combined);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the historic or forward rate of price index.
   * <p>
   * The value of the price index, such as 'GB-HICP', varies over time.
   * This retrieves the actual rate if the reference month is before the month of the valuation date and 
   * the index is already published, or the estimated rate if the index is not yet fixed.
   * 
   * @param index  the index of prices 
   * @param referenceMonth  the reference month for the index 
   * @param ratesProvider  the rate provider
   * @return the price index value 
   */
  public double inflationIndexRate(PriceIndex index, YearMonth referenceMonth, RatesProvider ratesProvider) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(referenceMonth, "referenceMonth");
    ArgChecker.notNull(ratesProvider, "ratesProvider");
    LocalDateDoubleTimeSeries timeSeries = ratesProvider.timeSeries(index);
    OptionalDouble fixedRate = timeSeries.get(referenceMonth.atEndOfMonth());
    if (fixedRate.isPresent()) {
      return fixedRate.getAsDouble();
    } else {
      return forwardRate(index, referenceMonth, ratesProvider);
    }
  }

  // forward rate
  private double forwardRate(PriceIndex index, YearMonth referenceMonth, RatesProvider ratesProvider) {
    PriceIndexCurve indexCurve = priceIndexCurves.get(index);
    double relativeTime = ratesProvider.relativeTime(referenceMonth.atEndOfMonth());
    return indexCurve.getPriceIndex(relativeTime);
  }

  /**
   * Gets the basic curve sensitivity for forward rate of price index.
   * <p>
   * This returns a sensitivity instance referring to the curve used to determine the forward rate.
   * The sensitivity will have the value 1.
   * The sensitivity refers to the result of {@link #inflationIndexRate(PriceIndex, YearMonth, RatesProvider)}.
   *  
   * @param index  the index of prices 
   * @param referenceMonth  the reference month for the index 
   * @param ratesProvider  the rate provider
   * @return the point sensitivity of the rate
   */
  public PointSensitivityBuilder inflationIndexRateSensitivity(
      PriceIndex index,
      YearMonth referenceMonth,
      RatesProvider ratesProvider) {

    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(referenceMonth, "referenceMonth");
    ArgChecker.notNull(ratesProvider, "ratesProvider");
    LocalDateDoubleTimeSeries timeSeries = ratesProvider.timeSeries(index);
    if (timeSeries.get(referenceMonth.atEndOfMonth()).isPresent()) {
      return PointSensitivityBuilder.none();
    }
    return InflationRateSensitivity.of(index, referenceMonth, 1.0d);
  }

  //-------------------------------------------------------------------------
  //TODO discuss appropriate location of parameterSensitivity method.
  /**
   * Computes the parameter sensitivity.
   * <p>
   * This computes the {@link CurveParameterSensitivity} associated with the {@link PointSensitivities}.
   * This corresponds to the projection of the point sensitivity to the curve internal parameters representation.
   * 
   * @param pointSensitivities the point sensitivity
   * @param ratesProvider the rate provider
   * @return  the sensitivity to the curve parameters
   */
  public CurveParameterSensitivity parameterSensitivity(
      PointSensitivities pointSensitivities,
      RatesProvider ratesProvider) {

    Map<SensitivityKey, double[]> mutableMap = new HashMap<>();
    // group by currency
    ListMultimap<IndexCurrencySensitivityKey, DoublesPair> grouped = ArrayListMultimap.create();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof InflationRateSensitivity) {
        InflationRateSensitivity pt = (InflationRateSensitivity) point;
        PriceIndex index = pt.getIndex();
        YearMonth referenceMonth = pt.getReferenceMonth();
        double relativeTime = ratesProvider.relativeTime(referenceMonth.atEndOfMonth());
        double sensitivityValue = pt.getSensitivity();
        IndexCurrencySensitivityKey key = IndexCurrencySensitivityKey.of(index, pt.getCurrency());
        grouped.put(key, DoublesPair.of(relativeTime, sensitivityValue));
      }
    }
    // calculate per currency
    for (IndexCurrencySensitivityKey key : grouped.keySet()) {
      PriceIndex index = (PriceIndex) key.getIndex();
      PriceIndexCurve curve = priceIndexCurves.get(index);
      SensitivityKey keyParam = NameCurrencySensitivityKey.of(curve.getName(), key.getCurrency());
      double[] sensiParam = parameterSensitivityIndex(curve, grouped.get(key));
      mutableMap.merge(keyParam, sensiParam, PriceIndexProvider::combineArrays);
    }
    return CurveParameterSensitivity.of(mutableMap);
  }

  // DoublesPair should contain a pair of reference time and point sensitivity value
  private double[] parameterSensitivityIndex(PriceIndexCurve curve, List<DoublesPair> pointSensitivity) {
    int nbParameters = curve.getNumberOfParameters();
    double[] result = new double[nbParameters];
    for (DoublesPair timeAndS : pointSensitivity) {
      double[] sensiPt = curve.getPriceIndexParameterSensitivity(timeAndS.getFirst());
      double forwardBar = timeAndS.getSecond();
      for (int i = 0; i < nbParameters; i++) {
        result[i] += sensiPt[i] * forwardBar;
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
      Map<PriceIndex, PriceIndexCurve> priceIndexCurves) {
    JodaBeanUtils.notNull(priceIndexCurves, "priceIndexCurves");
    this.priceIndexCurves = ImmutableMap.copyOf(priceIndexCurves);
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
   * Gets the map containing the price index curves.
   * The map links price indexes to their price index curves.
   * @return the value of the property, not null
   */
  public ImmutableMap<PriceIndex, PriceIndexCurve> getPriceIndexCurves() {
    return priceIndexCurves;
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
      return JodaBeanUtils.equal(getPriceIndexCurves(), other.getPriceIndexCurves());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getPriceIndexCurves());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("PriceIndexProvider{");
    buf.append("priceIndexCurves").append('=').append(JodaBeanUtils.toString(getPriceIndexCurves()));
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
     * The meta-property for the {@code priceIndexCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<PriceIndex, PriceIndexCurve>> priceIndexCurves = DirectMetaProperty.ofImmutable(
        this, "priceIndexCurves", PriceIndexProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "priceIndexCurves");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 897469389:  // priceIndexCurves
          return priceIndexCurves;
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
     * The meta-property for the {@code priceIndexCurves} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<PriceIndex, PriceIndexCurve>> priceIndexCurves() {
      return priceIndexCurves;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 897469389:  // priceIndexCurves
          return ((PriceIndexProvider) bean).getPriceIndexCurves();
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

    private Map<PriceIndex, PriceIndexCurve> priceIndexCurves = ImmutableMap.of();

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
      this.priceIndexCurves = beanToCopy.getPriceIndexCurves();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 897469389:  // priceIndexCurves
          return priceIndexCurves;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 897469389:  // priceIndexCurves
          this.priceIndexCurves = (Map<PriceIndex, PriceIndexCurve>) newValue;
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
          priceIndexCurves);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code priceIndexCurves} property in the builder.
     * @param priceIndexCurves  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder priceIndexCurves(Map<PriceIndex, PriceIndexCurve> priceIndexCurves) {
      JodaBeanUtils.notNull(priceIndexCurves, "priceIndexCurves");
      this.priceIndexCurves = priceIndexCurves;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("PriceIndexProvider.Builder{");
      buf.append("priceIndexCurves").append('=').append(JodaBeanUtils.toString(priceIndexCurves));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
