/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * The deformed surface.
 * <p>
 * The deformation is applied to {@code Surface}, and defined in terms of {@code Function}, which returns z-value and 
 * sensitivities to the nodes of the original surface.
 * <p>
 * Typical application of this class is to represent a surface constructed via model calibration to interpolated 
 * market data, where the market data points and interpolation are stored in {@code originalSurface}, 
 * and {@code deformationFunction} defines the constructed surface.
 */
@BeanDefinition
public final class DeformedSurface
    implements Surface, ImmutableBean, Serializable {

  /**
   * The surface metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SurfaceMetadata metadata;
  /**
   * The original surface.
   * <p>
   * The underlying surface which receives the deformation defined by {@code deformationFunction}.
   */
  @PropertyDefinition(validate = "notNull")
  private final Surface originalSurface;
  /**
   * The deformation function.
   * <p>
   * The deformation to the original surface is define by this function.
   * The function takes {@code DoublesPair} of x-value and y-value, then returns {@code ValueDerivatives} 
   * which contains z-value for the specified x,y values, and node sensitivities to the original surface.
   */
  @PropertyDefinition(validate = "notNull")
  private final Function<DoublesPair, ValueDerivatives> deformationFunction;

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return originalSurface.getParameterCount();
  }

  @Override
  public DeformedSurface withMetadata(SurfaceMetadata metadata) {
    return new DeformedSurface(metadata, originalSurface, deformationFunction);
  }

  @Override
  public double getParameter(int parameterIndex) {
    return originalSurface.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return originalSurface.getParameterMetadata(parameterIndex);
  }

  @Override
  public Surface withParameter(int parameterIndex, double newValue) {
    throw new IllegalArgumentException("deformationFunction must be redefined with the new value");
  }

  //-------------------------------------------------------------------------
  @Override
  public double zValue(double x, double y) {
    return deformationFunction.apply(DoublesPair.of(x, y)).getValue();
  }

  @Override
  public UnitParameterSensitivity zValueParameterSensitivity(double x, double y) {
    return getMetadata().getParameterMetadata().isPresent() ?
        UnitParameterSensitivity.of(
            getMetadata().getSurfaceName(),
            getMetadata().getParameterMetadata().get(),
            deformationFunction.apply(DoublesPair.of(x, y)).getDerivatives()) :
        UnitParameterSensitivity.of(
            getMetadata().getSurfaceName(),
            deformationFunction.apply(DoublesPair.of(x, y)).getDerivatives());
  }

  //-------------------------------------------------------------------------
  @Override
  public ValueDerivatives firstPartialDerivatives(double x, double y) {
    throw new UnsupportedOperationException("First partial derivatives not supported for deformed surfaces");
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance.
   * 
   * @param metadata  the surface metadata
   * @param originalSurface  the original surface
   * @param deformationFunction  the deformation function
   * @return the surface
   */
  public static DeformedSurface of(
      SurfaceMetadata metadata,
      Surface originalSurface,
      Function<DoublesPair, ValueDerivatives> deformationFunction) {

    return DeformedSurface.builder()
        .metadata(metadata)
        .originalSurface(originalSurface)
        .deformationFunction(deformationFunction)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code DeformedSurface}.
   * @return the meta-bean, not null
   */
  public static DeformedSurface.Meta meta() {
    return DeformedSurface.Meta.INSTANCE;
  }

  static {
    MetaBean.register(DeformedSurface.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static DeformedSurface.Builder builder() {
    return new DeformedSurface.Builder();
  }

  private DeformedSurface(
      SurfaceMetadata metadata,
      Surface originalSurface,
      Function<DoublesPair, ValueDerivatives> deformationFunction) {
    JodaBeanUtils.notNull(metadata, "metadata");
    JodaBeanUtils.notNull(originalSurface, "originalSurface");
    JodaBeanUtils.notNull(deformationFunction, "deformationFunction");
    this.metadata = metadata;
    this.originalSurface = originalSurface;
    this.deformationFunction = deformationFunction;
  }

  @Override
  public DeformedSurface.Meta metaBean() {
    return DeformedSurface.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the surface metadata.
   * <p>
   * The metadata includes an optional list of parameter metadata.
   * @return the value of the property, not null
   */
  @Override
  public SurfaceMetadata getMetadata() {
    return metadata;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the original surface.
   * <p>
   * The underlying surface which receives the deformation defined by {@code deformationFunction}.
   * @return the value of the property, not null
   */
  public Surface getOriginalSurface() {
    return originalSurface;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the deformation function.
   * <p>
   * The deformation to the original surface is define by this function.
   * The function takes {@code DoublesPair} of x-value and y-value, then returns {@code ValueDerivatives}
   * which contains z-value for the specified x,y values, and node sensitivities to the original surface.
   * @return the value of the property, not null
   */
  public Function<DoublesPair, ValueDerivatives> getDeformationFunction() {
    return deformationFunction;
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
      DeformedSurface other = (DeformedSurface) obj;
      return JodaBeanUtils.equal(metadata, other.metadata) &&
          JodaBeanUtils.equal(originalSurface, other.originalSurface) &&
          JodaBeanUtils.equal(deformationFunction, other.deformationFunction);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(metadata);
    hash = hash * 31 + JodaBeanUtils.hashCode(originalSurface);
    hash = hash * 31 + JodaBeanUtils.hashCode(deformationFunction);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("DeformedSurface{");
    buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata)).append(',').append(' ');
    buf.append("originalSurface").append('=').append(JodaBeanUtils.toString(originalSurface)).append(',').append(' ');
    buf.append("deformationFunction").append('=').append(JodaBeanUtils.toString(deformationFunction));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DeformedSurface}.
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
        this, "metadata", DeformedSurface.class, SurfaceMetadata.class);
    /**
     * The meta-property for the {@code originalSurface} property.
     */
    private final MetaProperty<Surface> originalSurface = DirectMetaProperty.ofImmutable(
        this, "originalSurface", DeformedSurface.class, Surface.class);
    /**
     * The meta-property for the {@code deformationFunction} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Function<DoublesPair, ValueDerivatives>> deformationFunction = DirectMetaProperty.ofImmutable(
        this, "deformationFunction", DeformedSurface.class, (Class) Function.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "metadata",
        "originalSurface",
        "deformationFunction");

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
        case 1982430620:  // originalSurface
          return originalSurface;
        case -360086200:  // deformationFunction
          return deformationFunction;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public DeformedSurface.Builder builder() {
      return new DeformedSurface.Builder();
    }

    @Override
    public Class<? extends DeformedSurface> beanType() {
      return DeformedSurface.class;
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
     * The meta-property for the {@code originalSurface} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Surface> originalSurface() {
      return originalSurface;
    }

    /**
     * The meta-property for the {@code deformationFunction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Function<DoublesPair, ValueDerivatives>> deformationFunction() {
      return deformationFunction;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return ((DeformedSurface) bean).getMetadata();
        case 1982430620:  // originalSurface
          return ((DeformedSurface) bean).getOriginalSurface();
        case -360086200:  // deformationFunction
          return ((DeformedSurface) bean).getDeformationFunction();
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
   * The bean-builder for {@code DeformedSurface}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<DeformedSurface> {

    private SurfaceMetadata metadata;
    private Surface originalSurface;
    private Function<DoublesPair, ValueDerivatives> deformationFunction;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(DeformedSurface beanToCopy) {
      this.metadata = beanToCopy.getMetadata();
      this.originalSurface = beanToCopy.getOriginalSurface();
      this.deformationFunction = beanToCopy.getDeformationFunction();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          return metadata;
        case 1982430620:  // originalSurface
          return originalSurface;
        case -360086200:  // deformationFunction
          return deformationFunction;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -450004177:  // metadata
          this.metadata = (SurfaceMetadata) newValue;
          break;
        case 1982430620:  // originalSurface
          this.originalSurface = (Surface) newValue;
          break;
        case -360086200:  // deformationFunction
          this.deformationFunction = (Function<DoublesPair, ValueDerivatives>) newValue;
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
    public DeformedSurface build() {
      return new DeformedSurface(
          metadata,
          originalSurface,
          deformationFunction);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the surface metadata.
     * <p>
     * The metadata includes an optional list of parameter metadata.
     * @param metadata  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder metadata(SurfaceMetadata metadata) {
      JodaBeanUtils.notNull(metadata, "metadata");
      this.metadata = metadata;
      return this;
    }

    /**
     * Sets the original surface.
     * <p>
     * The underlying surface which receives the deformation defined by {@code deformationFunction}.
     * @param originalSurface  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder originalSurface(Surface originalSurface) {
      JodaBeanUtils.notNull(originalSurface, "originalSurface");
      this.originalSurface = originalSurface;
      return this;
    }

    /**
     * Sets the deformation function.
     * <p>
     * The deformation to the original surface is define by this function.
     * The function takes {@code DoublesPair} of x-value and y-value, then returns {@code ValueDerivatives}
     * which contains z-value for the specified x,y values, and node sensitivities to the original surface.
     * @param deformationFunction  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder deformationFunction(Function<DoublesPair, ValueDerivatives> deformationFunction) {
      JodaBeanUtils.notNull(deformationFunction, "deformationFunction");
      this.deformationFunction = deformationFunction;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("DeformedSurface.Builder{");
      buf.append("metadata").append('=').append(JodaBeanUtils.toString(metadata)).append(',').append(' ');
      buf.append("originalSurface").append('=').append(JodaBeanUtils.toString(originalSurface)).append(',').append(' ');
      buf.append("deformationFunction").append('=').append(JodaBeanUtils.toString(deformationFunction));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
