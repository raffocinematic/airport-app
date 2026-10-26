package it.raffo.backend.destinations;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Produce la chiave testuale usata per cercare i nomi delle località.
 * Importazione e ricerca devono applicare la stessa normalizzazione.
 */
public final class DestinationNameNormalizer {

    private static final Pattern COMBINING_MARKS =
            Pattern.compile("\\p{M}+");

    private static final Pattern WHITESPACE =
            Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    // Classe di utilità: si usa tramite il metodo statico.
    private DestinationNameNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }

        // Separa i caratteri di base dai segni diacritici.
        String decomposed =
                Normalizer.normalize(value, Normalizer.Form.NFD);

        String withoutMarks =
                COMBINING_MARKS.matcher(decomposed).replaceAll("");

        // Converte sequenze di spazi e tabulazioni in un singolo spazio.
        String compacted =
                WHITESPACE.matcher(withoutMarks).replaceAll(" ");

        return compacted.strip().toLowerCase(Locale.ROOT);
    }
}