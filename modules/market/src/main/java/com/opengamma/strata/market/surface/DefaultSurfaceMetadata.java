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
 * Default metadata for a surface.
 * <p>
 * This implementation of {@link SurfaceMetadata} provides the surface name and nodes.
 */
@BeanDefinition(builderScope = "private", constructorScope = "package")
public final class DefaultSurfaceMetadata
    implements SurfaceMetadata, ImmutableBean, Serializable {

  /**
   * The surface name.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SurfaceName surfaceName;
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
   * This type provides meaning to the y-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ValueType yValueType;
  /**
   * The x-value type, providing meaning to the z-values of the curve.
   * <p>
   * This type provides meaning to the z-values.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ValueType zValueType;
  /**
   * The additional surface information.
   * <p>
   * This stores additional information for the surface.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<SurfaceInfoType<?>, Object> info;
  /**
   * The metadata about the parameters.
   * <p>
   * If present, the parameter metadata should match the number of parameters on the surface.
   */
  @PropertyDefinition(get = "optional", overrideGet = true, type = "List<>", builderType = "List<? extends ParameterMetadata>")
  private final ImmutableList<ParameterMetadata> parameterMetadata;

  //-------------------------------------------------------------------------
  /**
   * Creates the metadata.
   * <p>
   * No information will be available for the x-values, y-values, z-values or parameters.
   * 
   * @param name  the surface name
   * @return the metadata
   */
  public static DefaultSurfaceMetadata of(String name) {
    return of(SurfaceName.of(name));
  }

  /**
   * Creates the metadata.
   * <p>
   * No information will be available for the x-values, y-values, z-values or parameters.
   * 
   * @param name  the surface name
   * @return the metadata
   */
  public static DefaultSurfaceMetadata of(SurfaceName name) {
    return new DefaultSurfaceMetadata(
        name, ValueType.UNKNOWN, ValueType.UNKNOWN, ValueType.UNKNOWN, ImmutableMap.of(), null);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * 
   * @return the builder, not null
   */
  public static DefaultSurfaceMetadataBuilder builder() {
    return new DefaultSurfaceMetadataBuilder();
  }

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.xValueType = ValueType.UNKNOWN;
    builder.yValueType = ValueType.UNKNOWN;
    builder.zValueType = ValueType.UNKNOWN;
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> T getInfo(SurfaceInfoType<T> type) {
    // overridden for performance
    @SuppressWarnings("unchecked")
    T value = (T) info.get(type);
    if (value == null) {
      throw new IllegalArgumentException(Messages.format("Surface info not found for type '{}'", type));
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> findInfo(SurfaceInfoType<T> type) {
    return Optional.ofNullable((T) info.get(type));
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> DefaultSurfaceMetadata withInfo(SurfaceInfoType<T> type, T value) {
    return toBuilder().addInfo(type, value).build();
  }

  @Override
  public DefaultSurfaceMetadata withParameterMetadata(List<? extends ParameterMetadata> parameterMetadata) {
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
  public DefaultSurfaceMetadataBuilder toBuilder() {
    return new DefaultSurfaceMetadataBuilder(this);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DefaultSurfaceMetadata}.
   * @return the meta-bean, not null
   */
  public static DefaultSurfaceMetadata.Meta meta() {
    return DefaultSurfaceMetadata.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(DefaultSurfaceMetadata.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an instance.
   * @param surfaceName  the value of the property, not null
   * @param xValueType  the value of the property, not null
   * @param yValueType  the value of the property, not null
   * @param zValueType  the value of the property, not null
   * @param info  the value of the property, not null
   * @param parameterMetadata  the value of the property
   */
  DefaultSurfaceMetadata(
      SurfaceName surfaceName,
      ValueType xValueType,
      ValueType yValueType,
      ValueType zValueType,
      Map<SurfaceInfoType<?>, Object> info,
      List<? extends ParameterMetadata> parameterMetadata) {
    JodaBeanUtils.notNull(surfaceName, "surfaceName");
    JodaBeanUtils.notNull(xValueType, "xValueType");
    JodaBeanUtils.notNull(yValueType, "yValueType");
    JodaBeanUtils.notNull(zValueType, "zValueType");
    JodaBeanUtils.notNull(info, "info");
    this.surfaceName = surfaceName;
    this.xValueType = xValueType;
    this.yValueType = yValueType;
    this.zValueType = zValueType;
    this.info = ImmutableMap.copyOf(info);
    this.parameterMetadata = (parameterMetadata != null ? ImmutableList.copyOf(parameterMetadata) : null);
  }

  @Override
  public DefaultSurfaceMetadata.Meta metaBean() {
    return DefaultSurfaceMetadata.Meta.INSTANCE;
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
   * Gets the surface name.
   * @return the value of the property, not null
   */
  @Override
  public SurfaceName getSurfaceName() {
    return surfaceName;
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
   * Gets the x-value type, providing meaning to the z-values of the curve.
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
   * Gets the additional surface information.
   * <p>
   * This stores additional information for the surface.
   * @return the value of the property, not null
   */
  public ImmutableMap<SurfaceInfoType<?>, Object> getInfo() {
    return info;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the metadata about the parameters.
   * <p>
   * If present, the parameter metadata should match the number of parameters on the surface.
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
      DefaultSurfaceMetadata other = (DefaultSurfaceMetadata) obj;
      return JodaBeanUtils.equal(surfaceName, other.surfaceName) &&
          JodaBeanUtils.equal(xValueType, other.xValueType) &&
          JodaBeanUtils.equal(yValueType, other.yValueType) &&
          JodaBeanUtils.equal(zValueType, other.zValueType) &&
          JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(parameterMetadata, other.parameterMetadata);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(surfaceName);
    hash = hash * 31 + JodaBeanUtils.hashCode(xValueType);
    hash = hash * 31 + JodaBeanUtils.hashCode(yValueType);
    hash = hash * 31 + JodaBeanUtils.hashCode(zValueType);
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameterMetadata);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("DefaultSurfaceMetadata{");
    buf.append("surfaceName").append('=').append(surfaceName).append(',').append(' ');
    buf.append("xValueType").append('=').append(xValueType).append(',').append(' ');
    buf.append("yValueType").append('=').append(yValueType).append(',').append(' ');
    buf.append("zValueType").append('=').append(zValueType).append(',').append(' ');
    buf.append("info").append('=').append(info).append(',').append(' ');
    buf.append("parameterMetadata").append('=').append(JodaBeanUtils.toString(parameterMetadata));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DefaultSurfaceMetadata}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code surfaceName} property.
     */
    private final MetaProperty<SurfaceName> surfaceName = DirectMetaProperty.ofImmutable(
        this, "surfaceName", DefaultSurfaceMetadata.class, SurfaceName.class);
    /**
     * The meta-property for the {@code xValueType} property.
     */
    private final MetaProperty<ValueType> xValueType = DirectMetaProperty.ofImmutable(
        this, "xValueType", DefaultSurfaceMetadata.class, ValueType.class);
    /**
     * The meta-property for the {@code yValueType} property.
     */
    private final MetaProperty<ValueType> yValueType = DirectMetaProperty.ofImmutable(
        this, "yValueType", DefaultSurfaceMetadata.class, ValueType.class);
    /**
     * The meta-property for the {@code zValueType} property.
     */
    private final MetaProperty<ValueType> zValueType = DirectMetaProperty.ofImmutable(
        this, "zValueType", DefaultSurfaceMetadata.class, ValueType.class);
    /**
     * The meta-property for the {@code info} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<SurfaceInfoType<?>, Object>> info = DirectMetaProperty.ofImmutable(
        this, "info", DefaultSurfaceMetadata.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code parameterMetadata} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<ParameterMetadata>> parameterMetadata = DirectMetaProperty.ofImmutable(
        this, "parameterMetadata", DefaultSurfaceMetadata.class, (Class) List.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "surfaceName",
        "xValueType",
        "yValueType",
        "zValueType",
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
        case -1403077416:  // surfaceName
          return surfaceName;
        case -868509005:  // xValueType
          return xValueType;
        case -1065022510:  // yValueType
          return yValueType;
        case -1261536015:  // zValueType
          return zValueType;
        case 3237038:  // info
          return info;
        case -1169106440:  // parameterMetadata
          return parameterMetadata;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends DefaultSurfaceMetadata> builder() {
      return new DefaultSurfaceMetadata.Builder();
    }

    @Override
    public Class<? extends DefaultSurfaceMetadata> beanType() {
      return DefaultSurfaceMetadata.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code surfaceName} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SurfaceName> surfaceName() {
      return surfaceName;
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
     * The meta-property for the {@code info} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<SurfaceInfoType<?>, Object>> info() {
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
        case -1403077416:  // surfaceName
          return ((DefaultSurfaceMetadata) bean).getSurfaceName();
        case -868509005:  // xValueType
          return ((DefaultSurfaceMetadata) bean).getXValueType();
        case -1065022510:  // yValueType
          return ((DefaultSurfaceMetadata) bean).getYValueType();
        case -1261536015:  // zValueType
          return ((DefaultSurfaceMetadata) bean).getZValueType();
        case 3237038:  // info
          return ((DefaultSurfaceMetadata) bean).getInfo();
        case -1169106440:  // parameterMetadata
          return ((DefaultSurfaceMetadata) bean).parameterMetadata;
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
   * The bean-builder for {@code DefaultSurfaceMetadata}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<DefaultSurfaceMetadata> {

    private SurfaceName surfaceName;
    private ValueType xValueType;
    private ValueType yValueType;
    private ValueType zValueType;
    private Map<SurfaceInfoType<?>, Object> info = ImmutableMap.of();
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
        case -1403077416:  // surfaceName
          return surfaceName;
        case -868509005:  // xValueType
          return xValueType;
        case -1065022510:  // yValueType
          return yValueType;
        case -1261536015:  // zValueType
          return zValueType;
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
        case -1403077416:  // surfaceName
          this.surfaceName = (SurfaceName) newValue;
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
        case 3237038:  // info
          this.info = (Map<SurfaceInfoType<?>, Object>) newValue;
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
    public DefaultSurfaceMetadata build() {
      return new DefaultSurfaceMetadata(
          surfaceName,
          xValueType,
          yValueType,
          zValueType,
          info,
          parameterMetadata);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("DefaultSurfaceMetadata.Builder{");
      buf.append("surfaceName").append('=').append(JodaBeanUtils.toString(surfaceName)).append(',').append(' ');
      buf.append("xValueType").append('=').append(JodaBeanUtils.toString(xValueType)).append(',').append(' ');
      buf.append("yValueType").append('=').append(JodaBeanUtils.toString(yValueType)).append(',').append(' ');
      buf.append("zValueType").append('=').append(JodaBeanUtils.toString(zValueType)).append(',').append(' ');
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("parameterMetadata").append('=').append(JodaBeanUtils.toString(parameterMetadata));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
