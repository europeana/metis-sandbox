package eu.europeana.metis.sandbox.common.exception;

public class ThumbnailStoringException extends ServiceException {

  private static final long serialVersionUID = 2426586813201920583L;

  private final String targetName;

  public ThumbnailStoringException(String targetName, Throwable cause) {
    super("Issue processing thumbnail " + targetName, cause);
    this.targetName = targetName;
  }

  public String getTargetName() {
    return targetName;
  }
}
