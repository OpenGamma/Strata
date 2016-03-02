/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DateAdjuster;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.ReferenceData;

/**
 * Defines the observation of a rate of interest from a single Ibor index.
 * <p>
 * An interest rate determined directly from an Ibor index.
 * For example, a rate determined from 'GBP-LIBOR-3M' on a single fixing date.
 */
@BeanDefinition
public final class IborRateObservation
    implements RateObservation, ImmutableBean, Serializable {

  /**
   * The Ibor index.
   * <p>
   * The rate to be paid is based on this index.
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborIndex index;
  /**
   * The date of the index fixing.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * Valid business days are defined by {@link IborIndex#getFixingCalendar()}.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate fixingDate;
  /**
   * The effective date of the investment implied by the fixing date.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * This must be equal to {@link IborIndex#calculateEffectiveFromFixing(LocalDate, ReferenceData)}.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate effectiveDate;
  /**
   * The maturity date of the investment implied by the fixing date.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * This must be equal to {@link IborIndex#calculateMaturityFromEffective(LocalDate, ReferenceData)}.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate maturityDate;
  /**
   * The year fraction of the investment implied by the fixing date.
   * <p>
   * This is calculated using the day count of the index.
   * It represents the fraction of the year between the effective date and the maturity date.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   */
  @PropertyDefinition(validate = "notNull")
  private final double yearFraction;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance from an index and fixing date.
   * <p>
   * The reference data is used to find the maturity date from the fixing date.
   * 
   * @param index  the index
   * @param fixingDate  the fixing date
   * @param refData  the reference data to use when resolving holiday calendars
   * @return the rate observation
   */
  public static IborRateObservation of(
      IborIndex index,
      LocalDate fixingDate,
      ReferenceData refData) {

    LocalDate effectiveDate = index.calculateEffectiveFromFixing(fixingDate, refData);
    LocalDate maturityDate = index.calculateMaturityFromEffective(effectiveDate, refData);
    double yearFraction = index.getDayCount().yearFraction(effectiveDate, maturityDate);
    return new IborRateObservation(index, fixingDate, effectiveDate, maturityDate, yearFraction);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a function capable of producing {@code IborRateObservation} instances.
   * <p>
   * The resulting function is bound to the specified index and reference data.
   * The function will convert a fixing date, which must be valid according to
   * the fixing calendar, to an observation.
   * 
   * @param index  the index
   * @param refData  the reference data to use when resolving holiday calendars
   * @return the rate observation
   */
  public static Function<LocalDate, IborRateObservation> bind(IborIndex index, ReferenceData refData) {
    HolidayCalendar fixingCal = index.getFixingCalendar().resolve(refData);
    DateAdjuster effectiveAdjuster = index.getEffectiveDateOffset().resolve(refData);
    DateAdjuster maturityAdjuster = index.getMaturityDateOffset().resolve(refData);
    return fixingDate -> create(index, fixingDate, fixingCal, effectiveAdjuster, maturityAdjuster);
  }

  // creates an instance
  private static IborRateObservation create(
      IborIndex index,
      LocalDate fixingDate,
      HolidayCalendar fixingCal,
      DateAdjuster effectiveAdjuster,
      DateAdjuster maturityAdjuster) {

    LocalDate fixingBusinessDay = fixingCal.nextOrSame(fixingDate);
    LocalDate effectiveDate = effectiveAdjuster.adjust(fixingBusinessDay);
    LocalDate maturityDate = maturityAdjuster.adjust(effectiveDate);
    double yearFraction = index.getDayCount().yearFraction(effectiveDate, maturityDate);
    return new IborRateObservation(index, fixingDate, effectiveDate, maturityDate, yearFraction);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the Ibor index.
   * 
   * @return the currency of the index
   */
  public Currency getCurrency() {
    return index.getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    builder.add(index);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborRateObservation}.
   * @return the meta-bean, not null
   */
  public static IborRateObservation.Meta meta() {
    return IborRateObservation.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborRateObservation.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborRateObservation.Builder builder() {
    return new IborRateObservation.Builder();
  }

  private IborRateObservation(
      IborIndex index,
      LocalDate fixingDate,
      LocalDate effectiveDate,
      LocalDate maturityDate,
      double yearFraction) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(fixingDate, "fixingDate");
    JodaBeanUtils.notNull(effectiveDate, "effectiveDate");
    JodaBeanUtils.notNull(maturityDate, "maturityDate");
    JodaBeanUtils.notNull(yearFraction, "yearFraction");
    this.index = index;
    this.fixingDate = fixingDate;
    this.effectiveDate = effectiveDate;
    this.maturityDate = maturityDate;
    this.yearFraction = yearFraction;
  }

  @Override
  public IborRateObservation.Meta metaBean() {
    return IborRateObservation.Meta.INSTANCE;
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
   * Gets the Ibor index.
   * <p>
   * The rate to be paid is based on this index.
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   * @return the value of the property, not null
   */
  public IborIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date of the index fixing.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * Valid business days are defined by {@link IborIndex#getFixingCalendar()}.
   * @return the value of the property, not null
   */
  public LocalDate getFixingDate() {
    return fixingDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the effective date of the investment implied by the fixing date.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * This must be equal to {@link IborIndex#calculateEffectiveFromFixing(LocalDate, ReferenceData)}.
   * @return the value of the property, not null
   */
  public LocalDate getEffectiveDate() {
    return effectiveDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the maturity date of the investment implied by the fixing date.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * This must be equal to {@link IborIndex#calculateMaturityFromEffective(LocalDate, ReferenceData)}.
   * @return the value of the property, not null
   */
  public LocalDate getMaturityDate() {
    return maturityDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year fraction of the investment implied by the fixing date.
   * <p>
   * This is calculated using the day count of the index.
   * It represents the fraction of the year between the effective date and the maturity date.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * @return the value of the property, not null
   */
  public double getYearFraction() {
    return yearFraction;
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
      IborRateObservation other = (IborRateObservation) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(fixingDate, other.fixingDate) &&
          JodaBeanUtils.equal(effectiveDate, other.effectiveDate) &&
          JodaBeanUtils.equal(maturityDate, other.maturityDate) &&
          JodaBeanUtils.equal(yearFraction, other.yearFraction);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixingDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(effectiveDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(maturityDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(yearFraction);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("IborRateObservation{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("fixingDate").append('=').append(fixingDate).append(',').append(' ');
    buf.append("effectiveDate").append('=').append(effectiveDate).append(',').append(' ');
    buf.append("maturityDate").append('=').append(maturityDate).append(',').append(' ');
    buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborRateObservation}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", IborRateObservation.class, IborIndex.class);
    /**
     * The meta-property for the {@code fixingDate} property.
     */
    private final MetaProperty<LocalDate> fixingDate = DirectMetaProperty.ofImmutable(
        this, "fixingDate", IborRateObservation.class, LocalDate.class);
    /**
     * The meta-property for the {@code effectiveDate} property.
     */
    private final MetaProperty<LocalDate> effectiveDate = DirectMetaProperty.ofImmutable(
        this, "effectiveDate", IborRateObservation.class, LocalDate.class);
    /**
     * The meta-property for the {@code maturityDate} property.
     */
    private final MetaProperty<LocalDate> maturityDate = DirectMetaProperty.ofImmutable(
        this, "maturityDate", IborRateObservation.class, LocalDate.class);
    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", IborRateObservation.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "fixingDate",
        "effectiveDate",
        "maturityDate",
        "yearFraction");

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
        case 1255202043:  // fixingDate
          return fixingDate;
        case -930389515:  // effectiveDate
          return effectiveDate;
        case -414641441:  // maturityDate
          return maturityDate;
        case -1731780257:  // yearFraction
          return yearFraction;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborRateObservation.Builder builder() {
      return new IborRateObservation.Builder();
    }

    @Override
    public Class<? extends IborRateObservation> beanType() {
      return IborRateObservation.class;
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
    public MetaProperty<IborIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code fixingDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> fixingDate() {
      return fixingDate;
    }

    /**
     * The meta-property for the {@code effectiveDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> effectiveDate() {
      return effectiveDate;
    }

    /**
     * The meta-property for the {@code maturityDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> maturityDate() {
      return maturityDate;
    }

    /**
     * The meta-property for the {@code yearFraction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> yearFraction() {
      return yearFraction;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((IborRateObservation) bean).getIndex();
        case 1255202043:  // fixingDate
          return ((IborRateObservation) bean).getFixingDate();
        case -930389515:  // effectiveDate
          return ((IborRateObservation) bean).getEffectiveDate();
        case -414641441:  // maturityDate
          return ((IborRateObservation) bean).getMaturityDate();
        case -1731780257:  // yearFraction
          return ((IborRateObservation) bean).getYearFraction();
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
   * The bean-builder for {@code IborRateObservation}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborRateObservation> {

    private IborIndex index;
    private LocalDate fixingDate;
    private LocalDate effectiveDate;
    private LocalDate maturityDate;
    private double yearFraction;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(IborRateObservation beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.fixingDate = beanToCopy.getFixingDate();
      this.effectiveDate = beanToCopy.getEffectiveDate();
      this.maturityDate = beanToCopy.getMaturityDate();
      this.yearFraction = beanToCopy.getYearFraction();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 1255202043:  // fixingDate
          return fixingDate;
        case -930389515:  // effectiveDate
          return effectiveDate;
        case -414641441:  // maturityDate
          return maturityDate;
        case -1731780257:  // yearFraction
          return yearFraction;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (IborIndex) newValue;
          break;
        case 1255202043:  // fixingDate
          this.fixingDate = (LocalDate) newValue;
          break;
        case -930389515:  // effectiveDate
          this.effectiveDate = (LocalDate) newValue;
          break;
        case -414641441:  // maturityDate
          this.maturityDate = (LocalDate) newValue;
          break;
        case -1731780257:  // yearFraction
          this.yearFraction = (Double) newValue;
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
    public IborRateObservation build() {
      return new IborRateObservation(
          index,
          fixingDate,
          effectiveDate,
          maturityDate,
          yearFraction);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the Ibor index.
     * <p>
     * The rate to be paid is based on this index.
     * It will be a well known market index such as 'GBP-LIBOR-3M'.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(IborIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the date of the index fixing.
     * <p>
     * This is an adjusted date with any business day rule applied.
     * Valid business days are defined by {@link IborIndex#getFixingCalendar()}.
     * @param fixingDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingDate(LocalDate fixingDate) {
      JodaBeanUtils.notNull(fixingDate, "fixingDate");
      this.fixingDate = fixingDate;
      return this;
    }

    /**
     * Sets the effective date of the investment implied by the fixing date.
     * <p>
     * This is an adjusted date with any business day rule applied.
     * This must be equal to {@link IborIndex#calculateEffectiveFromFixing(LocalDate, ReferenceData)}.
     * @param effectiveDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder effectiveDate(LocalDate effectiveDate) {
      JodaBeanUtils.notNull(effectiveDate, "effectiveDate");
      this.effectiveDate = effectiveDate;
      return this;
    }

    /**
     * Sets the maturity date of the investment implied by the fixing date.
     * <p>
     * This is an adjusted date with any business day rule applied.
     * This must be equal to {@link IborIndex#calculateMaturityFromEffective(LocalDate, ReferenceData)}.
     * @param maturityDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder maturityDate(LocalDate maturityDate) {
      JodaBeanUtils.notNull(maturityDate, "maturityDate");
      this.maturityDate = maturityDate;
      return this;
    }

    /**
     * Sets the year fraction of the investment implied by the fixing date.
     * <p>
     * This is calculated using the day count of the index.
     * It represents the fraction of the year between the effective date and the maturity date.
     * Typically the value will be close to 1 for one year and close to 0.5 for six months.
     * @param yearFraction  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder yearFraction(double yearFraction) {
      JodaBeanUtils.notNull(yearFraction, "yearFraction");
      this.yearFraction = yearFraction;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("IborRateObservation.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("fixingDate").append('=').append(JodaBeanUtils.toString(fixingDate)).append(',').append(' ');
      buf.append("effectiveDate").append('=').append(JodaBeanUtils.toString(effectiveDate)).append(',').append(' ');
      buf.append("maturityDate").append('=').append(JodaBeanUtils.toString(maturityDate)).append(',').append(' ');
      buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
