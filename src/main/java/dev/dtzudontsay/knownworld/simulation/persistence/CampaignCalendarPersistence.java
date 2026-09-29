package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.time.CampaignCalendar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CampaignCalendarPersistence {

    private static final String HEADER =
            "KNOWNWORLD_CAMPAIGN_CALENDAR\t1";

    private static final String FILE_NAME =
            "campaign_calendar.tsv";

    private final Path file;

    public CampaignCalendarPersistence(
            Path directory
    ) {
        this.file =
                directory.resolve(
                        FILE_NAME
                );
    }

    public void save(
            CampaignCalendar calendar
    ) throws IOException {

        Files.createDirectories(
                file.getParent()
        );

        String content =
                HEADER
                        + "\nCALENDAR\t"
                        + calendar.absoluteDay()
                        + "\t"
                        + calendar.tickRemainder()
                        + "\t"
                        + calendar.ticksPerCampaignDay()
                        + "\n";

        Files.writeString(
                file,
                content,
                StandardCharsets.UTF_8
        );
    }

    public void loadInto(
            CampaignCalendar calendar
    ) throws IOException {

        if (!Files.exists(
                file
        )) {
            return;
        }

        var lines =
                Files.readAllLines(
                        file,
                        StandardCharsets.UTF_8
                );

        if (lines.isEmpty()
                || !HEADER.equals(
                lines.get(
                        0
                )
        )) {

            throw new IOException(
                    "Unsupported campaign calendar save format"
            );
        }

        if (lines.size() < 2) {
            return;
        }

        String[] p =
                lines.get(
                                1
                        )
                        .split(
                                "\t",
                                -1
                        );

        if (p.length != 4
                || !"CALENDAR".equals(
                p[0]
        )) {

            throw new IOException(
                    "Invalid campaign calendar record"
            );
        }

        calendar.restore(
                Long.parseLong(
                        p[1]
                ),
                Integer.parseInt(
                        p[2]
                ),
                Integer.parseInt(
                        p[3]
                )
        );
    }
}