package com.palisand.bones.tt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import com.palisand.bones.tt.Repository.Parser;
import lombok.Setter;

public class StringConverter implements Converter<String> {
  @Setter private Repository repository = null;

  @Override
  public String fromTypedText(Parser parser, BufferedReader in, Class<?> cls, Class<?> context,
      String margin) throws IOException {
    String result = parser.readUntilLineEnd(in, true);
    if (result.length() != 0 && result.charAt(result.length() - 1) == '\\') {
      StringBuilder sb = new StringBuilder(result);
      String strMargin = margin + Repository.MARGIN_STEP;
      do {
        sb.replace(sb.length() - 1, sb.length(), "\n");
        result = parser.readUntilLineEnd(in, false);
        sb.append(result.substring(strMargin.length()));
      } while (result.charAt(result.length() - 1) == '\\');
      result = sb.toString();
    }
    return result;
  }

  @Override
  public void toTypedText(Repository repository, String str, PrintWriter out, Class<?> context,
      String margin) throws IOException {
    out.println(str.replace("\n", "\\\n" + margin + Repository.MARGIN_STEP));
  }

  public Class<?> getType() {
    return String.class;
  }

}
