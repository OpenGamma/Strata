/**
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
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Default metadata for a curve.
 * <p>
 * This implementation of {@link CurveMetadata} provides the curve name and nodes.
 */
@BeanDefinition
public final class DefaultCurveMetadata
    implements CurveMetadata, ImmutableBean, Serializable {

  /**
   * The curve name.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveName curveName;
  /**
   * The day count, optional.
   * <p>
   * If the x-value of the curve represents time as a year fraction, the day count
   * can be specified to define how the year fraction is calculated.
   */
  @PropertyDefinition(get = "optional", overrideGet = true)
  private final DayCount dayCount;
  /**
   * The metadata about the parameters.
   * <p>
   * If present, the parameter metadata will match the number of parameters on the curve.
   */
  @PropertyDefinition(get = "optional", overrideGet = true, type = "List<>")
  private final ImmutableList<CurveParameterMetadata> parameterMetadata;

  //-------------------------------------------------------------------------
  /**
   * Creates the metadata.
   * <p>
   * There is no information about the day count.
   * 
   * @param name  the curve name
   * @return the metadata
   */
  public static DefaultCurveMetadata of(String name) {
    return new DefaultCurveMetadata(CurveName.of(name), null, null);
  }

  /**
   * Creates the metadata.
   * <p>
   * There is no information about the day count.
   * 
   * @param name  the curve name
   * @return the metadata
   */
  public static DefaultCurveMetadata of(CurveName name) {
    return new DefaultCurveMetadata(name, null, null);
  }

  /**
   * Creates the metadata.
   * <p>
   * There is no information about the day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count defining meaning for the x-values
   * @return the metadata
   */
  public static DefaultCurveMetadata of(String name, DayCount dayCount) {
    return new DefaultCurveMetadata(CurveName.of(name), dayCount, null);
  }

  /**
   * Creates the metadata, specifying day count.
   * 
   * @param name  the curve name
   * @param dayCount  the day count defining meaning for the x-values
   * @return the metadata
   */
  public static DefaultCurveMetadata of(CurveName name, DayCount dayCount) {
    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(dayCount, "dayCount");
    return new DefaultCurveMetadata(name, dayCount, null);
  }

  /**
   * Creates the metadata, specifying parameter metadata.
   * <p>
   * There is no information about the day count.
   * 
   * @param name  the curve name
   * @param parameterMetadata  the parameter metadata, null if not applicable
   * @return the metadata
   */
  public static DefaultCurveMetadata of(String name, List<? extends CurveParameterMetadata> parameterMetadata) {
    return of(CurveName.of(name), null, parameterMetadata);
  }

  /**
   * Creates the metadata, specifying parameter metadata.
   * <p>
   * There is no information about the day count.
   * 
   * @param name  the curve name
   * @param parameterMetadata  the parameter metadata, null if not applicable
   * @return the metadata
   */
  public static DefaultCurveMetadata of(CurveName name, List<? extends CurveParameterMetadata> parameterMetadata) {
    return of(name, null, parameterMetadata);
  }

  /**
   * Creates the metadata, specifying day count and parameter metadata.
   * 
   * @param name  the curve name
   * @param dayCount  the day count defining meaning for the x-values, null if not applicable
   * @param parameterMetadata  the parameter metadata, null if not applicable
   * @return the metadata
   */
  public static DefaultCurveMetadata of(
      CurveName name, 
      DayCount dayCount, 
      List<? extends CurveParameterMetadata> parameterMetadata) {
    
    return new DefaultCurveMetadata(name, dayCount, parameterMetadata);
  }

  @ImmutableConstructor
  private DefaultCurveMetadata(
      CurveName curveName,
      DayCount dayCount,
      List<? extends CurveParameterMetadata> parameterMetadata) {
    JodaBeanUtils.notNull(curveName, "curveName");
    this.curveName = curveName;
    this.dayCount = dayCount;
    this.parameterMetadata = (parameterMetadata != null ? ImmutableList.copyOf(parameterMetadata) : null);
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
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static DefaultCurveMetadata.Builder builder() {
    return new DefaultCurveMetadata.Builder();
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
   * Gets the day count, optional.
   * <p>
   * If the x-value of the curve represents time as a year fraction, the day count
   * can be specified to define how the year fraction is calculated.
   * @return the optional value of the property, not null
   */
  @Override
  public Optional<DayCount> getDayCount() {
    return Optional.ofNullable(dayCount);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the metadata about the parameters.
   * <p>
   * If present, the parameter metadata will match the number of parameters on the curve.
   * @return the optional value of the property, not null
   */
  @Override
  public Optional<List<CurveParameterMetadata>> getParameterMetadata() {
    return Optional.ofNullable(parameterMetadata);
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
      DefaultCurveMetadata other = (DefaultCurveMetadata) obj;
      return JodaBeanUtils.equal(getCurveName(), other.getCurveName()) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(parameterMetadata, other.parameterMetadata);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurveName());
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameterMetadata);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("DefaultCurveMetadata{");
    buf.append("curveName").append('=').append(getCurveName()).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
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
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", DefaultCurveMetadata.class, DayCount.class);
    /**
     * The meta-property for the {@code parameterMetadata} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<CurveParameterMetadata>> parameterMetadata = DirectMetaProperty.ofImmutable(
        this, "parameterMetadata", DefaultCurveMetadata.class, (Class) List.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "curveName",
        "dayCount",
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
        case 1905311443:  // dayCount
          return dayCount;
        case -1169106440:  // parameterMetadata
          return parameterMetadata;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public DefaultCurveMetadata.Builder builder() {
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
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code parameterMetadata} property.
     * @return the meta-property, not null
     */
    public MetaProperty<List<CurveParameterMetadata>> parameterMetadata() {
      return parameterMetadata;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 771153946:  // curveName
          return ((DefaultCurveMetadata) bean).getCurveName();
        case 1905311443:  // dayCount
          return ((DefaultCurveMetadata) bean).dayCount;
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
  public static final class Builder extends DirectFieldsBeanBuilder<DefaultCurveMetadata> {

    private CurveName curveName;
    private DayCount dayCount;
    private List<CurveParameterMetadata> parameterMetadata;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(DefaultCurveMetadata beanToCopy) {
      this.curveName = beanToCopy.getCurveName();
      this.dayCount = beanToCopy.dayCount;
      this.parameterMetadata = beanToCopy.parameterMetadata;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 771153946:  // curveName
          return curveName;
        case 1905311443:  // dayCount
          return dayCount;
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
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case -1169106440:  // parameterMetadata
          this.parameterMetadata = (List<CurveParameterMetadata>) newValue;
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
    public DefaultCurveMetadata build() {
      return new DefaultCurveMetadata(
          curveName,
          dayCount,
          parameterMetadata);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code curveName} property in the builder.
     * @param curveName  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder curveName(CurveName curveName) {
      JodaBeanUtils.notNull(curveName, "curveName");
      this.curveName = curveName;
      return this;
    }

    /**
     * Sets the {@code dayCount} property in the builder.
     * @param dayCount  the new value
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the {@code parameterMetadata} property in the builder.
     * @param parameterMetadata  the new value
     * @return this, for chaining, not null
     */
    public Builder parameterMetadata(List<CurveParameterMetadata> parameterMetadata) {
      this.parameterMetadata = parameterMetadata;
      return this;
    }

    /**
     * Sets the {@code parameterMetadata} property in the builder
     * from an array of objects.
     * @param parameterMetadata  the new value
     * @return this, for chaining, not null
     */
    public Builder parameterMetadata(CurveParameterMetadata... parameterMetadata) {
      return parameterMetadata(ImmutableList.copyOf(parameterMetadata));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("DefaultCurveMetadata.Builder{");
      buf.append("curveName").append('=').append(JodaBeanUtils.toString(curveName)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("parameterMetadata").append('=').append(JodaBeanUtils.toString(parameterMetadata));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
