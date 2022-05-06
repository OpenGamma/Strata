/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.joda.beans.ser.JodaBeanSer;
import org.junit.jupiter.api.Test;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import com.opengamma.strata.collect.function.CheckedSupplier;

/**
 * Test {@link StringCharSource}.
 */
public class StringCharSourceTest {

  @Test
  public void test_EMPTY() {
    StringCharSource test = StringCharSource.EMPTY;
    assertThat(test.isEmpty()).isEqualTo(true);
    assertThat(test.length()).isEqualTo(0);
    assertThat(test.getFileName()).isEmpty();
    assertThat(test.toString()).isEqualTo("StringCharSource[0 chars]");
  }

  @Test
  public void test_of() {
    StringCharSource test = StringCharSource.of("ABC\nDEF");
    assertThat(test.isEmpty()).isEqualTo(false);
    assertThat(test.length()).isEqualTo(7);
    assertThat(test.getFileName()).isEmpty();
  }

  @Test
  public void test_basics() throws IOException {
    StringCharSource test = StringCharSource.of("ABC\nDEF").withFileName("file.txt");
    assertThat(test.lengthIfKnown().get()).isEqualTo(7);
    assertThat(test.read()).isEqualTo("ABC\nDEF");
    assertThat(test.readFirstLine()).isEqualTo("ABC");
    assertThat(test.readLines()).containsExactly("ABC", "DEF");
    assertThat(test.load()).isSameAs(test);
    assertThat(test.getFileName()).hasValue("file.txt");
    assertThat(test.toString()).isEqualTo("StringCharSource[7 chars, file.txt]");

    List<String> list = new ArrayList<>();
    test.forEachLine(list::add);
    assertThat(list).containsExactly("ABC", "DEF");

    StringBuilder buf = new StringBuilder();
    test.copyTo(buf);
    assertThat(buf).hasToString("ABC\nDEF");

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteSink byteSink = new ByteSink() {
      @Override
      public OutputStream openStream() throws IOException {
        return out;
      }
    };
    test.copyTo(byteSink.asCharSink(StandardCharsets.UTF_8));
    assertThat(out).hasToString("ABC\nDEF");

    assertThat(test.asByteSourceUtf8().readUtf8()).isEqualTo("ABC\nDEF");
    assertThat(test.asByteSourceUtf8().getFileName()).hasValue("file.txt");
    assertThat(test.asByteSourceUtf8().asCharSourceUtf8().read()).isEqualTo("ABC\nDEF");
    assertThat(test.asByteSourceUtf8().asCharSourceUtf8().getFileName()).hasValue("file.txt");
  }

  @Test
  public void test_from_GuavaStringCharSource() {
    CharSource source = CharSource.wrap("ABC");
    StringCharSource test = StringCharSource.from(source);
    assertThat(test.read()).isEqualTo("ABC");
    assertThat(test.getFileName()).isEmpty();
  }

  @Test
  public void test_from_CharSource_alreadyStringCharSource() {
    StringCharSource base = StringCharSource.of("ABC").withFileName("file.txt");
    StringCharSource test = StringCharSource.from(base);
    assertThat(test).isSameAs(base);
    assertThat(test.read()).isEqualTo("ABC");
    assertThat(test.getFileName()).hasValue("file.txt");
  }

  @Test
  public void test_from_FileByteSource() {
    ByteSource source = FileByteSource.of(new File("pom.xml"));
    StringCharSource test = StringCharSource.from(source.asCharSource(StandardCharsets.UTF_8));
    assertThat(test.getFileName()).hasValue("pom.xml");
  }

  @Test
  public void test_from_UriByteSource() {
    ByteSource source = UriByteSource.of(new File("pom.xml").toURI());
    StringCharSource test = StringCharSource.from(source.asCharSource(StandardCharsets.UTF_8));
    assertThat(test.getFileName()).hasValue("pom.xml");
  }

  @Test
  public void test_from_GuavaFileByteSource() {
    ByteSource source = Files.asByteSource(new File("pom.xml"));
    StringCharSource test = StringCharSource.from(source.asCharSource(StandardCharsets.UTF_8));
    assertThat(test.getFileName()).hasValue("pom.xml");
  }

  @Test
  public void test_from_GuavaPathByteSource() {
    ByteSource source = MoreFiles.asByteSource(Paths.get("pom.xml"));
    StringCharSource test = StringCharSource.from(source.asCharSource(StandardCharsets.UTF_8));
    assertThat(test.getFileName()).hasValue("pom.xml");
  }

  @Test
  public void test_from_GuavaUrlByteSource() throws MalformedURLException {
    ByteSource source = Resources.asByteSource(new File("pom.xml").toURI().toURL());
    StringCharSource test = StringCharSource.from(source.asCharSource(StandardCharsets.UTF_8));
    assertThat(test.getFileName()).hasValue("pom.xml");
  }

  @Test
  public void test_from_GuavaSimpleByteSource() {
    byte[] bytes = new byte[] {'A', 'B', 'C'};
    ByteSource source = ByteSource.wrap(bytes);
    StringCharSource test = StringCharSource.from(source.asCharSource(StandardCharsets.UTF_8));
    assertThat(test.read()).isEqualTo("ABC");
    assertThat(test.getFileName()).isEmpty();
  }

  @Test
  public void test_from_GuavaSimpleByteSourceSliced() {
    byte[] bytes = new byte[] {'A', 'B', 'C'};
    ByteSource source = ByteSource.wrap(bytes).slice(1, 1);
    StringCharSource test = StringCharSource.from(source.asCharSource(StandardCharsets.UTF_8));
    assertThat(test.read()).isEqualTo("B");
    assertThat(test.getFileName()).isEmpty();
  }

  @Test
  public void test_from_Supplier() {
    CharSource source = ByteSource.wrap(new byte[] {'A', 'B', 'C'}).asCharSource(StandardCharsets.UTF_8);
    StringCharSource test = StringCharSource.from(() -> source.openStream());
    assertThat(test.getFileName()).isEmpty();
    assertThat(test.read()).isEqualTo("ABC");
  }

  @Test
  public void test_from_SupplierExceptionOnCreate() {
    CheckedSupplier<Reader> supplier = () -> {
      throw new IOException();
    };
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> StringCharSource.from(supplier));
  }

  @Test
  public void test_from_SupplierExceptionOnRead() {
    CheckedSupplier<Reader> supplier = () -> {
      return new Reader() {
        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
          throw new IOException();
        }

        @Override
        public void close() throws IOException {
        }
      };
    };
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> StringCharSource.from(supplier));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_from_Readable() throws IOException {
    CharSource source = ByteSource.wrap(new byte[] {'A', 'B', 'C'}).asCharSource(StandardCharsets.UTF_8);
    StringCharSource test = StringCharSource.from(source.openStream());
    assertThat(test.getFileName()).isEmpty();
    assertThat(test.read()).isEqualTo("ABC");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withFileName() {
    StringCharSource test = StringCharSource.of("ABC");
    assertThat(test.getFileName()).isEmpty();
    assertThat(test.withFileName("name.txt").getFileName()).hasValue("name.txt");
    assertThat(test.withFileName("foo/name.txt").getFileName()).hasValue("name.txt");
    assertThat(test.withFileName("foo/name.txt").getFileNameOrThrow()).isEqualTo("name.txt");
    assertThat(test.withFileName("").getFileName()).isEmpty();
    assertThatIllegalArgumentException().isThrownBy(() -> test.withFileName("").getFileNameOrThrow());
    assertThatIllegalArgumentException().isThrownBy(() -> test.withFileName(null).getFileName());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    StringCharSource test = StringCharSource.of("ABC").withFileName("file.txt");
    coverImmutableBean(test);
    test.metaBean().metaProperty("value").metaBean();
    test.metaBean().metaProperty("value").propertyGenericType();
    test.metaBean().metaProperty("value").annotations();
  }

  @Test
  public void testSerialize() {
    StringCharSource test = StringCharSource.of("ABC");
    String json = JodaBeanSer.PRETTY.jsonWriter().write(test);
    StringCharSource roundTrip = JodaBeanSer.PRETTY.jsonReader().read(json, StringCharSource.class);
    assertThat(roundTrip).isEqualTo(test);
    assertThat(roundTrip.getFileName()).isNotPresent();
  }

  @Test
  public void testSerializeNamed() {
    String fileName = "foo.txt";
    StringCharSource test = StringCharSource.of("ABC").withFileName(fileName);
    String json = JodaBeanSer.PRETTY.jsonWriter().write(test);
    StringCharSource roundTrip = JodaBeanSer.PRETTY.jsonReader().read(json, StringCharSource.class);
    assertThat(roundTrip).isEqualTo(test);
    // additional checks to ensure filenames are also equal
    assertThat(roundTrip.getFileName()).hasValue(fileName);
  }

}
