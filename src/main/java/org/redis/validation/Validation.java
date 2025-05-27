package org.redis.validation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Validation {

    public static Map<Violation.Type, List<Violation>> validate(Object object) {
        Map<Violation.Type, List<Violation>> violations = new HashMap<>();
        for(Field field: object.getClass().getDeclaredFields()){
            field.setAccessible(true);
            var annotation = field.getAnnotation(Validate.class);
            if(annotation != null){
                Validator validator = null;
                try {
                    validator = (Validator)annotation.validator().getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
                Violation violation = null;
                try {
                    violation = validator.validate((String) field.get(object));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                var list = violations.getOrDefault(violation.type(), new LinkedList<>());
                list.add(violation);
                violations.put(violation.type(), list);
            }

        }
        return violations;
    }
}
