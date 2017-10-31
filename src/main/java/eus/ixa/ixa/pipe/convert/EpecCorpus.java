package eus.ixa.ixa.pipe.convert;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EpecCorpus {

  private EpecCorpus() {
  }

  private static Set<String> categories = new HashSet<>(Arrays.asList("ADB",
      "ADI", "ADJ", "ADL", "ADT", "AMM", "ASP", "ATZ", "AUR", "BST", "DEK",
      "DET", "ELI", "ERL", "GRA", "HAOS", "IOR", "ITJ", "IZE", "LOT", "MAR",
      "PRT", "PUNT_KOMA", "PUNT_PUNT", "PUNT_BI_PUNT", "PUNT_GALD",
      "PUNT_PUNT_KOMA", "PUNT_HIRU", "PUNT_ESKL", "BEREIZ", "0"));

  private static Set<String> subcategories = new HashSet<>(
      Arrays.asList("ADK", "ADP", "ARR", "BAN", "BIH", "DZG", "DZH", "ELK",
          "ERKARR", "ERKIND", "FAK", "GAL", "IZEELI", "IZB", "IZGGAL", "IZGMGB",
          "JNT", "LIB", "LOK", "MEN", "NOLARR", "NOLGAL", "ORD", "ORO",
          "PERARR", "PERIND", "SIN", "ZKI", "0"));

  private static Set<String> cases = new HashSet<>(Arrays.asList("ABL", "ABU",
      "ABZ", "ALA", "SOZ", "DAT", "DES", "ERG", "GEL", "GEN", "INE", "INS",
      "MOT", "ABS", "PAR", "PRO", "BNK", "DESK", "0"));

  public static String formatCorpus(Path corpus, String option) throws IOException {
    String conllCorpus = null;
    // process one file
    if (Files.isRegularFile(corpus)) {
      List<String> inputLines = com.google.common.io.Files.readLines(corpus.toFile(), Charset.forName("UTF-8"));
      if (option.equalsIgnoreCase("threeLevel")) {
        conllCorpus = getThreeFields(inputLines);
      } else if (option.equalsIgnoreCase("twoLevel")) {
        conllCorpus = getTwoFields(inputLines);
      } else if (option.equalsIgnoreCase("oneLevel")) {
        conllCorpus = getOneField(inputLines);
      }
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
        String cleanLemma = cleanLemmas(word, lemma);
        String cleanWord = cleanWords(word,lemma);
        String category = "";
        String subcategory = "";
        String kasua = "";
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
        if (cleanWord.equalsIgnoreCase("zidorratrinkete")) {
          sb.append("\n");
        } else if (subcategory.equalsIgnoreCase("")) {
          sb.append(cleanWord).append("\t").append(cleanLemma).append("\t").append(category)
          .append("\n");
        } else if (kasua.equalsIgnoreCase("")){
          sb.append(cleanWord).append("\t").append(cleanLemma).append("\t").append(category).
          append("_").append(subcategory).append("\n");
        } else {
          sb.append(cleanWord).append("\t").append(cleanLemma).append("\t")
              .append(category).append("_").append(subcategory).append("_")
              .append(kasua).append("\n");
        }
      } else {
        System.err.println("-> Line: " + line);
      }
    }
    String corpus = sb.toString();
    corpus = corpus.replaceAll("\n\n\n", "\n\n");
    corpus = corpus.trim();
    return corpus;
  }
  
  private static String getTwoFields(List<String> inputLines) {
    StringBuilder sb = new StringBuilder();
    for (String line : inputLines) {
      String[] fields = line.split(" ");
      if (fields.length > 2) {
        String word = fields[0];
        String lemma = fields[1];
        String cleanLemma = cleanLemmas(word, lemma);
        String cleanWord = cleanWords(word,lemma);
        String category = "";
        String subcategory = "";
        for (String field : fields) {
          if (categories.contains(field)) {
            category = field;
          }
          if (subcategories.contains(field)) {
            subcategory = field;
          }
        }
        if (cleanWord.equalsIgnoreCase("zidorratrinkete")) {
          sb.append("\n");
        } else if (subcategory.equalsIgnoreCase("")) {
          sb.append(cleanWord).append("\t").append(cleanLemma).append("\t").append(category)
          .append("\n");
        } else {
          sb.append(cleanWord).append("\t").append(cleanLemma).append("\t")
              .append(category).append("_").append(subcategory).append("\n");
        }
      } else {
        System.err.println("-> Line: " + line);
      }
    }
    String corpus = sb.toString();
    corpus = corpus.replaceAll("\n\n\n", "\n\n");
    corpus = corpus.trim();
    return corpus;
  }
  
  private static String getOneField(List<String> inputLines) {
    StringBuilder sb = new StringBuilder();
    for (String line : inputLines) {
      String[] fields = line.split(" ");
      if (fields.length > 2) {
        String word = fields[0];
        String lemma = fields[1];
        String cleanLemma = cleanLemmas(word, lemma);
        String cleanWord = cleanWords(word,lemma);
        String category = "";
        for (String field : fields) {
          if (categories.contains(field)) {
            category = field;
          }
        }
        if (cleanWord.equalsIgnoreCase("zidorratrinkete")) {
          sb.append("\n");
        } else {
          sb.append(cleanWord).append("\t").append(cleanLemma).append("\t")
              .append(category).append("\n");
        }
      } else {
        System.err.println("-> Line: " + line);
      }
    }
    String corpus = sb.toString();
    corpus = corpus.replaceAll("\n\n\n", "\n\n");
    corpus = corpus.trim();
    return corpus;
  }

  private static String cleanLemmas(String word, String lemma) {
    if (lemma.startsWith("/")) {
      lemma = lemma.substring(1, lemma.length());
    }
    if (lemma.endsWith("/")) {
      lemma = lemma.substring(0, lemma.length() - 1);
    }
    if (lemma.equalsIgnoreCase("IDENT")) {
      lemma = word;
    }
    if (word.equalsIgnoreCase("/")) {
      lemma = word;
    }
    return lemma;
  }
  
  private static String cleanWords(String word, String lemma) {
    if (word.equalsIgnoreCase("@")) {
      word = lemma;
    }
    return word;
  }

}