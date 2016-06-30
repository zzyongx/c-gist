package commons.utils;

import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

public class XssHelper {
  public static String escape(String unsafe) {
    if (unsafe == null) return null;

    boolean f = false;
    char safe[] = new char[unsafe.length()];
    for (int i = 0; i < safe.length; ++i) {
      char c = unsafe.charAt(i);
      switch (c) {
      case '<':  c = '＜'; f = true; break;
      case '>':  c = '＞'; f = true; break;
      case '"':  c = '＂'; f = true; break;
      case '\'': c = '＇'; f = true; break;
      case '(' : c = '（'; f = true; break;
      case ')':  c = '）'; f = true; break;
      }
      safe[i] = c;
    }

    return f ? new String(safe) : unsafe;
  }

  static JsonNode makeJsonSafe(ArrayNode array) {
    boolean f = false;
    for (int i = 0, len = array.size(); i < len; ++i) {
      JsonNode node = array.get(i);
      if (node.isTextual()) {
        String unsafe = node.asText();
        String safe = escape(unsafe);
        if (unsafe != safe) {
          f = true;
          array.set(i, new TextNode(safe));
        }
      } else if (node.isArray() || node.isObject()) {
        JsonNode newNode;
        if (node.isArray()) newNode =  makeJsonSafe((ArrayNode) node);
        else newNode = makeJsonSafe((ObjectNode) node);
        
        if (newNode != null) {
          f = true;
          array.set(i, newNode);
        }
      }
    }
    return f ? array : null;
  }

  static JsonNode makeJsonSafe(ObjectNode object) {
    List<String> fields = new ArrayList<>();
    Iterator<String> iterator = object.fieldNames();
    while (iterator.hasNext()) fields.add(iterator.next());

    boolean f = false;
    for (String field : fields) {
      JsonNode node = object.get(field);
      if (node.isTextual()) {
        String unsafe = node.asText();
        String safe = escape(unsafe);
        if (unsafe != safe) {
          f = true;
          object.set(field, new TextNode(safe));
        }
      } else if (node.isArray() || node.isObject()) {
        JsonNode newNode;
        if (node.isArray()) newNode = makeJsonSafe((ArrayNode) node);
        else newNode = makeJsonSafe((ObjectNode) node);
        
        if (newNode != null) {
          f = true;
          object.set(field, newNode);
        }
      }
    }
    return f ? object : null;
  }

  public static String makeJsonSafe(String json) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      JsonNode root = mapper.readTree(json);
      if (root.isArray() || root.isObject()) {
        if (root.isArray()) root = makeJsonSafe((ArrayNode) root);
        else root = makeJsonSafe((ObjectNode) root);
        if (root != null) {
          json = JsonHelper.writeValueAsString(root);
        }
      }
      return json;
    } catch (Exception e) {
      return escape(json);
    }
  }
}
