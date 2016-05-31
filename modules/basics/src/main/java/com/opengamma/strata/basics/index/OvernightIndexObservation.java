/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;

/**
 * Information about a single observation of an Overnight index.
 * <p>
 * Observing an Overnight index requires knowledge of the index, fixing date,
 * publication date, effective date and maturity date.
 */
@BeanDefinition
public final class OvernightIndexObservation
    implements IndexObservation, ImmutableBean, Serializable {

  /**
   * The Overnight index.
   * <p>
   * The rate will be queried from this index.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final OvernightIndex index;
  /**
   * The date of the index fixing.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * Valid business days are defined by {@link OvernightIndex#getFixingCalendar()}.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate fixingDate;
  /**
   * The date that the rate implied by the fixing date is published.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * This must be equal to {@link OvernightIndex#calculatePublicationFromFixing(LocalDate, ReferenceData)}.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate publicationDate;
  /**
   * The effective date of the investment implied by the fixing date.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * This must be equal to {@link OvernightIndex#calculateEffectiveFromFixing(LocalDate, ReferenceData)}.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate effectiveDate;
  /**
   * The maturity date of the investment implied by the fixing date.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * This must be equal to {@link OvernightIndex#calculateMaturityFromEffective(LocalDate, ReferenceData)}.
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
   * Creates an {@code IborRateObservation} from an index and fixing date.
   * <p>
   * The reference data is used to find the maturity date from the fixing date.
   * 
   * @param index  the index
   * @param fixingDate  the fixing date
   * @param refData  the reference data to use when resolving holiday calendars
   * @return the rate observation
   */
  public static OvernightIndexObservation of(OvernightIndex index, LocalDate fixingDate, ReferenceData refData) {
    LocalDate publicationDate = index.calculatePublicationFromFixing(fixingDate, refData);
    LocalDate effectiveDate = index.calculateEffectiveFromFixing(fixingDate, refData);
    LocalDate maturityDate = index.calculateMaturityFromEffective(effectiveDate, refData);
    return OvernightIndexObservation.builder()
        .index(index)
        .fixingDate(fixingDate)
        .publicationDate(publicationDate)
        .effectiveDate(effectiveDate)
        .maturityDate(maturityDate)
        .yearFraction(index.getDayCount().yearFraction(effectiveDate, maturityDate))
        .build();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the Overnight index.
   * 
   * @return the currency of the index
   */
  public Currency getCurrency() {
    return index.getCurrency();
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this observation to another based on the index and fixing date.
   * <p>
   * The publication, effective and maturity dates are ignored.
   * 
   * @param obj  the other observation
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      OvernightIndexObservation other = (OvernightIndexObservation) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(fixingDate, other.fixingDate);
    }
    return false;
  }

  /**
   * Returns a hash code based on the index and fixing date.
   * <p>
   * The maturity date is ignored.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + index.hashCode();
    return hash * 31 + fixingDate.hashCode();
  }

  @Override
  public String toString() {
    return new StringBuilder(64)
        .append("OvernightIndexObservation[")
        .append(index)
        .append(" on ")
        .append(fixingDate)
        .append(']')
        .toString();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code OvernightIndexObservation}.
   * @return the meta-bean, not null
   */
  public static OvernightIndexObservation.Meta meta() {
    return OvernightIndexObservation.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(OvernightIndexObservation.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static OvernightIndexObservation.Builder builder() {
    return new OvernightIndexObservation.Builder();
  }

  private OvernightIndexObservation(
      OvernightIndex index,
      LocalDate fixingDate,
      LocalDate publicationDate,
      LocalDate effectiveDate,
      LocalDate maturityDate,
      double yearFraction) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(fixingDate, "fixingDate");
    JodaBeanUtils.notNull(publicationDate, "publicationDate");
    JodaBeanUtils.notNull(effectiveDate, "effectiveDate");
    JodaBeanUtils.notNull(maturityDate, "maturityDate");
    JodaBeanUtils.notNull(yearFraction, "yearFraction");
    this.index = index;
    this.fixingDate = fixingDate;
    this.publicationDate = publicationDate;
    this.effectiveDate = effectiveDate;
    this.maturityDate = maturityDate;
    this.yearFraction = yearFraction;
  }

  @Override
  public OvernightIndexObservation.Meta metaBean() {
    return OvernightIndexObservation.Meta.INSTANCE;
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
   * The rate will be queried from this index.
   * @return the value of the property, not null
   */
  @Override
  public OvernightIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date of the index fixing.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * Valid business days are defined by {@link OvernightIndex#getFixingCalendar()}.
   * @return the value of the property, not null
   */
  public LocalDate getFixingDate() {
    return fixingDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that the rate implied by the fixing date is published.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * This must be equal to {@link OvernightIndex#calculatePublicationFromFixing(LocalDate, ReferenceData)}.
   * @return the value of the property, not null
   */
  public LocalDate getPublicationDate() {
    return publicationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the effective date of the investment implied by the fixing date.
   * <p>
   * This is an adjusted date with any business day rule applied.
   * This must be equal to {@link OvernightIndex#calculateEffectiveFromFixing(LocalDate, ReferenceData)}.
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
   * This must be equal to {@link OvernightIndex#calculateMaturityFromEffective(LocalDate, ReferenceData)}.
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

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code OvernightIndexObservation}.
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
        this, "index", OvernightIndexObservation.class, OvernightIndex.class);
    /**
     * The meta-property for the {@code fixingDate} property.
     */
    private final MetaProperty<LocalDate> fixingDate = DirectMetaProperty.ofImmutable(
        this, "fixingDate", OvernightIndexObservation.class, LocalDate.class);
    /**
     * The meta-property for the {@code publicationDate} property.
     */
    private final MetaProperty<LocalDate> publicationDate = DirectMetaProperty.ofImmutable(
        this, "publicationDate", OvernightIndexObservation.class, LocalDate.class);
    /**
     * The meta-property for the {@code effectiveDate} property.
     */
    private final MetaProperty<LocalDate> effectiveDate = DirectMetaProperty.ofImmutable(
        this, "effectiveDate", OvernightIndexObservation.class, LocalDate.class);
    /**
     * The meta-property for the {@code maturityDate} property.
     */
    private final MetaProperty<LocalDate> maturityDate = DirectMetaProperty.ofImmutable(
        this, "maturityDate", OvernightIndexObservation.class, LocalDate.class);
    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", OvernightIndexObservation.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "fixingDate",
        "publicationDate",
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
        case 1470566394:  // publicationDate
          return publicationDate;
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
    public OvernightIndexObservation.Builder builder() {
      return new OvernightIndexObservation.Builder();
    }

    @Override
    public Class<? extends OvernightIndexObservation> beanType() {
      return OvernightIndexObservation.class;
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
     * The meta-property for the {@code fixingDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> fixingDate() {
      return fixingDate;
    }

    /**
     * The meta-property for the {@code publicationDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> publicationDate() {
      return publicationDate;
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
          return ((OvernightIndexObservation) bean).getIndex();
        case 1255202043:  // fixingDate
          return ((OvernightIndexObservation) bean).getFixingDate();
        case 1470566394:  // publicationDate
          return ((OvernightIndexObservation) bean).getPublicationDate();
        case -930389515:  // effectiveDate
          return ((OvernightIndexObservation) bean).getEffectiveDate();
        case -414641441:  // maturityDate
          return ((OvernightIndexObservation) bean).getMaturityDate();
        case -1731780257:  // yearFraction
          return ((OvernightIndexObservation) bean).getYearFraction();
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
   * The bean-builder for {@code OvernightIndexObservation}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<OvernightIndexObservation> {

    private OvernightIndex index;
    private LocalDate fixingDate;
    private LocalDate publicationDate;
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
    private Builder(OvernightIndexObservation beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.fixingDate = beanToCopy.getFixingDate();
      this.publicationDate = beanToCopy.getPublicationDate();
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
        case 1470566394:  // publicationDate
          return publicationDate;
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
          this.index = (OvernightIndex) newValue;
          break;
        case 1255202043:  // fixingDate
          this.fixingDate = (LocalDate) newValue;
          break;
        case 1470566394:  // publicationDate
          this.publicationDate = (LocalDate) newValue;
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
    public OvernightIndexObservation build() {
      return new OvernightIndexObservation(
          index,
          fixingDate,
          publicationDate,
          effectiveDate,
          maturityDate,
          yearFraction);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the Overnight index.
     * <p>
     * The rate will be queried from this index.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(OvernightIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the date of the index fixing.
     * <p>
     * This is an adjusted date with any business day rule applied.
     * Valid business days are defined by {@link OvernightIndex#getFixingCalendar()}.
     * @param fixingDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixingDate(LocalDate fixingDate) {
      JodaBeanUtils.notNull(fixingDate, "fixingDate");
      this.fixingDate = fixingDate;
      return this;
    }

    /**
     * Sets the date that the rate implied by the fixing date is published.
     * <p>
     * This is an adjusted date with any business day rule applied.
     * This must be equal to {@link OvernightIndex#calculatePublicationFromFixing(LocalDate, ReferenceData)}.
     * @param publicationDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder publicationDate(LocalDate publicationDate) {
      JodaBeanUtils.notNull(publicationDate, "publicationDate");
      this.publicationDate = publicationDate;
      return this;
    }

    /**
     * Sets the effective date of the investment implied by the fixing date.
     * <p>
     * This is an adjusted date with any business day rule applied.
     * This must be equal to {@link OvernightIndex#calculateEffectiveFromFixing(LocalDate, ReferenceData)}.
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
     * This must be equal to {@link OvernightIndex#calculateMaturityFromEffective(LocalDate, ReferenceData)}.
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
      StringBuilder buf = new StringBuilder(224);
      buf.append("OvernightIndexObservation.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("fixingDate").append('=').append(JodaBeanUtils.toString(fixingDate)).append(',').append(' ');
      buf.append("publicationDate").append('=').append(JodaBeanUtils.toString(publicationDate)).append(',').append(' ');
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
