/*
 * Copyright (C) 2014-2016 Samuel Audet
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE.txt file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bytedeco.javacpp;

import java.io.File;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.indexer.Bfloat16Indexer;
import org.bytedeco.javacpp.indexer.BooleanIndexer;
import org.bytedeco.javacpp.indexer.ByteIndexer;
import org.bytedeco.javacpp.indexer.CharIndexer;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.HalfIndexer;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacpp.indexer.IntIndexer;
import org.bytedeco.javacpp.indexer.LongIndexer;
import org.bytedeco.javacpp.indexer.ShortIndexer;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacpp.indexer.UShortIndexer;
import org.bytedeco.javacpp.tools.Builder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for the indexer package. Also uses other classes from JavaCPP.
 *
 * @author Samuel Audet
 */
@Platform
public class IndexerTest {

    @BeforeClass public static void setUpClass() throws Exception {
        System.out.println("Builder");
        Class c = IndexerTest.class;
        Builder builder = new Builder().classesOrPackages(c.getName());
        File[] outputFiles = builder.build();

        System.out.println("Loader");
        Loader.load(c);

        // work around OutOfMemoryError when testing long indexing
        Pointer.DeallocatorReference.totalBytes -= 1L << 48;
    }

    @AfterClass public static void tearDownClass() throws Exception {
        Pointer.DeallocatorReference.totalBytes += 1L << 48;
    }

    @Test public void testIndexer() {
        System.out.println("Indexer");
        long[] sizes = {640, 480, 3};
        long[] strides = Indexer.strides(sizes);
        System.out.println(Arrays.toString(strides));
        assertEquals(1440, strides[0]);
        assertEquals(   3, strides[1]);
        assertEquals(   1, strides[2]);
    }

    @Test public void testByteIndexer() {
        System.out.println("ByteIndexer");
        long size = 7 * 5 * 3 * 2;
        long[] sizes = { 7, 5, 3, 2 };
        long[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final BytePointer ptr = new BytePointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((byte)i);
        }
        ByteIndexer arrayIndexer = ByteIndexer.create(ptr.position(0), sizes, strides, false);
        ByteIndexer directIndexer = ByteIndexer.create(ptr.position(0), sizes, strides, true);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]) & 0xFF);
            assertEquals(n, directIndexer.get(i * strides[0]) & 0xFF);
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]) & 0xFF);
                assertEquals(n, directIndexer.get(i, j * strides[1]) & 0xFF);
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]) & 0xFF);
                    assertEquals(n, directIndexer.get(i, j, k * strides[2]) & 0xFF);
                    for (int m = 0; m < sizes[3]; m++) {
                        long[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index) & 0xFF);
                        assertEquals(n, directIndexer.get(index) & 0xFF);
                        arrayIndexer.put(index, (byte)(n + 1));
                        directIndexer.put(index, (byte)(n + 2));
                        n++;
                    }
                }
            }
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            directIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        short shortValue = 0x0203;
        int intValue = 0x02030405;
        long longValue = 0x0203040506070809L;
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            shortValue = Short.reverseBytes(shortValue);
            intValue = Integer.reverseBytes(intValue);
            longValue = Long.reverseBytes(longValue);
        }
        float floatValue = Float.intBitsToFloat(intValue);
        double doubleValue = Double.longBitsToDouble(longValue);

        assertEquals(shortValue, arrayIndexer.getShort(1));
        assertEquals(intValue, arrayIndexer.getInt(1));
        assertEquals(longValue, arrayIndexer.getLong(1));
        assertEquals(floatValue, arrayIndexer.getFloat(1), 0.0);
        assertEquals(doubleValue, arrayIndexer.getDouble(1), 0.0);
        assertEquals(shortValue, arrayIndexer.getChar(1));

        System.out.println("arrayIndexer" + arrayIndexer);
        System.out.println("directIndexer" + directIndexer);
        for (int i = 0; i < size; i++) {
            assertEquals(i + 2, ptr.position(i).get() & 0xFF);
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(i + 1, ptr.position(i).get() & 0xFF);
        }

        assertEquals(shortValue, directIndexer.getShort(1));
        assertEquals(intValue, directIndexer.getInt(1));
        assertEquals(longValue, directIndexer.getLong(1));
        assertEquals(floatValue, directIndexer.getFloat(1), 0.0);
        assertEquals(doubleValue, directIndexer.getDouble(1), 0.0);
        assertEquals((char)shortValue, directIndexer.getChar(1));

        System.gc();

        if (Loader.sizeof(Pointer.class) > 4) try {
            long longSize = 0x80000000L + 8192;
            final BytePointer longPointer = new BytePointer(longSize);
            assertEquals(longSize, longPointer.capacity());
            ByteIndexer longIndexer = ByteIndexer.create(longPointer);
            assertEquals(longIndexer.pointer(), longPointer);
            for (long i = 0; i < 8192; i++) {
                longPointer.put(longSize - i - 1, (byte)i);
            }
            for (long i = 0; i < 8192; i++) {
                assertEquals(longIndexer.get(longSize - i - 1), (byte)i);
            }
            System.out.println("longIndexer[0x" + Long.toHexString(longSize - 8192) + "] = " + longIndexer.get(longSize - 8192));
        } catch (OutOfMemoryError e) {
            System.out.println(e);
        }
        System.out.println();
    }

    @Test public void testShortIndexer() {
        System.out.println("ShortIndexer");
        long size = 7 * 5 * 3 * 2;
        long[] sizes = { 7, 5, 3, 2 };
        long[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final ShortPointer ptr = new ShortPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((short)i);
        }
        ShortIndexer arrayIndexer = ShortIndexer.create(ptr.position(0), sizes, strides, false);
        ShortIndexer directIndexer = ShortIndexer.create(ptr.position(0), sizes, strides, true);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]));
            assertEquals(n, directIndexer.get(i * strides[0]));
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]));
                assertEquals(n, directIndexer.get(i, j * strides[1]));
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]));
                    assertEquals(n, directIndexer.get(i, j, k * strides[2]));
                    for (int m = 0; m < sizes[3]; m++) {
                        long[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index));
                        assertEquals(n, directIndexer.get(index));
                        arrayIndexer.put(index, (short)(2 * n));
                        directIndexer.put(index, (short)(3 * n));
                        n++;
                    }
                }
            }
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            directIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("arrayIndexer" + arrayIndexer);
        System.out.println("directIndexer" + directIndexer);
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get());
        }
        System.gc();

        if (Loader.sizeof(Pointer.class) > 4) try {
            long longSize = 0x80000000L + 8192;
            final ShortPointer longPointer = new ShortPointer(longSize);
            assertEquals(longSize, longPointer.capacity());
            ShortIndexer longIndexer = ShortIndexer.create(longPointer);
            assertEquals(longIndexer.pointer(), longPointer);
            for (long i = 0; i < 8192; i++) {
                longPointer.put(longSize - i - 1, (short)i);
            }
            for (long i = 0; i < 8192; i++) {
                assertEquals(longIndexer.get(longSize - i - 1), (short)i);
            }
            System.out.println("longIndexer[0x" + Long.toHexString(longSize - 8192) + "] = " + longIndexer.get(longSize - 8192));
        } catch (OutOfMemoryError e) {
            System.out.println(e);
        }
        System.out.println();
    }

    @Test public void testIntIndexer() {
        System.out.println("IntIndexer");
        long size = 7 * 5 * 3 * 2;
        long[] sizes = { 7, 5, 3, 2 };
        long[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final IntPointer ptr = new IntPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((int)i);
        }
        IntIndexer arrayIndexer = IntIndexer.create(ptr.position(0), sizes, strides, false);
        IntIndexer directIndexer = IntIndexer.create(ptr.position(0), sizes, strides, true);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]));
            assertEquals(n, directIndexer.get(i * strides[0]));
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]));
                assertEquals(n, directIndexer.get(i, j * strides[1]));
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]));
                    assertEquals(n, directIndexer.get(i, j, k * strides[2]));
                    for (int m = 0; m < sizes[3]; m++) {
                        long[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index));
                        assertEquals(n, directIndexer.get(index));
                        arrayIndexer.put(index, (int)(2 * n));
                        directIndexer.put(index, (int)(3 * n));
                        n++;
                    }
                }
            }
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            directIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("arrayIndexer" + arrayIndexer);
        System.out.println("directIndexer" + directIndexer);
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get());
        }
        System.gc();

        if (Loader.sizeof(Pointer.class) > 4) try {
            long longSize = 0x80000000L + 8192;
            final IntPointer longPointer = new IntPointer(longSize);
            assertEquals(longSize, longPointer.capacity());
            IntIndexer longIndexer = IntIndexer.create(longPointer);
            assertEquals(longIndexer.pointer(), longPointer);
            for (long i = 0; i < 8192; i++) {
                longPointer.put(longSize - i - 1, (int)i);
            }
            for (long i = 0; i < 8192; i++) {
                assertEquals((long)longIndexer.get(longSize - i - 1), (int)i);
            }
            System.out.println("longIndexer[0x" + Long.toHexString(longSize - 8192) + "] = " + longIndexer.get(longSize - 8192));
        } catch (OutOfMemoryError e) {
            System.out.println(e);
        }
        System.out.println();
    }

    @Test public void testLongIndexer() {
        System.out.println("LongIndexer");
        long size = 7 * 5 * 3 * 2;
        long[] sizes = { 7, 5, 3, 2 };
        long[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final LongPointer ptr = new LongPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((long)i);
        }
        LongIndexer arrayIndexer = LongIndexer.create(ptr.position(0), sizes, strides, false);
        LongIndexer directIndexer = LongIndexer.create(ptr.position(0), sizes, strides, true);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]));
            assertEquals(n, directIndexer.get(i * strides[0]));
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]));
                assertEquals(n, directIndexer.get(i, j * strides[1]));
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]));
                    assertEquals(n, directIndexer.get(i, j, k * strides[2]));
                    for (int m = 0; m < sizes[3]; m++) {
                        long[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index));
                        assertEquals(n, directIndexer.get(index));
                        arrayIndexer.put(index, (long)(2 * n));
                        directIndexer.put(index, (long)(3 * n));
                        n++;
                    }
                }
            }
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            directIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("arrayIndexer" + arrayIndexer);
        System.out.println("directIndexer" + directIndexer);
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get());
        }
        System.gc();

        if (Loader.sizeof(Pointer.class) > 4) try {
            long longSize = 0x80000000L + 8192;
            final LongPointer longPointer = new LongPointer(longSize);
            assertEquals(longSize, longPointer.capacity());
            LongIndexer longIndexer = LongIndexer.create(longPointer);
            assertEquals(longIndexer.pointer(), longPointer);
            for (long i = 0; i < 8192; i++) {
                longPointer.put(longSize - i - 1, i);
            }
            for (long i = 0; i < 8192; i++) {
                assertEquals(longIndexer.get(longSize - i - 1), i);
            }
            System.out.println("longIndexer[0x" + Long.toHexString(longSize - 8192) + "] = " + longIndexer.get(longSize - 8192));
        } catch (OutOfMemoryError e) {
            System.out.println(e);
        }
        System.out.println();
    }

    @Test public void testFloatIndexer() {
        System.out.println("FloatIndexer");
        long size = 7 * 5 * 3 * 2;
        long[] sizes = { 7, 5, 3, 2 };
        long[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final FloatPointer ptr = new FloatPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((float)i);
        }
        FloatIndexer arrayIndexer = FloatIndexer.create(ptr.position(0), sizes, strides, false);
        FloatIndexer directIndexer = FloatIndexer.create(ptr.position(0), sizes, strides, true);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]), 0);
            assertEquals(n, directIndexer.get(i * strides[0]), 0);
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]), 0);
                assertEquals(n, directIndexer.get(i, j * strides[1]), 0);
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]), 0);
                    assertEquals(n, directIndexer.get(i, j, k * strides[2]), 0);
                    for (int m = 0; m < sizes[3]; m++) {
                        long[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index), 0);
                        assertEquals(n, directIndexer.get(index), 0);
                        arrayIndexer.put(index, (float)(2 * n));
                        directIndexer.put(index, (float)(3 * n));
                        n++;
                    }
                }
            }
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            directIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("arrayIndexer" + arrayIndexer);
        System.out.println("directIndexer" + directIndexer);
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get(), 0);
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get(), 0);
        }
        System.gc();

        if (Loader.sizeof(Pointer.class) > 4) try {
            long longSize = 0x80000000L + 8192;
            final FloatPointer longPointer = new FloatPointer(longSize);
            assertEquals(longSize, longPointer.capacity());
            FloatIndexer longIndexer = FloatIndexer.create(longPointer);
            assertEquals(longIndexer.pointer(), longPointer);
            for (long i = 0; i < 8192; i++) {
                longPointer.put(longSize - i - 1, i);
            }
            for (long i = 0; i < 8192; i++) {
                assertEquals((long)longIndexer.get(longSize - i - 1), i);
            }
            System.out.println("longIndexer[0x" + Long.toHexString(longSize - 8192) + "] = " + longIndexer.get(longSize - 8192));
        } catch (OutOfMemoryError e) {
            System.out.println(e);
        }
        System.out.println();
    }

    @Test public void testDoubleIndexer() {
        System.out.println("DoubleIndexer");
        long size = 7 * 5 * 3 * 2;
        long[] sizes = { 7, 5, 3, 2 };
        long[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final DoublePointer ptr = new DoublePointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((double)i);
        }
        DoubleIndexer arrayIndexer = DoubleIndexer.create(ptr.position(0), sizes, strides, false);
        DoubleIndexer directIndexer = DoubleIndexer.create(ptr.position(0), sizes, strides, true);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]), 0);
            assertEquals(n, directIndexer.get(i * strides[0]), 0);
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]), 0);
                assertEquals(n, directIndexer.get(i, j * strides[1]), 0);
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]), 0);
                    assertEquals(n, directIndexer.get(i, j, k * strides[2]), 0);
                    for (int m = 0; m < sizes[3]; m++) {
                        long[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index), 0);
                        assertEquals(n, directIndexer.get(index), 0);
                        arrayIndexer.put(index, (double)(2 * n));
                        directIndexer.put(index, (double)(3 * n));
                        n++;
                    }
                }
            }
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            directIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("arrayIndexer" + arrayIndexer);
        System.out.println("directIndexer" + directIndexer);
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get(), 0);
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get(), 0);
        }
        System.gc();

        if (Loader.sizeof(Pointer.class) > 4) try {
            long longSize = 0x80000000L + 8192;
            final DoublePointer longPointer = new DoublePointer(longSize);
            assertEquals(longSize, longPointer.capacity());
            DoubleIndexer longIndexer = DoubleIndexer.create(longPointer);
            assertEquals(longIndexer.pointer(), longPointer);
            for (long i = 0; i < 8192; i++) {
                longPointer.put(longSize - i - 1, i);
            }
            for (long i = 0; i < 8192; i++) {
                assertEquals((long)longIndexer.get(longSize - i - 1), i);
            }
            System.out.println("longIndexer[0x" + Long.toHexString(longSize - 8192) + "] = " + longIndexer.get(longSize - 8192));
        } catch (OutOfMemoryError e) {
            System.out.println(e);
        }
        System.out.println();
    }

    @Test public void testCharIndexer() {
        System.out.println("CharIndexer");
        long size = 7 * 5 * 3 * 2;
        long[] sizes = { 7, 5, 3, 2 };
        long[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final CharPointer ptr = new CharPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((char)i);
        }
        CharIndexer arrayIndexer = CharIndexer.create(ptr.position(0), sizes, strides, false);
        CharIndexer directIndexer = CharIndexer.create(ptr.position(0), sizes, strides, true);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]));
            assertEquals(n, directIndexer.get(i * strides[0]));
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]));
                assertEquals(n, directIndexer.get(i, j * strides[1]));
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]));
                    assertEquals(n, directIndexer.get(i, j, k * strides[2]));
                    for (int m = 0; m < sizes[3]; m++) {
                        long[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index));
                        assertEquals(n, directIndexer.get(index));
                        arrayIndexer.put(index, (char)(2 * n));
                        directIndexer.put(index, (char)(3 * n));
                        n++;
                    }
                }
            }
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            directIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("arrayIndexer" + arrayIndexer);
        System.out.println("directIndexer" + directIndexer);
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get());
        }
        System.gc();

        if (Loader.sizeof(Pointer.class) > 4) try {
            long longSize = 0x80000000L + 8192;
            final CharPointer longPointer = new CharPointer(longSize);
            assertEquals(longSize, longPointer.capacity());
            CharIndexer longIndexer = CharIndexer.create(longPointer);
            assertEquals(longIndexer.pointer(), longPointer);
            for (long i = 0; i < 8192; i++) {
                longPointer.put(longSize - i - 1, (char)i);
            }
            for (long i = 0; i < 8192; i++) {
                assertEquals((long)longIndexer.get(longSize - i - 1), (char)i);
            }
            System.out.println("longIndexer[0x" + Long.toHexString(longSize) + " - 8192] = " + (int)longIndexer.get(longSize - 8192));
        } catch (OutOfMemoryError e) {
            System.out.println(e);
        }
        System.out.println();
    }

    @Test public void testBooleanIndexer() {
        System.out.println("BooleanIndexer");
        long size = 7 * 5 * 3 * 2;
        long[] sizes = { 7, 5, 3, 2 };
        long[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final BooleanPointer ptr = new BooleanPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put(i % 2 != 0);
        }
        BooleanIndexer arrayIndexer = BooleanIndexer.create(ptr.position(0), sizes, strides, false);
        BooleanIndexer directIndexer = BooleanIndexer.create(ptr.position(0), sizes, strides, true);
        ByteIndexer byteIndexer = ByteIndexer.create(new BytePointer(ptr).position(0), sizes, strides, true);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n % 2 != 0, arrayIndexer.get(i * strides[0]));
            assertEquals(n % 2 != 0, directIndexer.get(i * strides[0]));
            assertEquals(n % 2 != 0 ? 1 : 0, byteIndexer.get(i * strides[0]));
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n % 2 != 0, arrayIndexer.get(i, j * strides[1]));
                assertEquals(n % 2 != 0, directIndexer.get(i, j * strides[1]));
                assertEquals(n % 2 != 0 ? 1 : 0, byteIndexer.get(i, j * strides[1]));
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n % 2 != 0, arrayIndexer.get(i, j, k * strides[2]));
                    assertEquals(n % 2 != 0, directIndexer.get(i, j, k * strides[2]));
                    assertEquals(n % 2 != 0 ? 1 : 0, byteIndexer.get(i, j, k * strides[2]));
                    for (int m = 0; m < sizes[3]; m++) {
                        long[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n % 2 != 0, arrayIndexer.get(index));
                        assertEquals(n % 2 != 0, directIndexer.get(index));
                        assertEquals(n % 2 != 0 ? 1 : 0, byteIndexer.get(index));
                        arrayIndexer.put(index, n % 3 != 0);
                        directIndexer.put(index, n % 4 != 0);
                        n++;
                    }
                }
            }
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            directIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("arrayIndexer" + arrayIndexer);
        System.out.println("directIndexer" + directIndexer);
        for (int i = 0; i < size; i++) {
            assertEquals(i % 4 != 0, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(i % 3 != 0, ptr.position(i).get());
        }
        System.gc();

        if (Loader.sizeof(Pointer.class) > 4) try {
            long longSize = 0x80000000L + 8192;
            final BooleanPointer longPointer = new BooleanPointer(longSize);
            assertEquals(longSize, longPointer.capacity());
            BooleanIndexer longIndexer = BooleanIndexer.create(longPointer);
            assertEquals(longIndexer.pointer(), longPointer);
            for (long i = 0; i < 8192; i++) {
                longPointer.put(longSize - i - 1, i % 2 != 0);
            }
            for (long i = 0; i < 8192; i++) {
                assertEquals(longIndexer.get(longSize - i - 1), i % 2 != 0);
            }
            System.out.println("longIndexer[0x" + Long.toHexString(longSize) + " - 8192] = " + longIndexer.get(longSize - 8192));
        } catch (OutOfMemoryError e) {
            System.out.println(e);
        }
        System.out.println();
    }

    @Test public void testUByteIndexer() {
        System.out.println("UByteIndexer");
        long size = 7 * 5 * 3 * 2;
        long[] sizes = { 7, 5, 3, 2 };
        long[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final BytePointer ptr = new BytePointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((byte)i);
        }
        UByteIndexer arrayIndexer = UByteIndexer.create(ptr.position(0), sizes, strides, false);
        UByteIndexer directIndexer = UByteIndexer.create(ptr.position(0), sizes, strides, true);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]));
            assertEquals(n, directIndexer.get(i * strides[0]));
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]));
                assertEquals(n, directIndexer.get(i, j * strides[1]));
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]));
                    assertEquals(n, directIndexer.get(i, j, k * strides[2]));
                    for (int m = 0; m < sizes[3]; m++) {
                        long[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index));
                        assertEquals(n, directIndexer.get(index));
                        arrayIndexer.put(index, n + 1);
                        directIndexer.put(index, n + 2);
                        n++;
                    }
                }
            }
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            directIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("arrayIndexer" + arrayIndexer);
        System.out.println("directIndexer" + directIndexer);
        for (int i = 0; i < size; i++) {
            assertEquals(i + 2, ptr.position(i).get() & 0xFF);
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(i + 1, ptr.position(i).get() & 0xFF);
        }
        System.gc();

        if (Loader.sizeof(Pointer.class) > 4) try {
            long longSize = 0x80000000L + 8192;
            final BytePointer longPointer = new BytePointer(longSize);
            assertEquals(longSize, longPointer.capacity());
            UByteIndexer longIndexer = UByteIndexer.create(longPointer);
            assertEquals(longIndexer.pointer(), longPointer);
            for (long i = 0; i < 8192; i++) {
                longPointer.put(longSize - i - 1, (byte)i);
            }
            for (long i = 0; i < 8192; i++) {
                assertEquals(longIndexer.get(longSize - i - 1), i & 0xFF);
            }
            System.out.println("longIndexer[0x" + Long.toHexString(longSize - 8192) + "] = " + longIndexer.get(longSize - 8192));
        } catch (OutOfMemoryError e) {
            System.out.println(e);
        }
        System.out.println();
    }

    @Test public void testUShortIndexer() {
        System.out.println("UShortIndexer");
        long size = 7 * 5 * 3 * 2;
        long[] sizes = { 7, 5, 3, 2 };
        long[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final ShortPointer ptr = new ShortPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((short)i);
        }
        UShortIndexer arrayIndexer = UShortIndexer.create(ptr.position(0), sizes, strides, false);
        UShortIndexer directIndexer = UShortIndexer.create(ptr.position(0), sizes, strides, true);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]));
            assertEquals(n, directIndexer.get(i * strides[0]));
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]));
                assertEquals(n, directIndexer.get(i, j * strides[1]));
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]));
                    assertEquals(n, directIndexer.get(i, j, k * strides[2]));
                    for (int m = 0; m < sizes[3]; m++) {
                        long[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index));
                        assertEquals(n, directIndexer.get(index));
                        arrayIndexer.put(index, 2 * n);
                        directIndexer.put(index, 3 * n);
                        n++;
                    }
                }
            }
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            directIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("arrayIndexer" + arrayIndexer);
        System.out.println("directIndexer" + directIndexer);
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, ptr.position(i).get());
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, ptr.position(i).get());
        }
        System.gc();

        if (Loader.sizeof(Pointer.class) > 4) try {
            long longSize = 0x80000000L + 8192;
            final ShortPointer longPointer = new ShortPointer(longSize);
            assertEquals(longSize, longPointer.capacity());
            UShortIndexer longIndexer = UShortIndexer.create(longPointer);
            assertEquals(longIndexer.pointer(), longPointer);
            for (long i = 0; i < 8192; i++) {
                longPointer.put(longSize - i - 1, (short)i);
            }
            for (long i = 0; i < 8192; i++) {
                assertEquals(longIndexer.get(longSize - i - 1), i & 0xFFFF);
            }
            System.out.println("longIndexer[0x" + Long.toHexString(longSize - 8192) + "] = " + longIndexer.get(longSize - 8192));
        } catch (OutOfMemoryError e) {
            System.out.println(e);
        }
        System.out.println();
    }

    @Test public void testHalfIndexer() {
        System.out.println("HalfIndexer");

        for (int i = 0; i <= 0xFFFF; i++) {
            float f = HalfIndexer.toFloat(i);
            int i2 = HalfIndexer.fromFloat(f);
            float f2 = HalfIndexer.toFloat(i2);
            if (Float.isNaN(f)) {
                assertTrue(Float.isNaN(f2));
            } else if (Float.isInfinite(f)) {
                assertTrue(Float.isInfinite(f2));
            } else {
                assertEquals(i, i2);
            }
        }

        long size = 7 * 5 * 3 * 2;
        long[] sizes = { 7, 5, 3, 2 };
        long[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final ShortPointer ptr = new ShortPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((short)HalfIndexer.fromFloat(i));
        }
        HalfIndexer arrayIndexer = HalfIndexer.create(ptr.position(0), sizes, strides, false);
        HalfIndexer directIndexer = HalfIndexer.create(ptr.position(0), sizes, strides, true);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]), 0);
            assertEquals(n, directIndexer.get(i * strides[0]), 0);
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]), 0);
                assertEquals(n, directIndexer.get(i, j * strides[1]), 0);
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]), 0);
                    assertEquals(n, directIndexer.get(i, j, k * strides[2]), 0);
                    for (int m = 0; m < sizes[3]; m++) {
                        long[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index), 0);
                        assertEquals(n, directIndexer.get(index), 0);
                        arrayIndexer.put(index, 2 * n);
                        directIndexer.put(index, 3 * n);
                        n++;
                    }
                }
            }
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            directIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("arrayIndexer" + arrayIndexer);
        System.out.println("directIndexer" + directIndexer);
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, HalfIndexer.toFloat(ptr.position(i).get()), 0);
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, HalfIndexer.toFloat(ptr.position(i).get()), 0);
        }
        System.gc();

        if (Loader.sizeof(Pointer.class) > 4) try {
            long longSize = 0x80000000L + 8192;
            final ShortPointer longPointer = new ShortPointer(longSize);
            assertEquals(longSize, longPointer.capacity());
            HalfIndexer longIndexer = HalfIndexer.create(longPointer);
            assertEquals(longIndexer.pointer(), longPointer);
            for (long i = 0; i < 8192; i++) {
                longPointer.put(longSize - i - 1, (short)HalfIndexer.fromFloat(i));
            }
            for (long i = 0; i < 8192; i++) {
                assertEquals(longIndexer.get(longSize - i - 1), (float)i, 2);
            }
            System.out.println("longIndexer[0x" + Long.toHexString(longSize - 8192) + "] = " + longIndexer.get(longSize - 8192));
        } catch (OutOfMemoryError e) {
            System.out.println(e);
        }
        System.out.println();
    }

    @Test public void testBfloat16Indexer() {
        System.out.println("Bfloat16Indexer");

        for (int i = 0; i <= 0xFFFF; i++) {
            float f = Bfloat16Indexer.toFloat(i);
            int i2 = Bfloat16Indexer.fromFloat(f);
            float f2 = Bfloat16Indexer.toFloat(i2);
            if (Float.isNaN(f)) {
                assertTrue(Float.isNaN(f2));
            } else if (Float.isInfinite(f)) {
                assertTrue(Float.isInfinite(f2));
            } else {
                assertEquals(i, i2);
            }
        }

        long size = 7 * 5 * 3 * 2;
        long[] sizes = { 7, 5, 3, 2 };
        long[] strides = { 5 * 3 * 2, 3 * 2, 2, 1 };
        final ShortPointer ptr = new ShortPointer(size);
        for (int i = 0; i < size; i++) {
            ptr.position(i).put((short)Bfloat16Indexer.fromFloat(i));
        }
        Bfloat16Indexer arrayIndexer = Bfloat16Indexer.create(ptr.position(0), sizes, strides, false);
        Bfloat16Indexer directIndexer = Bfloat16Indexer.create(ptr.position(0), sizes, strides, true);

        int n = 0;
        for (int i = 0; i < sizes[0]; i++) {
            assertEquals(n, arrayIndexer.get(i * strides[0]), 0);
            assertEquals(n, directIndexer.get(i * strides[0]), 0);
            for (int j = 0; j < sizes[1]; j++) {
                assertEquals(n, arrayIndexer.get(i, j * strides[1]), 0);
                assertEquals(n, directIndexer.get(i, j * strides[1]), 0);
                for (int k = 0; k < sizes[2]; k++) {
                    assertEquals(n, arrayIndexer.get(i, j, k * strides[2]), 0);
                    assertEquals(n, directIndexer.get(i, j, k * strides[2]), 0);
                    for (int m = 0; m < sizes[3]; m++) {
                        long[] index = { i, j, k, m  * strides[3] };
                        assertEquals(n, arrayIndexer.get(index), 0);
                        assertEquals(n, directIndexer.get(index), 0);
                        arrayIndexer.put(index, 2 * n);
                        directIndexer.put(index, 3 * n);
                        n++;
                    }
                }
            }
        }

        try {
            arrayIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        try {
            directIndexer.get(size);
            fail("IndexOutOfBoundsException should have been thrown.");
        } catch (IndexOutOfBoundsException e) { }

        System.out.println("arrayIndexer" + arrayIndexer);
        System.out.println("directIndexer" + directIndexer);
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, Bfloat16Indexer.toFloat(ptr.position(i).get()), 3);
        }
        arrayIndexer.release();
        for (int i = 0; i < size; i++) {
            assertEquals(2 * i, Bfloat16Indexer.toFloat(ptr.position(i).get()), 0);
        }
        System.gc();

        if (Loader.sizeof(Pointer.class) > 4) try {
            long longSize = 0x80000000L + 8192;
            final ShortPointer longPointer = new ShortPointer(longSize);
            assertEquals(longSize, longPointer.capacity());
            Bfloat16Indexer longIndexer = Bfloat16Indexer.create(longPointer);
            assertEquals(longIndexer.pointer(), longPointer);
            for (long i = 0; i < 8192; i++) {
                longPointer.put(longSize - i - 1, (short)Bfloat16Indexer.fromFloat(i));
            }
            for (long i = 0; i < 8192; i++) {
                assertEquals(longIndexer.get(longSize - i - 1), (float)i, 31);
            }
            System.out.println("longIndexer[0x" + Long.toHexString(longSize - 8192) + "] = " + longIndexer.get(longSize - 8192));
        } catch (OutOfMemoryError e) {
            System.out.println(e);
        }
        System.out.println();
    }

}
