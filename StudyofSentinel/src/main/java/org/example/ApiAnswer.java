package org.example;


//相应类定义
public class ApiAnswer {
    private int code;
    private String message;
    public ApiAnswer(int code, String message) {
        this.code = code;
        this.message = message;
    }
    public static ApiAnswer success() {
        return new ApiAnswer(200, "success");
    }
    public static ApiAnswer error(int code, String message) {
        return new ApiAnswer(code, message);
    }
    public int getCode() {return code;}
    public String getMessage() {return message;}
    public void setCode(int code) {this.code = code;}
    public void setMessage(String message) {this.message = message;}
}
