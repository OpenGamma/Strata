/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.PropertyDefinition;
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
 * Default metadata for a cube.
 * <p>
 * This implementation of {@link CubeMetadata} provides the cube name and nodes.
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class DefaultCubeMetadata
    implements CubeMetadata, ImmutableBean, Serializable {

  /**
   * The cube name.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CubeName cubeName;
  /**
   * The x-value type, providing meaning to the x-values of the cube.
   * <p>
   * This type provides meaning to the x-values. For example, the x-value might
   * represent a year fraction, as represented using {@link ValueType#YEAR_FRACTION}.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ValueType xValueType;
  /**
   * The y-value type, providing meaning to the y-values of the cube.
   * <p>
   * This type provides meaning to the y-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ValueType yValueType;
  /**
   * The z-value type, providing meaning to the z-values of the cube.
   * <p>
   * This type provides meaning to the z-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ValueType zValueType;
  /**
   * The w-value type, providing meaning to the w-values of the cube.
   * <p>
   * This type provides meaning to the w-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ValueType wValueType;
  /**
   * The additional cube information.
   * <p>
   * This stores additional information for the cube.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<CubeInfoType<?>, Object> info;
  /**
   * The metadata about the parameters.
   * <p>
   * If present, the parameter metadata should match the number of parameters on the cube.
   */
  @PropertyDefinition(get = "optional", overrideGet = true, type = "List<>", builderType = "List<? extends ParameterMetadata>")
  private final ImmutableList<ParameterMetadata> parameterMetadata;

  //-------------------------------------------------------------------------
  /**
   * Creates the metadata.
   * <p>
   * No information will be available for the x-values, y-values, z-values, w-values or parameters.
   *
   * @param name  the cube name
   * @return the metadata
   */
  public static DefaultCubeMetadata of(String name) {
    return of(CubeName.of(name));
  }

  /**
   * Creates the metadata.
   * <p>
   * No information will be available for the x-values, y-values, z-values or parameters.
   *
   * @param name  the cube name
   * @return the metadata
   */
  public static DefaultCubeMetadata of(CubeName name) {
    return new DefaultCubeMetadata(
        name, ValueType.UNKNOWN, ValueType.UNKNOWN, ValueType.UNKNOWN, ValueType.UNKNOWN, ImmutableMap.of(), null);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   *
   * @return the builder, not null
   */
  public static DefaultCubeMetadataBuilder builder() {
    return new DefaultCubeMetadataBuilder();
  }

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.xValueType = ValueType.UNKNOWN;
    builder.yValueType = ValueType.UNKNOWN;
    builder.zValueType = ValueType.UNKNOWN;
    builder.wValueType = ValueType.UNKNOWN;
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> T getInfo(CubeInfoType<T> type) {
    // overridden for performance
    @SuppressWarnings("unchecked")
    T value = (T) info.get(type);
    if (value == null) {
      throw new IllegalArgumentException(Messages.format("Cube info not found for type '{}'", type));
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> findInfo(CubeInfoType<T> type) {
    return Optional.ofNullable((T) info.get(type));
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> DefaultCubeMetadata withInfo(CubeInfoType<T> type, T value) {
    return toBuilder().addInfo(type, value).build();
  }

  @Override
  public DefaultCubeMetadata withParameterMetadata(List<? extends ParameterMetadata> parameterMetadata) {
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
  public DefaultCubeMetadataBuilder toBuilder() {
    return new DefaultCubeMetadataBuilder(this);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code DefaultCubeMetadata}.
   * @return the meta-bean, not null
   */
  public static DefaultCubeMetadata.Meta meta() {
    return DefaultCubeMetadata.Meta.INSTANCE;
  }

  static {
    MetaBean.register(DefaultCubeMetadata.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param cubeName  the value of the property, not null
   * @param xValueType  the value of the property, not null
   * @param yValueType  the value of the property, not null
   * @param zValueType  the value of the property, not null
   * @param wValueType  the value of the property, not null
   * @param info  the value of the property, not null
   * @param parameterMetadata  the value of the property
   */
  DefaultCubeMetadata(
      CubeName cubeName,
      ValueType xValueType,
      ValueType yValueType,
      ValueType zValueType,
      ValueType wValueType,
      Map<CubeInfoType<?>, Object> info,
      List<? extends ParameterMetadata> parameterMetadata) {
    JodaBeanUtils.notNull(cubeName, "cubeName");
    JodaBeanUtils.notNull(xValueType, "xValueType");
    JodaBeanUtils.notNull(yValueType, "yValueType");
    JodaBeanUtils.notNull(zValueType, "zValueType");
    JodaBeanUtils.notNull(wValueType, "wValueType");
    JodaBeanUtils.notNull(info, "info");
    this.cubeName = cubeName;
    this.xValueType = xValueType;
    this.yValueType = yValueType;
    this.zValueType = zValueType;
    this.wValueType = wValueType;
    this.info = ImmutableMap.copyOf(info);
    this.parameterMetadata = (parameterMetadata != null ? ImmutableList.copyOf(parameterMetadata) : null);
  }

  @Override
  public DefaultCubeMetadata.Meta metaBean() {
    return DefaultCubeMetadata.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the cube name.
   * @return the value of the property, not null
   */
  @Override
  public CubeName getCubeName() {
    return cubeName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the x-value type, providing meaning to the x-values of the cube.
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
   * Gets the y-value type, providing meaning to the y-values of the cube.
   * <p>
   * This type provides meaning to the y-values.
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
   * Gets the z-value type, providing meaning to the z-values of the cube.
   * <p>
   * This type provides meaning to the z-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   * @return the value of the property, not null
   */
  @Override
  public ValueType getZValueType() {
    return zValueType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the w-value type, providing meaning to the w-values of the cube.
   * <p>
   * This type provides meaning to the w-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   * @return the value of the property, not null
   */
  @Override
  public ValueType getWValueType() {
    return wValueType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional cube information.
   * <p>
   * This stores additional information for the cube.
   * @return the value of the property, not null
   */
  public ImmutableMap<CubeInfoType<?>, Object> getInfo() {
    return info;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the metadata about the parameters.
   * <p>
   * If present, the parameter metadata should match the number of parameters on the cube.
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
      DefaultCubeMetadata other = (DefaultCubeMetadata) obj;
      return JodaBeanUtils.equal(cubeName, other.cubeName) &&
          JodaBeanUtils.equal(xValueType, other.xValueType) &&
          JodaBeanUtils.equal(yValueType, other.yValueType) &&
          JodaBeanUtils.equal(zValueType, other.zValueType) &&
          JodaBeanUtils.equal(wValueType, other.wValueType) &&
          JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(parameterMetadata, other.parameterMetadata);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(cubeName);
    hash = hash * 31 + JodaBeanUtils.hashCode(xValueType);
    hash = hash * 31 + JodaBeanUtils.hashCode(yValueType);
    hash = hash * 31 + JodaBeanUtils.hashCode(zValueType);
    hash = hash * 31 + JodaBeanUtils.hashCode(wValueType);
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameterMetadata);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("DefaultCubeMetadata{");
    buf.append("cubeName").append('=').append(JodaBeanUtils.toString(cubeName)).append(',').append(' ');
    buf.append("xValueType").append('=').append(JodaBeanUtils.toString(xValueType)).append(',').append(' ');
    buf.append("yValueType").append('=').append(JodaBeanUtils.toString(yValueType)).append(',').append(' ');
    buf.append("zValueType").append('=').append(JodaBeanUtils.toString(zValueType)).append(',').append(' ');
    buf.append("wValueType").append('=').append(JodaBeanUtils.toString(wValueType)).append(',').append(' ');
    buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
    buf.append("parameterMetadata").append('=').append(JodaBeanUtils.toString(parameterMetadata));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DefaultCubeMetadata}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code cubeName} property.
     */
    private final MetaProperty<CubeName> cubeName = DirectMetaProperty.ofImmutable(
        this, "cubeName", DefaultCubeMetadata.class, CubeName.class);
    /**
     * The meta-property for the {@code xValueType} property.
     */
    private final MetaProperty<ValueType> xValueType = DirectMetaProperty.ofImmutable(
        this, "xValueType", DefaultCubeMetadata.class, ValueType.class);
    /**
     * The meta-property for the {@code yValueType} property.
     */
    private final MetaProperty<ValueType> yValueType = DirectMetaProperty.ofImmutable(
        this, "yValueType", DefaultCubeMetadata.class, ValueType.class);
    /**
     * The meta-property for the {@code zValueType} property.
     */
    private final MetaProperty<ValueType> zValueType = DirectMetaProperty.ofImmutable(
        this, "zValueType", DefaultCubeMetadata.class, ValueType.class);
    /**
     * The meta-property for the {@code wValueType} property.
     */
    private final MetaProperty<ValueType> wValueType = DirectMetaProperty.ofImmutable(
        this, "wValueType", DefaultCubeMetadata.class, ValueType.class);
    /**
     * The meta-property for the {@code info} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<CubeInfoType<?>, Object>> info = DirectMetaProperty.ofImmutable(
        this, "info", DefaultCubeMetadata.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code parameterMetadata} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<ParameterMetadata>> parameterMetadata = DirectMetaProperty.ofImmutable(
        this, "parameterMetadata", DefaultCubeMetadata.class, (Class) List.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "cubeName",
        "xValueType",
        "yValueType",
        "zValueType",
        "wValueType",
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
        case 104632416:  // cubeName
          return cubeName;
        case -868509005:  // xValueType
          return xValueType;
        case -1065022510:  // yValueType
          return yValueType;
        case -1261536015:  // zValueType
          return zValueType;
        case -671995500:  // wValueType
          return wValueType;
        case 3237038:  // info
          return info;
        case -1169106440:  // parameterMetadata
          return parameterMetadata;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends DefaultCubeMetadata> builder() {
      return new DefaultCubeMetadata.Builder();
    }

    @Override
    public Class<? extends DefaultCubeMetadata> beanType() {
      return DefaultCubeMetadata.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code cubeName} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CubeName> cubeName() {
      return cubeName;
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
     * The meta-property for the {@code zValueType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueType> zValueType() {
      return zValueType;
    }

    /**
     * The meta-property for the {@code wValueType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ValueType> wValueType() {
      return wValueType;
    }

    /**
     * The meta-property for the {@code info} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<CubeInfoType<?>, Object>> info() {
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
        case 104632416:  // cubeName
          return ((DefaultCubeMetadata) bean).getCubeName();
        case -868509005:  // xValueType
          return ((DefaultCubeMetadata) bean).getXValueType();
        case -1065022510:  // yValueType
          return ((DefaultCubeMetadata) bean).getYValueType();
        case -1261536015:  // zValueType
          return ((DefaultCubeMetadata) bean).getZValueType();
        case -671995500:  // wValueType
          return ((DefaultCubeMetadata) bean).getWValueType();
        case 3237038:  // info
          return ((DefaultCubeMetadata) bean).getInfo();
        case -1169106440:  // parameterMetadata
          return ((DefaultCubeMetadata) bean).parameterMetadata;
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
   * The bean-builder for {@code DefaultCubeMetadata}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<DefaultCubeMetadata> {

    private CubeName cubeName;
    private ValueType xValueType;
    private ValueType yValueType;
    private ValueType zValueType;
    private ValueType wValueType;
    private Map<CubeInfoType<?>, Object> info = ImmutableMap.of();
    private List<? extends ParameterMetadata> parameterMetadata;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 104632416:  // cubeName
          return cubeName;
        case -868509005:  // xValueType
          return xValueType;
        case -1065022510:  // yValueType
          return yValueType;
        case -1261536015:  // zValueType
          return zValueType;
        case -671995500:  // wValueType
          return wValueType;
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
        case 104632416:  // cubeName
          this.cubeName = (CubeName) newValue;
          break;
        case -868509005:  // xValueType
          this.xValueType = (ValueType) newValue;
          break;
        case -1065022510:  // yValueType
          this.yValueType = (ValueType) newValue;
          break;
        case -1261536015:  // zValueType
          this.zValueType = (ValueType) newValue;
          break;
        case -671995500:  // wValueType
          this.wValueType = (ValueType) newValue;
          break;
        case 3237038:  // info
          this.info = (Map<CubeInfoType<?>, Object>) newValue;
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
    public DefaultCubeMetadata build() {
      return new DefaultCubeMetadata(
          cubeName,
          xValueType,
          yValueType,
          zValueType,
          wValueType,
          info,
          parameterMetadata);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("DefaultCubeMetadata.Builder{");
      buf.append("cubeName").append('=').append(JodaBeanUtils.toString(cubeName)).append(',').append(' ');
      buf.append("xValueType").append('=').append(JodaBeanUtils.toString(xValueType)).append(',').append(' ');
      buf.append("yValueType").append('=').append(JodaBeanUtils.toString(yValueType)).append(',').append(' ');
      buf.append("zValueType").append('=').append(JodaBeanUtils.toString(zValueType)).append(',').append(' ');
      buf.append("wValueType").append('=').append(JodaBeanUtils.toString(wValueType)).append(',').append(' ');
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("parameterMetadata").append('=').append(JodaBeanUtils.toString(parameterMetadata));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
