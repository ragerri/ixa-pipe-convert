package eus.ixa.ixa.pipe.convert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import opennlp.tools.cmdline.CmdLineUtil;

public class Interstock {

  public static Pattern doubleSpaces = Pattern.compile("[\\  ]+");
  public static Pattern tabCharacter = Pattern.compile("\t");
  public static Pattern newLineCharacter = Pattern.compile("\n");
  
  public static Pattern opfplus = Pattern.compile("opf\\+");
  public static Pattern opfneg = Pattern.compile("opf-");
  public static Pattern opfneutral = Pattern.compile("opf");
  public static Pattern neutro = Pattern.compile("neutro");
  public static Pattern opneg = Pattern.compile("op-");
  public static Pattern opplus = Pattern.compile("op+");
  
  public static Pattern finance = Pattern.compile("opf.*");
  public static Pattern nonFinance = Pattern.compile("neutro|op-|op\\+");
  public static Pattern opfSubjective = Pattern.compile("opf\\+|opf-");
  
  public static String FINANCE = "finance";
  public static String NON_FINANCE = "nofinance";

  private Interstock() {
  }

  public static void getJsonFinanceBinaryDataset(String fileName) throws IOException {
    JSONParser parser = new JSONParser();
    Path filePath = Paths.get(fileName);
    InputStream inputStream = CmdLineUtil.openInFile(filePath.toFile());
    BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      try {
        Object obj = parser.parse(line);
        JSONObject jsonObject = (JSONObject) obj;
        //get opinions array
        JSONArray opinionsList = (JSONArray) jsonObject.get("opinions");
        String category = null;
        //get body and polarity for first opinion only
        for (int i = 0; i < opinionsList.size(); i++) {
         JSONObject opinion = (JSONObject) opinionsList.get(0);
         String validation = (String) opinion.get("validation");
         category = normalizeValidationToFinanceBinary(validation);
        }
        //get body text
        String body = (String) jsonObject.get("body");
        body = cleanExtraSpacesInBody(body);
        System.out.println(category + "\t" + body);
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    breader.close();
  }
  
  public static void getJsonFinanceOpinionsBinary(String fileName) throws IOException {
    JSONParser parser = new JSONParser();
    Path filePath = Paths.get(fileName);
    InputStream inputStream = CmdLineUtil.openInFile(filePath.toFile());
    BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      try {
        Object obj = parser.parse(line);
        JSONObject jsonObject = (JSONObject) obj;
        //get opinions array
        JSONArray opinionsList = (JSONArray) jsonObject.get("opinions");
        String category = null;
        //get body and polarity for first opinion only
        for (int i = 0; i < opinionsList.size(); i++) {
         JSONObject opinion = (JSONObject) opinionsList.get(0);
         String validation = (String) opinion.get("validation");
         category = normalizeFinanceToSubjectiveObjective(validation);
        }
        if (category.matches("subjective|objective")) {
          //get body text
          String body = (String) jsonObject.get("body");
          body = cleanExtraSpacesInBody(body);
          System.out.println(category + "\t" + body);
        }
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    breader.close();
  }
  
  public static void getJsonFinanceOpinionsPolarity(String fileName) throws IOException {
    JSONParser parser = new JSONParser();
    Path filePath = Paths.get(fileName);
    InputStream inputStream = CmdLineUtil.openInFile(filePath.toFile());
    BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    String line;
    while ((line = breader.readLine()) != null) {
      try {
        Object obj = parser.parse(line);
        JSONObject jsonObject = (JSONObject) obj;
        //get opinions array
        JSONArray opinionsList = (JSONArray) jsonObject.get("opinions");
        String category = null;
        //get body and polarity for first opinion only
        for (int i = 0; i < opinionsList.size(); i++) {
         JSONObject opinion = (JSONObject) opinionsList.get(0);
         category = (String) opinion.get("validation");
        }
        if (category.matches("opf\\+|opf-")) {
          //get body text
          String body = (String) jsonObject.get("body");
          body = cleanExtraSpacesInBody(body);
          System.out.println(category + "\t" + body);
        }
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    breader.close();
  }
  
  public static void getJsonAllOpinionsBinary(String fileName) throws IOException {
    JSONParser parser = new JSONParser();
    Path filePath = Paths.get(fileName);
    InputStream inputStream = CmdLineUtil.openInFile(filePath.toFile());
    BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    String line;
    int counter = 0;
    while ((line = breader.readLine()) != null) {
      try {
        Object obj = parser.parse(line);
        JSONObject jsonObject = (JSONObject) obj;
        //get opinions array
        JSONArray opinionsList = (JSONArray) jsonObject.get("opinions");
        for (int i = 0; i < opinionsList.size(); i++) {
         JSONObject opinion = (JSONObject) opinionsList.get(i);
         counter++;
         String body = (String) jsonObject.get("body");
         body = cleanExtraSpacesInBody(body);
         String text = (String) opinion.get("text");
         String validation = (String) opinion.get("validation");
         validation = Interstock.normalizeValidationToFinanceBinary(validation);
         System.out.println(validation + "\t" + body + "\t" + text);
        }
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    System.out.println(counter);
    breader.close();
  }
  
  
  
  public static void getJsonFinanceAllOpinionsBinary(String fileName) throws IOException {
    JSONParser parser = new JSONParser();
    Path filePath = Paths.get(fileName);
    InputStream inputStream = CmdLineUtil.openInFile(filePath.toFile());
    BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    String line;
    int counter = 0;
    while ((line = breader.readLine()) != null) {
      try {
        Object obj = parser.parse(line);
        JSONObject jsonObject = (JSONObject) obj;
        //get opinions array
        JSONArray opinionsList = (JSONArray) jsonObject.get("opinions");
        for (int i = 0; i < opinionsList.size(); i++) {
         JSONObject opinion = (JSONObject) opinionsList.get(i);
         counter++;
         String validation = (String) opinion.get("validation");
         validation = Interstock.normalizeFinanceToSubjectiveObjective(validation);
         if (validation.matches("objective|subjective")) {
           String body = (String) jsonObject.get("body");
           body = cleanExtraSpacesInBody(body);
           String text = (String) opinion.get("text");
           System.out.println(validation + "\t" + body + "\t" + text);
         }
        }
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    System.out.println(counter);
    breader.close();
  }
  
  public static void getJsonFinanceAllOpinionsPolarity(String fileName) throws IOException {
    JSONParser parser = new JSONParser();
    Path filePath = Paths.get(fileName);
    InputStream inputStream = CmdLineUtil.openInFile(filePath.toFile());
    BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    String line;
    int counter = 0;
    while ((line = breader.readLine()) != null) {
      try {
        Object obj = parser.parse(line);
        JSONObject jsonObject = (JSONObject) obj;
        //get opinions array
        JSONArray opinionsList = (JSONArray) jsonObject.get("opinions");
        for (int i = 0; i < opinionsList.size(); i++) {
         JSONObject opinion = (JSONObject) opinionsList.get(i);
         counter++;
         String validation = (String) opinion.get("validation");
         if (validation.matches("opf\\+|opf-")) {
           String body = (String) jsonObject.get("body");
           body = cleanExtraSpacesInBody(body);
           String text = (String) opinion.get("text");
           System.out.println(validation + "\t" + body + "\t" + text);
         }
        }
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    System.out.println(counter);
    breader.close();
  }
  
  public static void getJsonMultipleOpinions(String fileName) throws IOException {
    JSONParser parser = new JSONParser();
    Path filePath = Paths.get(fileName);
    InputStream inputStream = CmdLineUtil.openInFile(filePath.toFile());
    BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
    String line;
    int counter = 0;
    while ((line = breader.readLine()) != null) {
      try {
        Object obj = parser.parse(line);
        JSONObject jsonObject = (JSONObject) obj;
        //get opinions array
        JSONArray opinionsList = (JSONArray) jsonObject.get("opinions");
        for (int i = 0; i < opinionsList.size(); i++) {
         JSONObject opinion = (JSONObject) opinionsList.get(i);
         if (opinionsList.size() > 1) {
           counter++;
           String body = (String) jsonObject.get("body");
           body = cleanExtraSpacesInBody(body);
           String text = (String) opinion.get("text");
           String validation = (String) opinion.get("validation");
           System.out.println(validation + "\t" + body + "\t" + text);
         }
        }
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }
    System.out.println(counter);
    breader.close();
  }
  
  private static String normalizeValidationToFinanceBinary(String validation) {
    String category = finance.matcher(validation).replaceAll(FINANCE);
    category = nonFinance.matcher(category).replaceAll(NON_FINANCE);
    return category;
  }
  
  private static String normalizeFinanceToSubjectiveObjective(String validation) {
    String category =  opfSubjective.matcher(validation).replaceAll("subjective");
    category = opfneutral.matcher(category).replaceAll("objective");
    return category;
  }
  
  private static String cleanExtraSpacesInBody(String body) {
    String text = tabCharacter.matcher(body).replaceAll(" ");
    text = newLineCharacter.matcher(text).replaceAll(" ");
    return text;
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
