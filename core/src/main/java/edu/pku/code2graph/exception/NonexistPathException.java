package edu.pku.code2graph.exception;

public class NonexistPathException extends Exception {

  public NonexistPathException() {
    super("Nonexisit path.");
  }

  public NonexistPathException(String message) {
    super("Nonexisit path: \n" + message);
  }

  public NonexistPathException(String message, String path) {
    super(detailErrorMessage(message, path));
  }

  private static String detailErrorMessage(String message, String path) {
    return message + ": " + path + " does not exist! \n";
  }
}
