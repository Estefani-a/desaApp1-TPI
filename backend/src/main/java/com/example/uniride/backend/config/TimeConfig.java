package com.example.uniride.backend.config;

import com.example.uniride.backend.util.BusinessTime;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializerBase;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Dates and times as docs/API.md §1 defines them: times as {@code "22:15"} and instants
 * with the Buenos Aires offset ({@code "2026-11-20T21:40:12-03:00"}). Entities store
 * {@link Instant}; the conversion to the business zone happens only when writing JSON.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId BUSINESS_ZONE = BusinessTime.ZONE;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter INSTANT_FORMAT =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(BUSINESS_ZONE);

    /** Services ask the clock for "now", so tests can fix the date. */
    @Bean
    Clock clock() {
        return Clock.system(BUSINESS_ZONE);
    }

    @Bean
    Jackson2ObjectMapperBuilderCustomizer timeFormatCustomizer() {
        SimpleModule module = new SimpleModule("UniRideTime");
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(TIME_FORMAT));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FORMAT));
        module.addSerializer(Instant.class, new BusinessZoneInstantSerializer());
        return builder -> builder.modulesToInstall(module);
    }

    private static final class BusinessZoneInstantSerializer extends ToStringSerializerBase {

        BusinessZoneInstantSerializer() {
            super(Instant.class);
        }

        @Override
        public String valueToString(Object value) {
            return INSTANT_FORMAT.format(((Instant) value).truncatedTo(ChronoUnit.SECONDS));
        }
    }
}
