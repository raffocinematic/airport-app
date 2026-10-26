package it.raffo.backend.destinations.importer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;

/**
 * Verifica che i file sorgenti corrispondano alle impronte
 * registrate nel manifesto SHA256SUMS.
 */
public class GeoNamesChecksumVerifier {

    private static final Set<String> REQUIRED_FILES = Set.of(
            "raw/places/IT.zip",
            "raw/places/GB.zip",
            "raw/aliases/IT.zip",
            "raw/aliases/GB.zip",
            "raw/admin1CodesASCII.txt",
            "raw/admin2Codes.txt",
            "raw/readme.txt"
    );

    public void verify(Path geonamesDirectory) throws IOException {
        Objects.requireNonNull(
                geonamesDirectory,
                "geonamesDirectory must not be null"
        );

        Path manifest = geonamesDirectory.resolve("SHA256SUMS");
        Set<String> seenFiles = new HashSet<>();
        int lineNumber = 0;

        for (String line : Files.readAllLines(
                manifest,
                StandardCharsets.UTF_8
        )) {
            lineNumber++;

            if (line.isBlank()) {
                continue;
            }

            String[] parts = line.strip().split("\\s+", 2);

            if (parts.length != 2
                    || !parts[0].matches("[0-9a-fA-F]{64}")) {
                throw new IOException(
                        "Invalid checksum entry in "
                                + manifest
                                + ", line " + lineNumber
                );
            }

            String expectedHash = parts[0];
            String relativePath = parts[1];

            if (!REQUIRED_FILES.contains(relativePath)) {
                throw new IOException(
                        "Unexpected file in checksum manifest: "
                                + relativePath
                );
            }

            if (!seenFiles.add(relativePath)) {
                throw new IOException(
                        "Duplicate checksum entry: " + relativePath
                );
            }

            Path file = geonamesDirectory.resolve(relativePath);
            String actualHash = sha256(file);

            if (!expectedHash.equalsIgnoreCase(actualHash)) {
                throw new IOException(
                        "SHA-256 mismatch for " + file
                                + ": expected " + expectedHash
                                + ", actual " + actualHash
                );
            }
        }

        if (!seenFiles.equals(REQUIRED_FILES)) {
            Set<String> missing = new HashSet<>(REQUIRED_FILES);
            missing.removeAll(seenFiles);

            throw new IOException(
                    "Missing checksum entries: " + missing
            );
        }
    }

    private String sha256(Path file) throws IOException {
        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not supported",
                    exception
            );
        }

        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }
}