/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.Guavate.list;
import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An adjustable list of dates.
 * <p>
 * This class combines a list of unadjusted dates with the single business day adjustment necessary to adjust them.
 * Calling the {@link #adjusted(ReferenceData)} method will return the adjusted dates.
 */
@BeanDefinition(builderScope = "private")
public final class AdjustableDates
    implements ImmutableBean, Serializable {

  /**
   * The unadjusted dates, in order.
   * <p>
   * These dates may be non-business days.
   * The business day adjustment is used to ensure each date is a valid business day.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableList<LocalDate> unadjusted;
  /**
   * The business day adjustment that is to be applied to the unadjusted dates.
   * <p>
   * This is used to adjust each date if it is not a business day.
   */
  @PropertyDefinition(validate = "notNull")
  private final BusinessDayAdjustment adjustment;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with no business day adjustment.
   * <p>
   * This creates an instance from the specified dates.
   * No business day adjustment applies, thus the result of {@link #adjusted(ReferenceData)}
   * is the specified dates.
   * 
   * @param firstDate  the first date
   * @param remainingDates  the remaining dates, in order
   * @return the adjustable dates
   */
  public static AdjustableDates of(LocalDate firstDate, LocalDate... remainingDates) {
    return new AdjustableDates(list(firstDate, remainingDates), BusinessDayAdjustment.NONE);
  }

  /**
   * Obtains an instance with no business day adjustment.
   * <p>
   * This creates an instance from the specified dates.
   * No business day adjustment applies, thus the result of {@link #adjusted(ReferenceData)}
   * is the specified dates.
   * 
   * @param dates  the dates, at least size 1, in order
   * @return the adjustable dates
   */
  public static AdjustableDates of(List<LocalDate> dates) {
    return new AdjustableDates(ImmutableList.copyOf(dates), BusinessDayAdjustment.NONE);
  }

  /**
   * Obtains an instance with a business day adjustment.
   * <p>
   * This creates an instance from the unadjusted dates and business day adjustment.
   * The adjusted dates are accessible via {@link #adjusted(ReferenceData)}.
   * 
   * @param adjustment  the business day adjustment to apply to the unadjusted date
   * @param firstDate  the first date
   * @param remainingDates  the remaining dates, in order
   * @return the adjustable dates
   */
  public static AdjustableDates of(BusinessDayAdjustment adjustment, LocalDate firstDate, LocalDate... remainingDates) {
    return new AdjustableDates(list(firstDate, remainingDates), adjustment);
  }

  /**
   * Obtains an instance with a business day adjustment.
   * <p>
   * This creates an instance from the unadjusted dates and business day adjustment.
   * The adjusted dates are accessible via {@link #adjusted(ReferenceData)}.
   * 
   * @param adjustment  the business day adjustment to apply to the unadjusted date
   * @param dates  the dates, in order
   * @return the adjustable dates
   */
  public static AdjustableDates of(BusinessDayAdjustment adjustment, List<LocalDate> dates) {
    return new AdjustableDates(ImmutableList.copyOf(dates), adjustment);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(
        Ordering.natural().isStrictlyOrdered(unadjusted),
        "Dates must be in order and without duplicates");
  }

  //-------------------------------------------------------------------------
  /**
   * Adjusts the dates using the business day adjustment.
   * <p>
   * This returns the adjusted dates, calculated by applying the business day
   * adjustment to each unadjusted date. Duplicates are removed.
   * 
   * @param refData  the reference data to use
   * @return the adjusted dates
   */
  public ImmutableList<LocalDate> adjusted(ReferenceData refData) {
    DateAdjuster adjuster = adjustment.resolve(refData);
    return unadjusted.stream().map(adjuster::adjust).distinct().collect(toImmutableList());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a list of {@code AdjustableDate} equivalent to this instance.
   * 
   * @return the adjusted date
   */
  public ImmutableList<AdjustableDate> toAdjustableDateList() {
    return unadjusted.stream().map(date -> AdjustableDate.of(date, adjustment)).collect(toImmutableList());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a string describing the adjustable dates.
   * 
   * @return the descriptive string
   */
  @Override
  public String toString() {
    if (adjustment.equals(BusinessDayAdjustment.NONE)) {
      return unadjusted.toString();
    }
    return new StringBuilder(64)
        .append(unadjusted)
        .append(" adjusted by ")
        .append(adjustment).toString();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code AdjustableDates}.
   * @return the meta-bean, not null
   */
  public static AdjustableDates.Meta meta() {
    return AdjustableDates.Meta.INSTANCE;
  }

  static {
    MetaBean.register(AdjustableDates.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private AdjustableDates(
      List<LocalDate> unadjusted,
      BusinessDayAdjustment adjustment) {
    JodaBeanUtils.notEmpty(unadjusted, "unadjusted");
    JodaBeanUtils.notNull(adjustment, "adjustment");
    this.unadjusted = ImmutableList.copyOf(unadjusted);
    this.adjustment = adjustment;
    validate();
  }

  @Override
  public AdjustableDates.Meta metaBean() {
    return AdjustableDates.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the unadjusted dates, in order.
   * <p>
   * These dates may be non-business days.
   * The business day adjustment is used to ensure each date is a valid business day.
   * @return the value of the property, not empty
   */
  public ImmutableList<LocalDate> getUnadjusted() {
    return unadjusted;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day adjustment that is to be applied to the unadjusted dates.
   * <p>
   * This is used to adjust each date if it is not a business day.
   * @return the value of the property, not null
   */
  public BusinessDayAdjustment getAdjustment() {
    return adjustment;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      AdjustableDates other = (AdjustableDates) obj;
      return JodaBeanUtils.equal(unadjusted, other.unadjusted) &&
          JodaBeanUtils.equal(adjustment, other.adjustment);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(unadjusted);
    hash = hash * 31 + JodaBeanUtils.hashCode(adjustment);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code AdjustableDates}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code unadjusted} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<LocalDate>> unadjusted = DirectMetaProperty.ofImmutable(
        this, "unadjusted", AdjustableDates.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code adjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> adjustment = DirectMetaProperty.ofImmutable(
        this, "adjustment", AdjustableDates.class, BusinessDayAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "unadjusted",
        "adjustment");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 482476551:  // unadjusted
          return unadjusted;
        case 1977085293:  // adjustment
          return adjustment;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends AdjustableDates> builder() {
      return new AdjustableDates.Builder();
    }

    @Override
    public Class<? extends AdjustableDates> beanType() {
      return AdjustableDates.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code unadjusted} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<LocalDate>> unadjusted() {
      return unadjusted;
    }

    /**
     * The meta-property for the {@code adjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> adjustment() {
      return adjustment;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 482476551:  // unadjusted
          return ((AdjustableDates) bean).getUnadjusted();
        case 1977085293:  // adjustment
          return ((AdjustableDates) bean).getAdjustment();
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
   * The bean-builder for {@code AdjustableDates}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<AdjustableDates> {

    private List<LocalDate> unadjusted = ImmutableList.of();
    private BusinessDayAdjustment adjustment;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 482476551:  // unadjusted
          return unadjusted;
        case 1977085293:  // adjustment
          return adjustment;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 482476551:  // unadjusted
          this.unadjusted = (List<LocalDate>) newValue;
          break;
        case 1977085293:  // adjustment
          this.adjustment = (BusinessDayAdjustment) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public AdjustableDates build() {
      return new AdjustableDates(
          unadjusted,
          adjustment);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("AdjustableDates.Builder{");
      buf.append("unadjusted").append('=').append(JodaBeanUtils.toString(unadjusted)).append(',').append(' ');
      buf.append("adjustment").append('=').append(JodaBeanUtils.toString(adjustment));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
