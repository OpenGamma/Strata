/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

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

import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.function.DoubleTernaryOperator;

/**
 * A surface based on a single constant value.
 * <p>
 * This class defines a surface in terms of a single parameter, the constant value.
 * When queried, {@link #zValue(double, double)} always returns the constant value.
 * <p>
 * The {@link #getXValues()} method returns a single x-value of 0.
 * The {@link #getYValues()} method returns a single y-value of 0.
 * The {@link #getZValues()} method returns a single z-value of the constant.
 * The sensitivity is 1.
 */
@BeanDefinition(builderScope = "private")
public final class ConstantNodalSurface
    implements NodalSurface, ImmutableBean, Serializable {

  /**
   * X-values and y-values do not vary.
   */
  private static final DoubleArray VALUES = DoubleArray.of(0d);
  /**
   * Sensitivity does not vary.
   */
  private static final DoubleArray SENSITIVITY = DoubleArray.of(1d);

  /**
   * The surface metadata.
   * <p>
   * The metadata will have not have parameter metadata.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SurfaceMetadata metadata;
  /**
   * The single z-value.
   */
  @PropertyDefinition(get = "private")
  private final double zValue;

  //-------------------------------------------------------------------------
  /**
   * Creates a constant surface with a specific value.
   * 
   * @param name  the surface name
   * @param zValue  the constant z-value
   * @return the surface
   */
  public static ConstantNodalSurface of(String name, double zValue) {
    return of(SurfaceName.of(name), zValue);
  }

  /**
   * Creates a constant surface with a specific value.
   * 
   * @param name  the surface name
   * @param zValue  the constant z-value
   * @return the surface
   */
  public static ConstantNodalSurface of(SurfaceName name, double zValue) {
    return new ConstantNodalSurface(DefaultSurfaceMetadata.of(name), zValue);
  }

  /**
   * Creates a constant surface with a specific value.
   * 
   * @param metadata  the surface metadata
   * @param zValue  the constant z-value
   * @return the surface
   */
  public static ConstantNodalSurface of(SurfaceMetadata metadata, double zValue) {
    return new ConstantNodalSurface(metadata, zValue);
  }

  //-------------------------------------------------------------------------
  // ensure standard constructor is invoked
  private Object readResolve() {
    return new ConstantNodalSurface(metadata, zValue);
  }

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return 1;
  }

  @Override
  public DoubleArray getXValues() {
    return VALUES;
  }

  @Override
  public DoubleArray getYValues() {
    return VALUES;
  }

  @Override
  public DoubleArray getZValues() {
    return DoubleArray.of(zValue);
  }

  //-------------------------------------------------------------------------
  @Override
  public double zValue(double x, double y) {
    return zValue;
  }

  @Override
  public SurfaceUnitParameterSensitivity zValueParameterSensitivity(double x, double y) {
    return SurfaceUnitParameterSensitivity.of(metadata, SENSITIVITY);
  }

  //-------------------------------------------------------------------------
  @Override
  public ConstantNodalSurface withMetadata(SurfaceMetadata metadata) {
    return new ConstantNodalSurface(metadata.withParameterMetadata(null), zValue);
  }

  @Override
  public ConstantNodalSurface withZValues(DoubleArray zValues) {
    ArgChecker.isTrue(zValues.size() == 1, "ZValues array must be size one");
    return new ConstantNodalSurface(metadata, zValues.get(0));
  }

  @Override
  public ConstantNodalSurface shiftedBy(DoubleTernaryOperator operator) {
    return (ConstantNodalSurface) NodalSurface.super.shiftedBy(operator);
  }

  @Override
  public ConstantNodalSurface shiftedBy(List<ValueAdjustment> adjustments) {
    return (ConstantNodalSurface) NodalSurface.super.shiftedBy(adjustments);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ConstantNodalSurface}.
   * @return the meta-bean, not null
   */
  public static ConstantNodalSurface.Meta meta() {
    return ConstantNodalSurface.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ConstantNodalSurface.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ConstantNodalSurface(
      SurfaceMetadata metadata,
      double zValue) {
    JodaBeanUtils.notNull(metadata, "metadata");
    this.metadata = metadata;
    this.zValue = zValue;
  }

  @Override
  public ConstantNodalSurface.Meta metaBean() {
    return ConstantNodalSurface.Meta.INSTANCE;
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
   * The metadata will have not have parameter metadata.
   * @return the value of the property, not null
   */
  @Override
  public SurfaceMetadata getMetadata() {
    return metadata;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the single z-value.
   * @return the value of the property
   */
  private double getZValue() {
    return zValue;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ConstantNodalSurface other = (ConstantNodalSurface) obj;
      return JodaBeanUtils.equal(metadata, other.metadata) &&
          JodaBeanUtils.equal(zValue, other.zValue);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(metadata);
    hash = hash * 31 + JodaBeanUtils.hashCode(zValue);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("ConstantNodalSurface{");
    buf.append("metadata").append('=').append(metadata).append(',').append(' ');
    buf.append("zValue").append('=').append(JodaBeanUtils.toString(zValue));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ConstantNodalSurface}.
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
        this, "metadata", ConstantNodalSurface.class, SurfaceMetadata.class);
    /**
     * The meta-property for the {@code zValue} property.
     */
    private final MetaProperty<Double> zValue = DirectMetaProperty.ofImmutable(
        this, "zValue", ConstantNodalSurface.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "metadata",
        "zValue");

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
        case -719790825:  // zValue
          return zValue;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ConstantNodalSurface> builder() {
      return new ConstantNodalSurface.Builder();
    }

    @Override
    public Class<? extends ConstantNodalSurface> beanType() {
      return ConstantNodalSurface.class;
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
     * The meta-property for the {@code zValue} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> zValue() {
      return zValue;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return ((ConstantNodalSurface) bean).getMetadata();
        case -719790825:  // zValue
          return ((ConstantNodalSurface) bean).getZValue();
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
   * The bean-builder for {@code ConstantNodalSurface}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<ConstantNodalSurface> {

    private SurfaceMetadata metadata;
    private double zValue;

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
        case -719790825:  // zValue
          return zValue;
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
        case -719790825:  // zValue
          this.zValue = (Double) newValue;
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
    public ConstantNodalSurface build() {
      return new ConstantNodalSurface(
          metadata,
          zValue);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ConstantNodalSurface.Builder{");
      buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata)).append(',').append(' ');
      buf.append("zValue").append('=').append(JodaBeanUtils.toString(zValue));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
