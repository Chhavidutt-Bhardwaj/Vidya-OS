package com.ai.vidya.exception;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
@ResponseStatus(HttpStatus.CONFLICT)
public class AcademicYearAlreadyExistsException extends RuntimeException {
    public AcademicYearAlreadyExistsException(String label) {
        super("Academic year '%s' already exists for this school.".formatted(label));
    }
}