/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.util.stream.Collectors.toCollection;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.param.DatedParameterMetadata;

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
   * There must be at least two nodes in the curve.
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

  @ImmutableValidator
  private void validate() {
    if (nodes.size() < 2) {
      throw new IllegalArgumentException("Curve must have at least two nodes");
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public InterpolatedNodalCurveDefinition filtered(LocalDate valuationDate, ReferenceData refData) {
    // mutable list of date-node pairs
    ArrayList<Pair<LocalDate, CurveNode>> nodeDates = nodes.stream()
        .map(node -> Pair.of(node.date(valuationDate, refData), node))
        .collect(toCollection(ArrayList::new));
    // delete nodes if clash, but don't throw exceptions yet
    loop:
    for (int i = 0; i < nodeDates.size(); i++) {
      Pair<LocalDate, CurveNode> pair = nodeDates.get(i);
      CurveNodeDateOrder restriction = pair.getSecond().getDateOrder();
      // compare node to previous node
      if (i > 0) {
        Pair<LocalDate, CurveNode> pairBefore = nodeDates.get(i - 1);
        if (DAYS.between(pairBefore.getFirst(), pair.getFirst()) < restriction.getMinGapInDays()) {
          switch (restriction.getAction()) {
            case DROP_THIS:
              nodeDates.remove(i);
              i = -1;  // restart loop
              continue loop;
            case DROP_OTHER:
              nodeDates.remove(i - 1);
              i = -1;  // restart loop
              continue loop;
            case EXCEPTION:
              break;  // do nothing yet
          }
        }
      }
      // compare node to next node
      if (i < nodeDates.size() - 1) {
        Pair<LocalDate, CurveNode> pairAfter = nodeDates.get(i + 1);
        if (DAYS.between(pair.getFirst(), pairAfter.getFirst()) < restriction.getMinGapInDays()) {
          switch (restriction.getAction()) {
            case DROP_THIS:
              nodeDates.remove(i);
              i = -1;  // restart loop
              continue loop;
            case DROP_OTHER:
              nodeDates.remove(i + 1);
              i = -1;  // restart loop
              continue loop;
            case EXCEPTION:
              break;  // do nothing yet
          }
        }
      }
    }
    // throw exceptions if rules breached
    for (int i = 0; i < nodeDates.size(); i++) {
      Pair<LocalDate, CurveNode> pair = nodeDates.get(i);
      CurveNodeDateOrder restriction = pair.getSecond().getDateOrder();
      // compare node to previous node
      if (i > 0) {
        Pair<LocalDate, CurveNode> pairBefore = nodeDates.get(i - 1);
        if (DAYS.between(pairBefore.getFirst(), pair.getFirst()) < restriction.getMinGapInDays()) {
          throw new IllegalArgumentException(Messages.format(
              "Curve node dates clash, node '{}' and '{}' resolved to dates '{}' and '{}' respectively",
              pairBefore.getSecond().getLabel(),
              pair.getSecond().getLabel(),
              pairBefore.getFirst(),
              pair.getFirst()));
        }
      }
      // compare node to next node
      if (i < nodeDates.size() - 1) {
        Pair<LocalDate, CurveNode> pairAfter = nodeDates.get(i + 1);
        if (DAYS.between(pair.getFirst(), pairAfter.getFirst()) < restriction.getMinGapInDays()) {
          throw new IllegalArgumentException(Messages.format(
              "Curve node dates clash, node '{}' and '{}' resolved to dates '{}' and '{}' respectively",
              pair.getSecond().getLabel(),
              pairAfter.getSecond().getLabel(),
              pair.getFirst(),
              pairAfter.getFirst()));
        }
      }
    }
    // return the resolved definition
    List<CurveNode> filteredNodes = nodeDates.stream().map(p -> p.getSecond()).collect(toImmutableList());
    return new InterpolatedNodalCurveDefinition(
        name, xValueType, yValueType, dayCount, filteredNodes, interpolator, extrapolatorLeft, extrapolatorRight);
  }

  //-------------------------------------------------------------------------
  @Override
  public CurveMetadata metadata(LocalDate valuationDate, ReferenceData refData) {
    List<DatedParameterMetadata> nodeMetadata = nodes.stream()
        .map(node -> node.metadata(valuationDate, refData))
        .collect(toImmutableList());
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(xValueType)
        .yValueType(yValueType)
        .dayCount(dayCount)
        .parameterMetadata(nodeMetadata)
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public NodalCurve curve(
      LocalDate valuationDate,
      CurveMetadata metadata,
      DoubleArray parameters) {

    DoubleArray nodeTimes = buildNodeTimes(valuationDate, metadata);
    return InterpolatedNodalCurve.builder()
        .metadata(metadata)
        .xValues(nodeTimes)
        .yValues(parameters)
        .extrapolatorLeft(extrapolatorLeft)
        .interpolator(interpolator)
        .extrapolatorRight(extrapolatorRight).build();
  }

  // builds node times from node dates
  private DoubleArray buildNodeTimes(LocalDate valuationDate, CurveMetadata metadata) {
    if (metadata.getXValueType().equals(ValueType.YEAR_FRACTION)) {
      return DoubleArray.of(getParameterCount(), i -> {
        LocalDate nodeDate = ((DatedParameterMetadata) metadata.getParameterMetadata().get().get(i)).getDate();
        return getDayCount().get().yearFraction(valuationDate, nodeDate);
      });

    } else if (metadata.getXValueType().equals(ValueType.MONTHS)) {
      return DoubleArray.of(getParameterCount(), i -> {
        LocalDate nodeDate = ((DatedParameterMetadata) metadata.getParameterMetadata().get().get(i)).getDate();
        return YearMonth.from(valuationDate).until(YearMonth.from(nodeDate), MONTHS);
      });

    } else {
      throw new IllegalArgumentException("Metadata XValueType should be YearFraction or Months in curve definition");
    }
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
    validate();
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
   * There must be at least two nodes in the curve.
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
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(xValueType, other.xValueType) &&
          JodaBeanUtils.equal(yValueType, other.yValueType) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(nodes, other.nodes) &&
          JodaBeanUtils.equal(interpolator, other.interpolator) &&
          JodaBeanUtils.equal(extrapolatorLeft, other.extrapolatorLeft) &&
          JodaBeanUtils.equal(extrapolatorRight, other.extrapolatorRight);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(xValueType);
    hash = hash * 31 + JodaBeanUtils.hashCode(yValueType);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(nodes);
    hash = hash * 31 + JodaBeanUtils.hashCode(interpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(extrapolatorLeft);
    hash = hash * 31 + JodaBeanUtils.hashCode(extrapolatorRight);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(288);
    buf.append("InterpolatedNodalCurveDefinition{");
    buf.append("name").append('=').append(name).append(',').append(' ');
    buf.append("xValueType").append('=').append(xValueType).append(',').append(' ');
    buf.append("yValueType").append('=').append(yValueType).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("nodes").append('=').append(nodes).append(',').append(' ');
    buf.append("interpolator").append('=').append(interpolator).append(',').append(' ');
    buf.append("extrapolatorLeft").append('=').append(extrapolatorLeft).append(',').append(' ');
    buf.append("extrapolatorRight").append('=').append(JodaBeanUtils.toString(extrapolatorRight));
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

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    /**
     * @deprecated Loop in application code
     */
    @Override
    @Deprecated
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
     * There must be at least two nodes in the curve.
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
