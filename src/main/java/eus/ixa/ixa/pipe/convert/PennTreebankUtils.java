package eus.ixa.ixa.pipe.convert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import opennlp.tools.parser.Parse;

public class PennTreebankUtils {

  public PennTreebankUtils() {
  }

  /**
   * Takes a file containing Penn Treebank oneline annotation and creates
   * tokenized sentences saving it to a file with the *.tok extension.
   * 
   * @param treebankFile
   *          the input file
   * @throws IOException
   */
  public static void treebank2tokens(Path treebankFile) throws IOException {
    // process one file
    if (Files.isRegularFile(treebankFile)) {
      List<String> inputTrees = Files.readAllLines(treebankFile,
          StandardCharsets.UTF_8);
      Path outfile = Files
          .createFile(Paths.get(treebankFile.toString() + ".tok"));
      String outFile = getTokensFromTree(inputTrees);
      Files.write(outfile, outFile.getBytes(StandardCharsets.UTF_8));
      System.err.println(">> Wrote tokens to " + outfile);
    } else {
      System.out.println("Please choose a valid file as input.");
      System.exit(1);
    }
  }

  /**
   * Reads a list of Parse trees and calls {@code getTokens} to create tokenized
   * oneline text.
   * 
   * @param inputTrees
   *          the list of trees in penn treebank format
   * @return the tokenized document the document tokens
   */
  private static String getTokensFromTree(List<String> inputTrees) {

    StringBuilder parsedDoc = new StringBuilder();
    for (String parseSent : inputTrees) {
      Parse parse = Parse.parseParse(parseSent);
      StringBuilder sentBuilder = new StringBuilder();
      getTokens(parse, sentBuilder);
      parsedDoc.append(sentBuilder.toString()).append("\n");
    }
    return parsedDoc.toString();
  }

  /**
   * It converts a penn treebank constituent tree into tokens oneline form.
   * 
   * @param parse
   *          the parse tree
   * @param sb
   *          the stringbuilder to add the trees
   */
  private static void getTokens(Parse parse, StringBuilder sb) {
    if (parse.isPosTag()) {
      if (!parse.getType().equals("-NONE-")) {
        sb.append(parse.getCoveredText()).append(" ");
      }
    } else {
      Parse children[] = parse.getChildren();
      for (int i = 0; i < children.length; i++) {
        getTokens(children[i], sb);
      }
    }
  }

  /**
   * Takes a file containing Penn Treebank oneline annotation and creates
   * Word_POS sentences for POS tagger training, saving it to a file with the
   * *.pos extension.
   * 
   * @param treebankFile
   *          the input file
   * @throws IOException
   */
  public static void treebank2WordPos(Path treebankFile) throws IOException {
    // process one file
    if (Files.isRegularFile(treebankFile)) {
      List<String> inputTrees = Files.readAllLines(treebankFile);
      Path outfile = Paths.get(treebankFile.toString() + ".pos");
      String outFile = getPreTerminals(inputTrees);
      Files.write(outfile, outFile.getBytes());
      System.err
          .println(">> Wrote Apache OpenNLP POS training format to " + outfile);
    } else {
      System.out.println("Please choose a valid file as input.");
      System.exit(1);
    }
  }

  /**
   * Reads a list of Parse trees and calls {@code getWordType} to create POS
   * training data in Word_POS form
   * 
   * @param inputTrees
   * @return the document with Word_POS sentences
   */
  private static String getPreTerminals(List<String> inputTrees) {

    StringBuilder parsedDoc = new StringBuilder();
    for (String parseSent : inputTrees) {
      Parse parse = Parse.parseParse(parseSent);
      StringBuilder sentBuilder = new StringBuilder();
      getWordType(parse, sentBuilder);
      parsedDoc.append(sentBuilder.toString()).append("\n");
    }
    return parsedDoc.toString();
  }

  /**
   * It converts a penn treebank constituent tree into Word_POS form
   * 
   * @param parse
   * @param sb
   */
  private static void getWordType(Parse parse, StringBuilder sb) {
    if (parse.isPosTag()) {
      if (!parse.getType().equals("-NONE-")) {
        sb.append(parse.getCoveredText()).append("_").append(parse.getType())
            .append(" ");
      }
    } else {
      Parse children[] = parse.getChildren();
      for (int i = 0; i < children.length; i++) {
        getWordType(children[i], sb);
      }
    }
  }

  /**
   * It normalizes a oneline Penn treebank style tree removing trace nodes
   * (-NONE-) and pruning the empty trees created by removing the trace nodes.
   * 
   * @param treebankFile
   * @throws IOException
   */
  public static void getCleanPennTrees(Path treebankFile) throws IOException {
    if (Files.isRegularFile(treebankFile)) {
      List<String> inputTrees = Files.readAllLines(treebankFile,
          StandardCharsets.UTF_8);
      Path outfile = Files
          .createFile(Paths.get(treebankFile.toString() + ".treeN"));
      String outFile = normalizeParse(inputTrees);
      Files.write(outfile, outFile.getBytes(StandardCharsets.UTF_8));
      System.err.println(">> Wrote normalized parse to " + outfile);
    } else {
      System.out.println("Please choose a valid file as input.");
      System.exit(1);
    }
  }

  /**
   * It takes as input a semi-pruned penn treebank tree (e.g., with -NONE-
   * traces removed) via sed 's/-NONE-\s[\*A-Za-z0-9]*[\*]*[\-]*[A-Za-z0-9]*'
   * 
   * and prunes the empty trees remaining from the sed operation. The parseParse
   * function also removes function tags by default.
   * 
   * @param inputTrees
   * @return
   */
  // TODO add the sed regexp to this function
  private static String normalizeParse(List<String> inputTrees) {
    StringBuilder parsedDoc = new StringBuilder();
    for (String parseSent : inputTrees) {
      Parse parse = Parse.parseParse(parseSent);
      Parse.pruneParse(parse);
      StringBuffer sentBuilder = new StringBuffer();
      parse.show(sentBuilder);
      parsedDoc.append(sentBuilder.toString()).append("\n");
    }
    return parsedDoc.toString();
  }
}
