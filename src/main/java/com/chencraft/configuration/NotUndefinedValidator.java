package com.chencraft.configuration;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.jackson.nullable.JsonNullable;

import java.lang.reflect.Field;

@Slf4j
public class NotUndefinedValidator implements ConstraintValidator<NotUndefined, Object> {

    @Override
    public boolean isValid(Object addressInformation, ConstraintValidatorContext context) {
        Class<?> objClass = addressInformation.getClass();
        Field[] fields = objClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().equals(JsonNullable.class)) {
                field.setAccessible(true);
                try {
                    Object value = field.get(addressInformation);
                    if (value.equals(JsonNullable.undefined())) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate(field.getName() + " cannot be undefined")
                               .addConstraintViolation();
                        return false;
                    }
                } catch (IllegalAccessException e) {
                    log.error("Illegal Access Exception: ", e);
                }
            }
        }
        return true;
    }
}