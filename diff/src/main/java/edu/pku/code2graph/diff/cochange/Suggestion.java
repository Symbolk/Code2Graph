package edu.pku.code2graph.diff.cochange;

import edu.pku.code2graph.diff.model.ChangeType;

import java.util.Objects;

public class Suggestion {
  private ChangeType changeType = ChangeType.UNKNOWN;
  private EntityType entityType; // file, type, member
  private String identifier;

  private double confidence = 0D;

  public Suggestion(
      ChangeType changeType, EntityType entityType, String identifier, double confidence) {
    this.changeType = changeType;
    this.entityType = entityType;
    this.identifier = identifier;
    this.confidence = confidence;
  }

  public ChangeType getChangeType() {
    return changeType;
  }

  public EntityType getEntityType() {
    return entityType;
  }

  public String getIdentifier() {
    return identifier;
  }

  public double getConfidence() {
    return confidence;
  }

  public void setConfidence(double confidence) {
    this.confidence = confidence;
  }

  @Override
  public String toString() {
    return "Suggestion{"
        + "changeType="
        + changeType
        + ", entityType="
        + entityType
        + ", identifier='"
        + identifier
        + '\''
        + ", confidence="
        + confidence
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Suggestion that = (Suggestion) o;
    return entityType == that.entityType && Objects.equals(identifier, that.identifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityType, identifier);
  }
}
