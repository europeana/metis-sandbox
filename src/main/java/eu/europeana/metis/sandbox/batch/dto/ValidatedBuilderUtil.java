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

public class ValidatedBuilderUtil {

  private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private static final Validator validator = factory.getValidator();

  public static <T, B> T buildValidated(Supplier<B> builderSupplier, Function<B, T> builderFunction, Consumer<B> builderSetup) {
    return buildValidated(builderSupplier, builderFunction, b -> {}, builderSetup);
  }

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
