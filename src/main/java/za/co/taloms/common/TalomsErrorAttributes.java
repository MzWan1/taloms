package za.co.taloms.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.security.Principal;
import java.util.Map;

/**
 * Custom error attributes that enrich the error model with TALOMS-specific
 * metadata (title, icon, description, home URL) so that both the default
 * {@code BasicErrorController} rendering and custom view-based error pages
 * share a consistent set of attributes.
 */
@Slf4j
@Component
public class TalomsErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(
            WebRequest webRequest, ErrorAttributeOptions options) {

        Map<String, Object> attrs = super.getErrorAttributes(webRequest, options);

        Integer status = (Integer) attrs.get("status");
        if (status == null) {
            status = 500;
        }

        Principal principal = webRequest.getUserPrincipal();
        boolean authenticated = principal != null;

        attrs.put("errorTitle", ErrorPageData.getTitle(status));
        attrs.put("errorIcon", ErrorPageData.getIcon(status));
        attrs.put("errorDescription", ErrorPageData.getDescription(status));
        attrs.put("homeUrl", ErrorPageData.getHomeUrl(status, authenticated));
        attrs.put("errorColor", ErrorPageData.getColor(status));

        return attrs;
    }
}
