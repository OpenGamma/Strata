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
import com.opengamma.strata.collect.ArgChecker;

/**
 * Defines the observation of inflation figures from a price index with interpolation.
 * <p>
 * A price index is typically published monthly and has a delay before publication.
 * The rate observed by this instance will be based on four observations of the index,
 * two relative to the accrual start date and two relative to the accrual end date.
 * Linear interpolation based on the number of days of the payment month is used
 * to find the appropriate value for each pair of observations.
 */
@BeanDefinition
public class InflationInterpolatedRateObservation
    implements RateObservation, ImmutableBean, Serializable {

  /**
  * The index of prices.
  * <p>
  * The pay-off is computed based on this index
  */
  @PropertyDefinition(validate = "notNull")
  private final PriceIndex index;
  /**
   * The reference month for the index relative to the accrual start date.
   * <p>
   * The reference month is typically three months before the accrual start date.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth referenceStartMonth;
  /**
   * The reference month used for interpolation for the index relative to the accrual start date.
   * <p>
   * The reference month for interpolation is typically one month after the reference start month.
   * As such it is typically two months before the accrual start date.
   * Must be after the reference start month.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth referenceStartInterpolationMonth;
  /**
   * The reference month for the index relative to the accrual end date.
   * <p>
   * The reference month is typically three months before the accrual end date.
   * Must be after the reference start month.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth referenceEndMonth;
  /**
   * The reference month used for interpolation for the index relative to the accrual end date.
   * <p>
   * The reference month for interpolation is typically one month after the reference end month.
   * As such it is typically two months before the accrual end date.
   * Must be after the reference end month.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth referenceEndInterpolationMonth;
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
   * Creates an {@code InflationInterpolatedRateObservation} from an index,
   * reference start month and reference end month.
   * <p>
   * The interpolated start and end month will be one month later.
   * 
   * @param index  the index
   * @param referenceStartMonth  the reference start month
   * @param referenceEndMonth  the reference end month
   * @param weight  the weight
   * @return the inflation rate observation
   */
  public static InflationInterpolatedRateObservation of(
      PriceIndex index,
      YearMonth referenceStartMonth,
      YearMonth referenceEndMonth,
      double weight) {

    return InflationInterpolatedRateObservation.builder()
        .index(index)
        .referenceStartMonth(referenceStartMonth)
        .referenceStartInterpolationMonth(referenceStartMonth.plusMonths(1))
        .referenceEndMonth(referenceEndMonth)
        .referenceEndInterpolationMonth(referenceEndMonth.plusMonths(1))
        .weight(weight)
        .build();
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(referenceStartMonth, referenceEndMonth, "referenceStartMonth", "referenceEndMonth");
    ArgChecker.inOrderNotEqual(
        referenceStartMonth, referenceStartInterpolationMonth, "referenceStartMonth", "referenceStartInterpolationMonth");
    ArgChecker.inOrderNotEqual(
        referenceEndMonth, referenceEndInterpolationMonth, "referenceEndMonth", "referenceEndInterpolationMonth");
  }

  //-------------------------------------------------------------------------
  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    builder.add(index);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code InflationInterpolatedRateObservation}.
   * @return the meta-bean, not null
   */
  public static InflationInterpolatedRateObservation.Meta meta() {
    return InflationInterpolatedRateObservation.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(InflationInterpolatedRateObservation.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static InflationInterpolatedRateObservation.Builder builder() {
    return new InflationInterpolatedRateObservation.Builder();
  }

  /**
   * Restricted constructor.
   * @param builder  the builder to copy from, not null
   */
  protected InflationInterpolatedRateObservation(InflationInterpolatedRateObservation.Builder builder) {
    JodaBeanUtils.notNull(builder.index, "index");
    JodaBeanUtils.notNull(builder.referenceStartMonth, "referenceStartMonth");
    JodaBeanUtils.notNull(builder.referenceStartInterpolationMonth, "referenceStartInterpolationMonth");
    JodaBeanUtils.notNull(builder.referenceEndMonth, "referenceEndMonth");
    JodaBeanUtils.notNull(builder.referenceEndInterpolationMonth, "referenceEndInterpolationMonth");
    ArgChecker.notNegative(builder.weight, "weight");
    this.index = builder.index;
    this.referenceStartMonth = builder.referenceStartMonth;
    this.referenceStartInterpolationMonth = builder.referenceStartInterpolationMonth;
    this.referenceEndMonth = builder.referenceEndMonth;
    this.referenceEndInterpolationMonth = builder.referenceEndInterpolationMonth;
    this.weight = builder.weight;
    validate();
  }

  @Override
  public InflationInterpolatedRateObservation.Meta metaBean() {
    return InflationInterpolatedRateObservation.Meta.INSTANCE;
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
   * Gets the index of prices.
   * <p>
   * The pay-off is computed based on this index
   * @return the value of the property, not null
   */
  public PriceIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reference month for the index relative to the accrual start date.
   * <p>
   * The reference month is typically three months before the accrual start date.
   * @return the value of the property, not null
   */
  public YearMonth getReferenceStartMonth() {
    return referenceStartMonth;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reference month used for interpolation for the index relative to the accrual start date.
   * <p>
   * The reference month for interpolation is typically one month after the reference start month.
   * As such it is typically two months before the accrual start date.
   * Must be after the reference start month.
   * @return the value of the property, not null
   */
  public YearMonth getReferenceStartInterpolationMonth() {
    return referenceStartInterpolationMonth;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reference month for the index relative to the accrual end date.
   * <p>
   * The reference month is typically three months before the accrual end date.
   * Must be after the reference start month.
   * @return the value of the property, not null
   */
  public YearMonth getReferenceEndMonth() {
    return referenceEndMonth;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reference month used for interpolation for the index relative to the accrual end date.
   * <p>
   * The reference month for interpolation is typically one month after the reference end month.
   * As such it is typically two months before the accrual end date.
   * Must be after the reference end month.
   * @return the value of the property, not null
   */
  public YearMonth getReferenceEndInterpolationMonth() {
    return referenceEndInterpolationMonth;
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
      InflationInterpolatedRateObservation other = (InflationInterpolatedRateObservation) obj;
      return JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getReferenceStartMonth(), other.getReferenceStartMonth()) &&
          JodaBeanUtils.equal(getReferenceStartInterpolationMonth(), other.getReferenceStartInterpolationMonth()) &&
          JodaBeanUtils.equal(getReferenceEndMonth(), other.getReferenceEndMonth()) &&
          JodaBeanUtils.equal(getReferenceEndInterpolationMonth(), other.getReferenceEndInterpolationMonth()) &&
          JodaBeanUtils.equal(getWeight(), other.getWeight());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getReferenceStartMonth());
    hash = hash * 31 + JodaBeanUtils.hashCode(getReferenceStartInterpolationMonth());
    hash = hash * 31 + JodaBeanUtils.hashCode(getReferenceEndMonth());
    hash = hash * 31 + JodaBeanUtils.hashCode(getReferenceEndInterpolationMonth());
    hash = hash * 31 + JodaBeanUtils.hashCode(getWeight());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("InflationInterpolatedRateObservation{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("index").append('=').append(JodaBeanUtils.toString(getIndex())).append(',').append(' ');
    buf.append("referenceStartMonth").append('=').append(JodaBeanUtils.toString(getReferenceStartMonth())).append(',').append(' ');
    buf.append("referenceStartInterpolationMonth").append('=').append(JodaBeanUtils.toString(getReferenceStartInterpolationMonth())).append(',').append(' ');
    buf.append("referenceEndMonth").append('=').append(JodaBeanUtils.toString(getReferenceEndMonth())).append(',').append(' ');
    buf.append("referenceEndInterpolationMonth").append('=').append(JodaBeanUtils.toString(getReferenceEndInterpolationMonth())).append(',').append(' ');
    buf.append("weight").append('=').append(JodaBeanUtils.toString(getWeight())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code InflationInterpolatedRateObservation}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<PriceIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", InflationInterpolatedRateObservation.class, PriceIndex.class);
    /**
     * The meta-property for the {@code referenceStartMonth} property.
     */
    private final MetaProperty<YearMonth> referenceStartMonth = DirectMetaProperty.ofImmutable(
        this, "referenceStartMonth", InflationInterpolatedRateObservation.class, YearMonth.class);
    /**
     * The meta-property for the {@code referenceStartInterpolationMonth} property.
     */
    private final MetaProperty<YearMonth> referenceStartInterpolationMonth = DirectMetaProperty.ofImmutable(
        this, "referenceStartInterpolationMonth", InflationInterpolatedRateObservation.class, YearMonth.class);
    /**
     * The meta-property for the {@code referenceEndMonth} property.
     */
    private final MetaProperty<YearMonth> referenceEndMonth = DirectMetaProperty.ofImmutable(
        this, "referenceEndMonth", InflationInterpolatedRateObservation.class, YearMonth.class);
    /**
     * The meta-property for the {@code referenceEndInterpolationMonth} property.
     */
    private final MetaProperty<YearMonth> referenceEndInterpolationMonth = DirectMetaProperty.ofImmutable(
        this, "referenceEndInterpolationMonth", InflationInterpolatedRateObservation.class, YearMonth.class);
    /**
     * The meta-property for the {@code weight} property.
     */
    private final MetaProperty<Double> weight = DirectMetaProperty.ofImmutable(
        this, "weight", InflationInterpolatedRateObservation.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "referenceStartMonth",
        "referenceStartInterpolationMonth",
        "referenceEndMonth",
        "referenceEndInterpolationMonth",
        "weight");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -1306094359:  // referenceStartMonth
          return referenceStartMonth;
        case 1179606899:  // referenceStartInterpolationMonth
          return referenceStartInterpolationMonth;
        case 1861034704:  // referenceEndMonth
          return referenceEndMonth;
        case -227090196:  // referenceEndInterpolationMonth
          return referenceEndInterpolationMonth;
        case -791592328:  // weight
          return weight;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public InflationInterpolatedRateObservation.Builder builder() {
      return new InflationInterpolatedRateObservation.Builder();
    }

    @Override
    public Class<? extends InflationInterpolatedRateObservation> beanType() {
      return InflationInterpolatedRateObservation.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<PriceIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code referenceStartMonth} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<YearMonth> referenceStartMonth() {
      return referenceStartMonth;
    }

    /**
     * The meta-property for the {@code referenceStartInterpolationMonth} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<YearMonth> referenceStartInterpolationMonth() {
      return referenceStartInterpolationMonth;
    }

    /**
     * The meta-property for the {@code referenceEndMonth} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<YearMonth> referenceEndMonth() {
      return referenceEndMonth;
    }

    /**
     * The meta-property for the {@code referenceEndInterpolationMonth} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<YearMonth> referenceEndInterpolationMonth() {
      return referenceEndInterpolationMonth;
    }

    /**
     * The meta-property for the {@code weight} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Double> weight() {
      return weight;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((InflationInterpolatedRateObservation) bean).getIndex();
        case -1306094359:  // referenceStartMonth
          return ((InflationInterpolatedRateObservation) bean).getReferenceStartMonth();
        case 1179606899:  // referenceStartInterpolationMonth
          return ((InflationInterpolatedRateObservation) bean).getReferenceStartInterpolationMonth();
        case 1861034704:  // referenceEndMonth
          return ((InflationInterpolatedRateObservation) bean).getReferenceEndMonth();
        case -227090196:  // referenceEndInterpolationMonth
          return ((InflationInterpolatedRateObservation) bean).getReferenceEndInterpolationMonth();
        case -791592328:  // weight
          return ((InflationInterpolatedRateObservation) bean).getWeight();
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
   * The bean-builder for {@code InflationInterpolatedRateObservation}.
   */
  public static class Builder extends DirectFieldsBeanBuilder<InflationInterpolatedRateObservation> {

    private PriceIndex index;
    private YearMonth referenceStartMonth;
    private YearMonth referenceStartInterpolationMonth;
    private YearMonth referenceEndMonth;
    private YearMonth referenceEndInterpolationMonth;
    private double weight;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    protected Builder(InflationInterpolatedRateObservation beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.referenceStartMonth = beanToCopy.getReferenceStartMonth();
      this.referenceStartInterpolationMonth = beanToCopy.getReferenceStartInterpolationMonth();
      this.referenceEndMonth = beanToCopy.getReferenceEndMonth();
      this.referenceEndInterpolationMonth = beanToCopy.getReferenceEndInterpolationMonth();
      this.weight = beanToCopy.getWeight();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -1306094359:  // referenceStartMonth
          return referenceStartMonth;
        case 1179606899:  // referenceStartInterpolationMonth
          return referenceStartInterpolationMonth;
        case 1861034704:  // referenceEndMonth
          return referenceEndMonth;
        case -227090196:  // referenceEndInterpolationMonth
          return referenceEndInterpolationMonth;
        case -791592328:  // weight
          return weight;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (PriceIndex) newValue;
          break;
        case -1306094359:  // referenceStartMonth
          this.referenceStartMonth = (YearMonth) newValue;
          break;
        case 1179606899:  // referenceStartInterpolationMonth
          this.referenceStartInterpolationMonth = (YearMonth) newValue;
          break;
        case 1861034704:  // referenceEndMonth
          this.referenceEndMonth = (YearMonth) newValue;
          break;
        case -227090196:  // referenceEndInterpolationMonth
          this.referenceEndInterpolationMonth = (YearMonth) newValue;
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
    public InflationInterpolatedRateObservation build() {
      return new InflationInterpolatedRateObservation(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the index of prices.
     * <p>
     * The pay-off is computed based on this index
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(PriceIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the reference month for the index relative to the accrual start date.
     * <p>
     * The reference month is typically three months before the accrual start date.
     * @param referenceStartMonth  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceStartMonth(YearMonth referenceStartMonth) {
      JodaBeanUtils.notNull(referenceStartMonth, "referenceStartMonth");
      this.referenceStartMonth = referenceStartMonth;
      return this;
    }

    /**
     * Sets the reference month used for interpolation for the index relative to the accrual start date.
     * <p>
     * The reference month for interpolation is typically one month after the reference start month.
     * As such it is typically two months before the accrual start date.
     * Must be after the reference start month.
     * @param referenceStartInterpolationMonth  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceStartInterpolationMonth(YearMonth referenceStartInterpolationMonth) {
      JodaBeanUtils.notNull(referenceStartInterpolationMonth, "referenceStartInterpolationMonth");
      this.referenceStartInterpolationMonth = referenceStartInterpolationMonth;
      return this;
    }

    /**
     * Sets the reference month for the index relative to the accrual end date.
     * <p>
     * The reference month is typically three months before the accrual end date.
     * Must be after the reference start month.
     * @param referenceEndMonth  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceEndMonth(YearMonth referenceEndMonth) {
      JodaBeanUtils.notNull(referenceEndMonth, "referenceEndMonth");
      this.referenceEndMonth = referenceEndMonth;
      return this;
    }

    /**
     * Sets the reference month used for interpolation for the index relative to the accrual end date.
     * <p>
     * The reference month for interpolation is typically one month after the reference end month.
     * As such it is typically two months before the accrual end date.
     * Must be after the reference end month.
     * @param referenceEndInterpolationMonth  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceEndInterpolationMonth(YearMonth referenceEndInterpolationMonth) {
      JodaBeanUtils.notNull(referenceEndInterpolationMonth, "referenceEndInterpolationMonth");
      this.referenceEndInterpolationMonth = referenceEndInterpolationMonth;
      return this;
    }

    /**
     * Sets the positive weight used when interpolating.
     * <p>
     * Given two price index observations, typically in adjacent months, the weight is used
     * to determine the adjusted index value. The value is given by the formula
     * {@code (weight * price_index_1 + (1 - weight) * price_index_2)}.
     * @param weight  the new value
     * @return this, for chaining, not null
     */
    public Builder weight(double weight) {
      ArgChecker.notNegative(weight, "weight");
      this.weight = weight;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("InflationInterpolatedRateObservation.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("referenceStartMonth").append('=').append(JodaBeanUtils.toString(referenceStartMonth)).append(',').append(' ');
      buf.append("referenceStartInterpolationMonth").append('=').append(JodaBeanUtils.toString(referenceStartInterpolationMonth)).append(',').append(' ');
      buf.append("referenceEndMonth").append('=').append(JodaBeanUtils.toString(referenceEndMonth)).append(',').append(' ');
      buf.append("referenceEndInterpolationMonth").append('=').append(JodaBeanUtils.toString(referenceEndInterpolationMonth)).append(',').append(' ');
      buf.append("weight").append('=').append(JodaBeanUtils.toString(weight)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
