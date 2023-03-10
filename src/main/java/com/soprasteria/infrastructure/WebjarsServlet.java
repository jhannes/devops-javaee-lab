package com.soprasteria.infrastructure;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Properties;

public class WebjarsServlet extends HttpServlet {

    private final URL baseUrl;
    private final Instant lastModified;

    public WebjarsServlet(String artifactId) {
        this(baseUrl(artifactId));
    }

    public WebjarsServlet(URL baseUrl) {
        this.baseUrl = baseUrl;
        try {
            lastModified = Instant.ofEpochMilli(baseUrl.openConnection().getLastModified());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static URL baseUrl(String artifactId) {
        return WebjarsServlet.class.getResource(
                "/META-INF/resources/webjars/%s/%s/".formatted(artifactId, findVersion(artifactId))
        );
    }

    private static String findVersion(String artifactId) {
        try(var resource = WebjarsServlet.class.getResourceAsStream("/META-INF/maven/org.webjars/%s/pom.properties".formatted(artifactId))) {
            if (resource == null) {
                throw new IllegalArgumentException("Unknown webjar " + artifactId);
            }
            var properties = new Properties();
            properties.load(resource);
            return properties.getProperty("version");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getDateHeader("If-Modified-Since") > 0) {
            var ifModifiedHeader = Instant.ofEpochMilli(request.getDateHeader("If-Modified-Since"));
            if (!ifModifiedHeader.isAfter(lastModified)) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
        }
        var url = new URL(baseUrl, request.getPathInfo().substring(1));
        var mimeType = this.getServletContext().getMimeType(url.getFile());
        response.setContentType(mimeType != null ? mimeType : "application/octet-stream");
        response.setDateHeader("Last-Modified", lastModified.toEpochMilli());
        try (InputStream inputStream = url.openStream()) {
            inputStream.transferTo(response.getOutputStream());
        }

    }
}
