/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ComparisonChain;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Unit parameter sensitivity for a single curve.
 * <p>
 * Unit parameter sensitivity is the sensitivity of a value to the parameters of a curve used to
 * determine the value where no currency applies.
 * <p>
 * This class represents sensitivity to a single curve. The sensitivity is expressed as an array
 * of values, one for each parameter used to create the curve.
 */
@BeanDefinition(builderScope = "private")
public final class CurveUnitParameterSensitivity
    implements ImmutableBean {

  /**
   * The curve metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If present, the size of the parameter metadata list will match the number of parameters of this curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveMetadata metadata;
  /**
   * The parameter sensitivity values.
   * There will be one sensitivity value for each parameter of the curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the curve metadata and sensitivity.
   * 
   * @param metadata  the curve metadata
   * @param sensitivity  the sensitivity values, one for each node in the curve
   * @return the sensitivity object
   */
  public static CurveUnitParameterSensitivity of(CurveMetadata metadata, DoubleArray sensitivity) {
    return new CurveUnitParameterSensitivity(metadata, sensitivity);
  }

  @ImmutableValidator
  private void validate() {
    metadata.getParameterMetadata().ifPresent(params -> {
      if (sensitivity.size() != params.size()) {
        throw new IllegalArgumentException("Length of sensitivity and parameter metadata must match when metadata present");
      }
    });
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the curve name.
   * 
   * @return the curve name
   */
  public CurveName getCurveName() {
    return metadata.getCurveName();
  }

  /**
   * Gets the number of parameters in the curve.
   * <p>
   * This returns the number of parameters in the curve.
   * 
   * @return the number of parameters
   */
  public int getParameterCount() {
    return sensitivity.size();
  }

  /**
   * Compares the key of two sensitivity objects, excluding the parameter sensitivity values.
   * 
   * @param other  the other sensitivity object
   * @return positive if greater, zero if equal, negative if less
   */
  public int compareKey(CurveUnitParameterSensitivity other) {
    return ComparisonChain.start()
        .compare(metadata.getCurveName(), other.metadata.getCurveName())
        .result();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance in the specified currency with the sensitivity values multiplied by the specified factor.
   * <p>
   * Each value in the sensitivity array will be multiplied by the specified factor.
   * 
   * @param currency  the currency of the amount
   * @param amount  the amount to multiply by
   * @return the resulting sensitivity object
   */
  public CurveCurrencyParameterSensitivity multipliedBy(Currency currency, double amount) {
    return CurveCurrencyParameterSensitivity.of(metadata, currency, sensitivity.multipliedBy(amount));
  }

  /**
   * Returns an instance with the sensitivity values multiplied by the specified factor.
   * <p>
   * Each value in the sensitivity array will be multiplied by the factor.
   * 
   * @param factor  the multiplicative factor
   * @return an instance based on this one, with each sensitivity multiplied by the factor
   */
  public CurveUnitParameterSensitivity multipliedBy(double factor) {
    return mapSensitivity(s -> s * factor);
  }

  /**
   * Returns an instance with the specified operation applied to the sensitivity values.
   * <p>
   * Each value in the sensitivity array will be operated on.
   * For example, the operator could multiply the sensitivities by a constant, or take the inverse.
   * <pre>
   *   inverse = base.mapSensitivity(value -> 1 / value);
   * </pre>
   *
   * @param operator  the operator to be applied to the sensitivities
   * @return an instance based on this one, with the operator applied to the sensitivity values
   */
  public CurveUnitParameterSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new CurveUnitParameterSensitivity(metadata, sensitivity.map(operator));
  }

  /**
   * Returns an instance with the new parameter sensitivity values.
   * <p>
   * The implementation will clone the input array.
   * 
   * @param sensitivity  the new sensitivity values
   * @return an instance based on this one, with the specified sensitivity values
   */
  public CurveUnitParameterSensitivity withSensitivity(DoubleArray sensitivity) {
    if (sensitivity.size() != this.sensitivity.size()) {
      throw new IllegalArgumentException("Length of sensitivity must match parameter count");
    }
    return new CurveUnitParameterSensitivity(metadata, sensitivity);
  }

  //-------------------------------------------------------------------------
  /**
   * Totals the sensitivity values.
   * 
   * @return the total sensitivity values
   */
  public double total() {
    return sensitivity.total();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CurveUnitParameterSensitivity}.
   * @return the meta-bean, not null
   */
  public static CurveUnitParameterSensitivity.Meta meta() {
    return CurveUnitParameterSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CurveUnitParameterSensitivity.Meta.INSTANCE);
  }

  private CurveUnitParameterSensitivity(
      CurveMetadata metadata,
      DoubleArray sensitivity) {
    JodaBeanUtils.notNull(metadata, "metadata");
    JodaBeanUtils.notNull(sensitivity, "sensitivity");
    this.metadata = metadata;
    this.sensitivity = sensitivity;
    validate();
  }

  @Override
  public CurveUnitParameterSensitivity.Meta metaBean() {
    return CurveUnitParameterSensitivity.Meta.INSTANCE;
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
   * Gets the curve metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If present, the size of the parameter metadata list will match the number of parameters of this curve.
   * @return the value of the property, not null
   */
  public CurveMetadata getMetadata() {
    return metadata;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the parameter sensitivity values.
   * There will be one sensitivity value for each parameter of the curve.
   * @return the value of the property, not null
   */
  public DoubleArray getSensitivity() {
    return sensitivity;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CurveUnitParameterSensitivity other = (CurveUnitParameterSensitivity) obj;
      return JodaBeanUtils.equal(getMetadata(), other.getMetadata()) &&
          JodaBeanUtils.equal(getSensitivity(), other.getSensitivity());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getMetadata());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSensitivity());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("CurveUnitParameterSensitivity{");
    buf.append("metadata").append('=').append(getMetadata()).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(getSensitivity()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CurveUnitParameterSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code metadata} property.
     */
    private final MetaProperty<CurveMetadata> metadata = DirectMetaProperty.ofImmutable(
        this, "metadata", CurveUnitParameterSensitivity.class, CurveMetadata.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<DoubleArray> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", CurveUnitParameterSensitivity.class, DoubleArray.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "metadata",
        "sensitivity");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return metadata;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CurveUnitParameterSensitivity> builder() {
      return new CurveUnitParameterSensitivity.Builder();
    }

    @Override
    public Class<? extends CurveUnitParameterSensitivity> beanType() {
      return CurveUnitParameterSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code metadata} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveMetadata> metadata() {
      return metadata;
    }

    /**
     * The meta-property for the {@code sensitivity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DoubleArray> sensitivity() {
      return sensitivity;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return ((CurveUnitParameterSensitivity) bean).getMetadata();
        case 564403871:  // sensitivity
          return ((CurveUnitParameterSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code CurveUnitParameterSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<CurveUnitParameterSensitivity> {

    private CurveMetadata metadata;
    private DoubleArray sensitivity;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return metadata;
        case 564403871:  // sensitivity
          return sensitivity;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          this.metadata = (CurveMetadata) newValue;
          break;
        case 564403871:  // sensitivity
          this.sensitivity = (DoubleArray) newValue;
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
    public CurveUnitParameterSensitivity build() {
      return new CurveUnitParameterSensitivity(
          metadata,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("CurveUnitParameterSensitivity.Builder{");
      buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
