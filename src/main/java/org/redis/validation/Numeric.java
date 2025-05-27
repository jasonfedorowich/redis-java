package org.redis.validation;

public class Numeric {

    public static boolean isNotLong(String value){
        try{
            Long.parseLong(value);
            return false;
        }catch (NumberFormatException e){
            return true;
        }
    }
}
