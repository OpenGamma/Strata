/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.Lists;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.product.credit.type.IsdaYieldCurveConvention;

/**
 * The par rates used when calibrating an ISDA yield curve.
 */
@BeanDefinition(builderScope = "private")
public final class IsdaYieldCurveInputs
    implements ImmutableBean, Serializable {
  // TODO replace arrays with lists

  /**
   * The curve name.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveName name;
  /**
   * The tenor at each curve node.
   */
  @PropertyDefinition(validate = "notNull")
  private final Period[] yieldCurvePoints;
  /**
   * The end date at each curve node.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate[] endDatePoints;
  /**
   * The instrument type at each curve node.
   */
  @PropertyDefinition(validate = "notNull")
  private final IsdaYieldCurveUnderlyingType[] yieldCurveInstruments;
  /**
   * The par rate at each curve node.
   */
  @PropertyDefinition(validate = "notNull")
  private final double[] parRates;
  /**
   * The underlying convention.
   */
  @PropertyDefinition(validate = "notNull")
  private final IsdaYieldCurveConvention curveConvention;

  //-------------------------------------------------------------------------
  /**
   * Provides curve meta data to capture tenor and anchor point date information.
   * 
   * @return the curve metadata
   */
  public CurveMetadata getCurveMetaData() {
    List<ParameterMetadata> parameters = Lists.newArrayList();
    for (int i = 0; i < yieldCurvePoints.length; i++) {
      TenorDateParameterMetadata pointData = TenorDateParameterMetadata.of(endDatePoints[i], Tenor.of(yieldCurvePoints[i]));
      parameters.add(pointData);
    }
    return DefaultCurveMetadata.builder()
        .curveName(name)
        .parameterMetadata(parameters)
        .build();
  }

  /**
   * Creates an instance of the par rates.
   * 
   * @param name  the curve name
   * @param yieldCurvePoints  the tenor at each curve node
   * @param endDatePoints  the end dates
   * @param yieldCurveInstruments  the instrument type at each curve node
   * @param parRates  the par rate at each curve node
   * @param curveConvention  the underlying convention
   * @return the par rates
   */
  public static IsdaYieldCurveInputs of(
      CurveName name,
      Period[] yieldCurvePoints,
      LocalDate[] endDatePoints,
      IsdaYieldCurveUnderlyingType[] yieldCurveInstruments,
      double[] parRates,
      IsdaYieldCurveConvention curveConvention) {

    return new IsdaYieldCurveInputs(
        name,
        yieldCurvePoints,
        endDatePoints,
        yieldCurveInstruments,
        parRates,
        curveConvention);
  }

  @ImmutableValidator
  private void validate() {
    if (yieldCurvePoints.length <= 0) {
      throw new IllegalArgumentException("Cannot have zero points");
    }
    if (yieldCurvePoints.length != yieldCurveInstruments.length || yieldCurvePoints.length != parRates.length) {
      throw new IllegalArgumentException("Points do not line up");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Applies a parallel shift to all the nodes.
   * 
   * @param shift  the shift to apply
   * @return the bumped instance
   */
  public IsdaYieldCurveInputs parallelShiftParRatesinBps(double shift) {
    double[] shiftedRates = parRates.clone();
    for (int i = 0; i < shiftedRates.length; i++) {
      shiftedRates[i] = shiftedRates[i] + shift;
    }
    return applyShift(shiftedRates);
  }

  /**
   * Applies a bucketed shift to a single node.
   * 
   * @param index  the index of the node to shift
   * @param shift  the shift to apply
   * @return the bumped instance
   */
  public IsdaYieldCurveInputs bucketedShiftParRatesinBps(int index, double shift) {
    double[] shiftedRates = parRates.clone();
    shiftedRates[index] = shiftedRates[index] + shift;
    return applyShift(shiftedRates);
  }

  /**
   * Gets the number of nodes.
   * 
   * @return the number of points
   */
  public int getNumberOfPoints() {
    return yieldCurvePoints.length;
  }

  // applies the shift
  private IsdaYieldCurveInputs applyShift(double[] shiftedRates) {
    return IsdaYieldCurveInputs.of(
        name,
        yieldCurvePoints.clone(),
        endDatePoints.clone(),
        yieldCurveInstruments.clone(),
        shiftedRates,
        curveConvention);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IsdaYieldCurveInputs}.
   * @return the meta-bean, not null
   */
  public static IsdaYieldCurveInputs.Meta meta() {
    return IsdaYieldCurveInputs.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IsdaYieldCurveInputs.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private IsdaYieldCurveInputs(
      CurveName name,
      Period[] yieldCurvePoints,
      LocalDate[] endDatePoints,
      IsdaYieldCurveUnderlyingType[] yieldCurveInstruments,
      double[] parRates,
      IsdaYieldCurveConvention curveConvention) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(yieldCurvePoints, "yieldCurvePoints");
    JodaBeanUtils.notNull(endDatePoints, "endDatePoints");
    JodaBeanUtils.notNull(yieldCurveInstruments, "yieldCurveInstruments");
    JodaBeanUtils.notNull(parRates, "parRates");
    JodaBeanUtils.notNull(curveConvention, "curveConvention");
    this.name = name;
    this.yieldCurvePoints = yieldCurvePoints;
    this.endDatePoints = endDatePoints;
    this.yieldCurveInstruments = yieldCurveInstruments;
    this.parRates = parRates.clone();
    this.curveConvention = curveConvention;
    validate();
  }

  @Override
  public IsdaYieldCurveInputs.Meta metaBean() {
    return IsdaYieldCurveInputs.Meta.INSTANCE;
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
  public CurveName getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the tenor at each curve node.
   * @return the value of the property, not null
   */
  public Period[] getYieldCurvePoints() {
    return yieldCurvePoints;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date at each curve node.
   * @return the value of the property, not null
   */
  public LocalDate[] getEndDatePoints() {
    return endDatePoints;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the instrument type at each curve node.
   * @return the value of the property, not null
   */
  public IsdaYieldCurveUnderlyingType[] getYieldCurveInstruments() {
    return yieldCurveInstruments;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the par rate at each curve node.
   * @return the value of the property, not null
   */
  public double[] getParRates() {
    return parRates.clone();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying convention.
   * @return the value of the property, not null
   */
  public IsdaYieldCurveConvention getCurveConvention() {
    return curveConvention;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IsdaYieldCurveInputs other = (IsdaYieldCurveInputs) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(yieldCurvePoints, other.yieldCurvePoints) &&
          JodaBeanUtils.equal(endDatePoints, other.endDatePoints) &&
          JodaBeanUtils.equal(yieldCurveInstruments, other.yieldCurveInstruments) &&
          JodaBeanUtils.equal(parRates, other.parRates) &&
          JodaBeanUtils.equal(curveConvention, other.curveConvention);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(yieldCurvePoints);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDatePoints);
    hash = hash * 31 + JodaBeanUtils.hashCode(yieldCurveInstruments);
    hash = hash * 31 + JodaBeanUtils.hashCode(parRates);
    hash = hash * 31 + JodaBeanUtils.hashCode(curveConvention);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("IsdaYieldCurveInputs{");
    buf.append("name").append('=').append(name).append(',').append(' ');
    buf.append("yieldCurvePoints").append('=').append(yieldCurvePoints).append(',').append(' ');
    buf.append("endDatePoints").append('=').append(endDatePoints).append(',').append(' ');
    buf.append("yieldCurveInstruments").append('=').append(yieldCurveInstruments).append(',').append(' ');
    buf.append("parRates").append('=').append(parRates).append(',').append(' ');
    buf.append("curveConvention").append('=').append(JodaBeanUtils.toString(curveConvention));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IsdaYieldCurveInputs}.
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
        this, "name", IsdaYieldCurveInputs.class, CurveName.class);
    /**
     * The meta-property for the {@code yieldCurvePoints} property.
     */
    private final MetaProperty<Period[]> yieldCurvePoints = DirectMetaProperty.ofImmutable(
        this, "yieldCurvePoints", IsdaYieldCurveInputs.class, Period[].class);
    /**
     * The meta-property for the {@code endDatePoints} property.
     */
    private final MetaProperty<LocalDate[]> endDatePoints = DirectMetaProperty.ofImmutable(
        this, "endDatePoints", IsdaYieldCurveInputs.class, LocalDate[].class);
    /**
     * The meta-property for the {@code yieldCurveInstruments} property.
     */
    private final MetaProperty<IsdaYieldCurveUnderlyingType[]> yieldCurveInstruments = DirectMetaProperty.ofImmutable(
        this, "yieldCurveInstruments", IsdaYieldCurveInputs.class, IsdaYieldCurveUnderlyingType[].class);
    /**
     * The meta-property for the {@code parRates} property.
     */
    private final MetaProperty<double[]> parRates = DirectMetaProperty.ofImmutable(
        this, "parRates", IsdaYieldCurveInputs.class, double[].class);
    /**
     * The meta-property for the {@code curveConvention} property.
     */
    private final MetaProperty<IsdaYieldCurveConvention> curveConvention = DirectMetaProperty.ofImmutable(
        this, "curveConvention", IsdaYieldCurveInputs.class, IsdaYieldCurveConvention.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "yieldCurvePoints",
        "endDatePoints",
        "yieldCurveInstruments",
        "parRates",
        "curveConvention");

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
        case 695376101:  // yieldCurvePoints
          return yieldCurvePoints;
        case 578522476:  // endDatePoints
          return endDatePoints;
        case -1469575510:  // yieldCurveInstruments
          return yieldCurveInstruments;
        case 1157229426:  // parRates
          return parRates;
        case 1796217280:  // curveConvention
          return curveConvention;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends IsdaYieldCurveInputs> builder() {
      return new IsdaYieldCurveInputs.Builder();
    }

    @Override
    public Class<? extends IsdaYieldCurveInputs> beanType() {
      return IsdaYieldCurveInputs.class;
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
     * The meta-property for the {@code yieldCurvePoints} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Period[]> yieldCurvePoints() {
      return yieldCurvePoints;
    }

    /**
     * The meta-property for the {@code endDatePoints} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate[]> endDatePoints() {
      return endDatePoints;
    }

    /**
     * The meta-property for the {@code yieldCurveInstruments} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IsdaYieldCurveUnderlyingType[]> yieldCurveInstruments() {
      return yieldCurveInstruments;
    }

    /**
     * The meta-property for the {@code parRates} property.
     * @return the meta-property, not null
     */
    public MetaProperty<double[]> parRates() {
      return parRates;
    }

    /**
     * The meta-property for the {@code curveConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IsdaYieldCurveConvention> curveConvention() {
      return curveConvention;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((IsdaYieldCurveInputs) bean).getName();
        case 695376101:  // yieldCurvePoints
          return ((IsdaYieldCurveInputs) bean).getYieldCurvePoints();
        case 578522476:  // endDatePoints
          return ((IsdaYieldCurveInputs) bean).getEndDatePoints();
        case -1469575510:  // yieldCurveInstruments
          return ((IsdaYieldCurveInputs) bean).getYieldCurveInstruments();
        case 1157229426:  // parRates
          return ((IsdaYieldCurveInputs) bean).getParRates();
        case 1796217280:  // curveConvention
          return ((IsdaYieldCurveInputs) bean).getCurveConvention();
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
   * The bean-builder for {@code IsdaYieldCurveInputs}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<IsdaYieldCurveInputs> {

    private CurveName name;
    private Period[] yieldCurvePoints;
    private LocalDate[] endDatePoints;
    private IsdaYieldCurveUnderlyingType[] yieldCurveInstruments;
    private double[] parRates;
    private IsdaYieldCurveConvention curveConvention;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 695376101:  // yieldCurvePoints
          return yieldCurvePoints;
        case 578522476:  // endDatePoints
          return endDatePoints;
        case -1469575510:  // yieldCurveInstruments
          return yieldCurveInstruments;
        case 1157229426:  // parRates
          return parRates;
        case 1796217280:  // curveConvention
          return curveConvention;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (CurveName) newValue;
          break;
        case 695376101:  // yieldCurvePoints
          this.yieldCurvePoints = (Period[]) newValue;
          break;
        case 578522476:  // endDatePoints
          this.endDatePoints = (LocalDate[]) newValue;
          break;
        case -1469575510:  // yieldCurveInstruments
          this.yieldCurveInstruments = (IsdaYieldCurveUnderlyingType[]) newValue;
          break;
        case 1157229426:  // parRates
          this.parRates = (double[]) newValue;
          break;
        case 1796217280:  // curveConvention
          this.curveConvention = (IsdaYieldCurveConvention) newValue;
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
    public IsdaYieldCurveInputs build() {
      return new IsdaYieldCurveInputs(
          name,
          yieldCurvePoints,
          endDatePoints,
          yieldCurveInstruments,
          parRates,
          curveConvention);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("IsdaYieldCurveInputs.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("yieldCurvePoints").append('=').append(JodaBeanUtils.toString(yieldCurvePoints)).append(',').append(' ');
      buf.append("endDatePoints").append('=').append(JodaBeanUtils.toString(endDatePoints)).append(',').append(' ');
      buf.append("yieldCurveInstruments").append('=').append(JodaBeanUtils.toString(yieldCurveInstruments)).append(',').append(' ');
      buf.append("parRates").append('=').append(JodaBeanUtils.toString(parRates)).append(',').append(' ');
      buf.append("curveConvention").append('=').append(JodaBeanUtils.toString(curveConvention));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
