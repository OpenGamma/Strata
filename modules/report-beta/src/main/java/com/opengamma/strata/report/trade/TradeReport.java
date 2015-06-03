/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.trade;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.Instant;
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

import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.report.Report;
import com.opengamma.strata.report.ReportCalculationResults;

/**
 * Represents a trade report.
 */
@BeanDefinition
public class TradeReport implements Report, ImmutableBean {

  /** The valuation date. */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;

  /** The instant at which the report was run. */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Instant runInstant;

  /** The column headers. */
  @PropertyDefinition(validate = "notNull")
  private final String[] columnHeaders;

  /** The results table. */
  @PropertyDefinition(validate = "notNull")
  private final Result<?>[][] results;

  public static TradeReport of(ReportCalculationResults calculationResults, TradeReportTemplate reportTemplate) {
    TradeReportRunner reportRunner = new TradeReportRunner();
    return reportRunner.runReport(calculationResults, reportTemplate);
  }

  @Override
  public void writeCsv(OutputStream out) {
    TradeReportFormatter formatter = new TradeReportFormatter();
    formatter.writeCsv(this, out);
  }

  @Override
  public void writeAsciiTable(OutputStream out) {
    TradeReportFormatter formatter = new TradeReportFormatter();
    formatter.writeAsciiTable(this, out);
  }

  public String getAsciiTable() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    writeAsciiTable(os);
    return os.toString();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code TradeReport}.
   * @return the meta-bean, not null
   */
  public static TradeReport.Meta meta() {
    return TradeReport.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(TradeReport.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static TradeReport.Builder builder() {
    return new TradeReport.Builder();
  }

  /**
   * Restricted constructor.
   * @param builder  the builder to copy from, not null
   */
  protected TradeReport(TradeReport.Builder builder) {
    JodaBeanUtils.notNull(builder.valuationDate, "valuationDate");
    JodaBeanUtils.notNull(builder.runInstant, "runInstant");
    JodaBeanUtils.notNull(builder.columnHeaders, "columnHeaders");
    JodaBeanUtils.notNull(builder.results, "results");
    this.valuationDate = builder.valuationDate;
    this.runInstant = builder.runInstant;
    this.columnHeaders = builder.columnHeaders.clone();
    this.results = builder.results;
  }

  @Override
  public TradeReport.Meta metaBean() {
    return TradeReport.Meta.INSTANCE;
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
   * Gets the valuation date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the instant at which the report was run.
   * @return the value of the property, not null
   */
  @Override
  public Instant getRunInstant() {
    return runInstant;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the column headers.
   * @return the value of the property, not null
   */
  public String[] getColumnHeaders() {
    return (columnHeaders != null ? columnHeaders.clone() : null);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the results table.
   * @return the value of the property, not null
   */
  public Result<?>[][] getResults() {
    return results;
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
      TradeReport other = (TradeReport) obj;
      return JodaBeanUtils.equal(getValuationDate(), other.getValuationDate()) &&
          JodaBeanUtils.equal(getRunInstant(), other.getRunInstant()) &&
          JodaBeanUtils.equal(getColumnHeaders(), other.getColumnHeaders()) &&
          JodaBeanUtils.equal(getResults(), other.getResults());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getRunInstant());
    hash = hash * 31 + JodaBeanUtils.hashCode(getColumnHeaders());
    hash = hash * 31 + JodaBeanUtils.hashCode(getResults());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("TradeReport{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(getValuationDate())).append(',').append(' ');
    buf.append("runInstant").append('=').append(JodaBeanUtils.toString(getRunInstant())).append(',').append(' ');
    buf.append("columnHeaders").append('=').append(JodaBeanUtils.toString(getColumnHeaders())).append(',').append(' ');
    buf.append("results").append('=').append(JodaBeanUtils.toString(getResults())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code TradeReport}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", TradeReport.class, LocalDate.class);
    /**
     * The meta-property for the {@code runInstant} property.
     */
    private final MetaProperty<Instant> runInstant = DirectMetaProperty.ofImmutable(
        this, "runInstant", TradeReport.class, Instant.class);
    /**
     * The meta-property for the {@code columnHeaders} property.
     */
    private final MetaProperty<String[]> columnHeaders = DirectMetaProperty.ofImmutable(
        this, "columnHeaders", TradeReport.class, String[].class);
    /**
     * The meta-property for the {@code results} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<Result<?>[][]> results = DirectMetaProperty.ofImmutable(
        this, "results", TradeReport.class, (Class) Result.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "valuationDate",
        "runInstant",
        "columnHeaders",
        "results");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case 111354070:  // runInstant
          return runInstant;
        case 1598220112:  // columnHeaders
          return columnHeaders;
        case 1097546742:  // results
          return results;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public TradeReport.Builder builder() {
      return new TradeReport.Builder();
    }

    @Override
    public Class<? extends TradeReport> beanType() {
      return TradeReport.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code valuationDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> valuationDate() {
      return valuationDate;
    }

    /**
     * The meta-property for the {@code runInstant} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Instant> runInstant() {
      return runInstant;
    }

    /**
     * The meta-property for the {@code columnHeaders} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<String[]> columnHeaders() {
      return columnHeaders;
    }

    /**
     * The meta-property for the {@code results} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Result<?>[][]> results() {
      return results;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return ((TradeReport) bean).getValuationDate();
        case 111354070:  // runInstant
          return ((TradeReport) bean).getRunInstant();
        case 1598220112:  // columnHeaders
          return ((TradeReport) bean).getColumnHeaders();
        case 1097546742:  // results
          return ((TradeReport) bean).getResults();
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
   * The bean-builder for {@code TradeReport}.
   */
  public static class Builder extends DirectFieldsBeanBuilder<TradeReport> {

    private LocalDate valuationDate;
    private Instant runInstant;
    private String[] columnHeaders;
    private Result<?>[][] results;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    protected Builder(TradeReport beanToCopy) {
      this.valuationDate = beanToCopy.getValuationDate();
      this.runInstant = beanToCopy.getRunInstant();
      this.columnHeaders = beanToCopy.getColumnHeaders().clone();
      this.results = beanToCopy.getResults();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case 111354070:  // runInstant
          return runInstant;
        case 1598220112:  // columnHeaders
          return columnHeaders;
        case 1097546742:  // results
          return results;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          this.valuationDate = (LocalDate) newValue;
          break;
        case 111354070:  // runInstant
          this.runInstant = (Instant) newValue;
          break;
        case 1598220112:  // columnHeaders
          this.columnHeaders = (String[]) newValue;
          break;
        case 1097546742:  // results
          this.results = (Result<?>[][]) newValue;
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
    public TradeReport build() {
      return new TradeReport(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code valuationDate} property in the builder.
     * @param valuationDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationDate(LocalDate valuationDate) {
      JodaBeanUtils.notNull(valuationDate, "valuationDate");
      this.valuationDate = valuationDate;
      return this;
    }

    /**
     * Sets the {@code runInstant} property in the builder.
     * @param runInstant  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder runInstant(Instant runInstant) {
      JodaBeanUtils.notNull(runInstant, "runInstant");
      this.runInstant = runInstant;
      return this;
    }

    /**
     * Sets the {@code columnHeaders} property in the builder.
     * @param columnHeaders  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder columnHeaders(String... columnHeaders) {
      JodaBeanUtils.notNull(columnHeaders, "columnHeaders");
      this.columnHeaders = columnHeaders;
      return this;
    }

    /**
     * Sets the {@code results} property in the builder.
     * @param results  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder results(Result<?>[][] results) {
      JodaBeanUtils.notNull(results, "results");
      this.results = results;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("TradeReport.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("runInstant").append('=').append(JodaBeanUtils.toString(runInstant)).append(',').append(' ');
      buf.append("columnHeaders").append('=').append(JodaBeanUtils.toString(columnHeaders)).append(',').append(' ');
      buf.append("results").append('=').append(JodaBeanUtils.toString(results)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
