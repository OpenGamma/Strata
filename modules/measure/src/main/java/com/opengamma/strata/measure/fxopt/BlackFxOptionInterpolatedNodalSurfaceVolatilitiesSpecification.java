/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionSurfaceVolatilities;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilitiesName;
import com.opengamma.strata.pricer.fxopt.FxVolatilitySurfaceYearFractionParameterMetadata;

/**
 * The specification of how to build FX option volatilities. 
 * <p>
 * This is the specification for a single volatility object, {@link BlackFxOptionSurfaceVolatilities}. 
 * The underlying surface in the volatilities is {@code InterpolatedNodalSurface}.
 */
@BeanDefinition
public final class BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification
    implements FxOptionVolatilitiesSpecification, ImmutableBean, Serializable {

  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FxOptionVolatilitiesName name;

  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurrencyPair currencyPair;

  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;

  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ImmutableList<FxOptionVolatilitiesNode> nodes;
  /**
   * The interpolator used in the time dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveInterpolator timeInterpolator;
  /**
   * The left extrapolator used in the time dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator timeExtrapolatorLeft;
  /**
   * The right extrapolator used in the time dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator timeExtrapolatorRight;
  /**
   * The interpolator used in the strike dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveInterpolator strikeInterpolator;
  /**
   * The left extrapolator used in the strike dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator strikeExtrapolatorLeft;
  /**
   * The right extrapolator used in the strike dimension.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator strikeExtrapolatorRight;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    int nParams = nodes.size();
    for (int i = 0; i < nParams; ++i) {
      ArgChecker.isTrue(nodes.get(i).getCurrencyPair().equals(currencyPair), "Currency pair must be the same");
      ArgChecker.isTrue(nodes.get(i).getStrike() instanceof SimpleStrike, "Strike must be SimpleStrike");
      ArgChecker.isTrue(nodes.get(i).getQuoteValueType().equals(ValueType.BLACK_VOLATILITY),
          "Quote value type must be BLACK_VOLATILITY");
    }
  }

  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.strikeExtrapolatorLeft = FLAT;
    builder.strikeExtrapolatorRight = FLAT;
    builder.timeExtrapolatorLeft = FLAT;
    builder.timeExtrapolatorRight = FLAT;
  }

  //-------------------------------------------------------------------------
  @Override
  public BlackFxOptionSurfaceVolatilities volatilities(
      ZonedDateTime valuationDateTime,
      DoubleArray parameters,
      ReferenceData refData) {

    int nNodes = getParameterCount();
    ArgChecker.isTrue(parameters.size() == nNodes,
        Messages.format("size of parameters must be {}, but found {}", nNodes, parameters.size()));
    DoubleArray strikes = DoubleArray.of(nNodes, i -> nodes.get(i).getStrike().getValue());
    DoubleArray expiries = DoubleArray.of(nNodes, i -> nodes.get(i).timeToExpiry(valuationDateTime, dayCount, refData));
    SurfaceMetadata metadata = Surfaces.blackVolatilityByExpiryStrike(SurfaceName.of(name.getName()), dayCount)
        .withParameterMetadata(parameterMetadata(expiries));

    SurfaceInterpolator interp = GridSurfaceInterpolator.of(
        timeInterpolator,
        timeExtrapolatorLeft,
        timeExtrapolatorRight,
        strikeInterpolator,
        strikeExtrapolatorLeft,
        strikeExtrapolatorRight);
    InterpolatedNodalSurface surface = InterpolatedNodalSurface.ofUnsorted(metadata, expiries, strikes, parameters, interp);
    return BlackFxOptionSurfaceVolatilities.of(name, currencyPair, valuationDateTime, surface);
  }

  private ImmutableList<ParameterMetadata> parameterMetadata(DoubleArray expiries) {
    int nParams = nodes.size();
    return IntStream.range(0, nParams)
        .mapToObj(n -> FxVolatilitySurfaceYearFractionParameterMetadata.of(
            expiries.get(n), nodes.get(n).getStrike(), currencyPair))
        .collect(toImmutableList());
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification}.
   * @return the meta-bean, not null
   */
  public static BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.Meta meta() {
    return BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.Meta.INSTANCE;
  }

  static {
    MetaBean.register(BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.Builder builder() {
    return new BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.Builder();
  }

  private BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification(
      FxOptionVolatilitiesName name,
      CurrencyPair currencyPair,
      DayCount dayCount,
      List<FxOptionVolatilitiesNode> nodes,
      CurveInterpolator timeInterpolator,
      CurveExtrapolator timeExtrapolatorLeft,
      CurveExtrapolator timeExtrapolatorRight,
      CurveInterpolator strikeInterpolator,
      CurveExtrapolator strikeExtrapolatorLeft,
      CurveExtrapolator strikeExtrapolatorRight) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(currencyPair, "currencyPair");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(nodes, "nodes");
    JodaBeanUtils.notNull(timeInterpolator, "timeInterpolator");
    JodaBeanUtils.notNull(timeExtrapolatorLeft, "timeExtrapolatorLeft");
    JodaBeanUtils.notNull(timeExtrapolatorRight, "timeExtrapolatorRight");
    JodaBeanUtils.notNull(strikeInterpolator, "strikeInterpolator");
    JodaBeanUtils.notNull(strikeExtrapolatorLeft, "strikeExtrapolatorLeft");
    JodaBeanUtils.notNull(strikeExtrapolatorRight, "strikeExtrapolatorRight");
    this.name = name;
    this.currencyPair = currencyPair;
    this.dayCount = dayCount;
    this.nodes = ImmutableList.copyOf(nodes);
    this.timeInterpolator = timeInterpolator;
    this.timeExtrapolatorLeft = timeExtrapolatorLeft;
    this.timeExtrapolatorRight = timeExtrapolatorRight;
    this.strikeInterpolator = strikeInterpolator;
    this.strikeExtrapolatorLeft = strikeExtrapolatorLeft;
    this.strikeExtrapolatorRight = strikeExtrapolatorRight;
    validate();
  }

  @Override
  public BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.Meta metaBean() {
    return BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name.
   * @return the value of the property, not null
   */
  @Override
  public FxOptionVolatilitiesName getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currencyPair.
   * @return the value of the property, not null
   */
  @Override
  public CurrencyPair getCurrencyPair() {
    return currencyPair;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the dayCount.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the nodes.
   * @return the value of the property, not null
   */
  @Override
  public ImmutableList<FxOptionVolatilitiesNode> getNodes() {
    return nodes;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the interpolator used in the time dimension.
   * @return the value of the property, not null
   */
  public CurveInterpolator getTimeInterpolator() {
    return timeInterpolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the left extrapolator used in the time dimension.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getTimeExtrapolatorLeft() {
    return timeExtrapolatorLeft;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the right extrapolator used in the time dimension.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getTimeExtrapolatorRight() {
    return timeExtrapolatorRight;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the interpolator used in the strike dimension.
   * @return the value of the property, not null
   */
  public CurveInterpolator getStrikeInterpolator() {
    return strikeInterpolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the left extrapolator used in the strike dimension.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getStrikeExtrapolatorLeft() {
    return strikeExtrapolatorLeft;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the right extrapolator used in the strike dimension.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getStrikeExtrapolatorRight() {
    return strikeExtrapolatorRight;
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
      BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification other = (BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(currencyPair, other.currencyPair) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(nodes, other.nodes) &&
          JodaBeanUtils.equal(timeInterpolator, other.timeInterpolator) &&
          JodaBeanUtils.equal(timeExtrapolatorLeft, other.timeExtrapolatorLeft) &&
          JodaBeanUtils.equal(timeExtrapolatorRight, other.timeExtrapolatorRight) &&
          JodaBeanUtils.equal(strikeInterpolator, other.strikeInterpolator) &&
          JodaBeanUtils.equal(strikeExtrapolatorLeft, other.strikeExtrapolatorLeft) &&
          JodaBeanUtils.equal(strikeExtrapolatorRight, other.strikeExtrapolatorRight);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(currencyPair);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(nodes);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeInterpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeExtrapolatorLeft);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeExtrapolatorRight);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikeInterpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikeExtrapolatorLeft);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikeExtrapolatorRight);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification{");
    buf.append("name").append('=').append(name).append(',').append(' ');
    buf.append("currencyPair").append('=').append(currencyPair).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("nodes").append('=').append(nodes).append(',').append(' ');
    buf.append("timeInterpolator").append('=').append(timeInterpolator).append(',').append(' ');
    buf.append("timeExtrapolatorLeft").append('=').append(timeExtrapolatorLeft).append(',').append(' ');
    buf.append("timeExtrapolatorRight").append('=').append(timeExtrapolatorRight).append(',').append(' ');
    buf.append("strikeInterpolator").append('=').append(strikeInterpolator).append(',').append(' ');
    buf.append("strikeExtrapolatorLeft").append('=').append(strikeExtrapolatorLeft).append(',').append(' ');
    buf.append("strikeExtrapolatorRight").append('=').append(JodaBeanUtils.toString(strikeExtrapolatorRight));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<FxOptionVolatilitiesName> name = DirectMetaProperty.ofImmutable(
        this, "name", BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.class, FxOptionVolatilitiesName.class);
    /**
     * The meta-property for the {@code currencyPair} property.
     */
    private final MetaProperty<CurrencyPair> currencyPair = DirectMetaProperty.ofImmutable(
        this, "currencyPair", BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.class, DayCount.class);
    /**
     * The meta-property for the {@code nodes} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<FxOptionVolatilitiesNode>> nodes = DirectMetaProperty.ofImmutable(
        this, "nodes", BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code timeInterpolator} property.
     */
    private final MetaProperty<CurveInterpolator> timeInterpolator = DirectMetaProperty.ofImmutable(
        this, "timeInterpolator", BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code timeExtrapolatorLeft} property.
     */
    private final MetaProperty<CurveExtrapolator> timeExtrapolatorLeft = DirectMetaProperty.ofImmutable(
        this, "timeExtrapolatorLeft", BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code timeExtrapolatorRight} property.
     */
    private final MetaProperty<CurveExtrapolator> timeExtrapolatorRight = DirectMetaProperty.ofImmutable(
        this, "timeExtrapolatorRight", BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code strikeInterpolator} property.
     */
    private final MetaProperty<CurveInterpolator> strikeInterpolator = DirectMetaProperty.ofImmutable(
        this, "strikeInterpolator", BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.class, CurveInterpolator.class);
    /**
     * The meta-property for the {@code strikeExtrapolatorLeft} property.
     */
    private final MetaProperty<CurveExtrapolator> strikeExtrapolatorLeft = DirectMetaProperty.ofImmutable(
        this, "strikeExtrapolatorLeft", BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.class, CurveExtrapolator.class);
    /**
     * The meta-property for the {@code strikeExtrapolatorRight} property.
     */
    private final MetaProperty<CurveExtrapolator> strikeExtrapolatorRight = DirectMetaProperty.ofImmutable(
        this, "strikeExtrapolatorRight", BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.class, CurveExtrapolator.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "currencyPair",
        "dayCount",
        "nodes",
        "timeInterpolator",
        "timeExtrapolatorLeft",
        "timeExtrapolatorRight",
        "strikeInterpolator",
        "strikeExtrapolatorLeft",
        "strikeExtrapolatorRight");

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
        case 1005147787:  // currencyPair
          return currencyPair;
        case 1905311443:  // dayCount
          return dayCount;
        case 104993457:  // nodes
          return nodes;
        case -587914188:  // timeInterpolator
          return timeInterpolator;
        case -286652761:  // timeExtrapolatorLeft
          return timeExtrapolatorLeft;
        case -290640004:  // timeExtrapolatorRight
          return timeExtrapolatorRight;
        case 815202713:  // strikeInterpolator
          return strikeInterpolator;
        case -1176196724:  // strikeExtrapolatorLeft
          return strikeExtrapolatorLeft;
        case -2096699081:  // strikeExtrapolatorRight
          return strikeExtrapolatorRight;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.Builder builder() {
      return new BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.Builder();
    }

    @Override
    public Class<? extends BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification> beanType() {
      return BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.class;
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
    public MetaProperty<FxOptionVolatilitiesName> name() {
      return name;
    }

    /**
     * The meta-property for the {@code currencyPair} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyPair> currencyPair() {
      return currencyPair;
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
    public MetaProperty<ImmutableList<FxOptionVolatilitiesNode>> nodes() {
      return nodes;
    }

    /**
     * The meta-property for the {@code timeInterpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveInterpolator> timeInterpolator() {
      return timeInterpolator;
    }

    /**
     * The meta-property for the {@code timeExtrapolatorLeft} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> timeExtrapolatorLeft() {
      return timeExtrapolatorLeft;
    }

    /**
     * The meta-property for the {@code timeExtrapolatorRight} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> timeExtrapolatorRight() {
      return timeExtrapolatorRight;
    }

    /**
     * The meta-property for the {@code strikeInterpolator} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveInterpolator> strikeInterpolator() {
      return strikeInterpolator;
    }

    /**
     * The meta-property for the {@code strikeExtrapolatorLeft} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> strikeExtrapolatorLeft() {
      return strikeExtrapolatorLeft;
    }

    /**
     * The meta-property for the {@code strikeExtrapolatorRight} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurveExtrapolator> strikeExtrapolatorRight() {
      return strikeExtrapolatorRight;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification) bean).getName();
        case 1005147787:  // currencyPair
          return ((BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification) bean).getCurrencyPair();
        case 1905311443:  // dayCount
          return ((BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification) bean).getDayCount();
        case 104993457:  // nodes
          return ((BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification) bean).getNodes();
        case -587914188:  // timeInterpolator
          return ((BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification) bean).getTimeInterpolator();
        case -286652761:  // timeExtrapolatorLeft
          return ((BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification) bean).getTimeExtrapolatorLeft();
        case -290640004:  // timeExtrapolatorRight
          return ((BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification) bean).getTimeExtrapolatorRight();
        case 815202713:  // strikeInterpolator
          return ((BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification) bean).getStrikeInterpolator();
        case -1176196724:  // strikeExtrapolatorLeft
          return ((BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification) bean).getStrikeExtrapolatorLeft();
        case -2096699081:  // strikeExtrapolatorRight
          return ((BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification) bean).getStrikeExtrapolatorRight();
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
   * The bean-builder for {@code BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification> {

    private FxOptionVolatilitiesName name;
    private CurrencyPair currencyPair;
    private DayCount dayCount;
    private List<FxOptionVolatilitiesNode> nodes = ImmutableList.of();
    private CurveInterpolator timeInterpolator;
    private CurveExtrapolator timeExtrapolatorLeft;
    private CurveExtrapolator timeExtrapolatorRight;
    private CurveInterpolator strikeInterpolator;
    private CurveExtrapolator strikeExtrapolatorLeft;
    private CurveExtrapolator strikeExtrapolatorRight;

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
    private Builder(BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification beanToCopy) {
      this.name = beanToCopy.getName();
      this.currencyPair = beanToCopy.getCurrencyPair();
      this.dayCount = beanToCopy.getDayCount();
      this.nodes = beanToCopy.getNodes();
      this.timeInterpolator = beanToCopy.getTimeInterpolator();
      this.timeExtrapolatorLeft = beanToCopy.getTimeExtrapolatorLeft();
      this.timeExtrapolatorRight = beanToCopy.getTimeExtrapolatorRight();
      this.strikeInterpolator = beanToCopy.getStrikeInterpolator();
      this.strikeExtrapolatorLeft = beanToCopy.getStrikeExtrapolatorLeft();
      this.strikeExtrapolatorRight = beanToCopy.getStrikeExtrapolatorRight();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 1005147787:  // currencyPair
          return currencyPair;
        case 1905311443:  // dayCount
          return dayCount;
        case 104993457:  // nodes
          return nodes;
        case -587914188:  // timeInterpolator
          return timeInterpolator;
        case -286652761:  // timeExtrapolatorLeft
          return timeExtrapolatorLeft;
        case -290640004:  // timeExtrapolatorRight
          return timeExtrapolatorRight;
        case 815202713:  // strikeInterpolator
          return strikeInterpolator;
        case -1176196724:  // strikeExtrapolatorLeft
          return strikeExtrapolatorLeft;
        case -2096699081:  // strikeExtrapolatorRight
          return strikeExtrapolatorRight;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (FxOptionVolatilitiesName) newValue;
          break;
        case 1005147787:  // currencyPair
          this.currencyPair = (CurrencyPair) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 104993457:  // nodes
          this.nodes = (List<FxOptionVolatilitiesNode>) newValue;
          break;
        case -587914188:  // timeInterpolator
          this.timeInterpolator = (CurveInterpolator) newValue;
          break;
        case -286652761:  // timeExtrapolatorLeft
          this.timeExtrapolatorLeft = (CurveExtrapolator) newValue;
          break;
        case -290640004:  // timeExtrapolatorRight
          this.timeExtrapolatorRight = (CurveExtrapolator) newValue;
          break;
        case 815202713:  // strikeInterpolator
          this.strikeInterpolator = (CurveInterpolator) newValue;
          break;
        case -1176196724:  // strikeExtrapolatorLeft
          this.strikeExtrapolatorLeft = (CurveExtrapolator) newValue;
          break;
        case -2096699081:  // strikeExtrapolatorRight
          this.strikeExtrapolatorRight = (CurveExtrapolator) newValue;
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
    public BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification build() {
      return new BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification(
          name,
          currencyPair,
          dayCount,
          nodes,
          timeInterpolator,
          timeExtrapolatorLeft,
          timeExtrapolatorRight,
          strikeInterpolator,
          strikeExtrapolatorLeft,
          strikeExtrapolatorRight);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the name.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(FxOptionVolatilitiesName name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the currencyPair.
     * @param currencyPair  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currencyPair(CurrencyPair currencyPair) {
      JodaBeanUtils.notNull(currencyPair, "currencyPair");
      this.currencyPair = currencyPair;
      return this;
    }

    /**
     * Sets the dayCount.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets the nodes.
     * @param nodes  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder nodes(List<FxOptionVolatilitiesNode> nodes) {
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
    public Builder nodes(FxOptionVolatilitiesNode... nodes) {
      return nodes(ImmutableList.copyOf(nodes));
    }

    /**
     * Sets the interpolator used in the time dimension.
     * @param timeInterpolator  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder timeInterpolator(CurveInterpolator timeInterpolator) {
      JodaBeanUtils.notNull(timeInterpolator, "timeInterpolator");
      this.timeInterpolator = timeInterpolator;
      return this;
    }

    /**
     * Sets the left extrapolator used in the time dimension.
     * @param timeExtrapolatorLeft  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder timeExtrapolatorLeft(CurveExtrapolator timeExtrapolatorLeft) {
      JodaBeanUtils.notNull(timeExtrapolatorLeft, "timeExtrapolatorLeft");
      this.timeExtrapolatorLeft = timeExtrapolatorLeft;
      return this;
    }

    /**
     * Sets the right extrapolator used in the time dimension.
     * @param timeExtrapolatorRight  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder timeExtrapolatorRight(CurveExtrapolator timeExtrapolatorRight) {
      JodaBeanUtils.notNull(timeExtrapolatorRight, "timeExtrapolatorRight");
      this.timeExtrapolatorRight = timeExtrapolatorRight;
      return this;
    }

    /**
     * Sets the interpolator used in the strike dimension.
     * @param strikeInterpolator  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder strikeInterpolator(CurveInterpolator strikeInterpolator) {
      JodaBeanUtils.notNull(strikeInterpolator, "strikeInterpolator");
      this.strikeInterpolator = strikeInterpolator;
      return this;
    }

    /**
     * Sets the left extrapolator used in the strike dimension.
     * @param strikeExtrapolatorLeft  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder strikeExtrapolatorLeft(CurveExtrapolator strikeExtrapolatorLeft) {
      JodaBeanUtils.notNull(strikeExtrapolatorLeft, "strikeExtrapolatorLeft");
      this.strikeExtrapolatorLeft = strikeExtrapolatorLeft;
      return this;
    }

    /**
     * Sets the right extrapolator used in the strike dimension.
     * @param strikeExtrapolatorRight  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder strikeExtrapolatorRight(CurveExtrapolator strikeExtrapolatorRight) {
      JodaBeanUtils.notNull(strikeExtrapolatorRight, "strikeExtrapolatorRight");
      this.strikeExtrapolatorRight = strikeExtrapolatorRight;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(352);
      buf.append("BlackFxOptionInterpolatedNodalSurfaceVolatilitiesSpecification.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(currencyPair)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("nodes").append('=').append(JodaBeanUtils.toString(nodes)).append(',').append(' ');
      buf.append("timeInterpolator").append('=').append(JodaBeanUtils.toString(timeInterpolator)).append(',').append(' ');
      buf.append("timeExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(timeExtrapolatorLeft)).append(',').append(' ');
      buf.append("timeExtrapolatorRight").append('=').append(JodaBeanUtils.toString(timeExtrapolatorRight)).append(',').append(' ');
      buf.append("strikeInterpolator").append('=').append(JodaBeanUtils.toString(strikeInterpolator)).append(',').append(' ');
      buf.append("strikeExtrapolatorLeft").append('=').append(JodaBeanUtils.toString(strikeExtrapolatorLeft)).append(',').append(' ');
      buf.append("strikeExtrapolatorRight").append('=').append(JodaBeanUtils.toString(strikeExtrapolatorRight));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
