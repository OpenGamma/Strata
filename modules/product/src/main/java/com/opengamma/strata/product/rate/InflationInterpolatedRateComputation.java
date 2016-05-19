/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import java.io.Serializable;
import java.time.YearMonth;
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

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.index.PriceIndexObservation;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Defines the computation of inflation figures from a price index with interpolation.
 * <p>
 * A price index is typically published monthly and has a delay before publication.
 * The rate observed by this instance will be based on four observations of the index,
 * two relative to the accrual start date and two relative to the accrual end date.
 * Linear interpolation based on the number of days of the payment month is used
 * to find the appropriate value for each pair of observations.
 */
@BeanDefinition(builderScope = "private")
public final class InflationInterpolatedRateComputation
    implements RateComputation, ImmutableBean, Serializable {

  /**
   * The observation at the start.
   * <p>
   * The inflation rate is the ratio between the interpolated start and end observations.
   * The start month is typically three months before the start of the period.
   */
  @PropertyDefinition(validate = "notNull")
  private final PriceIndexObservation startObservation;
  /**
   * The observation for interpolation at the start.
   * <p>
   * The inflation rate is the ratio between the interpolated start and end observations.
   * The month is typically one month after the month of the start observation.
   */
  @PropertyDefinition(validate = "notNull")
  private final PriceIndexObservation startSecondObservation;
  /**
   * The observation at the end.
   * <p>
   * The inflation rate is the ratio between the interpolated start and end observations.
   * The end month is typically three months before the end of the period.
   */
  @PropertyDefinition(validate = "notNull")
  private final PriceIndexObservation endObservation;
  /**
   * The observation for interpolation at the end.
   * <p>
   * The inflation rate is the ratio between the interpolated start and end observations.
   * The month is typically one month after the month of the end observation.
   */
  @PropertyDefinition(validate = "notNull")
  private final PriceIndexObservation endSecondObservation;
  /**
   * The positive weight used when interpolating.
   * <p>
   * Given two price index observations, typically in adjacent months, the weight is used
   * to determine the adjusted index value. The value is given by the formula
   * {@code (weight * price_index_1 + (1 - weight) * price_index_2)}.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double weight;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance from an index, reference start month and reference end month.
   * <p>
   * The second start/end observations will be one month later than the start/end month.
   * 
   * @param index  the index
   * @param referenceStartMonth  the reference start month
   * @param referenceEndMonth  the reference end month
   * @param weight  the weight
   * @return the inflation rate computation
   */
  public static InflationInterpolatedRateComputation of(
      PriceIndex index,
      YearMonth referenceStartMonth,
      YearMonth referenceEndMonth,
      double weight) {

    return new InflationInterpolatedRateComputation(
        PriceIndexObservation.of(index, referenceStartMonth),
        PriceIndexObservation.of(index, referenceStartMonth.plusMonths(1)),
        PriceIndexObservation.of(index, referenceEndMonth),
        PriceIndexObservation.of(index, referenceEndMonth.plusMonths(1)),
        weight);
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(
        startObservation.getIndex().equals(endObservation.getIndex()), "All observations must be for the same index");
    ArgChecker.isTrue(
        startObservation.getIndex().equals(startSecondObservation.getIndex()), "All observations must be for the same index");
    ArgChecker.isTrue(
        startObservation.getIndex().equals(endSecondObservation.getIndex()), "All observations must be for the same index");
    ArgChecker.inOrderNotEqual(
        startObservation.getFixingMonth(), startSecondObservation.getFixingMonth(), "startObservation", "startSecondObservation");
    ArgChecker.inOrderOrEqual(
        startSecondObservation.getFixingMonth(), endObservation.getFixingMonth(), "startSecondObservation", "endObservation");
    ArgChecker.inOrderNotEqual(
        endObservation.getFixingMonth(), endSecondObservation.getFixingMonth(), "endObservation", "endSecondObservation");
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the Price index.
   * 
   * @return the Price index
   */
  public PriceIndex getIndex() {
    return startObservation.getIndex();
  }

  //-------------------------------------------------------------------------
  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    builder.add(getIndex());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code InflationInterpolatedRateComputation}.
   * @return the meta-bean, not null
   */
  public static InflationInterpolatedRateComputation.Meta meta() {
    return InflationInterpolatedRateComputation.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(InflationInterpolatedRateComputation.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private InflationInterpolatedRateComputation(
      PriceIndexObservation startObservation,
      PriceIndexObservation startSecondObservation,
      PriceIndexObservation endObservation,
      PriceIndexObservation endSecondObservation,
      double weight) {
    JodaBeanUtils.notNull(startObservation, "startObservation");
    JodaBeanUtils.notNull(startSecondObservation, "startSecondObservation");
    JodaBeanUtils.notNull(endObservation, "endObservation");
    JodaBeanUtils.notNull(endSecondObservation, "endSecondObservation");
    ArgChecker.notNegative(weight, "weight");
    this.startObservation = startObservation;
    this.startSecondObservation = startSecondObservation;
    this.endObservation = endObservation;
    this.endSecondObservation = endSecondObservation;
    this.weight = weight;
    validate();
  }

  @Override
  public InflationInterpolatedRateComputation.Meta metaBean() {
    return InflationInterpolatedRateComputation.Meta.INSTANCE;
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
   * Gets the observation at the start.
   * <p>
   * The inflation rate is the ratio between the interpolated start and end observations.
   * The start month is typically three months before the start of the period.
   * @return the value of the property, not null
   */
  public PriceIndexObservation getStartObservation() {
    return startObservation;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the observation for interpolation at the start.
   * <p>
   * The inflation rate is the ratio between the interpolated start and end observations.
   * The month is typically one month after the month of the start observation.
   * @return the value of the property, not null
   */
  public PriceIndexObservation getStartSecondObservation() {
    return startSecondObservation;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the observation at the end.
   * <p>
   * The inflation rate is the ratio between the interpolated start and end observations.
   * The end month is typically three months before the end of the period.
   * @return the value of the property, not null
   */
  public PriceIndexObservation getEndObservation() {
    return endObservation;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the observation for interpolation at the end.
   * <p>
   * The inflation rate is the ratio between the interpolated start and end observations.
   * The month is typically one month after the month of the end observation.
   * @return the value of the property, not null
   */
  public PriceIndexObservation getEndSecondObservation() {
    return endSecondObservation;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the positive weight used when interpolating.
   * <p>
   * Given two price index observations, typically in adjacent months, the weight is used
   * to determine the adjusted index value. The value is given by the formula
   * {@code (weight * price_index_1 + (1 - weight) * price_index_2)}.
   * @return the value of the property
   */
  public double getWeight() {
    return weight;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      InflationInterpolatedRateComputation other = (InflationInterpolatedRateComputation) obj;
      return JodaBeanUtils.equal(startObservation, other.startObservation) &&
          JodaBeanUtils.equal(startSecondObservation, other.startSecondObservation) &&
          JodaBeanUtils.equal(endObservation, other.endObservation) &&
          JodaBeanUtils.equal(endSecondObservation, other.endSecondObservation) &&
          JodaBeanUtils.equal(weight, other.weight);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(startObservation);
    hash = hash * 31 + JodaBeanUtils.hashCode(startSecondObservation);
    hash = hash * 31 + JodaBeanUtils.hashCode(endObservation);
    hash = hash * 31 + JodaBeanUtils.hashCode(endSecondObservation);
    hash = hash * 31 + JodaBeanUtils.hashCode(weight);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("InflationInterpolatedRateComputation{");
    buf.append("startObservation").append('=').append(startObservation).append(',').append(' ');
    buf.append("startSecondObservation").append('=').append(startSecondObservation).append(',').append(' ');
    buf.append("endObservation").append('=').append(endObservation).append(',').append(' ');
    buf.append("endSecondObservation").append('=').append(endSecondObservation).append(',').append(' ');
    buf.append("weight").append('=').append(JodaBeanUtils.toString(weight));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code InflationInterpolatedRateComputation}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code startObservation} property.
     */
    private final MetaProperty<PriceIndexObservation> startObservation = DirectMetaProperty.ofImmutable(
        this, "startObservation", InflationInterpolatedRateComputation.class, PriceIndexObservation.class);
    /**
     * The meta-property for the {@code startSecondObservation} property.
     */
    private final MetaProperty<PriceIndexObservation> startSecondObservation = DirectMetaProperty.ofImmutable(
        this, "startSecondObservation", InflationInterpolatedRateComputation.class, PriceIndexObservation.class);
    /**
     * The meta-property for the {@code endObservation} property.
     */
    private final MetaProperty<PriceIndexObservation> endObservation = DirectMetaProperty.ofImmutable(
        this, "endObservation", InflationInterpolatedRateComputation.class, PriceIndexObservation.class);
    /**
     * The meta-property for the {@code endSecondObservation} property.
     */
    private final MetaProperty<PriceIndexObservation> endSecondObservation = DirectMetaProperty.ofImmutable(
        this, "endSecondObservation", InflationInterpolatedRateComputation.class, PriceIndexObservation.class);
    /**
     * The meta-property for the {@code weight} property.
     */
    private final MetaProperty<Double> weight = DirectMetaProperty.ofImmutable(
        this, "weight", InflationInterpolatedRateComputation.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "startObservation",
        "startSecondObservation",
        "endObservation",
        "endSecondObservation",
        "weight");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1098347926:  // startObservation
          return startObservation;
        case 1287141078:  // startSecondObservation
          return startSecondObservation;
        case 82210897:  // endObservation
          return endObservation;
        case 1209389949:  // endSecondObservation
          return endSecondObservation;
        case -791592328:  // weight
          return weight;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends InflationInterpolatedRateComputation> builder() {
      return new InflationInterpolatedRateComputation.Builder();
    }

    @Override
    public Class<? extends InflationInterpolatedRateComputation> beanType() {
      return InflationInterpolatedRateComputation.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code startObservation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PriceIndexObservation> startObservation() {
      return startObservation;
    }

    /**
     * The meta-property for the {@code startSecondObservation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PriceIndexObservation> startSecondObservation() {
      return startSecondObservation;
    }

    /**
     * The meta-property for the {@code endObservation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PriceIndexObservation> endObservation() {
      return endObservation;
    }

    /**
     * The meta-property for the {@code endSecondObservation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PriceIndexObservation> endSecondObservation() {
      return endSecondObservation;
    }

    /**
     * The meta-property for the {@code weight} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> weight() {
      return weight;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1098347926:  // startObservation
          return ((InflationInterpolatedRateComputation) bean).getStartObservation();
        case 1287141078:  // startSecondObservation
          return ((InflationInterpolatedRateComputation) bean).getStartSecondObservation();
        case 82210897:  // endObservation
          return ((InflationInterpolatedRateComputation) bean).getEndObservation();
        case 1209389949:  // endSecondObservation
          return ((InflationInterpolatedRateComputation) bean).getEndSecondObservation();
        case -791592328:  // weight
          return ((InflationInterpolatedRateComputation) bean).getWeight();
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
   * The bean-builder for {@code InflationInterpolatedRateComputation}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<InflationInterpolatedRateComputation> {

    private PriceIndexObservation startObservation;
    private PriceIndexObservation startSecondObservation;
    private PriceIndexObservation endObservation;
    private PriceIndexObservation endSecondObservation;
    private double weight;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1098347926:  // startObservation
          return startObservation;
        case 1287141078:  // startSecondObservation
          return startSecondObservation;
        case 82210897:  // endObservation
          return endObservation;
        case 1209389949:  // endSecondObservation
          return endSecondObservation;
        case -791592328:  // weight
          return weight;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1098347926:  // startObservation
          this.startObservation = (PriceIndexObservation) newValue;
          break;
        case 1287141078:  // startSecondObservation
          this.startSecondObservation = (PriceIndexObservation) newValue;
          break;
        case 82210897:  // endObservation
          this.endObservation = (PriceIndexObservation) newValue;
          break;
        case 1209389949:  // endSecondObservation
          this.endSecondObservation = (PriceIndexObservation) newValue;
          break;
        case -791592328:  // weight
          this.weight = (Double) newValue;
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
    public InflationInterpolatedRateComputation build() {
      return new InflationInterpolatedRateComputation(
          startObservation,
          startSecondObservation,
          endObservation,
          endSecondObservation,
          weight);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("InflationInterpolatedRateComputation.Builder{");
      buf.append("startObservation").append('=').append(JodaBeanUtils.toString(startObservation)).append(',').append(' ');
      buf.append("startSecondObservation").append('=').append(JodaBeanUtils.toString(startSecondObservation)).append(',').append(' ');
      buf.append("endObservation").append('=').append(JodaBeanUtils.toString(endObservation)).append(',').append(' ');
      buf.append("endSecondObservation").append('=').append(JodaBeanUtils.toString(endSecondObservation)).append(',').append(' ');
      buf.append("weight").append('=').append(JodaBeanUtils.toString(weight));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
