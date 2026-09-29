package dev.dtzudontsay.knownworld.simulation.persistence;

import dev.dtzudontsay.knownworld.simulation.social.succession.SuccessionLaw;
import dev.dtzudontsay.knownworld.simulation.social.succession.SuccessionRuleManager;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleId;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SuccessionPersistence {

    private static final String HEADER =
            "KNOWNWORLD_SUCCESSION\t1";

    private static final String FILE_NAME =
            "succession.tsv";

    private final Path file;

    public SuccessionPersistence(
            Path directory
    ) {
        this.file =
                directory.resolve(
                        FILE_NAME
                );
    }

    public void save(
            SuccessionRuleManager rules
    ) throws IOException {

        Files.createDirectories(
                file.getParent()
        );

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                HEADER
        );

        builder.append(
                '\n'
        );

        for (
                var entry :
                rules.all()
                        .entrySet()
        ) {

            builder.append(
                    "RULE\t"
            );

            builder.append(
                    entry.getKey()
                            .value()
            );

            builder.append(
                    '\t'
            );

            builder.append(
                    entry.getValue()
                            .name()
            );

            builder.append(
                    '\n'
            );
        }

        Files.writeString(
                file,
                builder.toString(),
                StandardCharsets.UTF_8
        );
    }

    public void loadInto(
            SuccessionRuleManager rules
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
                    "Unsupported succession save format"
            );
        }

        for (int i = 1; i < lines.size(); i++) {

            String line =
                    lines.get(
                            i
                    );

            if (line.isBlank()) {
                continue;
            }

            String[] p =
                    line.split(
                            "\t",
                            -1
                    );

            if (p.length != 3
                    || !"RULE".equals(
                    p[0]
            )) {

                throw new IOException(
                        "Invalid succession record: "
                                + line
                );
            }

            rules.setLaw(
                    new TitleId(
                            Long.parseLong(
                                    p[1]
                            )
                    ),
                    SuccessionLaw.valueOf(
                            p[2]
                    )
            );
        }
    }
}