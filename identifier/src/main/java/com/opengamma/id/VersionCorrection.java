/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nullable;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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
import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * An immutable version-correction combination.
 * <p>
 * History can be stored in two dimensions and the version-correction provides the key.
 * The first historic dimension is the classic series of versions.
 * Each new version is stored in such a manor that previous versions can be accessed.
 * The second historic dimension is corrections.
 * A correction occurs when it is realized that the original data stored was incorrect.
 * <p>
 * A fully versioned object in an OpenGamma installation will have a single state for
 * any combination of version and correction. This state is assigned a version string
 * which is used as the third component in a {@link UniqueId}, where all versions
 * share the same {@link ObjectId}.
 * <p>
 * This class represents a single version-correction combination suitable for identifying a single state.
 * It is typically used to obtain an object, while the version string is used in the response.
 * <p>
 * This class is immutable and thread-safe.
 */
@BeanDefinition(builderScope = "private")
public final class VersionCorrection
    implements ImmutableBean, Comparable<VersionCorrection>, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * Version-correction instance representing the latest version and correction.
   */
  public static final VersionCorrection LATEST = new VersionCorrection(null, null);

  /**
   * The version instant, null means latest.
   * This locates the version that was active at this instant.
   */
  @PropertyDefinition
  @Nullable
  private final Instant versionAsOf;
  /**
   * The correction instant, null means latest.
   * This locates the correction that was active at this instant.
   */
  @Nullable
  @PropertyDefinition
  private final Instant correctedTo;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code VersionCorrection} from a version and correction instant.
   * 
   * @param versionAsOf  the version as of instant, null for latest
   * @param correctedTo  the corrected to instant, null for latest
   * @return the version-correction combination
   */
  public static VersionCorrection of(@Nullable Instant versionAsOf, @Nullable Instant correctedTo) {
    if (versionAsOf == null && correctedTo == null) {
      return LATEST;
    }
    return new VersionCorrection(versionAsOf, correctedTo);
  }

  /**
   * Obtains a {@code VersionCorrection} from a version instant and the latest correction.
   * 
   * @param versionAsOf  the version as of instant, null for latest
   * @return the version-correction combination
   */
  public static VersionCorrection ofVersionAsOf(@Nullable Instant versionAsOf) {
    return of(versionAsOf, null);
  }

  /**
   * Obtains a {@code VersionCorrection} from a correction instant and the latest version.
   * 
   * @param correctedTo  the corrected to instant, null for latest
   * @return the version-correction combination
   */
  public static VersionCorrection ofCorrectedTo(@Nullable Instant correctedTo) {
    return of(null, correctedTo);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a {@code VersionCorrection} from the standard string format.
   * <p>
   * This parses the version-correction from the form produced by {@code toString()}.
   * It consists of 'V' followed by the version, a dot, then 'C' followed by the correction,
   * such as {@code V2011-02-01T12:30:40Z.C2011-02-01T12:30:40Z}.
   * The text 'LATEST' is used in place of the instant for a latest version or correction.
   * 
   * @param str  the identifier to parse
   * @return the version-correction combination
   * @throws IllegalArgumentException if the version-correction cannot be parsed
   */
  @FromString
  public static VersionCorrection parse(String str) {
    ArgChecker.notNull(str, "str");
    int posC = str.indexOf(".C");
    if (str.isEmpty() || str.charAt(0) != 'V' || posC < 0) {
      throw new IllegalArgumentException("Invalid identifier format: " + str);
    }
    String verStr = str.substring(1, posC);
    String corrStr = str.substring(posC + 2);
    Instant versionAsOf = parseInstantString(verStr);
    Instant correctedTo = parseInstantString(corrStr);
    return of(versionAsOf, correctedTo);
  }

  /**
   * Parses a {@code VersionCorrection} from standard string representations
   * of the version and correction.
   * <p>
   * This parses the version-correction from the forms produced by
   * {@link #getVersionAsOfString()} and {@link #getCorrectedToString()}.
   * 
   * @param versionAsOfString  the version as of string, null treated as latest
   * @param correctedToString  the corrected to string, null treated as latest
   * @return the version-correction combination
   * @throws IllegalArgumentException if the version-correction cannot be parsed
   */
  public static VersionCorrection parse(@Nullable String versionAsOfString, @Nullable String correctedToString) {
    Instant versionAsOf = parseInstantString(versionAsOfString);
    Instant correctedTo = parseInstantString(correctedToString);
    return of(versionAsOf, correctedTo);
  }

  /**
   * Parses a version-correction {@code Instant} from a standard string representation.
   * <p>
   * The string representation must be either {@code LATEST} for null,
   * or the ISO-8601 representation of the desired {@code Instant}.
   * 
   * @param instantStr  the instant string, null treated as latest
   * @return the instant
   */
  private static Instant parseInstantString(@Nullable String instantStr) {
    if (instantStr == null || instantStr.equals("LATEST")) {
      return null;
    } else {
      try {
        return Instant.parse(instantStr);
      } catch (DateTimeException ex) {
        throw new IllegalArgumentException(ex);
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this object with the specified version as of instant.
   * <p>
   * This instance is immutable and unaffected by this method call.
   * 
   * @param versionAsOf  the version instant, null for latest
   * @return a version-correction based on this one with the version as of instant altered
   */
  public VersionCorrection withVersionAsOf(@Nullable Instant versionAsOf) {
    if (Objects.equal(this.versionAsOf, versionAsOf)) {
      return this;
    }
    return new VersionCorrection(versionAsOf, correctedTo);
  }

  /**
   * Returns a copy of this object with the specified corrected to instant.
   * <p>
   * This instance is immutable and unaffected by this method call.
   * 
   * @param correctedTo  the corrected to instant, null for latest
   * @return a version-correction based on this one with the corrected to instant altered
   */
  public VersionCorrection withCorrectedTo(@Nullable Instant correctedTo) {
    if (Objects.equal(this.correctedTo, correctedTo)) {
      return this;
    }
    return new VersionCorrection(versionAsOf, correctedTo);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks whether this object has either the version or correction instant set to 'latest'.
   * 
   * @return true if either instant is 'latest'
   */
  public boolean containsLatest() {
    return (versionAsOf == null || correctedTo == null);
  }

  /**
   * Returns a copy of this object with any latest instant fixed to the specified instant.
   * <p>
   * This instance is immutable and unaffected by this method call.
   * 
   * @param now  the current instant
   * @return a version-correction based on this one with the correction altered
   */
  public VersionCorrection withLatestFixed(Instant now) {
    ArgChecker.notNull(now, "now");
    if (containsLatest()) {
      return new VersionCorrection(
          (versionAsOf != null) ? versionAsOf : now,
          (correctedTo != null) ? correctedTo : now);
    }
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets a string representation of the version as of instant.
   * <p>
   * This is either the ISO-8601 representation of the version as of instant,
   * such as {@code 2011-02-01T12:30:40Z}, or {@code LATEST}.
   * 
   * @return the string version as of
   */
  public String getVersionAsOfString() {
    return Objects.firstNonNull(versionAsOf, "LATEST").toString();
  }

  /**
   * Gets a string representation of the corrected to instant.
   * <p>
   * This is either the ISO-8601 representation of the corrected to instant,
   * such as {@code 2011-02-01T12:30:40Z}, or {@code LATEST}.
   * 
   * @return the string corrected to
   */
  public String getCorrectedToString() {
    return Objects.firstNonNull(correctedTo, "LATEST").toString();
  }

  //-------------------------------------------------------------------------
  /**
   * Compares the version-corrections, sorting by version followed by correction.
   * 
   * @param other  the other identifier
   * @return negative if this is less, zero if equal, positive if greater
   */
  @Override
  public int compareTo(VersionCorrection other) {
    return ComparisonChain.start()
        .compare(versionAsOf, other.versionAsOf, Ordering.natural().nullsLast())
        .compare(correctedTo, other.correctedTo, Ordering.natural().nullsLast())
        .result();
  }

  /**
   * Returns the version-correction instants.
   * <p>
   * This is a standard format that can be parsed.
   * It consists of 'V' followed by the version, a dot, then 'C' followed by the correction,
   * such as {@code V2011-02-01T12:30:40Z.C2011-02-01T12:30:40Z}.
   * The text 'LATEST' is used in place of the instant for a latest version or correction.
   * 
   * @return the string version-correction
   */
  @Override
  @ToString
  public String toString() {
    return "V" + getVersionAsOfString() + ".C" + getCorrectedToString();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code VersionCorrection}.
   * @return the meta-bean, not null
   */
  public static VersionCorrection.Meta meta() {
    return VersionCorrection.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(VersionCorrection.Meta.INSTANCE);
  }

  private VersionCorrection(
      Instant versionAsOf,
      Instant correctedTo) {
    this.versionAsOf = versionAsOf;
    this.correctedTo = correctedTo;
  }

  @Override
  public VersionCorrection.Meta metaBean() {
    return VersionCorrection.Meta.INSTANCE;
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
   * Gets the version instant, null means latest.
   * This locates the version that was active at this instant.
   * @return the value of the property
   */
  public Instant getVersionAsOf() {
    return versionAsOf;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the correction instant, null means latest.
   * This locates the correction that was active at this instant.
   * @return the value of the property
   */
  public Instant getCorrectedTo() {
    return correctedTo;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      VersionCorrection other = (VersionCorrection) obj;
      return JodaBeanUtils.equal(getVersionAsOf(), other.getVersionAsOf()) &&
          JodaBeanUtils.equal(getCorrectedTo(), other.getCorrectedTo());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getVersionAsOf());
    hash += hash * 31 + JodaBeanUtils.hashCode(getCorrectedTo());
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code VersionCorrection}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code versionAsOf} property.
     */
    private final MetaProperty<Instant> versionAsOf = DirectMetaProperty.ofImmutable(
        this, "versionAsOf", VersionCorrection.class, Instant.class);
    /**
     * The meta-property for the {@code correctedTo} property.
     */
    private final MetaProperty<Instant> correctedTo = DirectMetaProperty.ofImmutable(
        this, "correctedTo", VersionCorrection.class, Instant.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "versionAsOf",
        "correctedTo");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 688535201:  // versionAsOf
          return versionAsOf;
        case 1465896676:  // correctedTo
          return correctedTo;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends VersionCorrection> builder() {
      return new VersionCorrection.Builder();
    }

    @Override
    public Class<? extends VersionCorrection> beanType() {
      return VersionCorrection.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code versionAsOf} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Instant> versionAsOf() {
      return versionAsOf;
    }

    /**
     * The meta-property for the {@code correctedTo} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Instant> correctedTo() {
      return correctedTo;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 688535201:  // versionAsOf
          return ((VersionCorrection) bean).getVersionAsOf();
        case 1465896676:  // correctedTo
          return ((VersionCorrection) bean).getCorrectedTo();
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
   * The bean-builder for {@code VersionCorrection}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<VersionCorrection> {

    private Instant versionAsOf;
    private Instant correctedTo;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 688535201:  // versionAsOf
          return versionAsOf;
        case 1465896676:  // correctedTo
          return correctedTo;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 688535201:  // versionAsOf
          this.versionAsOf = (Instant) newValue;
          break;
        case 1465896676:  // correctedTo
          this.correctedTo = (Instant) newValue;
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
    public VersionCorrection build() {
      return new VersionCorrection(
          versionAsOf,
          correctedTo);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("VersionCorrection.Builder{");
      buf.append("versionAsOf").append('=').append(JodaBeanUtils.toString(versionAsOf)).append(',').append(' ');
      buf.append("correctedTo").append('=').append(JodaBeanUtils.toString(correctedTo));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
