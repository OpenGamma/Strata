/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;

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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.math.DoubleMath;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Curve parameter sensitivity.
 * <p>
 * The sensitivity to the parameters of one or more curves.
 * The parameters can be the internal parameters of a curve or the market quotes.
 * <p>
 * The sensitivities are defined using {@link SensitivityKey}.
 * This allows different sensitivity classifications, such as {@linkplain NameSensitivityKey by curve}
 * and {@linkplain NameCurrencySensitivityKey by curve and currency}.
 */
@BeanDefinition
public final class CurveParameterSensitivities
    implements ImmutableBean {

  /**
   * An empty instance.
   */
  private static final CurveParameterSensitivities EMPTY = new CurveParameterSensitivities(ImmutableMap.of());

  /**
   * The map containing the sensitivities. 
   * The map links a key to a vector of sensitivities (sensitivities to parameters/inputs).
   * The {@code double} array contains the sensitivity values, which match the size and order
   * of the parameters of the curve identified by the key.
   * The {@code double} array must never be mutated.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<SensitivityKey, double[]> sensitivities;

  //-------------------------------------------------------------------------
  /**
   * An empty sensitivity instance.
   * 
   * @return the empty instance
   */
  public static CurveParameterSensitivities empty() {
    return EMPTY;
  }

  /**
   * Obtains a parameter sensitivity with a single key.
   * <p>
   * The {@code double} array is assigned, not cloned.
   * It must not be mutated once passed in.
   * 
   * @param key  the curve key
   * @param sensitivityArray  the sensitivity to the key, not cloned
   * @return the sensitivity instance
   */
  public static CurveParameterSensitivities of(SensitivityKey key, double[] sensitivityArray) {
    return new CurveParameterSensitivities(ImmutableMap.of(key, sensitivityArray));
  }

  /**
   * Obtains a parameter sensitivity from a map.
   * <p>
   * The {@code double} array is assigned, not cloned.
   * It must not be mutated once passed in.
   * 
   * @param map  the map of sensitivities
   * @return the sensitivity instance
   */
  public static CurveParameterSensitivities of(Map<SensitivityKey, double[]> map) {
    return new CurveParameterSensitivities(map);
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this sensitivity with an additional sensitivity.
   * <p>
   * This returns a new sensitivity instance with the additional sensitivity added.
   * The result contains all the keys from this instance plus the specified key.
   * If the additional key is present in this instance, the array of sensitivity values is added.
   * If the arrays are of different sizes, an exception is thrown.
   * <p>
   * The {@code double} array is assigned, not cloned.
   * It must not be mutated once passed in.
   * <p>
   * This instance is unaffected by this method.
   * 
   * @param key  the curve key
   * @param sensitivityArray  the sensitivity to the key, not cloned
   * @return a sensitivity instance based on this one, with the key added
   * @throws IllegalArgumentException if the other sensitivity cannot be combined
   */
  public CurveParameterSensitivities combinedWith(SensitivityKey key, double[] sensitivityArray) {
    Map<SensitivityKey, double[]> combined = new LinkedHashMap<>(sensitivities);
    combined.merge(key, sensitivityArray, this::combineArrays);
    return new CurveParameterSensitivities(combined);
  }

  /**
   * Combines this sensitivity with another instance.
   * <p>
   * This returns a new sensitivity instance with the entries combined.
   * The result contains the combined set of keys. If a key is present in both this
   * instance and the other instance, the array of sensitivity values is added.
   * If the arrays are of different sizes, an exception is thrown.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param other  the other sensitivity
   * @return a sensitivity instance based on this one, with the other instance added
   * @throws IllegalArgumentException if the other sensitivity cannot be combined
   */
  public CurveParameterSensitivities combinedWith(CurveParameterSensitivities other) {
    if (other.sensitivities.isEmpty()) {
      return this;
    }
    if (sensitivities.isEmpty()) {
      return other;
    }
    Map<SensitivityKey, double[]> combined = new LinkedHashMap<>(sensitivities);
    for (Entry<SensitivityKey, double[]> entry : other.sensitivities.entrySet()) {
      combined.merge(entry.getKey(), entry.getValue(), this::combineArrays);
    }
    return new CurveParameterSensitivities(combined);
  }

  // add two arrays
  private double[] combineArrays(double[] a, double[] b) {
    ArgChecker.isTrue(a.length == b.length, "Sensitivity array must have same length");
    double[] result = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = a[i] + b[i];
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Multiplies the sensitivity arrays by the specified factor.
   * <p>
   * The result will consist of the same keys, but with each sensitivity array value multiplied.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param factor  the multiplicative factor
   * @return a sensitivity instance based on this one, with all vectors multiplied by the factor
   */
  public CurveParameterSensitivities multipliedBy(double factor) {
    return mapSensitivity(s -> s * factor);
  }

  /**
   * Returns an instance with the specified operation applied to the sensitivity arrays.
   * <p>
   * The result will consist of the same keys, but with the operator applied to each sensitivity array value.
   * <p>
   * This is used to apply a mathematical operation to the sensitivities.
   * For example, the operator could multiply the sensitivities by a constant, or take the inverse.
   * <pre>
   *   inverse = base.mapSensitivities(value -> 1 / value);
   * </pre>
   * <p>
   * This instance is immutable and unaffected by this method.
   *
   * @param operator  the operator to be applied to the sensitivities
   * @return the resulting builder, replacing this builder
   */
  public CurveParameterSensitivities mapSensitivity(DoubleUnaryOperator operator) {
    Map<SensitivityKey, double[]> result = new LinkedHashMap<>();
    for (Entry<SensitivityKey, double[]> entry : sensitivities.entrySet()) {
      result.put(entry.getKey(), operateOnArray(operator, entry.getValue()));
    }
    return new CurveParameterSensitivities(ImmutableMap.copyOf(result));
  }

  // operate on an array
  private double[] operateOnArray(DoubleUnaryOperator operator, double[] array) {
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = operator.applyAsDouble(array[i]);
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Totals the sensitivity for each key.
   * 
   * @return a map of the total sensitivity per key
   */
  public ImmutableMap<SensitivityKey, Double> totalPerKey() {
    ImmutableMap.Builder<SensitivityKey, Double> builder = ImmutableMap.builder();
    for (SensitivityKey key : sensitivities.keySet()) {
      double[] values = sensitivities.get(key);
      builder.put(key, DoubleStream.of(values).sum());
    }
    return builder.build();
  }

  /**
   * Totals the sensitivity for all keys.
   * 
   * @return the total sensitivity across all keys
   */
  public double total() {
    return sensitivities.values().stream()
        .flatMapToDouble(DoubleStream::of)
        .sum();
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
  public boolean equalWithTolerance(CurveParameterSensitivities other, double tolerance) {
    if (!sensitivities.keySet().equals(other.sensitivities.keySet())) {
      // check that the element outside the intersection have a sensitivity below the tolerance
      Set<SensitivityKey> amb = Sets.difference(sensitivities.keySet(), other.sensitivities.keySet());
      if (!checkSmall(amb, sensitivities, tolerance)) {
        return false;
      }
      Set<SensitivityKey> bma = Sets.difference(other.sensitivities.keySet(), sensitivities.keySet());
      if (!checkSmall(bma, other.sensitivities, tolerance)) {
        return false;
      }
      // construct the key interestion set for the next step
      Set<SensitivityKey> intersection = Sets.intersection(sensitivities.keySet(), other.sensitivities.keySet());
      return checkCommon(intersection, sensitivities, other.sensitivities, tolerance);
    }
    return checkCommon(sensitivities.keySet(), sensitivities, other.sensitivities, tolerance);
  }

  // checks that the sensitivities in a curve sensitivity are small for key in a given key set
  private boolean checkSmall(Set<SensitivityKey> kSet, ImmutableMap<SensitivityKey, double[]> s, double tolerance) {
    for (SensitivityKey k : kSet) {
      double[] v = s.get(k);
      for (int i = 0; i < v.length; i++) {
        if (!DoubleMath.fuzzyEquals(v[i], 0, tolerance)) {
          return false;
        }
      }
    }
    return true;
  }

  // checks that the difference on the common keys are within hte tolerance
  private boolean checkCommon(
      Set<SensitivityKey> common,
      ImmutableMap<SensitivityKey, double[]> s1,
      ImmutableMap<SensitivityKey, double[]> s2,
      double tolerance) {

    for (SensitivityKey key : common) {
      double[] vector1 = s1.get(key);
      double[] vector2 = s2.get(key);
      if (vector1.length != vector2.length) {
        return false;
      }
      for (int i = 0; i < vector1.length; i++) {
        if (!DoubleMath.fuzzyEquals(vector1[i], vector2[i], tolerance)) {
          return false;
        }
      }
    }
    return true;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CurveParameterSensitivities other = (CurveParameterSensitivities) obj;
      if (!sensitivities.keySet().equals(other.sensitivities.keySet())) {
        return false;
      }
      for (SensitivityKey key : sensitivities.keySet()) {
        double[] vector1 = sensitivities.get(key);
        double[] vector2 = other.sensitivities.get(key);
        if (!Arrays.equals(vector1, vector2)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    int h = 0;
    for (Entry<SensitivityKey, double[]> entry : sensitivities.entrySet()) {
      h += (entry.getKey().hashCode() ^ Arrays.hashCode(entry.getValue()));
    }
    hash = hash * 31 + h;
    return hash;
  }

  @Override
  public String toString() {
    if (sensitivities.isEmpty()) {
      return "CurveParameterSensitivity{sensitivities={}}";
    }
    StringBuilder buf = new StringBuilder(64);
    buf.append("CurveParameterSensitivity{sensitivities={");
    for (Entry<SensitivityKey, double[]> entry : sensitivities.entrySet()) {
      buf.append(entry.getKey().toString()).append('=').append(Arrays.toString(entry.getValue())).append(", ");
    }
    buf.setCharAt(buf.length() - 2, '}');
    buf.setCharAt(buf.length() - 1, '}');
    return buf.toString();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CurveParameterSensitivities}.
   * @return the meta-bean, not null
   */
  public static CurveParameterSensitivities.Meta meta() {
    return CurveParameterSensitivities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CurveParameterSensitivities.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CurveParameterSensitivities.Builder builder() {
    return new CurveParameterSensitivities.Builder();
  }

  private CurveParameterSensitivities(
      Map<SensitivityKey, double[]> sensitivities) {
    JodaBeanUtils.notNull(sensitivities, "sensitivities");
    this.sensitivities = ImmutableMap.copyOf(sensitivities);
  }

  @Override
  public CurveParameterSensitivities.Meta metaBean() {
    return CurveParameterSensitivities.Meta.INSTANCE;
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
   * Gets the map containing the sensitivities.
   * The map links a key to a vector of sensitivities (sensitivities to parameters/inputs).
   * The {@code double} array contains the sensitivity values, which match the size and order
   * of the parameters of the curve identified by the key.
   * The {@code double} array must never be mutated.
   * @return the value of the property, not null
   */
  public ImmutableMap<SensitivityKey, double[]> getSensitivities() {
    return sensitivities;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CurveParameterSensitivities}.
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
    private final MetaProperty<ImmutableMap<SensitivityKey, double[]>> sensitivities = DirectMetaProperty.ofImmutable(
        this, "sensitivities", CurveParameterSensitivities.class, (Class) ImmutableMap.class);
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
    public CurveParameterSensitivities.Builder builder() {
      return new CurveParameterSensitivities.Builder();
    }

    @Override
    public Class<? extends CurveParameterSensitivities> beanType() {
      return CurveParameterSensitivities.class;
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
    public MetaProperty<ImmutableMap<SensitivityKey, double[]>> sensitivities() {
      return sensitivities;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          return ((CurveParameterSensitivities) bean).getSensitivities();
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
   * The bean-builder for {@code CurveParameterSensitivities}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CurveParameterSensitivities> {

    private Map<SensitivityKey, double[]> sensitivities = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(CurveParameterSensitivities beanToCopy) {
      this.sensitivities = beanToCopy.getSensitivities();
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
          this.sensitivities = (Map<SensitivityKey, double[]>) newValue;
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
    public CurveParameterSensitivities build() {
      return new CurveParameterSensitivities(
          sensitivities);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code sensitivities} property in the builder.
     * @param sensitivities  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder sensitivities(Map<SensitivityKey, double[]> sensitivities) {
      JodaBeanUtils.notNull(sensitivities, "sensitivities");
      this.sensitivities = sensitivities;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("CurveParameterSensitivities.Builder{");
      buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(sensitivities));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
