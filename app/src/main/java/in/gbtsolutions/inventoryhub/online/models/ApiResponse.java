package in.gbtsolutions.inventoryhub.online.models;

import com.google.gson.annotations.SerializedName;

/**
 * Standard server response wrapper matching server's ApiResponse / ApiErrorResponse.
 * @param <T> Type of payload inside data field
 */
public class ApiResponse<T> {

    @SerializedName("success")
    private boolean success;

    @SerializedName("statusCode")
    private int statusCode;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private T data;

    @SerializedName("sessionExpired")
    private Boolean sessionExpired;

    @SerializedName("seassonExpired")
    private Boolean seassonExpired;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, int statusCode, String message, T data) {
        this.success = success;
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSessionExpired() {
        if (sessionExpired != null) {
            return sessionExpired;
        }
        if (seassonExpired != null) {
            return seassonExpired;
        }
        return false;
    }

    public void setSessionExpired(Boolean sessionExpired) {
        this.sessionExpired = sessionExpired;
    }
}
