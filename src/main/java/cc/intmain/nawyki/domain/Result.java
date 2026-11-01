package cc.intmain.nawyki.domain;

import java.util.List;
import java.util.function.Function;

public sealed interface Result<T> permits Result.Ok, Result.Invalid {
    record Violation(String field, String message) {}
    record Ok<T>(T value) implements Result<T> {}
    record Invalid<T>(List<Violation> errors) implements Result<T> {
        public Invalid { errors = List.copyOf(errors); }
    }
    default <R> R fold(Function<T, R> success, Function<List<Violation>, R> failure) {
        if (this instanceof Ok<T> ok) return success.apply(ok.value());
        return failure.apply(((Invalid<T>) this).errors());
    }
}
