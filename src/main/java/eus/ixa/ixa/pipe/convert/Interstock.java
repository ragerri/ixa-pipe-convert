package eus.ixa.ixa.pipe.convert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import opennlp.tools.cmdline.CmdLineUtil;

public class Interstock {

  private Interstock() {
  }

  public static void getJSONBodyElem(String fileName) throws IOException {
    JSONParser parser = new JSONParser();
    Path filePath = Paths.get(fileName);
    BufferedReader breader = new BufferedReader(
        Files.newBufferedReader(filePath, StandardCharsets.UTF_8));
    String line;
    while ((line = breader.readLine()) != null) {
      try {
        Object obj = parser.parse(line);
        JSONObject jsonObject = (JSONObject) obj;
        String text = (String) jsonObject.get("body");
        System.out.println(text);
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    breader.close();
  }
  public static void getJSONElem(String inputFile) throws IOException {
    JSONParser parser = new JSONParser();
    Path filePath = Paths.get(inputFile);
    InputStream inputStream = CmdLineUtil.openInFile(filePath.toFile());
    BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    try {
      Object obj = parser.parse(breader);
      JSONObject jsonObject = (JSONObject) obj;
      String body = (String) jsonObject.get("body");
      System.out.println(body);
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }
}
