/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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

import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * An Overnight index curve providing rates from discount factors.
 * <p>
 * This provides historic and forward rates for a single {@link OvernightIndex}, such as 'EUR-EONIA'.
 * <p>
 * This implementation is based on an underlying curve that is stored with maturities
 * and zero-coupon continuously-compounded rates.
 */
@BeanDefinition(builderScope = "private")
public final class DiscountOvernightIndexRates
    implements OvernightIndexRates, ImmutableBean, Serializable {

  /**
   * The index that the rates are for.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final OvernightIndex index;
  /**
   * The time-series.
   * This covers known historical fixings and may be empty.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDateDoubleTimeSeries timeSeries;
  /**
   * The underlying discount factor curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final DiscountFactors discountFactors;

  //-------------------------------------------------------------------------
  /**
   * Creates a new Overnight index rates instance with no historic fixings.
   * <p>
   * The forward curve is specified by an instance of {@link DiscountFactors}.
   * 
   * @param index  the Overnight index
   * @param discountFactors  the underlying discount factor forward curve
   * @return the rates instance
   */
  public static DiscountOvernightIndexRates of(OvernightIndex index, DiscountFactors discountFactors) {
    return of(index, LocalDateDoubleTimeSeries.empty(), discountFactors);
  }

  /**
   * Creates a new Overnight index rates instance.
   * <p>
   * The forward curve is specified by an instance of {@link DiscountFactors}.
   * 
   * @param index  the Overnight index
   * @param knownFixings  the known historical fixings
   * @param discountFactors  the underlying discount factor forward curve
   * @return the rates instance
   */
  public static DiscountOvernightIndexRates of(
      OvernightIndex index,
      LocalDateDoubleTimeSeries knownFixings,
      DiscountFactors discountFactors) {

    return new DiscountOvernightIndexRates(index, knownFixings, discountFactors);
  }

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.timeSeries = LocalDateDoubleTimeSeries.empty();
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDate getValuationDate() {
    return discountFactors.getValuationDate();
  }

  @Override
  public CurveName getCurveName() {
    return discountFactors.getCurveName();
  }

  @Override
  public int getParameterCount() {
    return discountFactors.getParameterCount();
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(LocalDate fixingDate) {
    LocalDate publicationDate = index.calculatePublicationFromFixing(fixingDate);
    if (!publicationDate.isAfter(getValuationDate())) {
      return historicRate(fixingDate, publicationDate);
    }
    return forwardRate(fixingDate);
  }

  // historic rate
  private double historicRate(LocalDate fixingDate, LocalDate publicationDate) {
    OptionalDouble fixedRate = timeSeries.get(fixingDate);
    if (fixedRate.isPresent()) {
      return fixedRate.getAsDouble();
    } else if (publicationDate.isBefore(getValuationDate())) { // the fixing is required
      if (timeSeries.isEmpty()) {
        throw new IllegalArgumentException(
            Messages.format("Unable to get fixing for {} on date {}, no time-series supplied", index, fixingDate));
      }
      throw new IllegalArgumentException(Messages.format("Unable to get fixing for {} on date {}", index, fixingDate));
    } else {
      return forwardRate(fixingDate);
    }
  }

  // forward rate
  private double forwardRate(LocalDate fixingDate) {
    LocalDate fixingStartDate = index.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
    double fixingYearFraction = index.getDayCount().yearFraction(fixingStartDate, fixingEndDate);
    return simplyCompoundForwardRate(fixingStartDate, fixingEndDate, fixingYearFraction);
  }

  // compounded from discount factors
  private double simplyCompoundForwardRate(LocalDate startDate, LocalDate endDate, double accrualFactor) {
    return (discountFactors.discountFactor(startDate) / discountFactors.discountFactor(endDate) - 1) / accrualFactor;
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivityBuilder pointSensitivity(LocalDate fixingDate) {
    LocalDate valuationDate = getValuationDate();
    LocalDate publicationDate = index.calculatePublicationFromFixing(fixingDate);
    if (publicationDate.isBefore(valuationDate) ||
        (publicationDate.equals(valuationDate) && timeSeries.get(fixingDate).isPresent())) {
      return PointSensitivityBuilder.none();
    }
    LocalDate fixingStartDate = index.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
    return OvernightRateSensitivity.of(index, index.getCurrency(), fixingDate, fixingEndDate, 1d);
  }

  //-------------------------------------------------------------------------
  @Override
  public double periodRate(LocalDate startDate, LocalDate endDate) {
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
    ArgChecker.inOrderOrEqual(getValuationDate(), startDate, "valuationDate", "startDate");
    double fixingYearFraction = index.getDayCount().yearFraction(startDate, endDate);
    return simplyCompoundForwardRate(startDate, endDate, fixingYearFraction);
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivityBuilder periodRatePointSensitivity(LocalDate startDate, LocalDate endDate) {
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
    ArgChecker.inOrderOrEqual(getValuationDate(), startDate, "valuationDate", "startDate");
    return OvernightRateSensitivity.of(index, index.getCurrency(), startDate, endDate, 1d);
  }

  //-------------------------------------------------------------------------
  @Override
  public double[] unitParameterSensitivity(LocalDate date) {
    return discountFactors.unitParameterSensitivity(date);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new instance with different discount factors.
   * 
   * @param factors  the new discount factors
   * @return the new instance
   */
  public DiscountOvernightIndexRates withDiscountFactors(DiscountFactors factors) {
    return new DiscountOvernightIndexRates(index, timeSeries, factors);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code DiscountOvernightIndexRates}.
   * @return the meta-bean, not null
   */
  public static DiscountOvernightIndexRates.Meta meta() {
    return DiscountOvernightIndexRates.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(DiscountOvernightIndexRates.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private DiscountOvernightIndexRates(
      OvernightIndex index,
      LocalDateDoubleTimeSeries timeSeries,
      DiscountFactors discountFactors) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    JodaBeanUtils.notNull(discountFactors, "discountFactors");
    this.index = index;
    this.timeSeries = timeSeries;
    this.discountFactors = discountFactors;
  }

  @Override
  public DiscountOvernightIndexRates.Meta metaBean() {
    return DiscountOvernightIndexRates.Meta.INSTANCE;
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
   * Gets the index that the rates are for.
   * @return the value of the property, not null
   */
  @Override
  public OvernightIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-series.
   * This covers known historical fixings and may be empty.
   * @return the value of the property, not null
   */
  @Override
  public LocalDateDoubleTimeSeries getTimeSeries() {
    return timeSeries;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying discount factor curve.
   * @return the value of the property, not null
   */
  public DiscountFactors getDiscountFactors() {
    return discountFactors;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DiscountOvernightIndexRates other = (DiscountOvernightIndexRates) obj;
      return JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries()) &&
          JodaBeanUtils.equal(getDiscountFactors(), other.getDiscountFactors());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDiscountFactors());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("DiscountOvernightIndexRates{");
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(getTimeSeries()).append(',').append(' ');
    buf.append("discountFactors").append('=').append(JodaBeanUtils.toString(getDiscountFactors()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DiscountOvernightIndexRates}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<OvernightIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", DiscountOvernightIndexRates.class, OvernightIndex.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    private final MetaProperty<LocalDateDoubleTimeSeries> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", DiscountOvernightIndexRates.class, LocalDateDoubleTimeSeries.class);
    /**
     * The meta-property for the {@code discountFactors} property.
     */
    private final MetaProperty<DiscountFactors> discountFactors = DirectMetaProperty.ofImmutable(
        this, "discountFactors", DiscountOvernightIndexRates.class, DiscountFactors.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "timeSeries",
        "discountFactors");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 779431844:  // timeSeries
          return timeSeries;
        case -91613053:  // discountFactors
          return discountFactors;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends DiscountOvernightIndexRates> builder() {
      return new DiscountOvernightIndexRates.Builder();
    }

    @Override
    public Class<? extends DiscountOvernightIndexRates> beanType() {
      return DiscountOvernightIndexRates.class;
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
    public MetaProperty<OvernightIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code timeSeries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDateDoubleTimeSeries> timeSeries() {
      return timeSeries;
    }

    /**
     * The meta-property for the {@code discountFactors} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DiscountFactors> discountFactors() {
      return discountFactors;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((DiscountOvernightIndexRates) bean).getIndex();
        case 779431844:  // timeSeries
          return ((DiscountOvernightIndexRates) bean).getTimeSeries();
        case -91613053:  // discountFactors
          return ((DiscountOvernightIndexRates) bean).getDiscountFactors();
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
   * The bean-builder for {@code DiscountOvernightIndexRates}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<DiscountOvernightIndexRates> {

    private OvernightIndex index;
    private LocalDateDoubleTimeSeries timeSeries;
    private DiscountFactors discountFactors;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 779431844:  // timeSeries
          return timeSeries;
        case -91613053:  // discountFactors
          return discountFactors;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (OvernightIndex) newValue;
          break;
        case 779431844:  // timeSeries
          this.timeSeries = (LocalDateDoubleTimeSeries) newValue;
          break;
        case -91613053:  // discountFactors
          this.discountFactors = (DiscountFactors) newValue;
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
    public DiscountOvernightIndexRates build() {
      return new DiscountOvernightIndexRates(
          index,
          timeSeries,
          discountFactors);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("DiscountOvernightIndexRates.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries)).append(',').append(' ');
      buf.append("discountFactors").append('=').append(JodaBeanUtils.toString(discountFactors));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
