/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
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

import com.google.common.base.CharMatcher;
import com.google.common.collect.ComparisonChain;
import com.google.common.net.PercentEscaper;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;

/**
 * An immutable standard identifier for an item.
 * <p>
 * A standard identifier is used to uniquely identify domain objects.
 * It is formed from two parts, the scheme and value.
 * <p>
 * The scheme defines a single way of identifying items, while the value is an identifier
 * within that scheme. A value from one scheme may refer to a completely different
 * real-world item than the same value from a different scheme.
 * <p>
 * Real-world examples of {@code StandardId} include instances of:
 * <ul>
 *   <li>Cusip</li>
 *   <li>Isin</li>
 *   <li>Reuters RIC</li>
 *   <li>Bloomberg BUID</li>
 *   <li>Bloomberg Ticker</li>
 *   <li>Trading system OTC trade ID</li>
 * </ul>
 * <p>
 * This class is immutable and thread-safe.
 */
@BeanDefinition(builderScope = "private")
public final class StandardId
    implements Comparable<StandardId>, ImmutableBean, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /**
   * Matcher for checking the scheme.
   * It must only contains the characters A-Z, a-z, 0-9 and selected special characters.
   */
  private static final CharMatcher SCHEME_MATCHER =
      CharMatcher.inRange('A', 'Z')
          .or(CharMatcher.inRange('a', 'z'))
          .or(CharMatcher.inRange('0', '9'))
          .or(CharMatcher.is(':'))
          .or(CharMatcher.is('/'))
          .or(CharMatcher.is('+'))
          .or(CharMatcher.is('.'))
          .or(CharMatcher.is('='))
          .or(CharMatcher.is('_'))
          .or(CharMatcher.is('-'))
          .precomputed();
  /**
   * Matcher for checking the value.
   * It must contain ASCII printable characters excluding curly brackets, pipe and tilde.
   */
  private static final CharMatcher VALUE_MATCHER = CharMatcher.inRange(' ', 'z').precomputed();
  /**
   * The escaper.
   */
  private static final PercentEscaper SCHEME_ESCAPER = new PercentEscaper(":/+.=_-", false);

  /**
   * The scheme that categorizes the identifier value.
   * <p>
   * This provides the universe within which the identifier value has meaning.
   */
  @PropertyDefinition(validate = "notNull")
  private final String scheme;
  /**
   * The value of the identifier within the scheme.
   */
  @PropertyDefinition(validate = "notNull")
  private final String value;
  /**
   * The hash code.
   */
  private final transient int hashCode;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a scheme and value.
   * <p>
   * The scheme must be non-empty and match the regular expression '{@code [A-Za-z0-9:/+.=_-]*}'.
   * This permits letters, numbers, colon, forward-slash, plus, dot, equals, underscore and dash.
   * If necessary, the scheme can be encoded using {@link StandardId#encodeScheme(String)}.
   * <p>
   * The value must be non-empty and match the regular expression '{@code [!-z][ -z]*}'.
   * This includes all standard printable ASCII characters excluding curly brackets, pipe and tilde.
   *
   * @param scheme  the scheme of the identifier, not empty
   * @param value  the value of the identifier, not empty
   * @return the identifier
   */
  public static StandardId of(String scheme, String value) {
    return new StandardId(scheme, value);
  }

  /**
   * Parses an {@code StandardId} from a formatted scheme and value.
   * <p>
   * This parses the identifier from the form produced by {@code toString()}
   * which is '{@code $scheme~$value}'.
   *
   * @param str  the identifier to parse
   * @return the identifier
   * @throws IllegalArgumentException if the identifier cannot be parsed
   */
  @FromString
  public static StandardId parse(String str) {
    int pos = ArgChecker.notNull(str, "str").indexOf("~");
    if (pos < 0) {
      throw new IllegalArgumentException("Invalid identifier format: " + str);
    }
    return new StandardId(str.substring(0, pos), str.substring(pos + 1));
  }

  /**
   * Encode a string suitable for use as the scheme.
   * <p>
   * This uses percent encoding, just like URI.
   * 
   * @param scheme  the scheme to encode
   * @return the encoded scheme
   */
  public static String encodeScheme(String scheme) {
    return SCHEME_ESCAPER.escape(scheme);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an identifier.
   *
   * @param scheme  the scheme of the identifier, not empty
   * @param value  the value of the identifier, not empty
   */
  @ImmutableConstructor
  private StandardId(String scheme, String value) {
    ArgChecker.matches(SCHEME_MATCHER, 1, Integer.MAX_VALUE, scheme, "scheme", "[A-Za-z0-9:/+.=_-]+");
    ArgChecker.matches(VALUE_MATCHER, 1, Integer.MAX_VALUE, value, "value", "[!-z][ -z]+");
    if (value.charAt(0) == ' ') {
      throw new IllegalArgumentException(Messages.format(
          "Invalid initial space in value '{}' must match regex '[!-z][ -z]*'", value));
    }
    this.scheme = scheme;
    this.value = value;
    this.hashCode = scheme.hashCode() ^ value.hashCode();
  }

  // resolve after deserialization
  private Object readResolve() {
    return new StandardId(scheme, value);
  }

  //-------------------------------------------------------------------------
  /**
   * Compares the external identifiers, sorting alphabetically by scheme followed by value.
   * 
   * @param other  the other external identifier
   * @return negative if this is less, zero if equal, positive if greater
   */
  @Override
  public int compareTo(StandardId other) {
    return ComparisonChain.start()
        .compare(scheme, other.scheme)
        .compare(value, other.value)
        .result();
  }

  /**
   * Checks if this identifier equals another, comparing the scheme and value.
   * 
   * @param obj  the other object
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof StandardId) {
      StandardId other = (StandardId) obj;
      return scheme.equals(other.scheme) && value.equals(other.value);
    }
    return false;
  }

  /**
   * Returns a suitable hash code, based on the scheme and value.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * Returns the identifier in a standard string format.
   * <p>
   * The returned string is in the form '{@code $scheme~$value}'.
   * This is suitable for use with {@link #parse(String)}.
   * 
   * @return a parsable representation of the identifier
   */
  @Override
  @ToString
  public String toString() {
    return scheme + "~" + value;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code StandardId}.
   * @return the meta-bean, not null
   */
  public static StandardId.Meta meta() {
    return StandardId.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(StandardId.Meta.INSTANCE);
  }

  @Override
  public StandardId.Meta metaBean() {
    return StandardId.Meta.INSTANCE;
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
   * Gets the scheme that categorizes the identifier value.
   * <p>
   * This provides the universe within which the identifier value has meaning.
   * @return the value of the property, not null
   */
  public String getScheme() {
    return scheme;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value of the identifier within the scheme.
   * @return the value of the property, not null
   */
  public String getValue() {
    return value;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code StandardId}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code scheme} property.
     */
    private final MetaProperty<String> scheme = DirectMetaProperty.ofImmutable(
        this, "scheme", StandardId.class, String.class);
    /**
     * The meta-property for the {@code value} property.
     */
    private final MetaProperty<String> value = DirectMetaProperty.ofImmutable(
        this, "value", StandardId.class, String.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "scheme",
        "value");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -907987547:  // scheme
          return scheme;
        case 111972721:  // value
          return value;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends StandardId> builder() {
      return new StandardId.Builder();
    }

    @Override
    public Class<? extends StandardId> beanType() {
      return StandardId.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code scheme} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> scheme() {
      return scheme;
    }

    /**
     * The meta-property for the {@code value} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> value() {
      return value;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -907987547:  // scheme
          return ((StandardId) bean).getScheme();
        case 111972721:  // value
          return ((StandardId) bean).getValue();
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
   * The bean-builder for {@code StandardId}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<StandardId> {

    private String scheme;
    private String value;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -907987547:  // scheme
          return scheme;
        case 111972721:  // value
          return value;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -907987547:  // scheme
          this.scheme = (String) newValue;
          break;
        case 111972721:  // value
          this.value = (String) newValue;
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
    public StandardId build() {
      return new StandardId(
          scheme,
          value);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("StandardId.Builder{");
      buf.append("scheme").append('=').append(JodaBeanUtils.toString(scheme)).append(',').append(' ');
      buf.append("value").append('=').append(JodaBeanUtils.toString(value));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
