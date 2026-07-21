package za.co.taloms.household.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class HouseholdExceptionHandler {

    private final HouseholdPageController householdPageController;

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public String handleException(Exception ex) {
        log.error("Household page error: {}", ex.getMessage(), ex);
        return "Error loading household page: " + ex.getClass().getSimpleName() + " - " + ex.getMessage();
    }
}
