package main;

import java.util.regex.Matcher;

/**
 * Creer un opcode binaire a partir du resultat de {@link main.Assembler#PATTERN PATTERN}.
 *
 * @author Alexandre Clement
 * @since 30/12/2016.
 */
@FunctionalInterface
interface Opcode
{
    String match(Matcher matcher);
}
