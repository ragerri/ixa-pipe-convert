package eus.ixa.ixa.pipe.convert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EpecCorpus {

  private EpecCorpus() {
  }

  private static Set<String> categories = new HashSet<>(
      Arrays.asList("ADB", "ADI", "ADJ", "ADL", "ADT", "AMM", "ASP", "ATZ",
          "AUR", "BST", "DEK", "DET", "ELI", "ERL", "GRA", "HAOS", "IOR", "ITJ",
          "IZE", "LOT", "MAR", "PRT", "PUNT_KOMA", "PUNT_PUNT", "PUNT_BI_PUNT",
          "PUNT_GALD", "PUNT_PUNT_KOMA", "PUNT_HIRU", "PUNT_ESKL", "0"));

  private static Set<String> subcategories = new HashSet<>(
      Arrays.asList("ADK", "ADP", "ARR", "BAN", "BIH", "DZG", "DZH", "ELK",
          "ERKARR", "ERKIND", "FAK", "GAL", "IZEELI", "IZB", "IZGGAL", "IZGMGB",
          "JNT", "LIB", "LOK", "MEN", "NOLARR", "NOLGAL", "ORD", "ORO",
          "PERARR", "PERIND", "SIN", "ZKI", "0"));

  private static Set<String> cases = new HashSet<>(Arrays.asList("ABL", "ABU",
      "ABZ", "ALA", "SOZ", "DAT", "DES", "ERG", "GEL", "GEN", "INE", "INS",
      "MOT", "ABS", "PAR", "PRO", "BNK", "DESK", "0"));

  public static String getCatSubCatCase(Path corpus) throws IOException {
    String conllCorpus = null;
    // process one file
    if (Files.isRegularFile(corpus)) {
      List<String> inputLines = Files.readAllLines(corpus);
      conllCorpus = getThreeFields(inputLines);
    } else {
      System.out.println("Please choose a valid file as input.");
      System.exit(1);
    }
    return conllCorpus;
  }

  private static String getThreeFields(List<String> inputLines) {
    StringBuilder sb = new StringBuilder();
    for (String line : inputLines) {
      String[] fields = line.split(" ");
      if (fields.length > 2) {
        String word = fields[0];
        String lemma = fields[1];
        String category = "0";
        String subcategory = "0";
        String kasua = "0";
        for (String field : fields) {
          if (categories.contains(field)) {
            category = field;
          }
          if (subcategories.contains(field)) {
            subcategory = field;
          }
          if (cases.contains(field)) {
            kasua = field;
          }
        }
        sb.append(word).append("\t").append(lemma).append("\t").append(category)
            .append("\t").append(subcategory).append("\t").append(kasua)
            .append("\n");
      } else {
        System.err.println("-> Line: " + line);
      }
    }
    return sb.toString();
  }

}