/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.toCollection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
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
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Provides the definition of how to calibrate a parameterized functional curve.
 * <p>
 * A parameterized functional curve is built from a number of parameters and described by metadata.
 * Calibration is based on a list of {@link CurveNode} instances that specify the underlying instruments.
 * <p>
 * The number of the curve parameters is in general different from the number of the instruments.
 * However, the number mismatch tends to cause the root-finding failure in the curve calibration.
 */
@BeanDefinition
public final class ParameterizedFunctionalCurveDefinition
    implements CurveDefinition, ImmutableBean {

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
   * The nodes of the underlying instruments.
   * <p>
   * The nodes are used to find the quoted values to which the curve is calibrated.
   */
  @PropertyDefinition(validate = "notNull", builderType = "List<? extends CurveNode>", overrideGet = true)
  private final ImmutableList<CurveNode> nodes;
  /**
   * The initial guess values for the curve parameters.
   * <p>
   * The size must be the same as the number of the curve parameters. 
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<Double> initialGuess;
  /**
   * The parameter metadata of the curve, defaulted to empty metadata instances.
   * <p>
   * The size of the list must be the same as the number of the curve parameters.
   */
  @PropertyDefinition(validate = "notNull", builderType = "List<? extends ParameterMetadata>")
  private final ImmutableList<ParameterMetadata> parameterMetadata;
  /**
   * The y-value function.
   * <p>
   * The function takes {@code parameters} and x-value, then returns y-value.
   */
  @PropertyDefinition(validate = "notNull")
  private final BiFunction<DoubleArray, Double, Double> valueFunction;
  /**
   * The derivative function.
   * <p>
   * The function takes {@code parameters} and x-value, then returns the first derivative of y-value with respective to x, 
   * i.e., the gradient of the curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final BiFunction<DoubleArray, Double, Double> derivativeFunction;
  /**
   * The parameter sensitivity function.
   * <p>
   * The function takes {@code parameters} and x-value, then returns the sensitivities of y-value to the parameters.
   */
  @PropertyDefinition(validate = "notNull")
  private final BiFunction<DoubleArray, Double, DoubleArray> sensitivityFunction;

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.parameterMetadata.size() == 0) {
      if (builder.initialGuess != null) {
        builder.parameterMetadata = ParameterMetadata.listOfEmpty(builder.initialGuess.size());
      }
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public ParameterizedFunctionalCurveDefinition filtered(LocalDate valuationDate, ReferenceData refData) { 
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
    return new ParameterizedFunctionalCurveDefinition(
        name,
        xValueType,
        yValueType,
        dayCount,
        filteredNodes,
        initialGuess,
        parameterMetadata,
        valueFunction,
        derivativeFunction,
        sensitivityFunction);
  }

  @Override
  public CurveMetadata metadata(LocalDate valuationDate, ReferenceData refData) {
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .xValueType(xValueType)
        .yValueType(yValueType)
        .dayCount(dayCount)
        .parameterMetadata(parameterMetadata)
        .build();
  }

  @Override
  public ParameterizedFunctionalCurve curve(LocalDate valuationDate, CurveMetadata metadata, DoubleArray parameters) {
    return ParameterizedFunctionalCurve.of(
        metadata,
        parameters,
        valueFunction,
        derivativeFunction,
        sensitivityFunction);
  }

  @Override
  public int getParameterCount() {
    return initialGuess.size();
  }

  @Override
  public ImmutableList<Double> initialGuess(MarketData marketData) {
    return getInitialGuess();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ParameterizedFunctionalCurveDefinition}.
   * @return the meta-bean, not null
   */
  public static ParameterizedFunctionalCurveDefinition.Meta meta() {
    return ParameterizedFunctionalCurveDefinition.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ParameterizedFunctionalCurveDefinition.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ParameterizedFunctionalCurveDefinition.Builder builder() {
    return new ParameterizedFunctionalCurveDefinition.Builder();
  }

  private ParameterizedFunctionalCurveDefinition(
      CurveName name,
      ValueType xValueType,
      ValueType yValueType,
      DayCount dayCount,
      List<? extends CurveNode> nodes,
      List<Double> initialGuess,
      List<? extends ParameterMetadata> parameterMetadata,
      BiFunction<DoubleArray, Double, Double> valueFunction,
      BiFunction<DoubleArray, Double, Double> derivativeFunction,
      BiFunction<DoubleArray, Double, DoubleArray> sensitivityFunction) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(xValueType, "xValueType");
    JodaBeanUtils.notNull(yValueType, "yValueType");
    JodaBeanUtils.notNull(nodes, "nodes");
    JodaBeanUtils.notNull(initialGuess, "initialGuess");
    JodaBeanUtils.notNull(parameterMetadata, "parameterMetadata");
    JodaBeanUtils.notNull(valueFunction, "valueFunction");
    JodaBeanUtils.notNull(derivativeFunction, "derivativeFunction");
    JodaBeanUtils.notNull(sensitivityFunction, "sensitivityFunction");
    this.name = name;
    this.xValueType = xValueType;
    this.yValueType = yValueType;
    this.dayCount = dayCount;
    this.nodes = ImmutableList.copyOf(nodes);
    this.initialGuess = ImmutableList.copyOf(initialGuess);
    this.parameterMetadata = ImmutableList.copyOf(parameterMetadata);
    this.valueFunction = valueFunction;
    this.derivativeFunction = derivativeFunction;
    this.sensitivityFunction = sensitivityFunction;
  }

  @Override
  public ParameterizedFunctionalCurveDefinition.Meta metaBean() {
    return ParameterizedFunctionalCurveDefinition.Meta.INSTANCE;
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
   * Gets the nodes of the underlying instruments.
   * <p>
   * The nodes are used to find the quoted values to which the curve is calibrated.
   * @return the value of the property, not null
   */
  @Override
  public ImmutableList<CurveNode> getNodes() {
    return nodes;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the initial guess values for the curve parameters.
   * <p>
   * The size must be the same as the number of the curve parameters.
   * @return the value of the property, not null
   */
  public ImmutableList<Double> getInitialGuess() {
    return initialGuess;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the parameter metadata of the curve, defaulted to empty metadata instances.
   * <p>
   * The size of the list must be the same as the number of the curve parameters.
   * @return the value of the property, not null
   */
  public ImmutableList<ParameterMetadata> getParameterMetadata() {
    return parameterMetadata;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the y-value function.
   * <p>
   * The function takes {@code parameters} and x-value, then returns y-value.
   * @return the value of the property, not null
   */
  public BiFunction<DoubleArray, Double, Double> getValueFunction() {
    return valueFunction;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the derivative function.
   * <p>
   * The function takes {@code parameters} and x-value, then returns the first derivative of y-value with respective to x,
   * i.e., the gradient of the curve.
   * @return the value of the property, not null
   */
  public BiFunction<DoubleArray, Double, Double> getDerivativeFunction() {
    return derivativeFunction;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the parameter sensitivity function.
   * <p>
   * The function takes {@code parameters} and x-value, then returns the sensitivities of y-value to the parameters.
   * @return the value of the property, not null
   */
  public BiFunction<DoubleArray, Double, DoubleArray> getSensitivityFunction() {
    return sensitivityFunction;
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
      ParameterizedFunctionalCurveDefinition other = (ParameterizedFunctionalCurveDefinition) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(xValueType, other.xValueType) &&
          JodaBeanUtils.equal(yValueType, other.yValueType) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(nodes, other.nodes) &&
          JodaBeanUtils.equal(initialGuess, other.initialGuess) &&
          JodaBeanUtils.equal(parameterMetadata, other.parameterMetadata) &&
          JodaBeanUtils.equal(valueFunction, other.valueFunction) &&
          JodaBeanUtils.equal(derivativeFunction, other.derivativeFunction) &&
          JodaBeanUtils.equal(sensitivityFunction, other.sensitivityFunction);
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
    hash = hash * 31 + JodaBeanUtils.hashCode(initialGuess);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameterMetadata);
    hash = hash * 31 + JodaBeanUtils.hashCode(valueFunction);
    hash = hash * 31 + JodaBeanUtils.hashCode(derivativeFunction);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivityFunction);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("ParameterizedFunctionalCurveDefinition{");
    buf.append("name").append('=').append(name).append(',').append(' ');
    buf.append("xValueType").append('=').append(xValueType).append(',').append(' ');
    buf.append("yValueType").append('=').append(yValueType).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("nodes").append('=').append(nodes).append(',').append(' ');
    buf.append("initialGuess").append('=').append(initialGuess).append(',').append(' ');
    buf.append("parameterMetadata").append('=').append(parameterMetadata).append(',').append(' ');
    buf.append("valueFunction").append('=').append(valueFunction).append(',').append(' ');
    buf.append("derivativeFunction").append('=').append(derivativeFunction).append(',').append(' ');
    buf.append("sensitivityFunction").append('=').append(JodaBeanUtils.toString(sensitivityFunction));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ParameterizedFunctionalCurveDefinition}.
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
        this, "name", ParameterizedFunctionalCurveDefinition.class, CurveName.class);
    /**
     * The meta-property for the {@code xValueType} property.
     */
    private final MetaProperty<ValueType> xValueType = DirectMetaProperty.ofImmutable(
        this, "xValueType", ParameterizedFunctionalCurveDefinition.class, ValueType.class);
    /**
     * The meta-property for the {@code yValueType} property.
     */
    private final MetaProperty<ValueType> yValueType = DirectMetaProperty.ofImmutable(
        this, "yValueType", ParameterizedFunctionalCurveDefinition.class, ValueType.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", ParameterizedFunctionalCurveDefinition.class, DayCount.class);
    /**
     * The meta-property for the {@code nodes} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<CurveNode>> nodes = DirectMetaProperty.ofImmutable(
        this, "nodes", ParameterizedFunctionalCurveDefinition.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code initialGuess} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<Double>> initialGuess = DirectMetaProperty.ofImmutable(
        this, "initialGuess", ParameterizedFunctionalCurveDefinition.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code parameterMetadata} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<ParameterMetadata>> parameterMetadata = DirectMetaProperty.ofImmutable(
        this, "parameterMetadata", ParameterizedFunctionalCurveDefinition.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code valueFunction} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<BiFunction<DoubleArray, Double, Double>> valueFunction = DirectMetaProperty.ofImmutable(
        this, "valueFunction", ParameterizedFunctionalCurveDefinition.class, (Class) BiFunction.class);
    /**
     * The meta-property for the {@code derivativeFunction} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<BiFunction<DoubleArray, Double, Double>> derivativeFunction = DirectMetaProperty.ofImmutable(
        this, "derivativeFunction", ParameterizedFunctionalCurveDefinition.class, (Class) BiFunction.class);
    /**
     * The meta-property for the {@code sensitivityFunction} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<BiFunction<DoubleArray, Double, DoubleArray>> sensitivityFunction = DirectMetaProperty.ofImmutable(
        this, "sensitivityFunction", ParameterizedFunctionalCurveDefinition.class, (Class) BiFunction.class);
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
        "initialGuess",
        "parameterMetadata",
        "valueFunction",
        "derivativeFunction",
        "sensitivityFunction");

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
        case -431632141:  // initialGuess
          return initialGuess;
        case -1169106440:  // parameterMetadata
          return parameterMetadata;
        case 636119145:  // valueFunction
          return valueFunction;
        case 1663351423:  // derivativeFunction
          return derivativeFunction;
        case -1353652329:  // sensitivityFunction
          return sensitivityFunction;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ParameterizedFunctionalCurveDefinition.Builder builder() {
      return new ParameterizedFunctionalCurveDefinition.Builder();
    }

    @Override
    public Class<? extends ParameterizedFunctionalCurveDefinition> beanType() {
      return ParameterizedFunctionalCurveDefinition.class;
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
     * The meta-property for the {@code initialGuess} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<Double>> initialGuess() {
      return initialGuess;
    }

    /**
     * The meta-property for the {@code parameterMetadata} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<ParameterMetadata>> parameterMetadata() {
      return parameterMetadata;
    }

    /**
     * The meta-property for the {@code valueFunction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BiFunction<DoubleArray, Double, Double>> valueFunction() {
      return valueFunction;
    }

    /**
     * The meta-property for the {@code derivativeFunction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BiFunction<DoubleArray, Double, Double>> derivativeFunction() {
      return derivativeFunction;
    }

    /**
     * The meta-property for the {@code sensitivityFunction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BiFunction<DoubleArray, Double, DoubleArray>> sensitivityFunction() {
      return sensitivityFunction;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((ParameterizedFunctionalCurveDefinition) bean).getName();
        case -868509005:  // xValueType
          return ((ParameterizedFunctionalCurveDefinition) bean).getXValueType();
        case -1065022510:  // yValueType
          return ((ParameterizedFunctionalCurveDefinition) bean).getYValueType();
        case 1905311443:  // dayCount
          return ((ParameterizedFunctionalCurveDefinition) bean).dayCount;
        case 104993457:  // nodes
          return ((ParameterizedFunctionalCurveDefinition) bean).getNodes();
        case -431632141:  // initialGuess
          return ((ParameterizedFunctionalCurveDefinition) bean).getInitialGuess();
        case -1169106440:  // parameterMetadata
          return ((ParameterizedFunctionalCurveDefinition) bean).getParameterMetadata();
        case 636119145:  // valueFunction
          return ((ParameterizedFunctionalCurveDefinition) bean).getValueFunction();
        case 1663351423:  // derivativeFunction
          return ((ParameterizedFunctionalCurveDefinition) bean).getDerivativeFunction();
        case -1353652329:  // sensitivityFunction
          return ((ParameterizedFunctionalCurveDefinition) bean).getSensitivityFunction();
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
   * The bean-builder for {@code ParameterizedFunctionalCurveDefinition}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ParameterizedFunctionalCurveDefinition> {

    private CurveName name;
    private ValueType xValueType;
    private ValueType yValueType;
    private DayCount dayCount;
    private List<? extends CurveNode> nodes = ImmutableList.of();
    private List<Double> initialGuess = ImmutableList.of();
    private List<? extends ParameterMetadata> parameterMetadata = ImmutableList.of();
    private BiFunction<DoubleArray, Double, Double> valueFunction;
    private BiFunction<DoubleArray, Double, Double> derivativeFunction;
    private BiFunction<DoubleArray, Double, DoubleArray> sensitivityFunction;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ParameterizedFunctionalCurveDefinition beanToCopy) {
      this.name = beanToCopy.getName();
      this.xValueType = beanToCopy.getXValueType();
      this.yValueType = beanToCopy.getYValueType();
      this.dayCount = beanToCopy.dayCount;
      this.nodes = beanToCopy.getNodes();
      this.initialGuess = beanToCopy.getInitialGuess();
      this.parameterMetadata = beanToCopy.getParameterMetadata();
      this.valueFunction = beanToCopy.getValueFunction();
      this.derivativeFunction = beanToCopy.getDerivativeFunction();
      this.sensitivityFunction = beanToCopy.getSensitivityFunction();
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
        case -431632141:  // initialGuess
          return initialGuess;
        case -1169106440:  // parameterMetadata
          return parameterMetadata;
        case 636119145:  // valueFunction
          return valueFunction;
        case 1663351423:  // derivativeFunction
          return derivativeFunction;
        case -1353652329:  // sensitivityFunction
          return sensitivityFunction;
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
        case -431632141:  // initialGuess
          this.initialGuess = (List<Double>) newValue;
          break;
        case -1169106440:  // parameterMetadata
          this.parameterMetadata = (List<? extends ParameterMetadata>) newValue;
          break;
        case 636119145:  // valueFunction
          this.valueFunction = (BiFunction<DoubleArray, Double, Double>) newValue;
          break;
        case 1663351423:  // derivativeFunction
          this.derivativeFunction = (BiFunction<DoubleArray, Double, Double>) newValue;
          break;
        case -1353652329:  // sensitivityFunction
          this.sensitivityFunction = (BiFunction<DoubleArray, Double, DoubleArray>) newValue;
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
    public ParameterizedFunctionalCurveDefinition build() {
      preBuild(this);
      return new ParameterizedFunctionalCurveDefinition(
          name,
          xValueType,
          yValueType,
          dayCount,
          nodes,
          initialGuess,
          parameterMetadata,
          valueFunction,
          derivativeFunction,
          sensitivityFunction);
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
     * Sets the nodes of the underlying instruments.
     * <p>
     * The nodes are used to find the quoted values to which the curve is calibrated.
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
     * Sets the initial guess values for the curve parameters.
     * <p>
     * The size must be the same as the number of the curve parameters.
     * @param initialGuess  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder initialGuess(List<Double> initialGuess) {
      JodaBeanUtils.notNull(initialGuess, "initialGuess");
      this.initialGuess = initialGuess;
      return this;
    }

    /**
     * Sets the {@code initialGuess} property in the builder
     * from an array of objects.
     * @param initialGuess  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder initialGuess(Double... initialGuess) {
      return initialGuess(ImmutableList.copyOf(initialGuess));
    }

    /**
     * Sets the parameter metadata of the curve, defaulted to empty metadata instances.
     * <p>
     * The size of the list must be the same as the number of the curve parameters.
     * @param parameterMetadata  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder parameterMetadata(List<? extends ParameterMetadata> parameterMetadata) {
      JodaBeanUtils.notNull(parameterMetadata, "parameterMetadata");
      this.parameterMetadata = parameterMetadata;
      return this;
    }

    /**
     * Sets the {@code parameterMetadata} property in the builder
     * from an array of objects.
     * @param parameterMetadata  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder parameterMetadata(ParameterMetadata... parameterMetadata) {
      return parameterMetadata(ImmutableList.copyOf(parameterMetadata));
    }

    /**
     * Sets the y-value function.
     * <p>
     * The function takes {@code parameters} and x-value, then returns y-value.
     * @param valueFunction  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valueFunction(BiFunction<DoubleArray, Double, Double> valueFunction) {
      JodaBeanUtils.notNull(valueFunction, "valueFunction");
      this.valueFunction = valueFunction;
      return this;
    }

    /**
     * Sets the derivative function.
     * <p>
     * The function takes {@code parameters} and x-value, then returns the first derivative of y-value with respective to x,
     * i.e., the gradient of the curve.
     * @param derivativeFunction  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder derivativeFunction(BiFunction<DoubleArray, Double, Double> derivativeFunction) {
      JodaBeanUtils.notNull(derivativeFunction, "derivativeFunction");
      this.derivativeFunction = derivativeFunction;
      return this;
    }

    /**
     * Sets the parameter sensitivity function.
     * <p>
     * The function takes {@code parameters} and x-value, then returns the sensitivities of y-value to the parameters.
     * @param sensitivityFunction  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder sensitivityFunction(BiFunction<DoubleArray, Double, DoubleArray> sensitivityFunction) {
      JodaBeanUtils.notNull(sensitivityFunction, "sensitivityFunction");
      this.sensitivityFunction = sensitivityFunction;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(352);
      buf.append("ParameterizedFunctionalCurveDefinition.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("xValueType").append('=').append(JodaBeanUtils.toString(xValueType)).append(',').append(' ');
      buf.append("yValueType").append('=').append(JodaBeanUtils.toString(yValueType)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("nodes").append('=').append(JodaBeanUtils.toString(nodes)).append(',').append(' ');
      buf.append("initialGuess").append('=').append(JodaBeanUtils.toString(initialGuess)).append(',').append(' ');
      buf.append("parameterMetadata").append('=').append(JodaBeanUtils.toString(parameterMetadata)).append(',').append(' ');
      buf.append("valueFunction").append('=').append(JodaBeanUtils.toString(valueFunction)).append(',').append(' ');
      buf.append("derivativeFunction").append('=').append(JodaBeanUtils.toString(derivativeFunction)).append(',').append(' ');
      buf.append("sensitivityFunction").append('=').append(JodaBeanUtils.toString(sensitivityFunction));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
