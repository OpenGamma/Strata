/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

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

/**
 * Parameter sensitivity for a single surface.
* <p>
 * Surface parameter sensitivity is the sensitivity of a value to the parameters of a surface used
 * to determine the value.
 * <p>
 * This class represents sensitivity to a surface curve. The sensitivity is expressed as an array
 * of values, one for each parameter used to create the surface.
 */
@BeanDefinition(builderScope = "private")
public final class SurfaceUnitParameterSensitivity
    implements ImmutableBean {

  /**
   * The surface metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If present, the size of the parameter metadata list will match the number of parameters of this surface.
   */
  @PropertyDefinition(validate = "notNull")
  private final SurfaceMetadata metadata;
  /**
   * The parameter sensitivity values.
   * There will be one sensitivity value for each parameter of the surface.
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the surface metadata and sensitivity.
   * 
   * @param metadata  the surface metadata
   * @param sensitivity  the sensitivity values, one for each node in the surface
   * @return the sensitivity object
   */
  public static SurfaceUnitParameterSensitivity of(
      SurfaceMetadata metadata,
      DoubleArray sensitivity) {

    return new SurfaceUnitParameterSensitivity(metadata, sensitivity);
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
   * Gets the surface name.
   * 
   * @return the surface name
   */
  public SurfaceName getSurfaceName() {
    return metadata.getSurfaceName();
  }

  /**
   * Gets the number of parameters in the surface.
   * <p>
   * This returns the number of parameters in the surface.
   * 
   * @return the number of parameters
   */
  public int getParameterCount() {
    return sensitivity.size();
  }

  /**
   * Compares two sensitivity objects, excluding the parameter sensitivity values.
   * 
   * @param other  the other sensitivity object
   * @return positive if greater, zero if equal, negative if less
   */
  public int compareExcludingSensitivity(SurfaceUnitParameterSensitivity other) {
    return ComparisonChain.start()
        .compare(metadata.getSurfaceName(), other.metadata.getSurfaceName())
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
  public SurfaceCurrencyParameterSensitivity multipliedBy(Currency currency, double amount) {
    return SurfaceCurrencyParameterSensitivity.of(metadata, currency, sensitivity.multipliedBy(amount));
  }

  /**
   * Returns an instance with the sensitivity values multiplied by the specified factor.
   * <p>
   * Each value in the sensitivity array will be multiplied by the factor.
   * 
   * @param factor  the multiplicative factor
   * @return an instance based on this one, with each sensitivity multiplied by the factor
   */
  public SurfaceUnitParameterSensitivity multipliedBy(double factor) {
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
  public SurfaceUnitParameterSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new SurfaceUnitParameterSensitivity(metadata, sensitivity.map(operator));
  }

  /**
   * Returns an instance with the new parameter sensitivity values.
   * <p>
   * The implementation will clone the input array.
   * 
   * @param sensitivity  the new sensitivity values
   * @return an instance based on this one, with the specified sensitivity values
   */
  public SurfaceUnitParameterSensitivity withSensitivity(DoubleArray sensitivity) {
    if (sensitivity.size() != this.sensitivity.size()) {
      throw new IllegalArgumentException("Length of sensitivity must match parameter count");
    }
    return new SurfaceUnitParameterSensitivity(metadata, sensitivity);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SurfaceUnitParameterSensitivity}.
   * @return the meta-bean, not null
   */
  public static SurfaceUnitParameterSensitivity.Meta meta() {
    return SurfaceUnitParameterSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SurfaceUnitParameterSensitivity.Meta.INSTANCE);
  }

  private SurfaceUnitParameterSensitivity(
      SurfaceMetadata metadata,
      DoubleArray sensitivity) {
    JodaBeanUtils.notNull(metadata, "metadata");
    JodaBeanUtils.notNull(sensitivity, "sensitivity");
    this.metadata = metadata;
    this.sensitivity = sensitivity;
    validate();
  }

  @Override
  public SurfaceUnitParameterSensitivity.Meta metaBean() {
    return SurfaceUnitParameterSensitivity.Meta.INSTANCE;
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
   * Gets the surface metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * If present, the size of the parameter metadata list will match the number of parameters of this surface.
   * @return the value of the property, not null
   */
  public SurfaceMetadata getMetadata() {
    return metadata;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the parameter sensitivity values.
   * There will be one sensitivity value for each parameter of the surface.
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
      SurfaceUnitParameterSensitivity other = (SurfaceUnitParameterSensitivity) obj;
      return JodaBeanUtils.equal(metadata, other.metadata) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(metadata);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("SurfaceUnitParameterSensitivity{");
    buf.append("metadata").append('=').append(metadata).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SurfaceUnitParameterSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code metadata} property.
     */
    private final MetaProperty<SurfaceMetadata> metadata = DirectMetaProperty.ofImmutable(
        this, "metadata", SurfaceUnitParameterSensitivity.class, SurfaceMetadata.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<DoubleArray> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", SurfaceUnitParameterSensitivity.class, DoubleArray.class);
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
    public BeanBuilder<? extends SurfaceUnitParameterSensitivity> builder() {
      return new SurfaceUnitParameterSensitivity.Builder();
    }

    @Override
    public Class<? extends SurfaceUnitParameterSensitivity> beanType() {
      return SurfaceUnitParameterSensitivity.class;
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
    public MetaProperty<SurfaceMetadata> metadata() {
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
          return ((SurfaceUnitParameterSensitivity) bean).getMetadata();
        case 564403871:  // sensitivity
          return ((SurfaceUnitParameterSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code SurfaceUnitParameterSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<SurfaceUnitParameterSensitivity> {

    private SurfaceMetadata metadata;
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
          this.metadata = (SurfaceMetadata) newValue;
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
    public SurfaceUnitParameterSensitivity build() {
      return new SurfaceUnitParameterSensitivity(
          metadata,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("SurfaceUnitParameterSensitivity.Builder{");
      buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
