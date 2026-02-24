package eu.europeana.metis.sandbox.service.workflow.harvest;

import java.util.List;

/**
 * Represents the result of harvesting identifiers.
 * <p>
 * The HarvestIdentifiersResult class encapsulates a collection of harvested identifiers and indicates whether a record limit was
 * exceeded during the harvesting process.
 *
 * @param <T> The type of the identifiers contained in the result.
 * @param identifiers The list of identifiers retrieved from the harvesting process.
 * @param recordLimitExceeded A flag indicating whether the record limit was exceeded.
 */
public record HarvestIdentifiersResult<T>(List<T> identifiers, boolean recordLimitExceeded) {

}

