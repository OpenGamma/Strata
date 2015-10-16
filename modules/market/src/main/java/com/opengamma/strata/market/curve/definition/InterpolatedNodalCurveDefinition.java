/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.definition;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.interpolator.CurveExtrapolator;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.DatedCurveParameterMetadata;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.value.ValueType;

/**
 * Provides the definition of how to calibrate an interpolated nodal curve.
 * <p>
 * A nodal curve is built from a number of parameters and described by metadata.
 * Calibration is based on a list of {@link CurveNode} instances, one for each parameter,
 * that specify the underlying instruments.
 */
@BeanDefinition
public final class InterpolatedNodalCurveDefinition
    implements NodalCurveDefinition, ImmutableBean, Serializable {

  /**
   * The curve name.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurveName name;
  /**
   * The x-value type, providing meaning to the x-values of the curve.
   * <p>
   * This type provides meaning to the x-values. For example, the x-value might
   * represent a year fraction, as represented using {@link ValueType#YEAR_FRACTION}.
   * <p>
   * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
   */
  @PropertyDefinition(validate = "notNull")
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
   * The day count, optional.
   * <p>
   * If the x-value of the curve represents time as a year fraction, the day count
   * can be specified to define how the year fraction is calculated.
   */
  @PropertyDefinition(get = "optional")
  private final DayCount dayCount;
  /**
   * The nodes in the curve.
   * <p>
   * The nodes are used to find the par rates and calibrate the curve.
   */
  @PropertyDefinition(validate = "notNull", builderType = "List<? extends CurveNode>", overrideGet = true)
  private final ImmutableList<CurveNode> nodes;
  /**
   * The interpolator used to find points on the curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveInterpolator interpolator;
  /**
   * The extrapolator used to find points to the left of the leftmost point on the curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator extrapolatorLeft;
  /**
   * The extrapolator used to find points to the right of the rightmost point on the curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator extrapolatorRight;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.xValueType = ValueType.UNKNOWN;
    builder.yValueType = ValueType.UNKNOWN;
  }

  //-------------------------------------------------------------------------
  @Override
  public CurveMetadata metadata(LocalDate valuationDate) {
    return metadata(valuationDate, ImmutableMap.of());
  }

  // creates the metadata with optional calibration info
  private CurveMetadata metadata(LocalDate valuationDate, Map<CurveInfoType<?>, Object> additionalInfo) {
    List<CurveParameterMetadata> nodeMetadata = nodes.stream()
        .map(node -> node.metadata(valuationDate))
        .collect(toImmutableList());
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(xValueType)
        .yValueType(yValueType)
        .dayCount(dayCount)
        .addInfo(additionalInfo)
        .parameterMetadata(nodeMetadata)
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public NodalCurve curve(LocalDate valuationDate, DoubleArray parameters, Map<CurveInfoType<?>, Object> additionalInfo) {
    CurveMetadata meta = metadata(valuationDate, additionalInfo);
    DoubleArray nodeTimes = DoubleArray.of(getParameterCount(), i -> {
      LocalDate nodeDate = ((DatedCurveParameterMetadata) meta.getParameterMetadata().get().get(i)).getDate();
      return getDayCount().get().yearFraction(valuationDate, nodeDate);
    });
    return InterpolatedNodalCurve.builder()
        .metadata(meta)
        .xValues(nodeTimes)
        .yValues(parameters)
        .extrapolatorLeft(extrapolatorLeft)
        .interpolator(interpolator)
        .extrapolatorRight(extrapolatorRight).build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code InterpolatedNodalCurveDefinition}.
   * @return the meta-bean, not null
   */
  public static InterpolatedNodalCurveDefinition.Meta meta() {
    return InterpolatedNodalCurveDefinition.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(InterpolatedNodalCurveDefinition.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static InterpolatedNodalCurveDefinition.Builder builder() {
    return new InterpolatedNodalCurveDefinition.Builder();
  }

  private InterpolatedNodalCurveDefinition(
      CurveName name,
      ValueType xValueType,
      ValueType yValueType,
      DayCount dayCount,
      List<? extends CurveNode> nodes,
      CurveInterpolator interpolator,
      CurveExtrapolator extrapolatorLeft,
      CurveExtrapolator extrapolatorRight) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(xValueType, "xValueType");
    JodaBeanUtils.notNull(yValueType, "yValueType");
    JodaBeanUtils.notNull(nodes, "nodes");
    JodaBeanUtils.notNull(interpolator, "interpolator");
    JodaBeanUtils.notNull(extrapolatorLeft, "extrapolatorLeft");
    JodaBeanUtils.notNull(extrapolatorRight, "extrapolatorRight");
    this.name = name;
    this.xValueType = xValueType;
    this.yValueType = yValueType;
    this.dayCount = dayCount;
    this.nodes = ImmutableList.copyOf(nodes);
    this.interpolator = interpolator;
    this.extrapolatorLeft = extrapolatorLeft;
    this.extrapolatorRight = extrapolatorRight;
  }

  @Override
  public InterpolatedNodalCurveDefinition.Meta metaBean() {
    return InterpolatedNodalCurveDefinition.Meta.INSTANCE;
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
  public CurveName getName() {
    return name;
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
   * Gets the day count, optional.
   * <p>
   * If the x-value of the curve represents time as a year fraction, the day count
   * can be specified to define how the year fraction is calculated.
   * @return the optional value of the property, not null
   */
  public Optional<DayCount> getDayCount() {
    return Optional.ofNullable(dayCount);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the nodes in the curve.
   * <p>
   * The nodes are used to find the par rates and calibrate the curve.
   * @return the value of the property, not null
   */
  @Override
  public ImmutableList<CurveNode> getNodes() {
    return nodes;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the interpolator used to find points on the curve.
   * @return the value of the property, not null
   */
  public CurveInterpolator getInterpolator() {
    return interpolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the extrapolator used to find points to the left of the leftmost point on the curve.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getExtrapolatorLeft() {
    return extrapolatorLeft;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the extrapolator used to find points to the right of the rightmost point on the curve.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getExtrapolatorRight() {
    return extrapolatorRight;
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
      InterpolatedNodalCurveDefinition other = (InterpolatedNodalCurveDefinition) obj;
      return JodaBeanUtils.equal(getName(), other.getName()) &&
          JodaBeanUtils.equal(getXValueType(), other.getXValueType()) &&
          JodaBeanUtils.equal(getYValueType(), other.getYValueType()) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(getNodes(), other.getNodes()) &&
          JodaBeanUtils.equal(getInterpolator(), other.getInterpolator()) &&
          JodaBeanUtils.equal(getExtrapolatorLeft(), other.getExtrapolatorLeft()) &&
          JodaBeanUtils.equal(getExtrapolatorRight(), other.getExtrapolatorRight());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getName());
    hash = hash * 31 + JodaBeanUtils.hashCode(getXValueType());
    hash = hash * 31 + JodaBeanUtils.hashCode(getYValueType());
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(getNodes());
    hash = hash * 31 + JodaBeanUtils.hashCode(getInterpolator());
    hash = hash * 31 + JodaBeanUtils.hashCode(getExtrapolatorLeft());
    hash = hash * 31 + JodaBeanUtils.hashCode(getExtrapolatorRight());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(288);
    buf.append("InterpolatedNodalCurveDefinition{");
    buf.append("name").append('=').append(getName()).append(',').append(' ');
    buf.append("xValueType").append('=').append(getXValueType()).append(',').append(' ');
    buf.append("yValueType").append('=').append(getYValueType()).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("nodes").append('=').append(getNodes()).append(',').append(' ');
    buf.append("interpolator").append('=').append(getInterpolator()).append(',').append(' ');
    buf.append("extrapolatorLeft").append('=').append(getExtrapolatorLeft()).append(',').append(' ');
    buf.append("extrapolatorRight").append('=').append(JodaBeanUtils.toString(getExtrapolatorRight()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code InterpolatedNodalCurveDefinition}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<CurveName> name = DirectMetaProperty.ofImmutable(
        this, "name", InterpolatedNodalCurveDefinition.class, CurveName.class);
    /**
     * The meta-property for the {@code xValueType} property.
     */
    private final MetaProperty<ValueType> xValueType = DirectMetaProperty.ofImmutable(
        this, "xValueType", InterpolatedNodalCurveDefinition.class, ValueType.class);
    /**
     * The meta-property for the {@code yValueType} property.
     */
    private final MetaProperty<ValueType> yValueType = DirectMetaProperty.ofImmutable(
        this, "yValueType", InterpolatedNodalCurveDefinition.class, ValueType.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", InterpolatedNodalCurveDefinition.class, DayCount.class);
    /**
     * The meta-property for the {@code nodes} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<CurveNode>> nodes = DirectMetaProperty.ofImmutable(
        this, "nodes", InterpolatedNodalCurveDefinition.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code interpolator} property.
     */
    private final MetaProperty<CurveInterpolator> interpolator = DirectMetaProperty.ofImmutable(
        this, "interpolator", InterpolatedNodalCurveDefinition.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code extrapolatorLeft} property.
     */
    private final MetaProperty<CurveExtrapolator> extrapolatorLeft = DirectMetaProperty.ofImmutable(
        this, "extrapolatorLeft", InterpolatedNodalCurveDefinition.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code extrapolatorRight} property.
     */
    private final MetaProperty<CurveExtrapolator> extrapolatorRight = DirectMetaProperty.ofImmutable(
        this, "extrapolatorRight", InterpolatedNodalCurveDefinition.class, CurveExtrapolator.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "xValueType",
        "yValueType",
        "dayCount",
        "nodes",
        "interpolator",
        "extrapolatorLeft",
        "extrapolatorRight");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case -868509005:  // xValueType
          return xValueType;
        case -1065022510:  // yValueType
          return yValueType;
        case 1905311443:  // dayCount
          return dayCount;
        case 104993457:  // nodes
          return nodes;
        case 2096253127:  // interpolator
          return interpolator;
        case 1271703994:  // extrapolatorLeft
          return extrapolatorLeft;
        case 773779145:  // extrapolatorRight
          return extrapolatorRight;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public InterpolatedNodalCurveDefinition.Builder builder() {
      return new InterpolatedNodalCurveDefinition.Builder();
    }

    @Override
    public Class<? extends InterpolatedNodalCurveDefinition> beanType() {
      return InterpolatedNodalCurveDefinition.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveName> name() {
      return name;
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
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    /**
     * The meta-property for the {@code nodes} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<CurveNode>> nodes() {
      return nodes;
    }

    /**
     * The meta-property for the {@code interpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveInterpolator> interpolator() {
      return interpolator;
    }

    /**
     * The meta-property for the {@code extrapolatorLeft} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> extrapolatorLeft() {
      return extrapolatorLeft;
    }

    /**
     * The meta-property for the {@code extrapolatorRight} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> extrapolatorRight() {
      return extrapolatorRight;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((InterpolatedNodalCurveDefinition) bean).getName();
        case -868509005:  // xValueType
          return ((InterpolatedNodalCurveDefinition) bean).getXValueType();
        case -1065022510:  // yValueType
          return ((InterpolatedNodalCurveDefinition) bean).getYValueType();
        case 1905311443:  // dayCount
          return ((InterpolatedNodalCurveDefinition) bean).dayCount;
        case 104993457:  // nodes
          return ((InterpolatedNodalCurveDefinition) bean).getNodes();
        case 2096253127:  // interpolator
          return ((InterpolatedNodalCurveDefinition) bean).getInterpolator();
        case 1271703994:  // extrapolatorLeft
          return ((InterpolatedNodalCurveDefinition) bean).getExtrapolatorLeft();
        case 773779145:  // extrapolatorRight
          return ((InterpolatedNodalCurveDefinition) bean).getExtrapolatorRight();
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
   * The bean-builder for {@code InterpolatedNodalCurveDefinition}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<InterpolatedNodalCurveDefinition> {

    private CurveName name;
    private ValueType xValueType;
    private ValueType yValueType;
    private DayCount dayCount;
    private List<? extends CurveNode> nodes = ImmutableList.of();
    private CurveInterpolator interpolator;
    private CurveExtrapolator extrapolatorLeft;
    private CurveExtrapolator extrapolatorRight;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(InterpolatedNodalCurveDefinition beanToCopy) {
      this.name = beanToCopy.getName();
      this.xValueType = beanToCopy.getXValueType();
      this.yValueType = beanToCopy.getYValueType();
      this.dayCount = beanToCopy.dayCount;
      this.nodes = beanToCopy.getNodes();
      this.interpolator = beanToCopy.getInterpolator();
      this.extrapolatorLeft = beanToCopy.getExtrapolatorLeft();
      this.extrapolatorRight = beanToCopy.getExtrapolatorRight();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case -868509005:  // xValueType
          return xValueType;
        case -1065022510:  // yValueType
          return yValueType;
        case 1905311443:  // dayCount
          return dayCount;
        case 104993457:  // nodes
          return nodes;
        case 2096253127:  // interpolator
          return interpolator;
        case 1271703994:  // extrapolatorLeft
          return extrapolatorLeft;
        case 773779145:  // extrapolatorRight
          return extrapolatorRight;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (CurveName) newValue;
          break;
        case -868509005:  // xValueType
          this.xValueType = (ValueType) newValue;
          break;
        case -1065022510:  // yValueType
          this.yValueType = (ValueType) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 104993457:  // nodes
          this.nodes = (List<? extends CurveNode>) newValue;
          break;
        case 2096253127:  // interpolator
          this.interpolator = (CurveInterpolator) newValue;
          break;
        case 1271703994:  // extrapolatorLeft
          this.extrapolatorLeft = (CurveExtrapolator) newValue;
          break;
        case 773779145:  // extrapolatorRight
          this.extrapolatorRight = (CurveExtrapolator) newValue;
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
    public InterpolatedNodalCurveDefinition build() {
      return new InterpolatedNodalCurveDefinition(
          name,
          xValueType,
          yValueType,
          dayCount,
          nodes,
          interpolator,
          extrapolatorLeft,
          extrapolatorRight);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the curve name.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(CurveName name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the x-value type, providing meaning to the x-values of the curve.
     * <p>
     * This type provides meaning to the x-values. For example, the x-value might
     * represent a year fraction, as represented using {@link ValueType#YEAR_FRACTION}.
     * <p>
     * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
     * @param xValueType  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder xValueType(ValueType xValueType) {
      JodaBeanUtils.notNull(xValueType, "xValueType");
      this.xValueType = xValueType;
      return this;
    }

    /**
     * Sets the y-value type, providing meaning to the y-values of the curve.
     * <p>
     * This type provides meaning to the y-values. For example, the y-value might
     * represent a zero rate, as represented using {@link ValueType#ZERO_RATE}.
     * <p>
     * If using the builder, this defaults to {@link ValueType#UNKNOWN}.
     * @param yValueType  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder yValueType(ValueType yValueType) {
      JodaBeanUtils.notNull(yValueType, "yValueType");
      this.yValueType = yValueType;
      return this;
    }

    /**
     * Sets the day count, optional.
     * <p>
     * If the x-value of the curve represents time as a year fraction, the day count
     * can be specified to define how the year fraction is calculated.
     * @param dayCount  the new value
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the nodes in the curve.
     * <p>
     * The nodes are used to find the par rates and calibrate the curve.
     * @param nodes  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder nodes(List<? extends CurveNode> nodes) {
      JodaBeanUtils.notNull(nodes, "nodes");
      this.nodes = nodes;
      return this;
    }

    /**
     * Sets the {@code nodes} property in the builder
     * from an array of objects.
     * @param nodes  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder nodes(CurveNode... nodes) {
      return nodes(ImmutableList.copyOf(nodes));
    }

    /**
     * Sets the interpolator used to find points on the curve.
     * @param interpolator  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder interpolator(CurveInterpolator interpolator) {
      JodaBeanUtils.notNull(interpolator, "interpolator");
      this.interpolator = interpolator;
      return this;
    }

    /**
     * Sets the extrapolator used to find points to the left of the leftmost point on the curve.
     * @param extrapolatorLeft  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder extrapolatorLeft(CurveExtrapolator extrapolatorLeft) {
      JodaBeanUtils.notNull(extrapolatorLeft, "extrapolatorLeft");
      this.extrapolatorLeft = extrapolatorLeft;
      return this;
    }

    /**
     * Sets the extrapolator used to find points to the right of the rightmost point on the curve.
     * @param extrapolatorRight  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder extrapolatorRight(CurveExtrapolator extrapolatorRight) {
      JodaBeanUtils.notNull(extrapolatorRight, "extrapolatorRight");
      this.extrapolatorRight = extrapolatorRight;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(288);
      buf.append("InterpolatedNodalCurveDefinition.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("xValueType").append('=').append(JodaBeanUtils.toString(xValueType)).append(',').append(' ');
      buf.append("yValueType").append('=').append(JodaBeanUtils.toString(yValueType)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("nodes").append('=').append(JodaBeanUtils.toString(nodes)).append(',').append(' ');
      buf.append("interpolator").append('=').append(JodaBeanUtils.toString(interpolator)).append(',').append(' ');
      buf.append("extrapolatorLeft").append('=').append(JodaBeanUtils.toString(extrapolatorLeft)).append(',').append(' ');
      buf.append("extrapolatorRight").append('=').append(JodaBeanUtils.toString(extrapolatorRight));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
