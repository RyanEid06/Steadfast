package com.steadfast.app.smoke;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Test-only Preferences protobuf codec: no app/R8 names or DataStore instance needed. */
final class PreferenceWire {
    static final class Value {
        final byte[] encoded;
        final String canonical;
        Value(byte[] encoded) {
            this.encoded = encoded;
            Reader reader = new Reader(encoded);
            int tag = (int) reader.number(), field = tag >>> 3;
            switch (tag) {
                case 8: canonical = "boolean:" + (reader.number() != 0); break;
                case 21: canonical = "float:" + hex(reader.fixed(4)); break;
                case 24: canonical = "integer:" + (int) reader.number(); break;
                case 32: canonical = "long:" + reader.number(); break;
                case 42: canonical = "string:" + new String(reader.bytes(), StandardCharsets.UTF_8); break;
                case 50:
                    Reader set = new Reader(reader.bytes());
                    ArrayList<String> strings = new ArrayList<>();
                    while (!set.done()) {
                        require(set.number() == 10, "Unexpected string-set field");
                        strings.add(new String(set.bytes(), StandardCharsets.UTF_8));
                    }
                    Collections.sort(strings);
                    StringBuilder result = new StringBuilder("stringSet:");
                    for (String string : strings) result.append(string.length()).append(':').append(string);
                    canonical = result.toString();
                    break;
                case 57: canonical = "double:" + hex(reader.fixed(8)); break;
                case 66: canonical = "bytes:" + Base64.getEncoder().encodeToString(reader.bytes()); break;
                default: throw new AssertionError("Unsupported preference value field " + field);
            }
            require(reader.done(), "Multiple/unknown preference value fields");
        }
    }

    static TreeMap<String, Value> read(byte[] encoded) {
        TreeMap<String, Value> result = new TreeMap<>();
        Reader map = new Reader(encoded);
        while (!map.done()) {
            require(map.number() == 10, "Unexpected preference map field");
            Reader entry = new Reader(map.bytes());
            String key = null;
            Value value = null;
            while (!entry.done()) {
                int tag = (int) entry.number();
                if (tag == 10) { require(key == null, "Duplicate map key field"); key = new String(entry.bytes(), StandardCharsets.UTF_8); }
                else if (tag == 18) { require(value == null, "Duplicate map value field"); value = new Value(entry.bytes()); }
                else throw new AssertionError("Unexpected preference entry field");
            }
            require(key != null && value != null && !result.containsKey(key), "Missing/duplicate preference entry");
            result.put(key, value);
        }
        return result;
    }

    static byte[] write(Map<String, Value> values) {
        ByteArrayOutputStream map = new ByteArrayOutputStream();
        for (Map.Entry<String, Value> value : new TreeMap<>(values).entrySet()) {
            ByteArrayOutputStream entry = new ByteArrayOutputStream();
            bytes(entry, 10, value.getKey().getBytes(StandardCharsets.UTF_8));
            bytes(entry, 18, value.getValue().encoded);
            bytes(map, 10, entry.toByteArray());
        }
        return map.toByteArray();
    }

    static Value string(String value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bytes(out, 42, value.getBytes(StandardCharsets.UTF_8));
        return new Value(out.toByteArray());
    }
    static Value number(int tag, long value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        number(out, tag); number(out, value);
        return new Value(out.toByteArray());
    }
    private static void bytes(ByteArrayOutputStream out, int tag, byte[] value) {
        number(out, tag); number(out, value.length); out.write(value, 0, value.length);
    }
    private static void number(ByteArrayOutputStream out, long value) {
        while ((value & ~127L) != 0) { out.write(((int) value & 127) | 128); value >>>= 7; }
        out.write((int) value);
    }
    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        for (byte b : bytes) out.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return out.toString();
    }
    private static final class Reader {
        final byte[] data;
        int at;
        Reader(byte[] data) { this.data = data; }
        boolean done() { return at == data.length; }
        long number() {
            long value = 0;
            for (int shift = 0; shift < 64; shift += 7) {
                require(at < data.length, "Truncated varint");
                int next = data[at++] & 255;
                require(shift != 63 || next <= 1, "Overflowing varint");
                value |= (long) (next & 127) << shift;
                if ((next & 128) == 0) return value;
            }
            throw new AssertionError("Overlong varint");
        }
        byte[] bytes() {
            long length = number();
            require(length >= 0 && length <= data.length - at, "Invalid length");
            return fixed((int) length);
        }
        byte[] fixed(int length) {
            require(length >= 0 && length <= data.length - at, "Truncated value");
            byte[] result = Arrays.copyOfRange(data, at, at + length); at += length;
            return result;
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    /** Host self-check, also called on device before reading or mutating the fixture. */
    public static void main(String[] ignored) {
        TreeMap<String, Value> values = new TreeMap<>();
        values.put("boolean", number(8, 1)); values.put("integer", number(24, -123));
        values.put("long", number(32, 1790000000000L)); values.put("string", string("Unicode: grâce 🙏\n|:\u0000"));
        values.put("float", new Value(new byte[]{21, 0, 0, (byte) 128, 63}));
        values.put("double", new Value(new byte[]{57, 0, 0, 0, 0, 0, 0, (byte) 240, 63}));
        values.put("bytes", new Value(new byte[]{66, 3, 0, (byte) 255, 42}));
        values.put("set", new Value(new byte[]{50, 6, 10, 1, 98, 10, 1, 97}));
        TreeMap<String, Value> copy = read(write(values));
        require(copy.size() == 8, "Lost preference type");
        for (String key : values.keySet()) require(values.get(key).canonical.equals(copy.get(key).canonical), "Codec round trip " + key);
        require(copy.get("integer").canonical.equals("integer:-123"), "Signed int lost");
        require(copy.get("set").canonical.equals("stringSet:1:a1:b"), "Set not canonical");
        require(read(new byte[0]).isEmpty(), "Empty map");
        for (byte[] bad : new byte[][]{{10}, {10, 127}, {10, 2, 10, 0}, {10, 8, 10, 1, 97, 18, 3, 42, 5, 1}}) {
            boolean failed = false;
            try { read(bad); } catch (AssertionError expected) { failed = true; }
            require(failed, "Malformed wire accepted");
        }
        System.out.println("OK Preferences wire codec: eight types, signed/Unicode/set canonicalization, malformed inputs");
    }
}
