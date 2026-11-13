package com.jairomatias.eventix.demo;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Ensures public pages distinguish the disposable Render showcase from a
 * commercial ticketing deployment. Not registered in SQL Server profiles.
 */
@ControllerAdvice
@Profile("render")
public class RenderDemoModeAdvice {

    @ModelAttribute("eventixDemoMode")
    public boolean demoMode() {
        return true;
    }
}
