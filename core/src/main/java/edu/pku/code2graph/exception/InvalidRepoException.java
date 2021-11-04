package edu.pku.code2graph.exception;

public class InvalidRepoException extends Exception {

  public InvalidRepoException() {
    super("Invalid Git repository.");
  }

  public InvalidRepoException(String message) {
    super("Invalid Git repository: " + message);
  }

  public InvalidRepoException(String message, String repoPath) {
    super(detailErrorMessage(message, repoPath));
  }

  private static String detailErrorMessage(String message, String repoPath) {
    return repoPath + " is not a valid Git repository for " + message;
  }
}
