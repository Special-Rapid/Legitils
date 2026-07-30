package com.snkisk.hypixellegitils.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small dependency-free JSON codec for the versioned local configuration files. */
final class SimpleJson {
    private SimpleJson() {
    }

    static Object parse(String source) {
        Parser parser = new Parser(source);
        Object value = parser.value();
        parser.whitespace();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("Unexpected JSON content at index " + parser.index);
        }
        return value;
    }

    static String write(Object value) {
        StringBuilder output = new StringBuilder();
        append(output, value);
        return output.toString();
    }

    private static void append(StringBuilder output, Object value) {
        if (value == null) {
            output.append("null");
        } else if (value instanceof String) {
            output.append('"');
            String text = (String) value;
            for (int i = 0; i < text.length(); i++) {
                char character = text.charAt(i);
                switch (character) {
                    case '"': output.append("\\\""); break;
                    case '\\': output.append("\\\\"); break;
                    case '\b': output.append("\\b"); break;
                    case '\f': output.append("\\f"); break;
                    case '\n': output.append("\\n"); break;
                    case '\r': output.append("\\r"); break;
                    case '\t': output.append("\\t"); break;
                    default:
                        if (character < 0x20) {
                            output.append(String.format("\\u%04x", Integer.valueOf(character)));
                        } else {
                            output.append(character);
                        }
                }
            }
            output.append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            output.append(value.toString());
        } else if (value instanceof Map) {
            output.append('{');
            boolean first = true;
            for (Object rawEntry : ((Map<?, ?>) value).entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) rawEntry;
                if (!first) output.append(',');
                append(output, String.valueOf(entry.getKey()));
                output.append(':');
                append(output, entry.getValue());
                first = false;
            }
            output.append('}');
        } else if (value instanceof List) {
            output.append('[');
            boolean first = true;
            for (Object item : (List<?>) value) {
                if (!first) output.append(',');
                append(output, item);
                first = false;
            }
            output.append(']');
        } else {
            throw new IllegalArgumentException("Unsupported JSON value type: " + value.getClass().getName());
        }
    }

    private static final class Parser {
        private final String source;
        private int index;

        Parser(String source) {
            if (source == null) throw new IllegalArgumentException("JSON source is required");
            this.source = source;
        }

        Object value() {
            whitespace();
            if (atEnd()) throw new IllegalArgumentException("Expected JSON value");
            char character = source.charAt(index);
            if (character == '{') return object();
            if (character == '[') return array();
            if (character == '"') return string();
            if (character == 't') return literal("true", Boolean.TRUE);
            if (character == 'f') return literal("false", Boolean.FALSE);
            if (character == 'n') return literal("null", null);
            if (character == '-' || Character.isDigit(character)) return number();
            throw new IllegalArgumentException("Unexpected JSON token at index " + index);
        }

        Map<String, Object> object() {
            Map<String, Object> object = new LinkedHashMap<String, Object>();
            expect('{');
            whitespace();
            if (consume('}')) return object;
            while (true) {
                whitespace();
                if (atEnd() || source.charAt(index) != '"') throw new IllegalArgumentException("Expected object key at index " + index);
                String key = string();
                whitespace();
                expect(':');
                if (object.containsKey(key)) throw new IllegalArgumentException("Duplicate JSON key: " + key);
                object.put(key, value());
                whitespace();
                if (consume('}')) return object;
                expect(',');
            }
        }

        List<Object> array() {
            List<Object> array = new ArrayList<Object>();
            expect('[');
            whitespace();
            if (consume(']')) return array;
            while (true) {
                array.add(value());
                whitespace();
                if (consume(']')) return array;
                expect(',');
            }
        }

        String string() {
            StringBuilder value = new StringBuilder();
            expect('"');
            while (!atEnd()) {
                char character = source.charAt(index++);
                if (character == '"') return value.toString();
                if (character == '\\') {
                    if (atEnd()) throw new IllegalArgumentException("Unterminated JSON escape");
                    char escaped = source.charAt(index++);
                    switch (escaped) {
                        case '"': value.append('"'); break;
                        case '\\': value.append('\\'); break;
                        case '/': value.append('/'); break;
                        case 'b': value.append('\b'); break;
                        case 'f': value.append('\f'); break;
                        case 'n': value.append('\n'); break;
                        case 'r': value.append('\r'); break;
                        case 't': value.append('\t'); break;
                        case 'u': value.append(unicode()); break;
                        default: throw new IllegalArgumentException("Invalid JSON escape at index " + index);
                    }
                } else if (character < 0x20) {
                    throw new IllegalArgumentException("Control character in JSON string");
                } else {
                    value.append(character);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }

        private char unicode() {
            if (index + 4 > source.length()) throw new IllegalArgumentException("Incomplete unicode escape");
            String hex = source.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid unicode escape: " + hex);
            }
        }

        Number number() {
            int start = index;
            consume('-');
            digits();
            boolean decimal = false;
            if (consume('.')) {
                decimal = true;
                digits();
            }
            if (consume('e') || consume('E')) {
                decimal = true;
                consume('+');
                consume('-');
                digits();
            }
            String text = source.substring(start, index);
            try {
                if (decimal) return Double.valueOf(text);
                return Long.valueOf(text);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid JSON number: " + text);
            }
        }

        private void digits() {
            int start = index;
            while (!atEnd() && Character.isDigit(source.charAt(index))) index++;
            if (start == index) throw new IllegalArgumentException("Expected digits at index " + index);
        }

        private Object literal(String text, Object value) {
            if (!source.regionMatches(index, text, 0, text.length())) throw new IllegalArgumentException("Expected " + text + " at index " + index);
            index += text.length();
            return value;
        }

        void whitespace() {
            while (!atEnd() && Character.isWhitespace(source.charAt(index))) index++;
        }

        boolean consume(char expected) {
            if (!atEnd() && source.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        void expect(char expected) {
            if (!consume(expected)) throw new IllegalArgumentException("Expected '" + expected + "' at index " + index);
        }

        boolean atEnd() {
            return index >= source.length();
        }
    }
}
