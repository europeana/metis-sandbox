package eu.europeana.metis.sandbox.common.exception;

import lombok.experimental.StandardException;

/**
 * A runtime exception that serves as a base class for service-related errors. This class can be extended to define more specific
 * service exception types. It provides a standard mechanism to represent errors that occur during service operations or
 * workflows.
 */
@StandardException
public class ServiceException extends RuntimeException {

}
