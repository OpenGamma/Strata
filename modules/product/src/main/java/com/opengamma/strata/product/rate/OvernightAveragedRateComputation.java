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
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Defines the computation of a rate from a single Overnight index that is averaged daily.
 * <p>
 * An interest rate determined directly from an Overnight index by averaging the value
 * of each day's rate over the period.
 * For example, a rate determined averaging values from 'USD-FED-FUND'.
 */
@BeanDefinition
public final class OvernightAveragedRateComputation
    implements RateComputation, ImmutableBean, Serializable {

  /**
   * The Overnight index.
   * <p>
   * The rate to be paid is based on this index.
   * It will be a well known market index such as 'GBP-SONIA'.
   */
  @PropertyDefinition(validate = "notNull")
  private final OvernightIndex index;
  /**
   * The resolved calendar that the index uses.
   */
  @PropertyDefinition(validate = "notNull")
  private final HolidayCalendar fixingCalendar;
  /**
   * The fixing date associated with the start date of the accrual period.
   * <p>
   * This is also the first fixing date.
   * The overnight rate is observed from this date onwards.
   * <p>
   * In general, the fixing dates and accrual dates are the same for an overnight index.
   * However, in the case of a Tomorrow/Next index, the fixing period is one business day
   * before the accrual period.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The fixing date associated with the end date of the accrual period.
   * <p>
   * The overnight rate is accrued until the maturity date associated with this date.
   * <p>
   * In general, the fixing dates and accrual dates are the same for an overnight index.
   * However, in the case of a Tomorrow/Next index, the fixing period is one business day
   * before the accrual period.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
  /**
   * The number of business days before the end of the period that the rate is cut off.
   * <p>
   * When a rate cut-off applies, the final daily rate is determined this number of days
   * before the end of the period, with any subsequent days having the same rate.
   * <p>
   * The amount must be zero or positive.
   * A value of zero or one will have no effect on the standard calculation.
   * The fixing holiday calendar of the index is used to determine business days.
   * <p>
   * For example, a value of {@code 3} means that the rate observed on
   * {@code (periodEndDate - 3 business days)} is also to be used on
   * {@code (periodEndDate - 2 business days)} and {@code (periodEndDate - 1 business day)}.
   * <p>
   * If there are multiple accrual periods in the payment period, then this
   * should typically only be non-zero in the last accrual period.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final int rateCutOffDays;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance from an index and accrual period dates
   * <p>
   * The dates represent the accrual period.
   * <p>
   * No rate cut-off applies.
   * 
   * @param index  the index
   * @param startDate  the first date of the accrual period
   * @param endDate  the last date of the accrual period
   * @param refData  the reference data to use when resolving holiday calendars
   * @return the rate computation
   */
  public static OvernightAveragedRateComputation of(
      OvernightIndex index,
      LocalDate startDate,
      LocalDate endDate,
      ReferenceData refData) {

    return of(index, startDate, endDate, 0, refData);
  }

  /**
   * Creates an instance from an index, accrual period dates and rate cut-off.
   * <p>
   * Rate cut-off applies if the cut-off is 2 or greater.
   * A value of 0 or 1 should be used if no cut-off applies.
   * 
   * @param index  the index
   * @param startDate  the first date of the accrual period
   * @param endDate  the last date of the accrual period
   * @param rateCutOffDays  the rate cut-off days offset, not negative or zero
   * @param refData  the reference data to use when resolving holiday calendars
   * @return the rate computation
   */
  public static OvernightAveragedRateComputation of(
      OvernightIndex index,
      LocalDate startDate,
      LocalDate endDate,
      int rateCutOffDays,
      ReferenceData refData) {

    return OvernightAveragedRateComputation.builder()
        .index(index)
        .fixingCalendar(index.getFixingCalendar().resolve(refData))
        .startDate(index.calculateFixingFromEffective(startDate, refData))
        .endDate(index.calculateFixingFromEffective(endDate, refData))
        .rateCutOffDays(rateCutOffDays)
        .build();
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the publication date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The publication date is the date on which the fixed rate is actually published.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the publication date
   */
  public LocalDate calculatePublicationFromFixing(LocalDate fixingDate) {
    return fixingCalendar.shift(fixingCalendar.nextOrSame(fixingDate), index.getPublicationDateOffset());
  }

  /**
   * Calculates the effective date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the effective date
   */
  public LocalDate calculateEffectiveFromFixing(LocalDate fixingDate) {
    return fixingCalendar.shift(fixingCalendar.nextOrSame(fixingDate), index.getEffectiveDateOffset());
  }

  /**
   * Calculates the maturity date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The maturity date is the date on which the implied deposit ends.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the maturity date
   */
  public LocalDate calculateMaturityFromFixing(LocalDate fixingDate) {
    return fixingCalendar.shift(fixingCalendar.nextOrSame(fixingDate), index.getEffectiveDateOffset() + 1);
  }

  /**
   * Calculates the fixing date from the effective date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * 
   * @param effectiveDate  the effective date
   * @return the fixing date
   */
  public LocalDate calculateFixingFromEffective(LocalDate effectiveDate) {
    return fixingCalendar.shift(fixingCalendar.nextOrSame(effectiveDate), -index.getEffectiveDateOffset());
  }

  /**
   * Calculates the maturity date from the effective date.
   * <p>
   * The effective date is the date on which the implied deposit starts.
   * The maturity date is the date on which the implied deposit ends.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * 
   * @param effectiveDate  the effective date
   * @return the maturity date
   */
  public LocalDate calculateMaturityFromEffective(LocalDate effectiveDate) {
    return fixingCalendar.shift(fixingCalendar.nextOrSame(effectiveDate), 1);
  }

  /**
   * Creates an observation object for the specified fixing date.
   * 
   * @param fixingDate  the fixing date
   * @return the index observation
   */
  public OvernightIndexObservation observeOn(LocalDate fixingDate) {
    LocalDate publicationDate = calculatePublicationFromFixing(fixingDate);
    LocalDate effectiveDate = calculateEffectiveFromFixing(fixingDate);
    LocalDate maturityDate = calculateMaturityFromEffective(effectiveDate);
    return OvernightIndexObservation.builder()
        .index(getIndex())
        .fixingDate(fixingDate)
        .publicationDate(publicationDate)
        .effectiveDate(effectiveDate)
        .maturityDate(maturityDate)
        .yearFraction(getIndex().getDayCount().yearFraction(effectiveDate, maturityDate))
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public void collectIndices(ImmutableSet.Builder<Index> builder) {
    builder.add(getIndex());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code OvernightAveragedRateComputation}.
   * @return the meta-bean, not null
   */
  public static OvernightAveragedRateComputation.Meta meta() {
    return OvernightAveragedRateComputation.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(OvernightAveragedRateComputation.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static OvernightAveragedRateComputation.Builder builder() {
    return new OvernightAveragedRateComputation.Builder();
  }

  private OvernightAveragedRateComputation(
      OvernightIndex index,
      HolidayCalendar fixingCalendar,
      LocalDate startDate,
      LocalDate endDate,
      int rateCutOffDays) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(fixingCalendar, "fixingCalendar");
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    ArgChecker.notNegative(rateCutOffDays, "rateCutOffDays");
    this.index = index;
    this.fixingCalendar = fixingCalendar;
    this.startDate = startDate;
    this.endDate = endDate;
    this.rateCutOffDays = rateCutOffDays;
    validate();
  }

  @Override
  public OvernightAveragedRateComputation.Meta metaBean() {
    return OvernightAveragedRateComputation.Meta.INSTANCE;
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
   * Gets the Overnight index.
   * <p>
   * The rate to be paid is based on this index.
   * It will be a well known market index such as 'GBP-SONIA'.
   * @return the value of the property, not null
   */
  public OvernightIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the resolved calendar that the index uses.
   * @return the value of the property, not null
   */
  public HolidayCalendar getFixingCalendar() {
    return fixingCalendar;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixing date associated with the start date of the accrual period.
   * <p>
   * This is also the first fixing date.
   * The overnight rate is observed from this date onwards.
   * <p>
   * In general, the fixing dates and accrual dates are the same for an overnight index.
   * However, in the case of a Tomorrow/Next index, the fixing period is one business day
   * before the accrual period.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixing date associated with the end date of the accrual period.
   * <p>
   * The overnight rate is accrued until the maturity date associated with this date.
   * <p>
   * In general, the fixing dates and accrual dates are the same for an overnight index.
   * However, in the case of a Tomorrow/Next index, the fixing period is one business day
   * before the accrual period.
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of business days before the end of the period that the rate is cut off.
   * <p>
   * When a rate cut-off applies, the final daily rate is determined this number of days
   * before the end of the period, with any subsequent days having the same rate.
   * <p>
   * The amount must be zero or positive.
   * A value of zero or one will have no effect on the standard calculation.
   * The fixing holiday calendar of the index is used to determine business days.
   * <p>
   * For example, a value of {@code 3} means that the rate observed on
   * {@code (periodEndDate - 3 business days)} is also to be used on
   * {@code (periodEndDate - 2 business days)} and {@code (periodEndDate - 1 business day)}.
   * <p>
   * If there are multiple accrual periods in the payment period, then this
   * should typically only be non-zero in the last accrual period.
   * @return the value of the property
   */
  public int getRateCutOffDays() {
    return rateCutOffDays;
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
      OvernightAveragedRateComputation other = (OvernightAveragedRateComputation) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(fixingCalendar, other.fixingCalendar) &&
          JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          (rateCutOffDays == other.rateCutOffDays);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixingCalendar);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(rateCutOffDays);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("OvernightAveragedRateComputation{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("fixingCalendar").append('=').append(fixingCalendar).append(',').append(' ');
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("rateCutOffDays").append('=').append(JodaBeanUtils.toString(rateCutOffDays));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code OvernightAveragedRateComputation}.
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
        this, "index", OvernightAveragedRateComputation.class, OvernightIndex.class);
    /**
     * The meta-property for the {@code fixingCalendar} property.
     */
    private final MetaProperty<HolidayCalendar> fixingCalendar = DirectMetaProperty.ofImmutable(
        this, "fixingCalendar", OvernightAveragedRateComputation.class, HolidayCalendar.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", OvernightAveragedRateComputation.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", OvernightAveragedRateComputation.class, LocalDate.class);
    /**
     * The meta-property for the {@code rateCutOffDays} property.
     */
    private final MetaProperty<Integer> rateCutOffDays = DirectMetaProperty.ofImmutable(
        this, "rateCutOffDays", OvernightAveragedRateComputation.class, Integer.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "fixingCalendar",
        "startDate",
        "endDate",
        "rateCutOffDays");

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
        case 394230283:  // fixingCalendar
          return fixingCalendar;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -92095804:  // rateCutOffDays
          return rateCutOffDays;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public OvernightAveragedRateComputation.Builder builder() {
      return new OvernightAveragedRateComputation.Builder();
    }

    @Override
    public Class<? extends OvernightAveragedRateComputation> beanType() {
      return OvernightAveragedRateComputation.class;
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
     * The meta-property for the {@code fixingCalendar} property.
     * @return the meta-property, not null
     */
    public MetaProperty<HolidayCalendar> fixingCalendar() {
      return fixingCalendar;
    }

    /**
     * The meta-property for the {@code startDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> startDate() {
      return startDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> endDate() {
      return endDate;
    }

    /**
     * The meta-property for the {@code rateCutOffDays} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> rateCutOffDays() {
      return rateCutOffDays;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((OvernightAveragedRateComputation) bean).getIndex();
        case 394230283:  // fixingCalendar
          return ((OvernightAveragedRateComputation) bean).getFixingCalendar();
        case -2129778896:  // startDate
          return ((OvernightAveragedRateComputation) bean).getStartDate();
        case -1607727319:  // endDate
          return ((OvernightAveragedRateComputation) bean).getEndDate();
        case -92095804:  // rateCutOffDays
          return ((OvernightAveragedRateComputation) bean).getRateCutOffDays();
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
   * The bean-builder for {@code OvernightAveragedRateComputation}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<OvernightAveragedRateComputation> {

    private OvernightIndex index;
    private HolidayCalendar fixingCalendar;
    private LocalDate startDate;
    private LocalDate endDate;
    private int rateCutOffDays;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(OvernightAveragedRateComputation beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.fixingCalendar = beanToCopy.getFixingCalendar();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.rateCutOffDays = beanToCopy.getRateCutOffDays();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 394230283:  // fixingCalendar
          return fixingCalendar;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -92095804:  // rateCutOffDays
          return rateCutOffDays;
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
        case 394230283:  // fixingCalendar
          this.fixingCalendar = (HolidayCalendar) newValue;
          break;
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case -92095804:  // rateCutOffDays
          this.rateCutOffDays = (Integer) newValue;
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
    public OvernightAveragedRateComputation build() {
      return new OvernightAveragedRateComputation(
          index,
          fixingCalendar,
          startDate,
          endDate,
          rateCutOffDays);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the Overnight index.
     * <p>
     * The rate to be paid is based on this index.
     * It will be a well known market index such as 'GBP-SONIA'.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(OvernightIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the resolved calendar that the index uses.
     * @param fixingCalendar  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingCalendar(HolidayCalendar fixingCalendar) {
      JodaBeanUtils.notNull(fixingCalendar, "fixingCalendar");
      this.fixingCalendar = fixingCalendar;
      return this;
    }

    /**
     * Sets the fixing date associated with the start date of the accrual period.
     * <p>
     * This is also the first fixing date.
     * The overnight rate is observed from this date onwards.
     * <p>
     * In general, the fixing dates and accrual dates are the same for an overnight index.
     * However, in the case of a Tomorrow/Next index, the fixing period is one business day
     * before the accrual period.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the fixing date associated with the end date of the accrual period.
     * <p>
     * The overnight rate is accrued until the maturity date associated with this date.
     * <p>
     * In general, the fixing dates and accrual dates are the same for an overnight index.
     * However, in the case of a Tomorrow/Next index, the fixing period is one business day
     * before the accrual period.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the number of business days before the end of the period that the rate is cut off.
     * <p>
     * When a rate cut-off applies, the final daily rate is determined this number of days
     * before the end of the period, with any subsequent days having the same rate.
     * <p>
     * The amount must be zero or positive.
     * A value of zero or one will have no effect on the standard calculation.
     * The fixing holiday calendar of the index is used to determine business days.
     * <p>
     * For example, a value of {@code 3} means that the rate observed on
     * {@code (periodEndDate - 3 business days)} is also to be used on
     * {@code (periodEndDate - 2 business days)} and {@code (periodEndDate - 1 business day)}.
     * <p>
     * If there are multiple accrual periods in the payment period, then this
     * should typically only be non-zero in the last accrual period.
     * @param rateCutOffDays  the new value
     * @return this, for chaining, not null
     */
    public Builder rateCutOffDays(int rateCutOffDays) {
      ArgChecker.notNegative(rateCutOffDays, "rateCutOffDays");
      this.rateCutOffDays = rateCutOffDays;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("OvernightAveragedRateComputation.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("fixingCalendar").append('=').append(JodaBeanUtils.toString(fixingCalendar)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("rateCutOffDays").append('=').append(JodaBeanUtils.toString(rateCutOffDays));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
