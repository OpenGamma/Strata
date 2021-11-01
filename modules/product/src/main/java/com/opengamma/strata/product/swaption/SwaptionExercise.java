/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.zip;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.LongStream;

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
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.AdjustableDates;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DateAdjuster;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Details as to when a swaption can be exercised.
 * <p>
 * A swaption can have three different kinds of exercise - European, American and Bermudan.
 * A European swaption has one exercise date, an American can exercise on any date, and a Bermudan
 * can exercise on a fixed set of dates.
 */
@BeanDefinition(builderScope = "private")
public final class SwaptionExercise
    implements ImmutableBean, Serializable {

  /**
   * An explicit list of exercise dates.
   * <p>
   * A European swaption has one date in the list.
   * A Bermudan swaption has at least two dates in the list.
   * An American swaption has at exactly two dates in the list, the earliest and latest dates.
   */
  @PropertyDefinition(validate = "notNull")
  private final AdjustableDates dateDefinition;
  /**
   * The frequency of exercise between the earliest and latest dates.
   * <p>
   * An American swaption must set this to one day.
   * <p>
   * A Bermudan swaption might set this to a specific frequency instead of pre-calculating the dates.
   * If it does this, there must only be two dates in the list.
   * The intermediate dates will be calculated by adding multiples of the frequency to the earliest date.
   */
  @PropertyDefinition(get = "optional")
  private final Frequency frequency;
  /**
   * The offset to the swap start date.
   * <p>
   * Each adjusted exercise date has this offset applied to get the start date of the underlying swap.
   */
  @PropertyDefinition(validate = "notNull")
  private final DaysAdjustment swapStartDateOffset;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance for a European swaption.
   * 
   * @param exerciseDate the exercise date
   * @param swapStartDateOffset the swap start date offset
   * @return the exercise
   */
  public static SwaptionExercise ofEuropean(AdjustableDate exerciseDate, DaysAdjustment swapStartDateOffset) {
    AdjustableDates dates = AdjustableDates.of(exerciseDate.getAdjustment(), exerciseDate.getUnadjusted());
    return new SwaptionExercise(dates, null, swapStartDateOffset);
  }

  /**
   * Obtains an instance for an American swaption.
   * 
   * @param earliestExerciseDate the earliest exercise date
   * @param latestExerciseDate the latest exercise date
   * @param dateAdjustment the date adjustment
   * @param swapStartDateOffset the swap start date offset
   * @return the exercise
   */
  public static SwaptionExercise ofAmerican(
      LocalDate earliestExerciseDate,
      LocalDate latestExerciseDate,
      BusinessDayAdjustment dateAdjustment,
      DaysAdjustment swapStartDateOffset) {

    AdjustableDates dates = AdjustableDates.of(dateAdjustment, earliestExerciseDate, latestExerciseDate);
    return new SwaptionExercise(dates, Frequency.P1D, swapStartDateOffset);
  }

  /**
   * Obtains an instance for a Bermudan swaption.
   * 
   * @param exerciseDates the exercise dates
   * @param swapStartDateOffset the swap start date offset
   * @return the exercise
   */
  public static SwaptionExercise ofBermudan(AdjustableDates exerciseDates, DaysAdjustment swapStartDateOffset) {
    return new SwaptionExercise(exerciseDates, null, swapStartDateOffset);
  }

  /**
   * Obtains an instance for a Bermudan swaption where the dates are calculated.
   * <p>
   * For example, if the dates represent a 5 year period and the frequency is yearly then
   * the Bermudan swaption can be exercised each year in the period. The exact dates are
   * calculated by adding multiples of the frequency to the earliest date.
   * 
   * @param earliestExerciseDate the earliest exercise date
   * @param latestExerciseDate the latest exercise date
   * @param dateAdjustment the date adjustment
   * @param frequency the frequency
   * @param swapStartDateOffset the swap start date offset
   * @return the exercise
   */
  public static SwaptionExercise ofBermudan(
      LocalDate earliestExerciseDate,
      LocalDate latestExerciseDate,
      BusinessDayAdjustment dateAdjustment,
      Frequency frequency,
      DaysAdjustment swapStartDateOffset) {

    AdjustableDates dates = AdjustableDates.of(dateAdjustment, earliestExerciseDate, latestExerciseDate);
    Frequency normalizedFrequency = Frequency.of(frequency.getPeriod().normalized());
    return new SwaptionExercise(dates, normalizedFrequency, swapStartDateOffset);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(
        Ordering.natural().isStrictlyOrdered(dateDefinition.getUnadjusted()),
        "Dates must be in order and without duplicates");
    if (frequency != null && dateDefinition.getUnadjusted().size() != 2) {
      throw new IllegalArgumentException("Frequency can only be used when there two exercise dates are defined");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the exercise is European.
   * 
   * @return true if European exercise on a single date
   */
  public boolean isEuropean() {
    return dateDefinition.getUnadjusted().size() == 1;
  }

  /**
   * Checks if the exercise is American.
   * 
   * @return true if American exercise on any date
   */
  public boolean isAmerican() {
    return dateDefinition.getUnadjusted().size() == 2 && Frequency.P1D.equals(frequency);
  }

  /**
   * Checks if the exercise is Bermudan.
   * 
   * @return true if Bermudan exercise on a specific set of dates
   */
  public boolean isBermudan() {
    return !isEuropean() && !isAmerican();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the calculated list of exercise dates.
   * <p>
   * This could be a large list in the case of an American exercise.
   * 
   * @return the exercise dates
   */
  public AdjustableDates calculateDates() {
    if (frequency != null) {
      LocalDate start = dateDefinition.getUnadjusted().get(0);
      LocalDate end = dateDefinition.getUnadjusted().get(1);
      if (frequency.equals(Frequency.P1D)) {
        ImmutableList<LocalDate> dates = LongStream.rangeClosed(start.toEpochDay(), end.toEpochDay())
            .mapToObj(LocalDate::ofEpochDay)
            .collect(toImmutableList());
        return AdjustableDates.of(dateDefinition.getAdjustment(), dates);
      }
      ImmutableList.Builder<LocalDate> dates = ImmutableList.<LocalDate>builder();
      dates.add(start);
      for (int i = 1; ; i++) {
        LocalDate date = start.plus(frequency.getPeriod().multipliedBy(i));
        if (date.isBefore(end)) {
          dates.add(date);
        } else {
          dates.add(end);
          break;
        }
      }
      return AdjustableDates.of(dateDefinition.getAdjustment(), dates.build());
    } else {
      return dateDefinition;
    }
  }

  //-------------------------------------------------------------------------
  // resolves the date definition
  SwaptionExerciseDates resolve(ReferenceData refData) {
    AdjustableDates defn = isBermudan() ? calculateDates() : dateDefinition;
    ImmutableList<LocalDate> unadjusted = defn.getUnadjusted();
    ImmutableList<LocalDate> adjusted = defn.adjusted(refData);
    DateAdjuster startDateOffset = swapStartDateOffset.resolve(refData);
    ImmutableList<SwaptionExerciseDate> dates = zip(adjusted.stream(), unadjusted.stream())
        .map(pair -> SwaptionExerciseDate.builder()
            .exerciseDate(pair.getFirst())
            .unadjustedExerciseDate(pair.getSecond())
            .swapStartDate(startDateOffset.adjust(pair.getFirst()))
            .build())
        .collect(toImmutableList());
    return SwaptionExerciseDates.of(dates, isAmerican());
  }

  //-------------------------------------------------------------------------
  /**
   * Selects a single exercise date based on the proposed date.
   * <p>
   * This validates the proposed exercise date and returns it.
   * <p>
   * The date is matched as an adjusted date first, then as an unadjusted date.
   * If the date can only be an adjusted date, the result will use {@link BusinessDayAdjustment#NONE}.
   * 
   * @param proposedExerciseDate  the proposed exercise date
   * @param refData  the reference data
   * @return the exercise dates
   * @throws IllegalArgumentException if the proposed exercise date is not valid
   */
  public AdjustableDate selectDate(LocalDate proposedExerciseDate, ReferenceData refData) {
    DateAdjuster adjuster = dateDefinition.getAdjustment().resolve(refData);
    if (Frequency.P1D.equals(frequency)) {
      return selectAmerican(proposedExerciseDate, adjuster);
    } else {
      return selectStandard(proposedExerciseDate, adjuster);
    }
  }

  // American (avoid calculating the whole set of dates)
  private AdjustableDate selectAmerican(LocalDate proposedExerciseDate, DateAdjuster adjuster) {
    LocalDate start = dateDefinition.getUnadjusted().get(0);
    LocalDate end = dateDefinition.getUnadjusted().get(1);
    // search adjusted dates
    for (LocalDate unadjusted = end; !unadjusted.isBefore(start); unadjusted = unadjusted.minusDays(1)) {
      if (adjuster.adjust(unadjusted).equals(proposedExerciseDate)) {
        return unadjusted.equals(proposedExerciseDate) ?
            AdjustableDate.of(proposedExerciseDate, dateDefinition.getAdjustment()) :
            AdjustableDate.of(proposedExerciseDate);
      }
    }
    // search unadjusted dates
    if (!proposedExerciseDate.isBefore(start) && !proposedExerciseDate.isAfter(end)) {
      return AdjustableDate.of(proposedExerciseDate, dateDefinition.getAdjustment());
    }
    throw new IllegalArgumentException("Invalid exercise date: " + proposedExerciseDate);
  }

  // Bermudan or European
  private AdjustableDate selectStandard(LocalDate proposedExerciseDate, DateAdjuster adjuster) {
    AdjustableDates dates = calculateDates();
    // search adjusted dates
    for (LocalDate unadjusted : dates.getUnadjusted()) {
      if (adjuster.adjust(unadjusted).equals(proposedExerciseDate)) {
        return unadjusted.equals(proposedExerciseDate) ?
            AdjustableDate.of(proposedExerciseDate, dateDefinition.getAdjustment()) :
            AdjustableDate.of(proposedExerciseDate);
      }
    }
    // search unadjusted dates
    if (dates.getUnadjusted().contains(proposedExerciseDate)) {
      return AdjustableDate.of(proposedExerciseDate, dateDefinition.getAdjustment());
    }
    throw new IllegalArgumentException("Invalid exercise date: " + proposedExerciseDate);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SwaptionExercise}.
   * @return the meta-bean, not null
   */
  public static SwaptionExercise.Meta meta() {
    return SwaptionExercise.Meta.INSTANCE;
  }

  static {
    MetaBean.register(SwaptionExercise.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private SwaptionExercise(
      AdjustableDates dateDefinition,
      Frequency frequency,
      DaysAdjustment swapStartDateOffset) {
    JodaBeanUtils.notNull(dateDefinition, "dateDefinition");
    JodaBeanUtils.notNull(swapStartDateOffset, "swapStartDateOffset");
    this.dateDefinition = dateDefinition;
    this.frequency = frequency;
    this.swapStartDateOffset = swapStartDateOffset;
    validate();
  }

  @Override
  public SwaptionExercise.Meta metaBean() {
    return SwaptionExercise.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets an explicit list of exercise dates.
   * <p>
   * A European swaption has one date in the list.
   * A Bermudan swaption has at least two dates in the list.
   * An American swaption has at exactly two dates in the list, the earliest and latest dates.
   * @return the value of the property, not null
   */
  public AdjustableDates getDateDefinition() {
    return dateDefinition;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the frequency of exercise between the earliest and latest dates.
   * <p>
   * An American swaption must set this to one day.
   * <p>
   * A Bermudan swaption might set this to a specific frequency instead of pre-calculating the dates.
   * If it does this, there must only be two dates in the list.
   * The intermediate dates will be calculated by adding multiples of the frequency to the earliest date.
   * @return the optional value of the property, not null
   */
  public Optional<Frequency> getFrequency() {
    return Optional.ofNullable(frequency);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset to the swap start date.
   * <p>
   * Each adjusted exercise date has this offset applied to get the start date of the underlying swap.
   * @return the value of the property, not null
   */
  public DaysAdjustment getSwapStartDateOffset() {
    return swapStartDateOffset;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SwaptionExercise other = (SwaptionExercise) obj;
      return JodaBeanUtils.equal(dateDefinition, other.dateDefinition) &&
          JodaBeanUtils.equal(frequency, other.frequency) &&
          JodaBeanUtils.equal(swapStartDateOffset, other.swapStartDateOffset);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(dateDefinition);
    hash = hash * 31 + JodaBeanUtils.hashCode(frequency);
    hash = hash * 31 + JodaBeanUtils.hashCode(swapStartDateOffset);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("SwaptionExercise{");
    buf.append("dateDefinition").append('=').append(JodaBeanUtils.toString(dateDefinition)).append(',').append(' ');
    buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ');
    buf.append("swapStartDateOffset").append('=').append(JodaBeanUtils.toString(swapStartDateOffset));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SwaptionExercise}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code dateDefinition} property.
     */
    private final MetaProperty<AdjustableDates> dateDefinition = DirectMetaProperty.ofImmutable(
        this, "dateDefinition", SwaptionExercise.class, AdjustableDates.class);
    /**
     * The meta-property for the {@code frequency} property.
     */
    private final MetaProperty<Frequency> frequency = DirectMetaProperty.ofImmutable(
        this, "frequency", SwaptionExercise.class, Frequency.class);
    /**
     * The meta-property for the {@code swapStartDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> swapStartDateOffset = DirectMetaProperty.ofImmutable(
        this, "swapStartDateOffset", SwaptionExercise.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "dateDefinition",
        "frequency",
        "swapStartDateOffset");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 257736609:  // dateDefinition
          return dateDefinition;
        case -70023844:  // frequency
          return frequency;
        case 1366770128:  // swapStartDateOffset
          return swapStartDateOffset;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SwaptionExercise> builder() {
      return new SwaptionExercise.Builder();
    }

    @Override
    public Class<? extends SwaptionExercise> beanType() {
      return SwaptionExercise.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code dateDefinition} property.
     * @return the meta-property, not null
     */
    public MetaProperty<AdjustableDates> dateDefinition() {
      return dateDefinition;
    }

    /**
     * The meta-property for the {@code frequency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Frequency> frequency() {
      return frequency;
    }

    /**
     * The meta-property for the {@code swapStartDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> swapStartDateOffset() {
      return swapStartDateOffset;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 257736609:  // dateDefinition
          return ((SwaptionExercise) bean).getDateDefinition();
        case -70023844:  // frequency
          return ((SwaptionExercise) bean).frequency;
        case 1366770128:  // swapStartDateOffset
          return ((SwaptionExercise) bean).getSwapStartDateOffset();
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
   * The bean-builder for {@code SwaptionExercise}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<SwaptionExercise> {

    private AdjustableDates dateDefinition;
    private Frequency frequency;
    private DaysAdjustment swapStartDateOffset;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 257736609:  // dateDefinition
          return dateDefinition;
        case -70023844:  // frequency
          return frequency;
        case 1366770128:  // swapStartDateOffset
          return swapStartDateOffset;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 257736609:  // dateDefinition
          this.dateDefinition = (AdjustableDates) newValue;
          break;
        case -70023844:  // frequency
          this.frequency = (Frequency) newValue;
          break;
        case 1366770128:  // swapStartDateOffset
          this.swapStartDateOffset = (DaysAdjustment) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public SwaptionExercise build() {
      return new SwaptionExercise(
          dateDefinition,
          frequency,
          swapStartDateOffset);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("SwaptionExercise.Builder{");
      buf.append("dateDefinition").append('=').append(JodaBeanUtils.toString(dateDefinition)).append(',').append(' ');
      buf.append("frequency").append('=').append(JodaBeanUtils.toString(frequency)).append(',').append(' ');
      buf.append("swapStartDateOffset").append('=').append(JodaBeanUtils.toString(swapStartDateOffset));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
