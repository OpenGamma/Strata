/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.joda.beans.ser.JodaBeanSer;
import org.junit.jupiter.api.Test;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteProcessor;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import com.google.common.io.Resources;
import com.opengamma.strata.collect.function.CheckedConsumer;
import com.opengamma.strata.collect.function.CheckedSupplier;

/**
 * Test {@link ArrayByteSource}.
 */
public class ArrayByteSourceTest {

  @Test
  public void test_EMPTY() {
    ArrayByteSource test = ArrayByteSource.EMPTY;
    assertThat(test.isEmpty()).isEqualTo(true);
    assertThat(test.size()).isEqualTo(0);
    assertThat(test.getFileName()).isEmpty();
  }

  @Test
  public void test_copyOf() {
    byte[] bytes = {1, 2, 3};
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.getFileName()).isEmpty();
    assertThat(test.read()[0]).isEqualTo((byte) 1);
    assertThat(test.read()[1]).isEqualTo((byte) 2);
    assertThat(test.read()[2]).isEqualTo((byte) 3);
    bytes[0] = 4;
    assertThat(test.read()[0]).isEqualTo((byte) 1);
  }

  @Test
  public void test_copyOf_from() {
    byte[] bytes = {1, 2, 3};
    ArrayByteSource test = ArrayByteSource.copyOf(bytes, 1);
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.read()[0]).isEqualTo((byte) 2);
    assertThat(test.read()[1]).isEqualTo((byte) 3);
  }

  @Test
  public void test_copyOf_fromTo() {
    byte[] bytes = {1, 2, 3};
    ArrayByteSource test = ArrayByteSource.copyOf(bytes, 1, 2);
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.read()[0]).isEqualTo((byte) 2);
  }

  @Test
  public void test_copyOf_fromTo_empty() {
    byte[] bytes = {1, 2, 3};
    ArrayByteSource test = ArrayByteSource.copyOf(bytes, 1, 1);
    assertThat(test.size()).isEqualTo(0);
  }

  @Test
  public void test_copyOf_fromTo_bad() {
    byte[] bytes = {1, 2, 3};
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> ArrayByteSource.copyOf(bytes, -1, 2));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> ArrayByteSource.copyOf(bytes, 0, 4));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> ArrayByteSource.copyOf(bytes, 4, 5));
  }

  @Test
  public void test_ofUnsafe() {
    byte[] bytes = {1, 2, 3};
    ArrayByteSource test = ArrayByteSource.ofUnsafe(bytes);
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.getFileName()).isEmpty();
    assertThat(test.read()[0]).isEqualTo((byte) 1);
    assertThat(test.read()[1]).isEqualTo((byte) 2);
    assertThat(test.read()[2]).isEqualTo((byte) 3);
    bytes[0] = 4;  // abusing the unsafe factory
    assertThat(test.read()[0]).isEqualTo((byte) 4);
  }

  @Test
  public void test_ofUtf8() {
    ArrayByteSource test = ArrayByteSource.ofUtf8("ABC");
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.getFileName()).isEmpty();
    assertThat(test.read()[0]).isEqualTo((byte) 'A');
    assertThat(test.read()[1]).isEqualTo((byte) 'B');
    assertThat(test.read()[2]).isEqualTo((byte) 'C');
  }

  @Test
  public void test_from_ByteSource() {
    ByteSource source = ByteSource.wrap(new byte[] {1, 2, 3});
    ArrayByteSource test = ArrayByteSource.from(source);
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.getFileName()).isEmpty();
    assertThat(test.read()[0]).isEqualTo((byte) 1);
    assertThat(test.read()[1]).isEqualTo((byte) 2);
    assertThat(test.read()[2]).isEqualTo((byte) 3);
  }

  @Test
  public void test_from_ByteSource_alreadyArrayByteSource() {
    ArrayByteSource base = ArrayByteSource.copyOf(new byte[] {1, 2, 3});
    ArrayByteSource test = ArrayByteSource.from(base);
    assertThat(test).isSameAs(base);
    assertThat(test.getFileName()).isEmpty();
  }

  @Test
  public void test_from_FileByteSource() {
    ByteSource source = FileByteSource.of(new File("pom.xml"));
    ArrayByteSource test = ArrayByteSource.from(source);
    assertThat(test.getFileName()).hasValue("pom.xml");
  }

  @Test
  public void test_from_UriByteSource() {
    ByteSource source = UriByteSource.of(new File("pom.xml").toURI());
    ArrayByteSource test = ArrayByteSource.from(source);
    assertThat(test.getFileName()).hasValue("pom.xml");
  }

  @Test
  public void test_from_GuavaFileByteSource() {
    ByteSource source = Files.asByteSource(new File("pom.xml"));
    ArrayByteSource test = ArrayByteSource.from(source);
    assertThat(test.getFileName()).hasValue("pom.xml");
  }

  @Test
  public void test_from_GuavaPathByteSource() {
    ByteSource source = MoreFiles.asByteSource(Paths.get("pom.xml"));
    ArrayByteSource test = ArrayByteSource.from(source);
    assertThat(test.getFileName()).hasValue("pom.xml");
  }

  @Test
  public void test_from_GuavaUrlByteSource() throws MalformedURLException {
    ByteSource source = Resources.asByteSource(new File("pom.xml").toURI().toURL());
    ArrayByteSource test = ArrayByteSource.from(source);
    assertThat(test.getFileName()).hasValue("pom.xml");
  }

  @Test
  public void test_from_GuavaSimpleByteSource() {
    byte[] bytes = new byte[] {1, 2, 3};
    ByteSource source = ByteSource.wrap(bytes);
    ArrayByteSource test = ArrayByteSource.from(source);
    assertThat(test.readUnsafe()).isSameAs(bytes);
  }

  @Test
  public void test_from_GuavaSimpleByteSourceSliced() {
    byte[] bytes = new byte[] {1, 2, 3};
    ByteSource source = ByteSource.wrap(bytes).slice(1, 1);
    ArrayByteSource test = ArrayByteSource.from(source);
    assertThat(test.readUnsafe()).containsExactly(2);
  }

  @Test
  public void test_from_Supplier() {
    ByteSource source = ByteSource.wrap(new byte[] {1, 2, 3});
    ArrayByteSource test = ArrayByteSource.from(() -> source.openStream());
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.getFileName()).isEmpty();
    assertThat(test.read()[0]).isEqualTo((byte) 1);
    assertThat(test.read()[1]).isEqualTo((byte) 2);
    assertThat(test.read()[2]).isEqualTo((byte) 3);
  }

  @Test
  public void test_from_SupplierExceptionOnCreate() {
    CheckedSupplier<InputStream> supplier = () -> {
      throw new IOException();
    };
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> ArrayByteSource.from(supplier));
  }

  @Test
  public void test_from_SupplierExceptionOnRead() {
    CheckedSupplier<InputStream> supplier = () -> {
      return new InputStream() {
        @Override
        public int read() throws IOException {
          throw new IOException();
        }
      };
    };
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> ArrayByteSource.from(supplier));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_from_InputStream_sizeCorrect() throws IOException {
    ByteSource source = ByteSource.wrap(new byte[] {1, 2, 3, 4, 5});
    ArrayByteSource test = ArrayByteSource.from(source.openStream(), 5);
    assertThat(test.size()).isEqualTo(5);
    assertThat(test.getFileName()).isEmpty();
    assertThat(test.read()).containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  public void test_from_InputStream_sizeTooSmall() throws IOException {
    ByteSource source = ByteSource.wrap(new byte[] {1, 2, 3, 4, 5});
    ArrayByteSource test = ArrayByteSource.from(source.openStream(), 2);
    assertThat(test.size()).isEqualTo(5);
    assertThat(test.getFileName()).isEmpty();
    assertThat(test.read()).containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  public void test_from_InputStream_sizeTooBig() throws IOException {
    ByteSource source = ByteSource.wrap(new byte[] {1, 2, 3, 4, 5});
    ArrayByteSource test = ArrayByteSource.from(source.openStream(), 6);
    assertThat(test.size()).isEqualTo(5);
    assertThat(test.getFileName()).isEmpty();
    assertThat(test.read()).containsExactly(1, 2, 3, 4, 5);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_fromOutput_Consumer() {
    byte[] bytes = new byte[] {1, 2, 3};
    ArrayByteSource test = ArrayByteSource.fromOutput(outputStream -> outputStream.write(bytes));
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.getFileName()).isEmpty();
    assertThat(test.read()[0]).isEqualTo((byte) 1);
    assertThat(test.read()[1]).isEqualTo((byte) 2);
    assertThat(test.read()[2]).isEqualTo((byte) 3);
  }

  @Test
  public void test_fromOutput_ConsumerExceptionOnWrite() {
    CheckedConsumer<OutputStream> consumer = out -> {
      throw new IOException();
    };
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> ArrayByteSource.fromOutput(consumer));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withFileName() {
    ArrayByteSource test = ArrayByteSource.copyOf(new byte[] {1, 2, 3});
    assertThat(test.getFileName()).isEmpty();
    assertThat(test.withFileName("name.txt").getFileName()).hasValue("name.txt");
    assertThat(test.withFileName("foo/name.txt").getFileName()).hasValue("name.txt");
    assertThat(test.withFileName("foo/name.txt").getFileNameOrThrow()).isEqualTo("name.txt");
    assertThat(test.withFileName("").getFileName()).isEmpty();
    assertThatIllegalArgumentException().isThrownBy(() -> test.withFileName("").getFileNameOrThrow());
    assertThatIllegalArgumentException().isThrownBy(() -> test.withFileName(null).getFileName());
  }

  @Test
  public void test_read() {
    ArrayByteSource test = ArrayByteSource.copyOf(new byte[] {1, 2, 3});
    assertThat(test.size()).isEqualTo(3);
    byte[] safeArray = test.read();
    safeArray[0] = 4;
    assertThat(test.read()[0]).isEqualTo((byte) 1);
    assertThat(test.read()[1]).isEqualTo((byte) 2);
    assertThat(test.read()[2]).isEqualTo((byte) 3);
  }

  @Test
  public void test_readUnsafe() {
    ArrayByteSource test = ArrayByteSource.copyOf(new byte[] {1, 2, 3});
    assertThat(test.size()).isEqualTo(3);
    byte[] unsafeArray = test.readUnsafe();
    unsafeArray[0] = 4;  // abusing the unsafe array
    assertThat(test.read()[0]).isEqualTo((byte) 4);
    assertThat(test.read()[1]).isEqualTo((byte) 2);
    assertThat(test.read()[2]).isEqualTo((byte) 3);
  }

  @Test
  public void test_read_processor() throws IOException {
    ArrayByteSource test = ArrayByteSource.copyOf(new byte[] {1, 2, 3});
    ByteProcessor<Integer> processor = new ByteProcessor<Integer>() {

      @Override
      public boolean processBytes(byte[] buf, int off, int len) throws IOException {
        assertThat(off).isEqualTo(0);
        assertThat(len).isEqualTo(3);
        assertThat(buf[0]).isEqualTo((byte) 1);
        assertThat(buf[1]).isEqualTo((byte) 2);
        assertThat(buf[2]).isEqualTo((byte) 3);
        return false;
      }

      @Override
      public Integer getResult() {
        return 123;
      }
    };
    assertThat(test.read(processor)).isEqualTo(123);
  }

  @Test
  public void test_slice() throws IOException {
    ArrayByteSource test = ArrayByteSource.copyOf(new byte[] {65, 66, 67, 68, 69});
    assertThat(test.size()).isEqualTo(5);
    assertThat(test.slice(0, 3).readUtf8()).isEqualTo("ABC");
    assertThat(test.slice(0, 5).readUtf8()).isEqualTo("ABCDE");
    assertThat(test.slice(0, Long.MAX_VALUE).readUtf8()).isEqualTo("ABCDE");
    assertThat(test.slice(1, 1).readUtf8()).isEqualTo("B");
    assertThat(test.slice(1, 2).readUtf8()).isEqualTo("BC");
    assertThat(test.slice(1, 3).readUtf8()).isEqualTo("BCD");
    assertThat(test.slice(1, 4).readUtf8()).isEqualTo("BCDE");
    assertThat(test.slice(2, 1).readUtf8()).isEqualTo("C");
    assertThat(test.slice(2, 2).readUtf8()).isEqualTo("CD");
    assertThat(test.slice(2, 3).readUtf8()).isEqualTo("CDE");
    assertThat(test.slice(2, Long.MAX_VALUE).readUtf8()).isEqualTo("CDE");
    assertThat(test.slice(5, 6).readUtf8()).isEqualTo("");
    assertThat(test.slice(5, Long.MAX_VALUE).readUtf8()).isEqualTo("");
    assertThat(test.slice(Long.MAX_VALUE - 10, Long.MAX_VALUE).readUtf8()).isEmpty();
  }

  @Test
  public void test_methods() throws IOException {
    ArrayByteSource test = ArrayByteSource.copyOf(new byte[] {65, 66, 67});
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.isEmpty()).isEqualTo(false);
    assertThat(test.sizeIfKnown().isPresent()).isEqualTo(true);
    assertThat(test.sizeIfKnown().get()).isEqualTo((Long) 3L);
    assertThat(test.readUtf8()).isEqualTo("ABC");
    assertThat(test.readUtf8UsingBom()).isEqualTo("ABC");
    assertThat(test.asCharSource(StandardCharsets.US_ASCII).read()).isEqualTo("ABC");
    assertThat(test.asCharSourceUtf8().read()).isEqualTo("ABC");
    assertThat(test.asCharSourceUtf8UsingBom().read()).isEqualTo("ABC");
    assertThat(test.asCharSourceUtf8().asByteSourceUtf8()).isEqualTo(test);
    assertThat(test.contentEquals(test)).isTrue();
    assertThat(test.toString()).isEqualTo("ArrayByteSource[3 bytes]");
    assertThat(test.withFileName("name.txt").toString()).isEqualTo("ArrayByteSource[3 bytes, name.txt]");
    assertThat(test.withFileName("name.txt").contentEquals(test)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toHash() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    HashCode hash = Hashing.crc32().hashBytes(bytes);
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    assertThat(test.toHash(Hashing.crc32())).isEqualTo(ArrayByteSource.ofUnsafe(hash.asBytes()));
    assertThat(test.toHashString(Hashing.crc32())).isEqualTo(hash.toString());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void test_md5() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    @SuppressWarnings("deprecation")
    byte[] hash = Hashing.md5().hashBytes(bytes).asBytes();
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    assertThat(test.toMd5()).isEqualTo(ArrayByteSource.ofUnsafe(hash));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void test_sha512() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    byte[] hash = Hashing.sha512().hashBytes(bytes).asBytes();
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    assertThat(test.toSha512()).isEqualTo(ArrayByteSource.ofUnsafe(hash));
  }

  @Test
  public void test_base64() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    @SuppressWarnings("deprecation")
    byte[] base64 = BaseEncoding.base64().encode(bytes).getBytes(StandardCharsets.UTF_8);
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    assertThat(test.toBase64()).isEqualTo(ArrayByteSource.ofUnsafe(base64));
  }

  @Test
  public void test_base64String() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    @SuppressWarnings("deprecation")
    String base64 = BaseEncoding.base64().encode(bytes);
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    assertThat(test.toBase64String()).isEqualTo(base64);
    ArrayByteSource roundtrip = ArrayByteSource.fromBase64(base64);
    assertThat(roundtrip).isEqualTo(test);
    assertThat(test.toBase64String()).isEqualTo(test.toBase64().readUtf8());
  }

  @Test
  public void test_hexString() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    String hex = "41424363";
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    assertThat(test.toHexString()).isEqualTo(hex);
    ArrayByteSource roundtrip = ArrayByteSource.fromHex(hex);
    assertThat(roundtrip).isEqualTo(test);
  }

  @Test
  public void test_load() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    ArrayByteSource loaded = test.load();
    assertThat(loaded).isSameAs(test);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    coverImmutableBean(test);
    coverImmutableBean(test.asCharSourceUtf8());
    test.metaBean().metaProperty("array").metaBean();
    test.metaBean().metaProperty("array").propertyGenericType();
    test.metaBean().metaProperty("array").annotations();
  }

  @Test
  public void testSerialize() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    ArrayByteSource test = ArrayByteSource.copyOf(bytes);
    String json = JodaBeanSer.PRETTY.jsonWriter().write(test);
    ArrayByteSource roundTrip = JodaBeanSer.PRETTY.jsonReader().read(json, ArrayByteSource.class);
    assertThat(roundTrip).isEqualTo(test);
    assertThat(roundTrip.getFileName()).isNotPresent();
  }

  @Test
  public void testSerializeNamed() {
    byte[] bytes = new byte[] {65, 66, 67, 99};
    String fileName = "foo.txt";
    ArrayByteSource test = ArrayByteSource.copyOf(bytes).withFileName(fileName);
    String json = JodaBeanSer.PRETTY.jsonWriter().write(test);
    ArrayByteSource roundTrip = JodaBeanSer.PRETTY.jsonReader().read(json, ArrayByteSource.class);
    assertThat(roundTrip).isEqualTo(test);
    // additional checks to ensure filenames are also equal
    assertThat(roundTrip.getFileName()).hasValue(fileName);
  }

}
