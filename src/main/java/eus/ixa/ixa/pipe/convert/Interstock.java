package eus.ixa.ixa.pipe.convert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import opennlp.tools.cmdline.CmdLineUtil;

public class Interstock {
  
  private Interstock() {
  }

  public static void getJSONTextElem(String inputFile) {
    JSONParser parser = new JSONParser();
    Path filePath = Paths.get(inputFile);
    InputStream inputStream = CmdLineUtil.openInFile(filePath.toFile());
    BufferedReader breader = new BufferedReader(
        new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    String line;
    try {
      while ((line = breader.readLine()) != null) {
        try {
          Object obj = parser.parse(line);
          JSONObject jsonObject = (JSONObject) obj;
          String text = (String) jsonObject.get("text");
          System.out.println(text);
        } catch (ParseException e) {
          e.printStackTrace();
        }
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      breader.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
    
  }
