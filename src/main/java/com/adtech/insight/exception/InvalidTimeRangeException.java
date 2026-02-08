package com.adtech.insight.exception;


public class InvalidTimeRangeException extends RuntimeException{
    public InvalidTimeRangeException(Object from, Object to) {
        super("Invalid time range: from=" + from + " to=" + to);
    }
}
