/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import java.io.Serializable;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.PropertyDefinition;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ShiftType;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

/**
 * Provides the definition of seasonality for price index curve.
 * <p>
 * The seasonality is describe by a adjustment type and the month on month adjustments.
 * The adjustment type is usually {@link ShiftType#SCALED} (multiplicative) or {@link ShiftType#ABSOLUTE} (additive).
 * The month on month adjustment is an array of length 12 with the first element the adjustment from January to February,
 * the second element the adjustment from February to March, etc. to the 12th element the adjustment from 
 * December to January.
 */
@BeanDefinition
public class SeasonalityDefinition
    implements ImmutableBean, Serializable {

  /**
   * The month on month adjustment. 
   */
  @PropertyDefinition(validate = "notNull")
  private final DoubleArray seasonalityMonthOnMonth;
  /**
   * The shift type applied to the unadjusted value and the adjustment.
   * (value, seasonality) -> adjustmentType.applyShift(value, seasonality).
   */
  @PropertyDefinition(validate = "notNull")
  private final ShiftType adjustmentType;
  
  /**
   * Create an instance of the seasonality.
   * 
   * @param seasonalityMonthOnMonth  the month-on-month seasonality
   * @param adjustmentType  the adjustment type
   * @return the instance
   */
  public static SeasonalityDefinition of(DoubleArray seasonalityMonthOnMonth, ShiftType adjustmentType) {
    return SeasonalityDefinition.builder()
        .seasonalityMonthOnMonth(seasonalityMonthOnMonth)
        .adjustmentType(adjustmentType).build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SeasonalityDefinition}.
   * @return the meta-bean, not null
   */
  public static SeasonalityDefinition.Meta meta() {
    return SeasonalityDefinition.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SeasonalityDefinition.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static SeasonalityDefinition.Builder builder() {
    return new SeasonalityDefinition.Builder();
  }

  /**
   * Restricted constructor.
   * @param builder  the builder to copy from, not null
   */
  protected SeasonalityDefinition(SeasonalityDefinition.Builder builder) {
    JodaBeanUtils.notNull(builder.seasonalityMonthOnMonth, "seasonalityMonthOnMonth");
    JodaBeanUtils.notNull(builder.adjustmentType, "adjustmentType");
    this.seasonalityMonthOnMonth = builder.seasonalityMonthOnMonth;
    this.adjustmentType = builder.adjustmentType;
  }

  @Override
  public SeasonalityDefinition.Meta metaBean() {
    return SeasonalityDefinition.Meta.INSTANCE;
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
   * Gets the month on month adjustment.
   * @return the value of the property, not null
   */
  public DoubleArray getSeasonalityMonthOnMonth() {
    return seasonalityMonthOnMonth;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the shift type applied to the unadjusted value and the adjustment.
   * (value, seasonality) -> adjustmentType.applyShift(value, seasonality).
   * @return the value of the property, not null
   */
  public ShiftType getAdjustmentType() {
    return adjustmentType;
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
      SeasonalityDefinition other = (SeasonalityDefinition) obj;
      return JodaBeanUtils.equal(seasonalityMonthOnMonth, other.seasonalityMonthOnMonth) &&
          JodaBeanUtils.equal(adjustmentType, other.adjustmentType);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(seasonalityMonthOnMonth);
    hash = hash * 31 + JodaBeanUtils.hashCode(adjustmentType);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("SeasonalityDefinition{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("seasonalityMonthOnMonth").append('=').append(JodaBeanUtils.toString(seasonalityMonthOnMonth)).append(',').append(' ');
    buf.append("adjustmentType").append('=').append(JodaBeanUtils.toString(adjustmentType)).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SeasonalityDefinition}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code seasonalityMonthOnMonth} property.
     */
    private final MetaProperty<DoubleArray> seasonalityMonthOnMonth = DirectMetaProperty.ofImmutable(
        this, "seasonalityMonthOnMonth", SeasonalityDefinition.class, DoubleArray.class);
    /**
     * The meta-property for the {@code adjustmentType} property.
     */
    private final MetaProperty<ShiftType> adjustmentType = DirectMetaProperty.ofImmutable(
        this, "adjustmentType", SeasonalityDefinition.class, ShiftType.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "seasonalityMonthOnMonth",
        "adjustmentType");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -731183871:  // seasonalityMonthOnMonth
          return seasonalityMonthOnMonth;
        case -1002343865:  // adjustmentType
          return adjustmentType;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public SeasonalityDefinition.Builder builder() {
      return new SeasonalityDefinition.Builder();
    }

    @Override
    public Class<? extends SeasonalityDefinition> beanType() {
      return SeasonalityDefinition.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code seasonalityMonthOnMonth} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DoubleArray> seasonalityMonthOnMonth() {
      return seasonalityMonthOnMonth;
    }

    /**
     * The meta-property for the {@code adjustmentType} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ShiftType> adjustmentType() {
      return adjustmentType;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -731183871:  // seasonalityMonthOnMonth
          return ((SeasonalityDefinition) bean).getSeasonalityMonthOnMonth();
        case -1002343865:  // adjustmentType
          return ((SeasonalityDefinition) bean).getAdjustmentType();
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
   * The bean-builder for {@code SeasonalityDefinition}.
   */
  public static class Builder extends DirectFieldsBeanBuilder<SeasonalityDefinition> {

    private DoubleArray seasonalityMonthOnMonth;
    private ShiftType adjustmentType;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    protected Builder(SeasonalityDefinition beanToCopy) {
      this.seasonalityMonthOnMonth = beanToCopy.getSeasonalityMonthOnMonth();
      this.adjustmentType = beanToCopy.getAdjustmentType();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -731183871:  // seasonalityMonthOnMonth
          return seasonalityMonthOnMonth;
        case -1002343865:  // adjustmentType
          return adjustmentType;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -731183871:  // seasonalityMonthOnMonth
          this.seasonalityMonthOnMonth = (DoubleArray) newValue;
          break;
        case -1002343865:  // adjustmentType
          this.adjustmentType = (ShiftType) newValue;
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
    public SeasonalityDefinition build() {
      return new SeasonalityDefinition(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the month on month adjustment.
     * @param seasonalityMonthOnMonth  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder seasonalityMonthOnMonth(DoubleArray seasonalityMonthOnMonth) {
      JodaBeanUtils.notNull(seasonalityMonthOnMonth, "seasonalityMonthOnMonth");
      this.seasonalityMonthOnMonth = seasonalityMonthOnMonth;
      return this;
    }

    /**
     * Sets the shift type applied to the unadjusted value and the adjustment.
     * (value, seasonality) -> adjustmentType.applyShift(value, seasonality).
     * @param adjustmentType  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder adjustmentType(ShiftType adjustmentType) {
      JodaBeanUtils.notNull(adjustmentType, "adjustmentType");
      this.adjustmentType = adjustmentType;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("SeasonalityDefinition.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("seasonalityMonthOnMonth").append('=').append(JodaBeanUtils.toString(seasonalityMonthOnMonth)).append(',').append(' ');
      buf.append("adjustmentType").append('=').append(JodaBeanUtils.toString(adjustmentType)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
