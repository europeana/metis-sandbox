package eu.europeana.metis.sandbox.service.workflow.harvest;

import eu.europeana.metis.sandbox.common.FileType;

/**
 * Represents a target file to be harvested in the system.
 * <p>
 * The FileHarvestTarget class encapsulates the metadata and content of a file to be used for harvesting operations. It includes
 * the file's name, type, and raw byte content.
 */
@SuppressWarnings("java:S6218") // We don't want to expose the file content for equality.
public record FileHarvestTarget(String fileName, FileType fileType, byte[] fileContent) {

}
