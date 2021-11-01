/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.collect.Guavate.ensureOnlyOne;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLegType;

/**
 * An option on an underlying swap.
 * <p>
 * A swaption is a financial instrument that provides an option based on the future value of a swap.
 * The underlying swap must be a single currency, Fixed-Float swap with a single index.
 * The index may be Ibor or Overnight.
 * <p>
 * Strata supports European swaptions for pricing.
 * However, the trade model supports Bermudan and American exercise.
 */
@BeanDefinition
public final class Swaption
    implements Product, Resolvable<ResolvedSwaption>, ImmutableBean, Serializable {

  /**
   * Whether the option is long or short.
   * <p>
   * Long indicates that the owner wants the option to be in the money at expiry.
   * Short indicates that the owner wants the option to be out of the money at expiry.
   */
  @PropertyDefinition(validate = "notNull")
  private final LongShort longShort;
  /**
   * Settlement method.
   * <p>
   * The settlement of the option is specified by {@link SwaptionSettlement}.
   */
  @PropertyDefinition(validate = "notNull")
  private final SwaptionSettlement swaptionSettlement;
  /**
   * The exercise information, optional.
   * <p>
   * A swaption can have three different kinds of exercise - European, American and Bermudan.
   * A European swaption has one exercise date, an American can exercise on any date, and a Bermudan
   * can exercise on a fixed set of dates.
   * <p>
   * If not present, the swaption is considered to be a European swaption as per the expiry date.
   */
  @PropertyDefinition(get = "optional")
  private final SwaptionExercise exerciseInfo;
  /**
   * The expiry date of the option.
   * <p>
   * This is the last date that the swaption can be exercised.
   * To represent Bermudan and American swaptions, or to represent a European swaption where the swap start
   * date is calculated dynamically, see the {@code exerciseOptions} field.
   * <p>
   * This date is typically set to be a valid business day.
   * However, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   */
  @PropertyDefinition(validate = "notNull")
  private final AdjustableDate expiryDate;
  /**
   * The expiry time of the option.
   * <p>
   * The expiry time is related to the expiry date and time-zone.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalTime expiryTime;
  /**
   * The time-zone of the expiry time.
   * <p>
   * The expiry time-zone is related to the expiry date and time.
   */
  @PropertyDefinition(validate = "notNull")
  private final ZoneId expiryZone;
  /**
   * The underlying swap.
   * <p>
   * At expiry, if the option is exercised, this swap will be entered into.
   * The swap description is the swap as viewed by the party long the option.
   */
  @PropertyDefinition(validate = "notNull")
  private final Swap underlying;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(!underlying.isCrossCurrency(), "Underlying swap must not be cross-currency");
    ArgChecker.isTrue(underlying.getLegs(SwapLegType.FIXED).size() == 1, "Underlying swap must have one fixed leg");
    ArgChecker.isTrue(
        (underlying.getLegs(SwapLegType.IBOR).size() == 1) || (underlying.getLegs(SwapLegType.OVERNIGHT).size() == 1),
        "Underlying swap must have one Ibor or Overnight leg");
    ArgChecker.isTrue(underlying.allIndices().size() == 1, "Underlying swap must have one index");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the expiry date-time.
   * <p>
   * The option expires at this date and time.
   * <p>
   * The result is returned by combining the expiry date, time and time-zone.
   * 
   * @return the expiry date and time
   */
  public ZonedDateTime getExpiry() {
    return expiryDate.getUnadjusted().atTime(expiryTime).atZone(expiryZone);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency of the swaption.
   * <p>
   * This is the currency of the underlying swap, which is not allowed to be cross-currency.
   * 
   * @return the expiry date and time
   */
  public Currency getCurrency() {
    return underlying.getLegs().stream()
        .map(leg -> leg.getCurrency())
        .distinct()
        .reduce(ensureOnlyOne())
        .get();
  }

  @Override
  public ImmutableSet<Currency> allCurrencies() {
    return ImmutableSet.of(getCurrency());
  }

  /**
   * Gets the index of the underlying swap.
   * 
   * @return the index of the underlying swap
   */
  public RateIndex getIndex() {
    return (RateIndex) underlying.allIndices().iterator().next();
  }

  //-------------------------------------------------------------------------
  /**
   * Selects one of the exercise dates.
   * <p>
   * This returns a swaption with the {@code exerciseInfo} removed.
   * The expiry date and underlying swap will be set in accordance with the selected exercise date.
   * If the swaption has no exercise information, this checks that the exercise date matches the expiry date.
   * <p>
   * The date is matched as an adjusted date first, then as an unadjusted date.
   * If the date can only be an adjusted date, the result will use {@link BusinessDayAdjustment#NONE}.
   * 
   * @param exerciseDate  the exercise date, which must be valid for the swaption
   * @param refData  the reference data
   * @return the swaption in European form with the specified exercise date
   * @throws IllegalArgumentException if the proposed exercise date is not valid
   */
  public Swaption selectExerciseDate(LocalDate exerciseDate, ReferenceData refData) {
    if (exerciseInfo == null) {
      LocalDate adjustedExpiryDate = expiryDate.adjusted(refData);
      if (exerciseDate.equals(adjustedExpiryDate)) {
        if (expiryDate.getUnadjusted().equals(exerciseDate)) {
          return this;
        } else {
          return toBuilder().expiryDate(AdjustableDate.of(adjustedExpiryDate)).build();
        }
      }
      if (exerciseDate.equals(expiryDate.getUnadjusted())) {
        return this;
      }
      throw new IllegalArgumentException(
          "Unable to exercise swaption, valid exercise dates unknown: " + exerciseDate);
    }
    AdjustableDate adjutableExerciseDate = exerciseInfo.selectDate(exerciseDate, refData);
    LocalDate adjustedExerciseDate = adjutableExerciseDate.adjusted(refData);
    LocalDate swapStartDate = exerciseInfo.getSwapStartDateOffset().adjust(adjustedExerciseDate, refData);
    return toBuilder()
        .exerciseInfo(null)
        .expiryDate(adjutableExerciseDate)
        .underlying(underlying.replaceStartDate(swapStartDate))
        .build();
  }

  /**
   * Exercises the swaption into a swap at one of the optional exercise dates.
   * <p>
   * The start date of the swap will be set in accordance with the selected exercise date.
   * See {@link #selectExerciseDate(LocalDate, ReferenceData)} for more info.
   * 
   * @param exerciseDate  the exercise date, which must be valid for the swaption
   * @param refData  the reference data
   * @return the exercised swap
   * @throws IllegalArgumentException if the proposed exercise date is not valid
   */
  public Swap exercise(LocalDate exerciseDate, ReferenceData refData) {
    return selectExerciseDate(exerciseDate, refData).getUnderlying();
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedSwaption resolve(ReferenceData refData) {
    LocalDate unadjustedExpiry = expiryDate.getUnadjusted();
    LocalDate adjustedExpiry = expiryDate.adjusted(refData);

    // setup suitable exercise info where it was not defined
    SwaptionExerciseDates exerciseDates;
    if (exerciseInfo == null) {
      LocalDate swapStartDate = underlying.getStartDate().adjusted(refData);
      SwaptionExerciseDate exercise = SwaptionExerciseDate.of(adjustedExpiry, unadjustedExpiry, swapStartDate);
      exerciseDates = SwaptionExerciseDates.ofEuropean(exercise);
    } else {
      exerciseDates = exerciseInfo.resolve(refData);
    }
    // trust that the expiry date and swap start date are valid
    // throwing an exception if they are not seems to harsh
    return ResolvedSwaption.builder()
        .expiry(unadjustedExpiry.atTime(expiryTime).atZone(expiryZone))
        .longShort(longShort)
        .swaptionSettlement(swaptionSettlement)
        .exerciseInfo(exerciseDates)
        .underlying(underlying.resolve(refData))
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code Swaption}.
   * @return the meta-bean, not null
   */
  public static Swaption.Meta meta() {
    return Swaption.Meta.INSTANCE;
  }

  static {
    MetaBean.register(Swaption.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static Swaption.Builder builder() {
    return new Swaption.Builder();
  }

  private Swaption(
      LongShort longShort,
      SwaptionSettlement swaptionSettlement,
      SwaptionExercise exerciseInfo,
      AdjustableDate expiryDate,
      LocalTime expiryTime,
      ZoneId expiryZone,
      Swap underlying) {
    JodaBeanUtils.notNull(longShort, "longShort");
    JodaBeanUtils.notNull(swaptionSettlement, "swaptionSettlement");
    JodaBeanUtils.notNull(expiryDate, "expiryDate");
    JodaBeanUtils.notNull(expiryTime, "expiryTime");
    JodaBeanUtils.notNull(expiryZone, "expiryZone");
    JodaBeanUtils.notNull(underlying, "underlying");
    this.longShort = longShort;
    this.swaptionSettlement = swaptionSettlement;
    this.exerciseInfo = exerciseInfo;
    this.expiryDate = expiryDate;
    this.expiryTime = expiryTime;
    this.expiryZone = expiryZone;
    this.underlying = underlying;
    validate();
  }

  @Override
  public Swaption.Meta metaBean() {
    return Swaption.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the option is long or short.
   * <p>
   * Long indicates that the owner wants the option to be in the money at expiry.
   * Short indicates that the owner wants the option to be out of the money at expiry.
   * @return the value of the property, not null
   */
  public LongShort getLongShort() {
    return longShort;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets settlement method.
   * <p>
   * The settlement of the option is specified by {@link SwaptionSettlement}.
   * @return the value of the property, not null
   */
  public SwaptionSettlement getSwaptionSettlement() {
    return swaptionSettlement;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the exercise information, optional.
   * <p>
   * A swaption can have three different kinds of exercise - European, American and Bermudan.
   * A European swaption has one exercise date, an American can exercise on any date, and a Bermudan
   * can exercise on a fixed set of dates.
   * <p>
   * If not present, the swaption is considered to be a European swaption as per the expiry date.
   * @return the optional value of the property, not null
   */
  public Optional<SwaptionExercise> getExerciseInfo() {
    return Optional.ofNullable(exerciseInfo);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiry date of the option.
   * <p>
   * This is the last date that the swaption can be exercised.
   * To represent Bermudan and American swaptions, or to represent a European swaption where the swap start
   * date is calculated dynamically, see the {@code exerciseOptions} field.
   * <p>
   * This date is typically set to be a valid business day.
   * However, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
   * @return the value of the property, not null
   */
  public AdjustableDate getExpiryDate() {
    return expiryDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiry time of the option.
   * <p>
   * The expiry time is related to the expiry date and time-zone.
   * @return the value of the property, not null
   */
  public LocalTime getExpiryTime() {
    return expiryTime;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-zone of the expiry time.
   * <p>
   * The expiry time-zone is related to the expiry date and time.
   * @return the value of the property, not null
   */
  public ZoneId getExpiryZone() {
    return expiryZone;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying swap.
   * <p>
   * At expiry, if the option is exercised, this swap will be entered into.
   * The swap description is the swap as viewed by the party long the option.
   * @return the value of the property, not null
   */
  public Swap getUnderlying() {
    return underlying;
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
      Swaption other = (Swaption) obj;
      return JodaBeanUtils.equal(longShort, other.longShort) &&
          JodaBeanUtils.equal(swaptionSettlement, other.swaptionSettlement) &&
          JodaBeanUtils.equal(exerciseInfo, other.exerciseInfo) &&
          JodaBeanUtils.equal(expiryDate, other.expiryDate) &&
          JodaBeanUtils.equal(expiryTime, other.expiryTime) &&
          JodaBeanUtils.equal(expiryZone, other.expiryZone) &&
          JodaBeanUtils.equal(underlying, other.underlying);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(longShort);
    hash = hash * 31 + JodaBeanUtils.hashCode(swaptionSettlement);
    hash = hash * 31 + JodaBeanUtils.hashCode(exerciseInfo);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryZone);
    hash = hash * 31 + JodaBeanUtils.hashCode(underlying);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("Swaption{");
    buf.append("longShort").append('=').append(JodaBeanUtils.toString(longShort)).append(',').append(' ');
    buf.append("swaptionSettlement").append('=').append(JodaBeanUtils.toString(swaptionSettlement)).append(',').append(' ');
    buf.append("exerciseInfo").append('=').append(JodaBeanUtils.toString(exerciseInfo)).append(',').append(' ');
    buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(expiryDate)).append(',').append(' ');
    buf.append("expiryTime").append('=').append(JodaBeanUtils.toString(expiryTime)).append(',').append(' ');
    buf.append("expiryZone").append('=').append(JodaBeanUtils.toString(expiryZone)).append(',').append(' ');
    buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Swaption}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code longShort} property.
     */
    private final MetaProperty<LongShort> longShort = DirectMetaProperty.ofImmutable(
        this, "longShort", Swaption.class, LongShort.class);
    /**
     * The meta-property for the {@code swaptionSettlement} property.
     */
    private final MetaProperty<SwaptionSettlement> swaptionSettlement = DirectMetaProperty.ofImmutable(
        this, "swaptionSettlement", Swaption.class, SwaptionSettlement.class);
    /**
     * The meta-property for the {@code exerciseInfo} property.
     */
    private final MetaProperty<SwaptionExercise> exerciseInfo = DirectMetaProperty.ofImmutable(
        this, "exerciseInfo", Swaption.class, SwaptionExercise.class);
    /**
     * The meta-property for the {@code expiryDate} property.
     */
    private final MetaProperty<AdjustableDate> expiryDate = DirectMetaProperty.ofImmutable(
        this, "expiryDate", Swaption.class, AdjustableDate.class);
    /**
     * The meta-property for the {@code expiryTime} property.
     */
    private final MetaProperty<LocalTime> expiryTime = DirectMetaProperty.ofImmutable(
        this, "expiryTime", Swaption.class, LocalTime.class);
    /**
     * The meta-property for the {@code expiryZone} property.
     */
    private final MetaProperty<ZoneId> expiryZone = DirectMetaProperty.ofImmutable(
        this, "expiryZone", Swaption.class, ZoneId.class);
    /**
     * The meta-property for the {@code underlying} property.
     */
    private final MetaProperty<Swap> underlying = DirectMetaProperty.ofImmutable(
        this, "underlying", Swaption.class, Swap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "longShort",
        "swaptionSettlement",
        "exerciseInfo",
        "expiryDate",
        "expiryTime",
        "expiryZone",
        "underlying");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          return longShort;
        case -1937554512:  // swaptionSettlement
          return swaptionSettlement;
        case -466669914:  // exerciseInfo
          return exerciseInfo;
        case -816738431:  // expiryDate
          return expiryDate;
        case -816254304:  // expiryTime
          return expiryTime;
        case -816069761:  // expiryZone
          return expiryZone;
        case -1770633379:  // underlying
          return underlying;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public Swaption.Builder builder() {
      return new Swaption.Builder();
    }

    @Override
    public Class<? extends Swaption> beanType() {
      return Swaption.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code longShort} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LongShort> longShort() {
      return longShort;
    }

    /**
     * The meta-property for the {@code swaptionSettlement} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SwaptionSettlement> swaptionSettlement() {
      return swaptionSettlement;
    }

    /**
     * The meta-property for the {@code exerciseInfo} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SwaptionExercise> exerciseInfo() {
      return exerciseInfo;
    }

    /**
     * The meta-property for the {@code expiryDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<AdjustableDate> expiryDate() {
      return expiryDate;
    }

    /**
     * The meta-property for the {@code expiryTime} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalTime> expiryTime() {
      return expiryTime;
    }

    /**
     * The meta-property for the {@code expiryZone} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZoneId> expiryZone() {
      return expiryZone;
    }

    /**
     * The meta-property for the {@code underlying} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Swap> underlying() {
      return underlying;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          return ((Swaption) bean).getLongShort();
        case -1937554512:  // swaptionSettlement
          return ((Swaption) bean).getSwaptionSettlement();
        case -466669914:  // exerciseInfo
          return ((Swaption) bean).exerciseInfo;
        case -816738431:  // expiryDate
          return ((Swaption) bean).getExpiryDate();
        case -816254304:  // expiryTime
          return ((Swaption) bean).getExpiryTime();
        case -816069761:  // expiryZone
          return ((Swaption) bean).getExpiryZone();
        case -1770633379:  // underlying
          return ((Swaption) bean).getUnderlying();
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
   * The bean-builder for {@code Swaption}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<Swaption> {

    private LongShort longShort;
    private SwaptionSettlement swaptionSettlement;
    private SwaptionExercise exerciseInfo;
    private AdjustableDate expiryDate;
    private LocalTime expiryTime;
    private ZoneId expiryZone;
    private Swap underlying;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(Swaption beanToCopy) {
      this.longShort = beanToCopy.getLongShort();
      this.swaptionSettlement = beanToCopy.getSwaptionSettlement();
      this.exerciseInfo = beanToCopy.exerciseInfo;
      this.expiryDate = beanToCopy.getExpiryDate();
      this.expiryTime = beanToCopy.getExpiryTime();
      this.expiryZone = beanToCopy.getExpiryZone();
      this.underlying = beanToCopy.getUnderlying();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          return longShort;
        case -1937554512:  // swaptionSettlement
          return swaptionSettlement;
        case -466669914:  // exerciseInfo
          return exerciseInfo;
        case -816738431:  // expiryDate
          return expiryDate;
        case -816254304:  // expiryTime
          return expiryTime;
        case -816069761:  // expiryZone
          return expiryZone;
        case -1770633379:  // underlying
          return underlying;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          this.longShort = (LongShort) newValue;
          break;
        case -1937554512:  // swaptionSettlement
          this.swaptionSettlement = (SwaptionSettlement) newValue;
          break;
        case -466669914:  // exerciseInfo
          this.exerciseInfo = (SwaptionExercise) newValue;
          break;
        case -816738431:  // expiryDate
          this.expiryDate = (AdjustableDate) newValue;
          break;
        case -816254304:  // expiryTime
          this.expiryTime = (LocalTime) newValue;
          break;
        case -816069761:  // expiryZone
          this.expiryZone = (ZoneId) newValue;
          break;
        case -1770633379:  // underlying
          this.underlying = (Swap) newValue;
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
    public Swaption build() {
      return new Swaption(
          longShort,
          swaptionSettlement,
          exerciseInfo,
          expiryDate,
          expiryTime,
          expiryZone,
          underlying);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the option is long or short.
     * <p>
     * Long indicates that the owner wants the option to be in the money at expiry.
     * Short indicates that the owner wants the option to be out of the money at expiry.
     * @param longShort  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder longShort(LongShort longShort) {
      JodaBeanUtils.notNull(longShort, "longShort");
      this.longShort = longShort;
      return this;
    }

    /**
     * Sets settlement method.
     * <p>
     * The settlement of the option is specified by {@link SwaptionSettlement}.
     * @param swaptionSettlement  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder swaptionSettlement(SwaptionSettlement swaptionSettlement) {
      JodaBeanUtils.notNull(swaptionSettlement, "swaptionSettlement");
      this.swaptionSettlement = swaptionSettlement;
      return this;
    }

    /**
     * Sets the exercise information, optional.
     * <p>
     * A swaption can have three different kinds of exercise - European, American and Bermudan.
     * A European swaption has one exercise date, an American can exercise on any date, and a Bermudan
     * can exercise on a fixed set of dates.
     * <p>
     * If not present, the swaption is considered to be a European swaption as per the expiry date.
     * @param exerciseInfo  the new value
     * @return this, for chaining, not null
     */
    public Builder exerciseInfo(SwaptionExercise exerciseInfo) {
      this.exerciseInfo = exerciseInfo;
      return this;
    }

    /**
     * Sets the expiry date of the option.
     * <p>
     * This is the last date that the swaption can be exercised.
     * To represent Bermudan and American swaptions, or to represent a European swaption where the swap start
     * date is calculated dynamically, see the {@code exerciseOptions} field.
     * <p>
     * This date is typically set to be a valid business day.
     * However, the {@code businessDayAdjustment} property may be set to provide a rule for adjustment.
     * @param expiryDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryDate(AdjustableDate expiryDate) {
      JodaBeanUtils.notNull(expiryDate, "expiryDate");
      this.expiryDate = expiryDate;
      return this;
    }

    /**
     * Sets the expiry time of the option.
     * <p>
     * The expiry time is related to the expiry date and time-zone.
     * @param expiryTime  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryTime(LocalTime expiryTime) {
      JodaBeanUtils.notNull(expiryTime, "expiryTime");
      this.expiryTime = expiryTime;
      return this;
    }

    /**
     * Sets the time-zone of the expiry time.
     * <p>
     * The expiry time-zone is related to the expiry date and time.
     * @param expiryZone  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryZone(ZoneId expiryZone) {
      JodaBeanUtils.notNull(expiryZone, "expiryZone");
      this.expiryZone = expiryZone;
      return this;
    }

    /**
     * Sets the underlying swap.
     * <p>
     * At expiry, if the option is exercised, this swap will be entered into.
     * The swap description is the swap as viewed by the party long the option.
     * @param underlying  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder underlying(Swap underlying) {
      JodaBeanUtils.notNull(underlying, "underlying");
      this.underlying = underlying;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("Swaption.Builder{");
      buf.append("longShort").append('=').append(JodaBeanUtils.toString(longShort)).append(',').append(' ');
      buf.append("swaptionSettlement").append('=').append(JodaBeanUtils.toString(swaptionSettlement)).append(',').append(' ');
      buf.append("exerciseInfo").append('=').append(JodaBeanUtils.toString(exerciseInfo)).append(',').append(' ');
      buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(expiryDate)).append(',').append(' ');
      buf.append("expiryTime").append('=').append(JodaBeanUtils.toString(expiryTime)).append(',').append(' ');
      buf.append("expiryZone").append('=').append(JodaBeanUtils.toString(expiryZone)).append(',').append(' ');
      buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
