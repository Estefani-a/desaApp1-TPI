package com.example.uniride.backend.util;

import java.time.ZoneId;

/** Time zone of the business: dates and times of the trips are in Buenos Aires (docs/API.md §1). */
public final class BusinessTime {

    public static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private BusinessTime() {
    }
}
