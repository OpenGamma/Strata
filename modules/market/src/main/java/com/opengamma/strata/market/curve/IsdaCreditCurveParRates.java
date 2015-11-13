/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

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
import com.opengamma.strata.product.credit.type.CdsConvention;

/**
 * The par rates used when calibrating an ISDA credit curve.
 */
@BeanDefinition(builderScope = "private")
public final class IsdaCreditCurveParRates
    implements ImmutableBean, Serializable {
  // TODO the recovery rate is not really a part of the curve, but the data is available along side when
  // TODO as parsing the curves, so it is convenient to put it here for the moment.
  // TODO the scaling factor is the index factor and not really part of the curve
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
  private final Period[] creditCurvePoints;
  /**
   * The end date at each curve node.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate[] endDatePoints;
  /**
   * The par rate at each curve node.
   */
  @PropertyDefinition(validate = "notNull")
  private final double[] parRates;
  /**
   * The underlying convention.
   */
  @PropertyDefinition(validate = "notNull")
  private final CdsConvention cdsConvention;
  /**
   * The scaling factor.
   */
  @PropertyDefinition(validate = "notNull")
  private final double scalingFactor;

  //-------------------------------------------------------------------------
  /**
   * Provide curve meta data to capture tenor and anchor point date information
   * @return curve metadata
   */
  public CurveMetadata getCurveMetaData() {
    List<CurveParameterMetadata> parameters = Lists.newArrayList();
    for (int i = 0; i < creditCurvePoints.length; i++) {
      TenorCurveNodeMetadata pointData = TenorCurveNodeMetadata.of(endDatePoints[i], Tenor.of(creditCurvePoints[i]));
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
   * @param creditCurvePoints  the tenor at each curve node
   * @param parRates  the par rate at each curve node
   * @param endDatePoints  the end date at each curve node
   * @param cdsConvention  the underlying convention
   * @param scalingFactor  the scaling factor
   * @return the par rates
   */
  public static IsdaCreditCurveParRates of(
      CurveName name,
      Period[] creditCurvePoints,
      LocalDate[] endDatePoints,
      double[] parRates,
      CdsConvention cdsConvention,
      double scalingFactor) {

    return new IsdaCreditCurveParRates(
        name,
        creditCurvePoints,
        endDatePoints,
        parRates,
        cdsConvention,
        scalingFactor);
  }

  @ImmutableValidator
  private void validate() {
    if (creditCurvePoints.length <= 0) {
      throw new IllegalArgumentException("Cannot have zero points");
    }
    if (creditCurvePoints.length != parRates.length) {
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
  public IsdaCreditCurveParRates parallelShiftParRatesinBps(double shift) {
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
  public IsdaCreditCurveParRates bucketedShiftParRatesinBps(int index, double shift) {
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
    return creditCurvePoints.length;
  }

  // applies the shift
  private IsdaCreditCurveParRates applyShift(double[] shiftedRates) {
    return IsdaCreditCurveParRates.of(
        name,
        creditCurvePoints.clone(),
        endDatePoints.clone(),
        shiftedRates,
        cdsConvention,
        scalingFactor);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IsdaCreditCurveParRates}.
   * @return the meta-bean, not null
   */
  public static IsdaCreditCurveParRates.Meta meta() {
    return IsdaCreditCurveParRates.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IsdaCreditCurveParRates.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private IsdaCreditCurveParRates(
      CurveName name,
      Period[] creditCurvePoints,
      LocalDate[] endDatePoints,
      double[] parRates,
      CdsConvention cdsConvention,
      double scalingFactor) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(creditCurvePoints, "creditCurvePoints");
    JodaBeanUtils.notNull(endDatePoints, "endDatePoints");
    JodaBeanUtils.notNull(parRates, "parRates");
    JodaBeanUtils.notNull(cdsConvention, "cdsConvention");
    JodaBeanUtils.notNull(scalingFactor, "scalingFactor");
    this.name = name;
    this.creditCurvePoints = creditCurvePoints;
    this.endDatePoints = endDatePoints;
    this.parRates = parRates.clone();
    this.cdsConvention = cdsConvention;
    this.scalingFactor = scalingFactor;
    validate();
  }

  @Override
  public IsdaCreditCurveParRates.Meta metaBean() {
    return IsdaCreditCurveParRates.Meta.INSTANCE;
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
  public Period[] getCreditCurvePoints() {
    return creditCurvePoints;
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
  public CdsConvention getCdsConvention() {
    return cdsConvention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the scaling factor.
   * @return the value of the property, not null
   */
  public double getScalingFactor() {
    return scalingFactor;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IsdaCreditCurveParRates other = (IsdaCreditCurveParRates) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(creditCurvePoints, other.creditCurvePoints) &&
          JodaBeanUtils.equal(endDatePoints, other.endDatePoints) &&
          JodaBeanUtils.equal(parRates, other.parRates) &&
          JodaBeanUtils.equal(cdsConvention, other.cdsConvention) &&
          JodaBeanUtils.equal(scalingFactor, other.scalingFactor);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(creditCurvePoints);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDatePoints);
    hash = hash * 31 + JodaBeanUtils.hashCode(parRates);
    hash = hash * 31 + JodaBeanUtils.hashCode(cdsConvention);
    hash = hash * 31 + JodaBeanUtils.hashCode(scalingFactor);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("IsdaCreditCurveParRates{");
    buf.append("name").append('=').append(name).append(',').append(' ');
    buf.append("creditCurvePoints").append('=').append(creditCurvePoints).append(',').append(' ');
    buf.append("endDatePoints").append('=').append(endDatePoints).append(',').append(' ');
    buf.append("parRates").append('=').append(parRates).append(',').append(' ');
    buf.append("cdsConvention").append('=').append(cdsConvention).append(',').append(' ');
    buf.append("scalingFactor").append('=').append(JodaBeanUtils.toString(scalingFactor));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IsdaCreditCurveParRates}.
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
        this, "name", IsdaCreditCurveParRates.class, CurveName.class);
    /**
     * The meta-property for the {@code creditCurvePoints} property.
     */
    private final MetaProperty<Period[]> creditCurvePoints = DirectMetaProperty.ofImmutable(
        this, "creditCurvePoints", IsdaCreditCurveParRates.class, Period[].class);
    /**
     * The meta-property for the {@code endDatePoints} property.
     */
    private final MetaProperty<LocalDate[]> endDatePoints = DirectMetaProperty.ofImmutable(
        this, "endDatePoints", IsdaCreditCurveParRates.class, LocalDate[].class);
    /**
     * The meta-property for the {@code parRates} property.
     */
    private final MetaProperty<double[]> parRates = DirectMetaProperty.ofImmutable(
        this, "parRates", IsdaCreditCurveParRates.class, double[].class);
    /**
     * The meta-property for the {@code cdsConvention} property.
     */
    private final MetaProperty<CdsConvention> cdsConvention = DirectMetaProperty.ofImmutable(
        this, "cdsConvention", IsdaCreditCurveParRates.class, CdsConvention.class);
    /**
     * The meta-property for the {@code scalingFactor} property.
     */
    private final MetaProperty<Double> scalingFactor = DirectMetaProperty.ofImmutable(
        this, "scalingFactor", IsdaCreditCurveParRates.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "creditCurvePoints",
        "endDatePoints",
        "parRates",
        "cdsConvention",
        "scalingFactor");

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
        case -1771294215:  // creditCurvePoints
          return creditCurvePoints;
        case 578522476:  // endDatePoints
          return endDatePoints;
        case 1157229426:  // parRates
          return parRates;
        case 288334147:  // cdsConvention
          return cdsConvention;
        case -794828874:  // scalingFactor
          return scalingFactor;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends IsdaCreditCurveParRates> builder() {
      return new IsdaCreditCurveParRates.Builder();
    }

    @Override
    public Class<? extends IsdaCreditCurveParRates> beanType() {
      return IsdaCreditCurveParRates.class;
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
     * The meta-property for the {@code creditCurvePoints} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Period[]> creditCurvePoints() {
      return creditCurvePoints;
    }

    /**
     * The meta-property for the {@code endDatePoints} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate[]> endDatePoints() {
      return endDatePoints;
    }

    /**
     * The meta-property for the {@code parRates} property.
     * @return the meta-property, not null
     */
    public MetaProperty<double[]> parRates() {
      return parRates;
    }

    /**
     * The meta-property for the {@code cdsConvention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CdsConvention> cdsConvention() {
      return cdsConvention;
    }

    /**
     * The meta-property for the {@code scalingFactor} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> scalingFactor() {
      return scalingFactor;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((IsdaCreditCurveParRates) bean).getName();
        case -1771294215:  // creditCurvePoints
          return ((IsdaCreditCurveParRates) bean).getCreditCurvePoints();
        case 578522476:  // endDatePoints
          return ((IsdaCreditCurveParRates) bean).getEndDatePoints();
        case 1157229426:  // parRates
          return ((IsdaCreditCurveParRates) bean).getParRates();
        case 288334147:  // cdsConvention
          return ((IsdaCreditCurveParRates) bean).getCdsConvention();
        case -794828874:  // scalingFactor
          return ((IsdaCreditCurveParRates) bean).getScalingFactor();
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
   * The bean-builder for {@code IsdaCreditCurveParRates}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<IsdaCreditCurveParRates> {

    private CurveName name;
    private Period[] creditCurvePoints;
    private LocalDate[] endDatePoints;
    private double[] parRates;
    private CdsConvention cdsConvention;
    private double scalingFactor;

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
        case -1771294215:  // creditCurvePoints
          return creditCurvePoints;
        case 578522476:  // endDatePoints
          return endDatePoints;
        case 1157229426:  // parRates
          return parRates;
        case 288334147:  // cdsConvention
          return cdsConvention;
        case -794828874:  // scalingFactor
          return scalingFactor;
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
        case -1771294215:  // creditCurvePoints
          this.creditCurvePoints = (Period[]) newValue;
          break;
        case 578522476:  // endDatePoints
          this.endDatePoints = (LocalDate[]) newValue;
          break;
        case 1157229426:  // parRates
          this.parRates = (double[]) newValue;
          break;
        case 288334147:  // cdsConvention
          this.cdsConvention = (CdsConvention) newValue;
          break;
        case -794828874:  // scalingFactor
          this.scalingFactor = (Double) newValue;
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
    public IsdaCreditCurveParRates build() {
      return new IsdaCreditCurveParRates(
          name,
          creditCurvePoints,
          endDatePoints,
          parRates,
          cdsConvention,
          scalingFactor);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("IsdaCreditCurveParRates.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("creditCurvePoints").append('=').append(JodaBeanUtils.toString(creditCurvePoints)).append(',').append(' ');
      buf.append("endDatePoints").append('=').append(JodaBeanUtils.toString(endDatePoints)).append(',').append(' ');
      buf.append("parRates").append('=').append(JodaBeanUtils.toString(parRates)).append(',').append(' ');
      buf.append("cdsConvention").append('=').append(JodaBeanUtils.toString(cdsConvention)).append(',').append(' ');
      buf.append("scalingFactor").append('=').append(JodaBeanUtils.toString(scalingFactor));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
