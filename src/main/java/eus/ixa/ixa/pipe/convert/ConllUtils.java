package eus.ixa.ixa.pipe.convert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ixa.kaflib.Entity;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;
import ixa.kaflib.WF;

public class ConllUtils {

  public ConllUtils() {
  }

  public static void nafToCoNLL2002(Path dir) throws IOException {
    // process one file
    if (Files.isRegularFile(dir) && !dir.endsWith(".conll02")) {
      KAFDocument kaf = KAFDocument.createFromFile(dir.toFile());
      Path outfile = Files.createFile(Paths.get(dir.toString() + ".conll02"));
      String outKAF = nafToCoNLLConvert2002(kaf);
      Files.write(outfile, outKAF.getBytes(StandardCharsets.UTF_8));
      System.err.println(">> Wrote CoNLL document to " + outfile);
    } else {
      // recursively process directories
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            nafToCoNLL2002(file);
          } else {
            if (!file.endsWith(".conll02")) {
              Path outfile = Files
                  .createFile(Paths.get(file.toString() + ".conll02"));
              KAFDocument kaf = KAFDocument.createFromFile(file.toFile());
              String outKAF = nafToCoNLLConvert2002(kaf);
              Files.write(outfile, outKAF.getBytes(StandardCharsets.UTF_8));
              System.err.println(">> Wrote CoNLL02 document to " + outfile);
            }
          }
        }
      }
    }
  }

  /**
   * Output Conll2002 format.
   * 
   * @param kaf
   *          the kaf document
   * @return the annotated named entities in conll02 format
   */
  public static String nafToCoNLLConvert2002(KAFDocument kaf) {
    List<Entity> namedEntityList = kaf.getEntities();
    Map<String, Integer> entityToSpanSize = new HashMap<>();
    Map<String, String> entityToType = new HashMap<>();
    for (Entity ne : namedEntityList) {
      List<ixa.kaflib.Span<Term>> entitySpanList = ne.getSpans();
      for (ixa.kaflib.Span<Term> spanTerm : entitySpanList) {
        Term neTerm = spanTerm.getFirstTarget();
        entityToSpanSize.put(neTerm.getId(), spanTerm.size());
        entityToType.put(neTerm.getId(), ne.getType());
      }
    }

    List<List<WF>> sentences = kaf.getSentences();
    StringBuilder sb = new StringBuilder();
    for (List<WF> sentence : sentences) {
      int sentNumber = sentence.get(0).getSent();
      List<Term> sentenceTerms = kaf.getSentenceTerms(sentNumber);

      for (int i = 0; i < sentenceTerms.size(); i++) {
        Term thisTerm = sentenceTerms.get(i);
        // System.err.println("--> thisterm: " + thisTerm.getForm() + " " +
        // thisTerm.getId());

        if (entityToSpanSize.get(thisTerm.getId()) != null) {
          int neSpanSize = entityToSpanSize.get(thisTerm.getId());
          // System.err.println("--> neSpanSize: " + neSpanSize);
          String neClass = entityToType.get(thisTerm.getId());
          // String neType = convertToConLLTypes(neClass);
          if (neSpanSize > 1) {
            for (int j = 0; j < neSpanSize; j++) {
              // System.err.println("-> sentenceTerms: " +
              // sentenceTerms.size());
              // System.err.println("-> indexes: " + (i + j));
              // System.err.println("-> terms: " + sentenceTerms.get(i +
              // j).getId());
              thisTerm = sentenceTerms.get(i + j);
              sb.append(thisTerm.getForm());
              sb.append("\t");
              sb.append(thisTerm.getLemma());
              sb.append("\t");
              sb.append(thisTerm.getMorphofeat());
              sb.append("\t");
              if (j == 0) {
                sb.append(BIO.BEGIN.toString());
              } else {
                sb.append(BIO.IN.toString());
              }
              sb.append(neClass);
              sb.append("\n");
            }
          } else {
            sb.append(thisTerm.getForm());
            sb.append("\t");
            sb.append(thisTerm.getLemma());
            sb.append("\t");
            sb.append(thisTerm.getMorphofeat());
            sb.append("\t");
            sb.append(BIO.BEGIN.toString());
            sb.append(neClass);
            sb.append("\n");
          }
          i += neSpanSize - 1;
        } else {
          sb.append(thisTerm.getForm());
          sb.append("\t");
          sb.append(thisTerm.getLemma());
          sb.append("\t");
          sb.append(thisTerm.getMorphofeat());
          sb.append("\t");
          sb.append(BIO.OUT);
          sb.append("\n");
        }
      }
      sb.append("\n");// end of sentence
    }
    return sb.toString();
  }

  public static void nafToCoNLL2003(Path dir) throws IOException {
    // process one file
    if (Files.isRegularFile(dir) && !dir.endsWith(".conll03")) {
      KAFDocument kaf = KAFDocument.createFromFile(dir.toFile());
      Path outfile = Files.createFile(Paths.get(dir.toString() + ".conll03"));
      String outKAF = nafToCoNLLConvert2003(kaf);
      Files.write(outfile, outKAF.getBytes(StandardCharsets.UTF_8));
      System.err.println(">> Wrote CoNLL document to " + outfile);
    } else {
      // recursively process directories
      try (DirectoryStream<Path> filesDir = Files.newDirectoryStream(dir)) {
        for (Path file : filesDir) {
          if (Files.isDirectory(file)) {
            nafToCoNLL2003(file);
          } else {
            if (!file.endsWith(".conll03")) {
              Path outfile = Files
                  .createFile(Paths.get(file.toString() + ".conll02"));
              KAFDocument kaf = KAFDocument.createFromFile(file.toFile());
              String outKAF = nafToCoNLLConvert2003(kaf);
              Files.write(outfile, outKAF.getBytes());
              System.err.println(">> Wrote CoNLL03 document to " + outfile);
            }
          }
        }
      }
    }
  }

  /**
   * Output Conll2003 format.
   * 
   * @param kaf
   *          the kaf document
   * @return the annotated named entities in conll03 format
   */
  public static String nafToCoNLLConvert2003(KAFDocument kaf) {
    List<Entity> namedEntityList = kaf.getEntities();
    Map<String, Integer> entityToSpanSize = new HashMap<>();
    Map<String, String> entityToType = new HashMap<>();
    for (Entity ne : namedEntityList) {
      List<ixa.kaflib.Span<Term>> entitySpanList = ne.getSpans();
      for (ixa.kaflib.Span<Term> spanTerm : entitySpanList) {
        Term neTerm = spanTerm.getFirstTarget();
        // create map from term Id to Entity span size
        entityToSpanSize.put(neTerm.getId(), spanTerm.size());
        // create map from term Id to Entity type
        entityToType.put(neTerm.getId(), ne.getType());
      }
    }

    List<List<WF>> sentences = kaf.getSentences();
    StringBuilder sb = new StringBuilder();
    for (List<WF> sentence : sentences) {
      int sentNumber = sentence.get(0).getSent();
      List<Term> sentenceTerms = kaf.getSentenceTerms(sentNumber);
      boolean previousIsEntity = false;
      String previousType = null;

      for (int i = 0; i < sentenceTerms.size(); i++) {
        Term thisTerm = sentenceTerms.get(i);
        // if term is inside an entity span then annotate B-I entities
        if (entityToSpanSize.get(thisTerm.getId()) != null) {
          int neSpanSize = entityToSpanSize.get(thisTerm.getId());
          String neClass = entityToType.get(thisTerm.getId());
          String neType = convertToConLLTypes(neClass);
          // if Entity span is multi token
          if (neSpanSize > 1) {
            for (int j = 0; j < neSpanSize; j++) {
              thisTerm = sentenceTerms.get(i + j);
              sb.append(thisTerm.getForm());
              sb.append("\t");
              sb.append(thisTerm.getLemma());
              sb.append("\t");
              sb.append(thisTerm.getMorphofeat());
              sb.append("\t");
              if (j == 0 && previousIsEntity
                  && previousType.equalsIgnoreCase(neType)) {
                sb.append(BIO.BEGIN.toString());
              } else {
                sb.append(BIO.IN.toString());
              }
              sb.append(neType);
              sb.append("\n");
            }
            previousType = neType;
          } else {
            sb.append(thisTerm.getForm());
            sb.append("\t");
            sb.append(thisTerm.getLemma());
            sb.append("\t");
            sb.append(thisTerm.getMorphofeat());
            sb.append("\t");
            if (previousIsEntity && previousType.equalsIgnoreCase(neType)) {
              sb.append(BIO.BEGIN.toString());
            } else {
              sb.append(BIO.IN.toString());
            }
            sb.append(neType);
            sb.append("\n");
          }
          previousIsEntity = true;
          previousType = neType;
          i += neSpanSize - 1;
        } else {
          sb.append(thisTerm.getForm());
          sb.append("\t");
          sb.append(thisTerm.getLemma());
          sb.append("\t");
          sb.append(thisTerm.getMorphofeat());
          sb.append("\t");
          sb.append(BIO.OUT);
          sb.append("\n");
          previousIsEntity = false;
          previousType = BIO.OUT.toString();
        }
      }
      sb.append("\n");// end of sentence
    }
    return sb.toString();
  }

  /**
   * Convert Entity class annotation to CoNLL formats.
   * 
   * @param neType
   *          named entity class
   * @return the converted string
   */
  public static String convertToConLLTypes(String neType) {
    String conllType = null;
    if (neType.startsWith("PER") || neType.startsWith("ORG")
        || neType.startsWith("LOC") || neType.startsWith("GPE")
        || neType.length() == 3) {
      conllType = neType.substring(0, 3);
    } else {
      conllType = neType;
    }
    return conllType;
  }

  /**
   * Enumeration class for CoNLL 2003 BIO format
   */
  public static enum BIO {
    BEGIN("B-"), IN("I-"), OUT("O");
    String tag;

    BIO(String tag) {
      this.tag = tag;
    }

    public String toString() {
      return this.tag;
    }
  }

}
