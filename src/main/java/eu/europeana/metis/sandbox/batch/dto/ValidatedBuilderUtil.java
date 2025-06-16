package eu.europeana.metis.sandbox.batch.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class providing methods for creating and validating objects built using builders.
 */
public class ValidatedBuilderUtil {

  private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private static final Validator validator = factory.getValidator();

  /**
   * Builds and validates an object using the supplied builder components.
   *
   * @param <T> The type of object to be built.
   * @param <B> The type of the builder used to create the object.
   * @param builderSupplier Supplier providing a new instance of the builder.
   * @param builderFunction Function converting the builder into the final object.
   * @param builderSetup Consumer for configuring the builder before building the object.
   * @return A validated instance of the built object.
   * @throws ConstraintViolationException If validation of the built object fails.
   */
  public static <T, B> T buildValidated(Supplier<B> builderSupplier, Function<B, T> builderFunction, Consumer<B> builderSetup) {
    return buildValidated(builderSupplier, builderFunction, b -> {}, builderSetup);
  }

  /**
   * Builds and validates an instance of type T using the provided builder.
   *
   * @param <T> The type of the object to be built and validated.
   * @param <B> The type of the builder used to construct the object.
   * @param builderSupplier Supplies an instance of the builder. Must not be null.
   * @param builderFunction Converts the builder into the desired object. Must not be null.
   * @param preBuilderSetup Performs initial setup on the builder before configuration. Must not be null.
   * @param builderSetup Configures the builder before building the object. Must not be null.
   * @return A validated instance of type T.
   * @throws ConstraintViolationException If any validation constraints are violated.
   */
  public static <T, B> T buildValidated(
      Supplier<B> builderSupplier,
      Function<B, T> builderFunction,
      Consumer<B> preBuilderSetup,
      Consumer<B> builderSetup) {

    B builder = builderSupplier.get();
    preBuilderSetup.accept(builder);
    builderSetup.accept(builder);
    T dto = builderFunction.apply(builder);

    Set<ConstraintViolation<T>> violations = validator.validate(dto);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }

    return dto;
  }
}
