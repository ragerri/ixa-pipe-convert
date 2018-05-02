package eus.ixa.ixa.pipe.convert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.jdom2.Content;
import org.jdom2.Content.CType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import eus.ixa.ixa.pipe.ml.tok.Token;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.KAFDocument.FileDesc;
import opennlp.tools.cmdline.CmdLineUtil;
import ixa.kaflib.WF;

public class DiannFormat {

  private static final Pattern disPattern = Pattern.compile("<dis>(\\.*?)<\\/dis>");
  
  private DiannFormat() {
  }
  
  private static void diannToNAFNER(KAFDocument kaf, Path fileName, String language) throws IOException {
    //reading one Diann file
    if (Files.isRegularFile(fileName)) {
      List<String> inputLines = com.google.common.io.Files.readLines(fileName.toFile(), Charset.forName("UTF-8"));
      for (String line : inputLines) {
        StringBuilder sb = new StringBuilder();
        String[] lines = line.split(" ");
        for (int i = 0; i < lines.length; i++) {
          
          if (lines[i].matches("<dis>")) {
            
            
            
          }
        }
      }
    } else {
      System.out.println("Please choose a valid file as input.");
      System.exit(1);
    }
  }
  
  public static String diannToCoNLL2002(Path fileName, String language) throws IOException {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
    diannToNAFNER(kaf, fileName, language);
    String conllFile = ConllUtils.nafToCoNLLConvert2002(kaf);
    return conllFile;
  }
  
  public static String timeMLToRawNAF(String fileName, String language) {
    KAFDocument kaf = new KAFDocument("en", "v1.naf");
    SAXBuilder sax = new SAXBuilder();
    try {
      Document doc = sax.build(fileName);
      Element rootElement = doc.getRootElement();
      //getting the Document Creation Time
      Element dctElement = rootElement.getChild("DCT");
      Element dctTimex = dctElement.getChild("TIMEX3");
      String dctTimexValue = dctTimex.getAttributeValue("value");
      kaf.createFileDesc().creationtime = dctTimexValue;
      //getting the TEXT
      Element textElement = rootElement.getChild("TEXT");
      String words = textElement.getValue(); 
      kaf.setRawText(words);
    } catch (JDOMException | IOException e) {
      e.printStackTrace();
    }
    return kaf.toString();
  }
}
