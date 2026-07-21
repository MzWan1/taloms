package za.co.taloms.household.presentation;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HouseholdExceptionHandler {

    @PostConstruct
    public void init() {
        log.warn("!!! HouseholdExceptionHandler INITIALIZED - this should appear in logs !!!");
    }

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    public String handleException(Throwable ex) {
        log.error("Household page error: {}", ex.getMessage(), ex);
        return "Error loading household page: " + ex.getClass().getSimpleName() + " - " + ex.getMessage();
    }
}
