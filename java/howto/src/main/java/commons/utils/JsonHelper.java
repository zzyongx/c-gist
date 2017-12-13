package commons.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JsonHelper {
  static ObjectMapper mapper = new ObjectMapper();
  static SimpleModule module = new SimpleModule("java.time.*");
  static {
    module.addSerializer(LocalDate.class, new LocalDateJsonSerializer());
    module.addDeserializer(LocalDate.class, new LocalDateJsonDeserializer());
    module.addSerializer(LocalDateTime.class, new LocalDateTimeJsonSerializer());
    module.addDeserializer(LocalDateTime.class, new LocalDateTimeJsonDeserializer());

    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setSerializationInclusion(Include.NON_NULL);
    mapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
    mapper.registerModule(module);
  }

  public static ObjectMapper factory() {
    return mapper;
  }

  public static <T> T readValue(String src, Class<T> valueType) {
    try {
      return factory().readValue(src, valueType);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }

  public static <T> T readValue(String src, TypeReference<T> valueTypeRef) {
    try {
      return factory().readValue(src, valueTypeRef);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }

  public static <T> List<T> readListValue(String src, Class<T> valueType) {
    try {
      return factory().readValue(src, factory().getTypeFactory().constructCollectionType(List.class, valueType));
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }

  public static <T> T readValue(JsonNode root, Class<T> valueType) {
    try {
      return factory().treeToValue(root, valueType);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }

  public static <T> T readPath(String src, Class<T> rtype, Object ... paths) {
    try {
      return readPath(factory().readTree(src), rtype, paths);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }

  public static void readPath(String src, List<Object> paths, Consumer<JsonNode> fun) {
    try {
      readPath(factory().readTree(src), paths, fun);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }

  public static void readPath(String src, List<Object> paths, BiConsumer<String, JsonNode> fun) {
    try {
      readPath(factory().readTree(src), paths, fun);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }

  public static <T> T readPath(JsonNode root, Class<T> rtype, Object ... paths) {
    return readPath(root, rtype, Arrays.asList(paths));
  }

  public static <T> T readPath(JsonNode root, Class<T> rtype, List<Object> paths) {
    for (Object path : paths) {
      if (path instanceof String) {
        root = ((ObjectNode) root).get((String) path);
      } else if (path instanceof Integer) {
        root = ((ArrayNode) root).get((Integer) path);
      } else {
        throw new IllegalArgumentException("unsupported paths type " + path.getClass());
      }
      if (root == null) return null;
    }
    return cast(root, rtype);
  }

  public static void readPath(JsonNode root, List<Object> paths, Consumer<JsonNode> fun) {
    JsonNode jsonNode = readPath(root, JsonNode.class, paths);
    if (jsonNode instanceof ArrayNode) {
      ArrayNode array = (ArrayNode) jsonNode;
      for (int i = 0; i < array.size(); ++i) {
        fun.accept(array.get(i));
      }
    } else if (jsonNode instanceof ObjectNode) {
      for (JsonNode node : (ObjectNode) jsonNode) {
        fun.accept(node);
      }
    } else {
      fun.accept(jsonNode);
    }
  }

  public static void readPath(JsonNode root, List<Object> paths, BiConsumer<String, JsonNode> fun) {
    ObjectNode object = readPath(root, ObjectNode.class, paths);
    Iterator<Map.Entry<String, JsonNode>> ite = object.fields();
    while (ite.hasNext()) {
      Map.Entry<String, JsonNode> field = ite.next();
      fun.accept(field.getKey(), field.getValue());
    }
  }

  public static <T> T cast(JsonNode root, Class<T> rtype) {
    if (rtype == Boolean.class) {
      return rtype.cast(root.asBoolean());
    } else if (rtype == Double.class) {
      return rtype.cast(root.asDouble());
    } else if (rtype == Integer.class) {
      return rtype.cast(root.asInt());
    } else if (rtype == Long.class) {
      return rtype.cast(root.asLong());
    } else if (rtype == String.class) {
      return rtype.cast(root.asText());
    } else if (JsonNode.class.isAssignableFrom(rtype)) {
      return rtype.cast(root);
    } else {
      throw new IllegalArgumentException("unsupported return type " + rtype);
    }
  }

  public static byte[] writeValueAsBytes(Object value) {
    try {
      return factory().writeValueAsBytes(value);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }

  public static String writeValueAsString(Object value) {
    try {
      return factory().writeValueAsString(value);
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }
}
