/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Default metadata for a curve.
 * <p>
 * This implementation of {@link CurveMetadata} provides the curve name and nodes.
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class DefaultCurveMetadata
    implements CurveMetadata, ImmutableBean, Serializable {

  /**
   * The curve name.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveName curveName;
  /**
   * The x-value type, providing meaning to the x-values of the curve.
   * <p>
   * This type provides meaning to the x-values. For example, the x-value might
   * represent a year fraction, as represented using {@link ValueType#YEAR_FRACTION}.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ValueType xValueType;
  /**
   * The y-value type, providing meaning to the y-values of the curve.
   * <p>
   * This type provides meaning to the y-values. For example, the y-value might
   * represent a zero rate, as represented using {@link ValueType#ZERO_RATE}.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ValueType yValueType;
  /**
   * The additional curve information.
   * <p>
   * This stores additional information for the curve.
   * <p>
   * The most common information is the {@linkplain CurveInfoType#DAY_COUNT day count}
   * and {@linkplain CurveInfoType#JACOBIAN curve calibration Jacobian}.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<CurveInfoType<?>, Object> info;
  /**
   * The metadata about the parameters.
   * <p>
   * If present, the parameter metadata will match the number of parameters on the curve.
   */
  @PropertyDefinition(get = "optional", overrideGet = true, type = "List<>", builderType = "List<? extends ParameterMetadata>")
  private final ImmutableList<ParameterMetadata> parameterMetadata;

  //-------------------------------------------------------------------------
  /**
   * Creates the metadata.
   * <p>
   * No information will be available for the x-values, y-values or parameters.
   * 
   * @param name  the curve name
   * @return the metadata
   */
  public static DefaultCurveMetadata of(String name) {
    return of(CurveName.of(name));
  }

  /**
   * Creates the metadata.
   * <p>
   * No information will be available for the x-values, y-values or parameters.
   * 
   * @param name  the curve name
   * @return the metadata
   */
  public static DefaultCurveMetadata of(CurveName name) {
    return new DefaultCurveMetadata(name, ValueType.UNKNOWN, ValueType.UNKNOWN, ImmutableMap.of(), null);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * 
   * @return the builder, not null
   */
  public static DefaultCurveMetadataBuilder builder() {
    return new DefaultCurveMetadataBuilder();
  }

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.xValueType = ValueType.UNKNOWN;
    builder.yValueType = ValueType.UNKNOWN;
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> T getInfo(CurveInfoType<T> type) {
    // overridden for performance
    @SuppressWarnings("unchecked")
    T value = (T) info.get(type);
    if (value == null) {
      throw new IllegalArgumentException(Messages.format("Curve info not found for type '{}'", type));
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> findInfo(CurveInfoType<T> type) {
    return Optional.ofNullable((T) info.get(type));
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> DefaultCurveMetadata withInfo(CurveInfoType<T> type, T value) {
    return toBuilder().addInfo(type, value).build();
  }

  @Override
  public DefaultCurveMetadata withParameterMetadata(List<? extends ParameterMetadata> parameterMetadata) {
    if (parameterMetadata == null) {
      return this.parameterMetadata != null ? toBuilder().clearParameterMetadata().build() : this;
    }
    return toBuilder().parameterMetadata(parameterMetadata).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a mutable builder initialized with the state of this bean.
   * 
   * @return the mutable builder, not null
   */
  public DefaultCurveMetadataBuilder toBuilder() {
    return new DefaultCurveMetadataBuilder(this);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DefaultCurveMetadata}.
   * @return the meta-bean, not null
   */
  public static DefaultCurveMetadata.Meta meta() {
    return DefaultCurveMetadata.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(DefaultCurveMetadata.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param curveName  the value of the property, not null
   * @param xValueType  the value of the property, not null
   * @param yValueType  the value of the property, not null
   * @param info  the value of the property, not null
   * @param parameterMetadata  the value of the property
   */
  DefaultCurveMetadata(
      CurveName curveName,
      ValueType xValueType,
      ValueType yValueType,
      Map<CurveInfoType<?>, Object> info,
      List<? extends ParameterMetadata> parameterMetadata) {
    JodaBeanUtils.notNull(curveName, "curveName");
    JodaBeanUtils.notNull(xValueType, "xValueType");
    JodaBeanUtils.notNull(yValueType, "yValueType");
    JodaBeanUtils.notNull(info, "info");
    this.curveName = curveName;
    this.xValueType = xValueType;
    this.yValueType = yValueType;
    this.info = ImmutableMap.copyOf(info);
    this.parameterMetadata = (parameterMetadata != null ? ImmutableList.copyOf(parameterMetadata) : null);
  }

  @Override
  public DefaultCurveMetadata.Meta metaBean() {
    return DefaultCurveMetadata.Meta.INSTANCE;
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
   * Gets the curve name.
   * @return the value of the property, not null
   */
  @Override
  public CurveName getCurveName() {
    return curveName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the x-value type, providing meaning to the x-values of the curve.
   * <p>
   * This type provides meaning to the x-values. For example, the x-value might
   * represent a year fraction, as represented using {@link ValueType#YEAR_FRACTION}.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   * @return the value of the property, not null
   */
  @Override
  public ValueType getXValueType() {
    return xValueType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the y-value type, providing meaning to the y-values of the curve.
   * <p>
   * This type provides meaning to the y-values. For example, the y-value might
   * represent a zero rate, as represented using {@link ValueType#ZERO_RATE}.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   * @return the value of the property, not null
   */
  @Override
  public ValueType getYValueType() {
    return yValueType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional curve information.
   * <p>
   * This stores additional information for the curve.
   * <p>
   * The most common information is the {@linkplain CurveInfoType#DAY_COUNT day count}
   * and {@linkplain CurveInfoType#JACOBIAN curve calibration Jacobian}.
   * @return the value of the property, not null
   */
  public ImmutableMap<CurveInfoType<?>, Object> getInfo() {
    return info;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the metadata about the parameters.
   * <p>
   * If present, the parameter metadata will match the number of parameters on the curve.
   * @return the optional value of the property, not null
   */
  @Override
  public Optional<List<ParameterMetadata>> getParameterMetadata() {
    return Optional.ofNullable(parameterMetadata);
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DefaultCurveMetadata other = (DefaultCurveMetadata) obj;
      return JodaBeanUtils.equal(curveName, other.curveName) &&
          JodaBeanUtils.equal(xValueType, other.xValueType) &&
          JodaBeanUtils.equal(yValueType, other.yValueType) &&
          JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(parameterMetadata, other.parameterMetadata);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(curveName);
    hash = hash * 31 + JodaBeanUtils.hashCode(xValueType);
    hash = hash * 31 + JodaBeanUtils.hashCode(yValueType);
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameterMetadata);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("DefaultCurveMetadata{");
    buf.append("curveName").append('=').append(curveName).append(',').append(' ');
    buf.append("xValueType").append('=').append(xValueType).append(',').append(' ');
    buf.append("yValueType").append('=').append(yValueType).append(',').append(' ');
    buf.append("info").append('=').append(info).append(',').append(' ');
    buf.append("parameterMetadata").append('=').append(JodaBeanUtils.toString(parameterMetadata));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DefaultCurveMetadata}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code curveName} property.
     */
    private final MetaProperty<CurveName> curveName = DirectMetaProperty.ofImmutable(
        this, "curveName", DefaultCurveMetadata.class, CurveName.class);
    /**
     * The meta-property for the {@code xValueType} property.
     */
    private final MetaProperty<ValueType> xValueType = DirectMetaProperty.ofImmutable(
        this, "xValueType", DefaultCurveMetadata.class, ValueType.class);
    /**
     * The meta-property for the {@code yValueType} property.
     */
    private final MetaProperty<ValueType> yValueType = DirectMetaProperty.ofImmutable(
        this, "yValueType", DefaultCurveMetadata.class, ValueType.class);
    /**
     * The meta-property for the {@code info} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<CurveInfoType<?>, Object>> info = DirectMetaProperty.ofImmutable(
        this, "info", DefaultCurveMetadata.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code parameterMetadata} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<ParameterMetadata>> parameterMetadata = DirectMetaProperty.ofImmutable(
        this, "parameterMetadata", DefaultCurveMetadata.class, (Class) List.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "curveName",
        "xValueType",
        "yValueType",
        "info",
        "parameterMetadata");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 771153946:  // curveName
          return curveName;
        case -868509005:  // xValueType
          return xValueType;
        case -1065022510:  // yValueType
          return yValueType;
        case 3237038:  // info
          return info;
        case -1169106440:  // parameterMetadata
          return parameterMetadata;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends DefaultCurveMetadata> builder() {
      return new DefaultCurveMetadata.Builder();
    }

    @Override
    public Class<? extends DefaultCurveMetadata> beanType() {
      return DefaultCurveMetadata.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code curveName} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveName> curveName() {
      return curveName;
    }

    /**
     * The meta-property for the {@code xValueType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueType> xValueType() {
      return xValueType;
    }

    /**
     * The meta-property for the {@code yValueType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueType> yValueType() {
      return yValueType;
    }

    /**
     * The meta-property for the {@code info} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<CurveInfoType<?>, Object>> info() {
      return info;
    }

    /**
     * The meta-property for the {@code parameterMetadata} property.
     * @return the meta-property, not null
     */
    public MetaProperty<List<ParameterMetadata>> parameterMetadata() {
      return parameterMetadata;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 771153946:  // curveName
          return ((DefaultCurveMetadata) bean).getCurveName();
        case -868509005:  // xValueType
          return ((DefaultCurveMetadata) bean).getXValueType();
        case -1065022510:  // yValueType
          return ((DefaultCurveMetadata) bean).getYValueType();
        case 3237038:  // info
          return ((DefaultCurveMetadata) bean).getInfo();
        case -1169106440:  // parameterMetadata
          return ((DefaultCurveMetadata) bean).parameterMetadata;
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
   * The bean-builder for {@code DefaultCurveMetadata}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<DefaultCurveMetadata> {

    private CurveName curveName;
    private ValueType xValueType;
    private ValueType yValueType;
    private Map<CurveInfoType<?>, Object> info = ImmutableMap.of();
    private List<? extends ParameterMetadata> parameterMetadata;

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
      applyDefaults(this);
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 771153946:  // curveName
          return curveName;
        case -868509005:  // xValueType
          return xValueType;
        case -1065022510:  // yValueType
          return yValueType;
        case 3237038:  // info
          return info;
        case -1169106440:  // parameterMetadata
          return parameterMetadata;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 771153946:  // curveName
          this.curveName = (CurveName) newValue;
          break;
        case -868509005:  // xValueType
          this.xValueType = (ValueType) newValue;
          break;
        case -1065022510:  // yValueType
          this.yValueType = (ValueType) newValue;
          break;
        case 3237038:  // info
          this.info = (Map<CurveInfoType<?>, Object>) newValue;
          break;
        case -1169106440:  // parameterMetadata
          this.parameterMetadata = (List<? extends ParameterMetadata>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public DefaultCurveMetadata build() {
      return new DefaultCurveMetadata(
          curveName,
          xValueType,
          yValueType,
          info,
          parameterMetadata);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("DefaultCurveMetadata.Builder{");
      buf.append("curveName").append('=').append(JodaBeanUtils.toString(curveName)).append(',').append(' ');
      buf.append("xValueType").append('=').append(JodaBeanUtils.toString(xValueType)).append(',').append(' ');
      buf.append("yValueType").append('=').append(JodaBeanUtils.toString(yValueType)).append(',').append(' ');
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("parameterMetadata").append('=').append(JodaBeanUtils.toString(parameterMetadata));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
