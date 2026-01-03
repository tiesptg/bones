package com.palisand.bones;

public class Names {
  public static String toCamelCase(String source, boolean firstUpperCase) {
    StringBuilder sb = new StringBuilder(source);
    for (int i = 0; i < sb.length(); ++i) {
      if (!Character.isLetterOrDigit(sb.charAt(i))) {
        sb.deleteCharAt(i);
        sb.setCharAt(i, Character.toUpperCase(sb.charAt(i)));
        --i;
      }
    }
    if (sb.length() != 0) {
      if (firstUpperCase) {
        if (!Character.isUpperCase(sb.charAt(0))) {
          sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        }
      } else if (!Character.isLowerCase(sb.charAt(0))) {
        sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
      }
    }
    return sb.toString();
  }

  public static String toSnakeCase(String source) {
    return toSeparatedCase(source, false, '_');
  }

  public static String toKebabCase(String source) {
    return toSeparatedCase(source, true, '-');
  }

  public static String toSpacedCase(String source) {
    return capitalise(toSeparatedCase(source, false, ' '));
  }

  public static String toSeparatedCase(String source, boolean upperCase, char separator) {
    if (source == null) {
      return null;
    }
    boolean doInsert = source.matches(".*[a-z].*") && source.matches(".*[A-Z].*");
    StringBuilder sb = new StringBuilder(source);
    for (int i = 0; i < sb.length(); ++i) {
      if (doInsert && i > 0 && sb.charAt(i - 1) != separator
          && Character.isUpperCase(sb.charAt(i))) {
        sb.insert(i++, separator);
      }
      if (sb.charAt(i) != separator && !Character.isLetterOrDigit(sb.charAt(i))) {
        if (i > 0 && sb.charAt(i - 1) == separator) {
          sb.deleteCharAt(i--);
        } else {
          sb.setCharAt(i, separator);
        }
      }
    }
    if (upperCase) {
      return sb.toString().toUpperCase();
    }
    return sb.toString().toLowerCase();
  }

  public static String capitalise(String word) {
    if (word != null) {
      return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }
    return null;
  }

}
