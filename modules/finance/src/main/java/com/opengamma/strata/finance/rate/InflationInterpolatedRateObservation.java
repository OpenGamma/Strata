/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate;

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

import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Defines the observation of inflation figures from a price index.
 * <p>
 * the reference index is linearly interpolated between two months. 
 * The interpolation is done with the number of days of the payment month.   
 * The most common implementations of price indexes are provided in {@link PriceIndices}.
 */
@BeanDefinition
public class InflationInterpolatedRateObservation
    implements RateObservation, ImmutableBean, Serializable {

  /**
  * The index of prices.
  * <p>
  * The pay-off is computed based on this index
  * The most common implementations are provided in {@link PriceIndices}.
  */
  @PropertyDefinition(validate = "notNull")
  private final PriceIndex index;
  /**
   * The first reference month for the index at the coupon start. 
   * <p>
   * Two reference months are required for the interpolation.
   * There is usually a difference of three months between the first reference month and the accrual start month.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth referenceStartMonthFirst;
  /**
   * The second reference month for the index at the coupon start. 
   * <p>
   * Two reference months are required for the interpolation.
   * There is usually a difference of two months between the second reference month and the accrual start month.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth referenceStartMonthSecond;
  /**
   * The first reference month for the index at the coupon end. 
   * <p>
   * Two reference months are required for the interpolation.
   * There is usually a difference of three months between the first reference month and the payment month.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth referenceEndMonthFirst;
  /**
   * The second reference month for the index at the coupon end. 
   * <p>
   * Two reference months are required for the interpolation.
   * There is usually a difference of two months between the second reference month and the payment month.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth referenceEndMonthSecond;
  /**
   * The weight for the index value in the interpolation.
   * <p>
   * This must be non-negative. 
   * The reference index is then given by weight * price_index_1 + (1-weight) * price_index_2. 
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double weight;

  /**
   * Creates an {@code InflationInterpolatedRateObservation} from an index, reference start months 
   * and reference end months.
   * @param index The index
   * @param referenceStartMonthFirst The first reference start month. 
   * @param referenceStartMonthSecond The second reference start month. 
   * @param referenceEndMonthFirst The first reference end month. 
   * @param referenceEndMonthSecond The second reference end month. 
   * @param weight The weight. 
   * @return The inflation rate observation
   */
  public static InflationInterpolatedRateObservation of(
      PriceIndex index,
      YearMonth referenceStartMonthFirst,
      YearMonth referenceStartMonthSecond,
      YearMonth referenceEndMonthFirst,
      YearMonth referenceEndMonthSecond,
      double weight) {
    return InflationInterpolatedRateObservation.builder()
        .index(index)
        .referenceStartMonthFirst(referenceStartMonthFirst)
        .referenceStartMonthSecond(referenceStartMonthSecond)
        .referenceEndMonthFirst(referenceEndMonthFirst)
        .referenceEndMonthSecond(referenceEndMonthSecond)
        .weight(weight)
        .build();
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(
        referenceStartMonthFirst, referenceEndMonthFirst, "referenceStartMonthFirst", "referenceEndMonthFirst");
    ArgChecker.inOrderNotEqual(
        referenceStartMonthSecond, referenceEndMonthSecond, "referenceStartMonthSecond", "referenceEndMonthSecond");
  }

  @Override
  public void collectIndices(com.google.common.collect.ImmutableSet.Builder<Index> builder) {
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
    JodaBeanUtils.notNull(builder.referenceStartMonthFirst, "referenceStartMonthFirst");
    JodaBeanUtils.notNull(builder.referenceStartMonthSecond, "referenceStartMonthSecond");
    JodaBeanUtils.notNull(builder.referenceEndMonthFirst, "referenceEndMonthFirst");
    JodaBeanUtils.notNull(builder.referenceEndMonthSecond, "referenceEndMonthSecond");
    ArgChecker.notNegative(builder.weight, "weight");
    this.index = builder.index;
    this.referenceStartMonthFirst = builder.referenceStartMonthFirst;
    this.referenceStartMonthSecond = builder.referenceStartMonthSecond;
    this.referenceEndMonthFirst = builder.referenceEndMonthFirst;
    this.referenceEndMonthSecond = builder.referenceEndMonthSecond;
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
   * The most common implementations are provided in {@link PriceIndices}.
   * @return the value of the property, not null
   */
  public PriceIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the first reference month for the index at the coupon start.
   * <p>
   * Two reference months are required for the interpolation.
   * There is usually a difference of three months between the first reference month and the accrual start month.
   * @return the value of the property, not null
   */
  public YearMonth getReferenceStartMonthFirst() {
    return referenceStartMonthFirst;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the second reference month for the index at the coupon start.
   * <p>
   * Two reference months are required for the interpolation.
   * There is usually a difference of two months between the second reference month and the accrual start month.
   * @return the value of the property, not null
   */
  public YearMonth getReferenceStartMonthSecond() {
    return referenceStartMonthSecond;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the first reference month for the index at the coupon end.
   * <p>
   * Two reference months are required for the interpolation.
   * There is usually a difference of three months between the first reference month and the payment month.
   * @return the value of the property, not null
   */
  public YearMonth getReferenceEndMonthFirst() {
    return referenceEndMonthFirst;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the second reference month for the index at the coupon end.
   * <p>
   * Two reference months are required for the interpolation.
   * There is usually a difference of two months between the second reference month and the payment month.
   * @return the value of the property, not null
   */
  public YearMonth getReferenceEndMonthSecond() {
    return referenceEndMonthSecond;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the weight for the index value in the interpolation.
   * <p>
   * This must be non-negative.
   * The reference index is then given by weight * price_index_1 + (1-weight) * price_index_2.
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
          JodaBeanUtils.equal(getReferenceStartMonthFirst(), other.getReferenceStartMonthFirst()) &&
          JodaBeanUtils.equal(getReferenceStartMonthSecond(), other.getReferenceStartMonthSecond()) &&
          JodaBeanUtils.equal(getReferenceEndMonthFirst(), other.getReferenceEndMonthFirst()) &&
          JodaBeanUtils.equal(getReferenceEndMonthSecond(), other.getReferenceEndMonthSecond()) &&
          JodaBeanUtils.equal(getWeight(), other.getWeight());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getReferenceStartMonthFirst());
    hash = hash * 31 + JodaBeanUtils.hashCode(getReferenceStartMonthSecond());
    hash = hash * 31 + JodaBeanUtils.hashCode(getReferenceEndMonthFirst());
    hash = hash * 31 + JodaBeanUtils.hashCode(getReferenceEndMonthSecond());
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
    buf.append("referenceStartMonthFirst").append('=').append(JodaBeanUtils.toString(getReferenceStartMonthFirst())).append(',').append(' ');
    buf.append("referenceStartMonthSecond").append('=').append(JodaBeanUtils.toString(getReferenceStartMonthSecond())).append(',').append(' ');
    buf.append("referenceEndMonthFirst").append('=').append(JodaBeanUtils.toString(getReferenceEndMonthFirst())).append(',').append(' ');
    buf.append("referenceEndMonthSecond").append('=').append(JodaBeanUtils.toString(getReferenceEndMonthSecond())).append(',').append(' ');
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
     * The meta-property for the {@code referenceStartMonthFirst} property.
     */
    private final MetaProperty<YearMonth> referenceStartMonthFirst = DirectMetaProperty.ofImmutable(
        this, "referenceStartMonthFirst", InflationInterpolatedRateObservation.class, YearMonth.class);
    /**
     * The meta-property for the {@code referenceStartMonthSecond} property.
     */
    private final MetaProperty<YearMonth> referenceStartMonthSecond = DirectMetaProperty.ofImmutable(
        this, "referenceStartMonthSecond", InflationInterpolatedRateObservation.class, YearMonth.class);
    /**
     * The meta-property for the {@code referenceEndMonthFirst} property.
     */
    private final MetaProperty<YearMonth> referenceEndMonthFirst = DirectMetaProperty.ofImmutable(
        this, "referenceEndMonthFirst", InflationInterpolatedRateObservation.class, YearMonth.class);
    /**
     * The meta-property for the {@code referenceEndMonthSecond} property.
     */
    private final MetaProperty<YearMonth> referenceEndMonthSecond = DirectMetaProperty.ofImmutable(
        this, "referenceEndMonthSecond", InflationInterpolatedRateObservation.class, YearMonth.class);
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
        "referenceStartMonthFirst",
        "referenceStartMonthSecond",
        "referenceEndMonthFirst",
        "referenceEndMonthSecond",
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
        case -730138809:  // referenceStartMonthFirst
          return referenceStartMonthFirst;
        case -791432515:  // referenceStartMonthSecond
          return referenceStartMonthSecond;
        case 1223950784:  // referenceEndMonthFirst
          return referenceEndMonthFirst;
        case -344197276:  // referenceEndMonthSecond
          return referenceEndMonthSecond;
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
     * The meta-property for the {@code referenceStartMonthFirst} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<YearMonth> referenceStartMonthFirst() {
      return referenceStartMonthFirst;
    }

    /**
     * The meta-property for the {@code referenceStartMonthSecond} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<YearMonth> referenceStartMonthSecond() {
      return referenceStartMonthSecond;
    }

    /**
     * The meta-property for the {@code referenceEndMonthFirst} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<YearMonth> referenceEndMonthFirst() {
      return referenceEndMonthFirst;
    }

    /**
     * The meta-property for the {@code referenceEndMonthSecond} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<YearMonth> referenceEndMonthSecond() {
      return referenceEndMonthSecond;
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
        case -730138809:  // referenceStartMonthFirst
          return ((InflationInterpolatedRateObservation) bean).getReferenceStartMonthFirst();
        case -791432515:  // referenceStartMonthSecond
          return ((InflationInterpolatedRateObservation) bean).getReferenceStartMonthSecond();
        case 1223950784:  // referenceEndMonthFirst
          return ((InflationInterpolatedRateObservation) bean).getReferenceEndMonthFirst();
        case -344197276:  // referenceEndMonthSecond
          return ((InflationInterpolatedRateObservation) bean).getReferenceEndMonthSecond();
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
    private YearMonth referenceStartMonthFirst;
    private YearMonth referenceStartMonthSecond;
    private YearMonth referenceEndMonthFirst;
    private YearMonth referenceEndMonthSecond;
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
      this.referenceStartMonthFirst = beanToCopy.getReferenceStartMonthFirst();
      this.referenceStartMonthSecond = beanToCopy.getReferenceStartMonthSecond();
      this.referenceEndMonthFirst = beanToCopy.getReferenceEndMonthFirst();
      this.referenceEndMonthSecond = beanToCopy.getReferenceEndMonthSecond();
      this.weight = beanToCopy.getWeight();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -730138809:  // referenceStartMonthFirst
          return referenceStartMonthFirst;
        case -791432515:  // referenceStartMonthSecond
          return referenceStartMonthSecond;
        case 1223950784:  // referenceEndMonthFirst
          return referenceEndMonthFirst;
        case -344197276:  // referenceEndMonthSecond
          return referenceEndMonthSecond;
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
        case -730138809:  // referenceStartMonthFirst
          this.referenceStartMonthFirst = (YearMonth) newValue;
          break;
        case -791432515:  // referenceStartMonthSecond
          this.referenceStartMonthSecond = (YearMonth) newValue;
          break;
        case 1223950784:  // referenceEndMonthFirst
          this.referenceEndMonthFirst = (YearMonth) newValue;
          break;
        case -344197276:  // referenceEndMonthSecond
          this.referenceEndMonthSecond = (YearMonth) newValue;
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
     * Sets the {@code index} property in the builder.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(PriceIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the {@code referenceStartMonthFirst} property in the builder.
     * @param referenceStartMonthFirst  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceStartMonthFirst(YearMonth referenceStartMonthFirst) {
      JodaBeanUtils.notNull(referenceStartMonthFirst, "referenceStartMonthFirst");
      this.referenceStartMonthFirst = referenceStartMonthFirst;
      return this;
    }

    /**
     * Sets the {@code referenceStartMonthSecond} property in the builder.
     * @param referenceStartMonthSecond  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceStartMonthSecond(YearMonth referenceStartMonthSecond) {
      JodaBeanUtils.notNull(referenceStartMonthSecond, "referenceStartMonthSecond");
      this.referenceStartMonthSecond = referenceStartMonthSecond;
      return this;
    }

    /**
     * Sets the {@code referenceEndMonthFirst} property in the builder.
     * @param referenceEndMonthFirst  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceEndMonthFirst(YearMonth referenceEndMonthFirst) {
      JodaBeanUtils.notNull(referenceEndMonthFirst, "referenceEndMonthFirst");
      this.referenceEndMonthFirst = referenceEndMonthFirst;
      return this;
    }

    /**
     * Sets the {@code referenceEndMonthSecond} property in the builder.
     * @param referenceEndMonthSecond  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder referenceEndMonthSecond(YearMonth referenceEndMonthSecond) {
      JodaBeanUtils.notNull(referenceEndMonthSecond, "referenceEndMonthSecond");
      this.referenceEndMonthSecond = referenceEndMonthSecond;
      return this;
    }

    /**
     * Sets the {@code weight} property in the builder.
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
      buf.append("referenceStartMonthFirst").append('=').append(JodaBeanUtils.toString(referenceStartMonthFirst)).append(',').append(' ');
      buf.append("referenceStartMonthSecond").append('=').append(JodaBeanUtils.toString(referenceStartMonthSecond)).append(',').append(' ');
      buf.append("referenceEndMonthFirst").append('=').append(JodaBeanUtils.toString(referenceEndMonthFirst)).append(',').append(' ');
      buf.append("referenceEndMonthSecond").append('=').append(JodaBeanUtils.toString(referenceEndMonthSecond)).append(',').append(' ');
      buf.append("weight").append('=').append(JodaBeanUtils.toString(weight)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
