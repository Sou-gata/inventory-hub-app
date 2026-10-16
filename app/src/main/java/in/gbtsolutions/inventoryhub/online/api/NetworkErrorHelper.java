package in.gbtsolutions.inventoryhub.online.api;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

public class NetworkErrorHelper {

    public static final String DEFAULT_CONNECTION_ERROR = "Failed to connect with the server";

    /**
     * Converts a network throwable or exception into a clean, user-friendly error message.
     */
    public static String getErrorMessage(Throwable t) {
        return getErrorMessage(t, DEFAULT_CONNECTION_ERROR);
    }

    /**
     * Converts a network throwable into a friendly error message, falling back to a custom default.
     */
    public static String getErrorMessage(Throwable t, String defaultFallback) {
        String fallback = (defaultFallback != null && !defaultFallback.trim().isEmpty())
                ? defaultFallback
                : DEFAULT_CONNECTION_ERROR;

        if (t == null) {
            return fallback;
        }

        if (t instanceof ConnectException
                || t instanceof SocketTimeoutException
                || t instanceof UnknownHostException
                || t instanceof NoRouteToHostException
                || t instanceof PortUnreachableException
                || t instanceof SocketException
                || t instanceof SSLException
                || t instanceof IOException) {
            return DEFAULT_CONNECTION_ERROR;
        }

        String msg = t.getMessage();
        if (msg != null && !msg.trim().isEmpty()) {
            String lower = msg.toLowerCase();
            if (lower.contains("failed to connect")
                    || lower.contains("connection error")
                    || lower.contains("unable to reach")
                    || lower.contains("connection refused")
                    || lower.contains("timed out")
                    || lower.contains("timeout")
                    || lower.contains("route to host")
                    || lower.contains("network is unreachable")
                    || lower.contains("unknown host")
                    || lower.contains("socket")
                    || lower.contains("reset by peer")
                    || lower.contains("broken pipe")
                    || lower.contains("port")
                    || lower.contains("ms")) {
                return DEFAULT_CONNECTION_ERROR;
            }
            return sanitizeMessage(msg.trim());
        }

        return DEFAULT_CONNECTION_ERROR;
    }

    /**
     * Sanitizes any raw socket/connection error string to avoid exposing internal IP addresses or technical socket details.
     */
    public static String sanitizeMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return DEFAULT_CONNECTION_ERROR;
        }
        String lower = message.toLowerCase();
        if (lower.contains("failed to connect")
                || (lower.contains("connect") && lower.contains("port"))
                || lower.contains("connection refused")
                || lower.contains("sockettimeoutexception")
                || lower.contains("connectexception")
                || lower.contains("unknownhostexception")) {
            return DEFAULT_CONNECTION_ERROR;
        }
        return message;
    }
}
