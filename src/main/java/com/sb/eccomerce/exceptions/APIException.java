package com.sb.eccomerce.exceptions;

public class APIException extends RuntimeException{
    private static final long serialVersionUID = 1l;

    public APIException(){

    }
    public APIException(String message){
        super(message);
    }
}
