package main;

import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Convertie un programme ARM assembleur en fichier binaire d'initiation memoire logisim.
 *
 * @author Alexandre Clement
 * @since 29/12/2016.
 *
 * @see Instructions
 * @see InstructionType
 * @see Condition
 * @see Opcode
 */
public class Assembler
{

    // Constantes representant les groupes du patterne

    private static final int LABEL_GROUP = 1;
    static final int INSTRUCTION_GROUP = 2;
    static final int CHANGE_LABEL_GROUP = 3;
    static final int CONDITION_GROUP = 4;
    static final int FIRST_OPERAND_GROUP = 5;
    static final int BRANCH_LABEL_GROUP = 6;
    static final int SECOND_OPERAND_GROUP = 7;
    static final int ADDRESSING_OFFSET = 8;
    static final int ADDRESSING_REGISTER = 9;
    static final int ADDRESSING_INDEX = 10;
    static final int ADDRESSING_OFFSET_REGISTER = 11;
    static final int ADDRESSING_SHIFT_INSTRUCTION = 12;
    static final int ADDRESSING_SHIFT_OFFSET = 13;
    static final int THIRD_OPERAND = 14;
    static final int TWO_OPERAND_OFFSET = 15;

    // Sous-parties du patterne

    private static final String LABEL = "^(?:\\s*(\\w+)(?:\\s*:)?\\s+)?\\s*";                                                                                                   // Recupere le label
    private static final String COMMENT = "\\s*(?:;.*)?$";                                                                                                                      // Permet d'ajouter des commentaires à la fin d'une instruction après un ';'
    private static final String INSTRUCTIONS = "(" + Arrays.stream(Instructions.values()).map(Instructions::toString).collect(Collectors.joining("|")) + ")(S)?";               // Recupere l'instruction utiliser
    private static final String CONDITIONS = "(" + Arrays.stream(Condition.values()).map(Condition::toString).collect(Collectors.joining("|")) + ")?";                          // Recupere la conditions utiliser
    private static final String SHIFT = "(" + Arrays.stream(Instructions.ofType(InstructionType.SHIFT)).map(Instructions::toString).collect(Collectors.joining("|")) + ")";     // Recupere une instruction SHIFT dans l'adresse
    private static final String OPERAND_1 = "\\s*(?:(?:R(\\d+))|(\\w+))";                                                                                                       // Recupere l'operande 1
    private static final String ADDRESSING = "(?:\\[\\s*(?:R(\\d+)\\s*(?:,\\s*(?:(?:R(\\d+))|(?:#(\\d+)))\\s*(?:,\\s*" + SHIFT + "(?:\\s+#(\\d+))?)?)?)\\s*])";                 // Recupere l'adresse contenue entre '[' ']'
    private static final String OPERAND_2 = "(?:\\s*,\\s*(?:(?:R(\\d+))|#(\\d+)|" + ADDRESSING + "))?";                                                                         // Recupere l'operande 2
    private static final String OPERAND_3 = "(?:\\s*,\\s*(?:(?:R(\\d+))|#(\\d+)))?";                                                                                            // Recupere l'operande 3
    /**
     * Pattern pour recuperer les instructions.
     * <pre>
     * Group1: Les labels.
     *         <u>label</u> ADC R1, R2
     * Group2: Le nom de l'instruction.
     *         label <u>ADC</u> R1, R2
     * Group3: Specifie si l'instruction update le flag
     *         label ADC<u>S</u> R1, R2
     * Group4: Condition d'execution de l'instruciton.
     *         B<u>EQ</u> label
     * Group5: Premiere operande.
     *         ADC <u>R1</u>, R2
     * Group6: Label de branchement.
     *         B <u>label</u>
     * Group7: Seconde operande.
     *         ADC R1, <u>R2</u>
     * Group8: Offset d'adressage.
     *         MOV R1, <u>#2</u>
     * Group9: Registre d'adressage.
     *         LDR R1, [<u>R2</u>]
     * Group10: Index d'adressage.
     *          LDR R1, [R2, <u>R3</u>]
     * Group11: Offset du registre d'adressage.
     *          LDR R1, [R2, R3, <u>#2</u>]
     * Group12: Instruction de decalage lors de l'adressage.
     *          LDR R1, [R2, R3, <u>LSL</u> #2]
     * Group13: Offset de decalage.
     *          LDR R1, [R2, R3, LSL <u>#2</u>]
     * Group14: Troisieme operande.
     *          ADD R1, R2, <u>R3</u>
     * Group15: Offset a deux operandes.
     *          LSL R1, R2, <u>#2</u>
     * </pre>
     * <blockquote><pre>
     * exemples:
     *
     * Instruction:             label   ADD     R0,     R1,     #3
     * Groupes correspondants:  group1  group2  group5  group7  group15
     * Valeur de chaque groupe: "label" "ADD"   0       1       3
     *
     * Instruction:                     BEQ         label   ; Branchement conditionnel à "label"
     * Groupes correspondants      group2 group4    group6
     * Valeur de chaque groupe:    "B"    "EQ"      "label"
     *
     * Instruction:                     LDR     R2,     [R0,    R1,     LSL     #2]
     * Groupes correspondants:          group2  group5  group9  group10 group12 group13
     * Valeur de chaque groupe:         "LDR"   2       0       1       "LSL"   2
     *
     * </pre></blockquote>
     */
    // On ordonne toutes les sous-partie du patterne et on le rend insensible à la casse
    private static final Pattern PATTERN = Pattern.compile(LABEL + INSTRUCTIONS + CONDITIONS + OPERAND_1 + OPERAND_2 + OPERAND_3 + COMMENT, Pattern.CASE_INSENSITIVE);
    /**
     * Entete du fichier binaire.
     */
    private static final String HEADER = "v2.0 raw\n";
    /**
     * Nom du fichier binaire {@value}.
     */
    private static final String OUT_FILE = "rom.ini";
    /**
     * Table liant un label a l'instruction correspondante.
     */
    private final Map<String, Long> link;
    private final RandomAccessFile file;
    private final Writer memory;
    /**
     * Numero de l'instruction en cours de traitement.
     * (appartient a [0...n] avec n le nombre total d'instruction contenue dans le programme)
     */
    private long step;

    /**
     * Initialise l'input et l'output de l'assembleur.
     * <p>
     * Creer un fichier {@value OUT_FILE} contenant les donnees d'initialisation memoire
     *
     * @param filename le nom du fichier contenant le programme
     * @throws IOException si le fichier n'existe pas
     */
    private Assembler(String filename) throws IOException
    {
        file = new RandomAccessFile(filename, "r");
        link = new HashMap<>();
        memory = new FileWriter(OUT_FILE);
        memory.write(HEADER);
    }

    /**
     * Convertie le programme ARM assembleur en donnee binaire permettant l'initialisation de la memoire logisim.
     *
     * @throws IOException si le fichier n'existe pas
     */
    private void build() throws IOException
    {
        // Initialise le nombre d'instruction à 0
        step = 0;
        // Lit et applique le pattern sur chaque ligne du fichier
        for (String line = file.readLine(); line != null; line = file.readLine())
        {
            memory.write(match(PATTERN.matcher(line)));
        }
        file.close();
        memory.close();
    }

    /**
     * Recupere le resultat de l'application du patterne sur la ligne
     * et renvoie le code Hexa de l'instruction présente sur celle-ci.
     * Renvoie la chaine vide <tt>""</tt> si aucune instruction n'est présente.
     * Renvoie <tt>0</tt> si une instruction inconnue est présente sur la ligne.
     *
     * @param matcher le resultat de l'application du patterne sur la ligne.
     * @return le codage Hexadecimale de l'instruction.
     * @throws IOException si le fichier n'existe pas
     */
    private String match(Matcher matcher) throws IOException
    {
        // Si le patterne ne correspond pas à la ligne, renvoie la chaine vide ""
        if (!matcher.matches())
            return "";

        // Si un label est présent, ajout du label à la table des labels
        if (matcher.group(1) != null)
            link.put(matcher.group(LABEL_GROUP), step);

        // Si aucune instruction n'est trouvé (i.e une ligne avec uniquement un label), renvoie la chaine vide ""
        if (matcher.group(2) == null)
            return "";

        /*
        Recupere les indices des groupes non null
        Recupere l'instruction correspondant au nom et au groupes présents
        Recupere l'opcode de l'instruction
         */
        int[] groups = IntStream.range(2, matcher.groupCount() + 1).filter(n -> matcher.group(n) != null).toArray();
        Instructions instruction = Instructions.getEnum(matcher.group(INSTRUCTION_GROUP), groups);
        String binaryOpcode = instruction.getOpcode(matcher);

        /*
        Si l'instruction est un branchement
        Ajoute l'indide de l'instruction associée au label à l'opcode de l'instruction
        */
        if (instruction.getType() == InstructionType.BRANCH)
        {
            /*
            Si le label de branchement n'est pas présent dans la table des labels (i.e le label est présent plus loin dans le programme
            Cherche le label plus loin dans le programme et l'ajoute à la table
            */
            if (!link.containsKey(matcher.group(BRANCH_LABEL_GROUP)))
                findLabel(matcher.group(BRANCH_LABEL_GROUP));
            binaryOpcode += String.format("%8s", Long.toBinaryString(link.get(matcher.group(BRANCH_LABEL_GROUP)))).replace(' ', '0');
        }

        // Une instruction de plus a été traitée
        step += 1;

        // Convertion de l'opcode en hexadecimal
        return Integer.toHexString(Integer.parseInt(binaryOpcode, 2)) + " ";
    }

    /**
     * Cherche et ajoute un label pas encore present dans la table.
     *
     * @param label le label a trouve
     * @throws IOException si le fichier n'existe pas
     */
    private void findLabel(String label) throws IOException
    {
        // Demarre a l'instruction actuel
        long temp = step;

        // Sauvegarde la position actuelle de lecture du fichier source
        long backup = file.getFilePointer();

        /*
        Parcourt le reste du fichier
        Si le patterne correspond, ajoute une instruction à la taille temporaire du programme
        Si le patterne et le label correspondent,
        Ajout à la table des labels du label ainsi que de son numéro d'instruction correspondant
        Retourne à l'endroit de lecture du fichier précédement sauvegarder
         */
        for (String line = file.readLine(); line != null; line = file.readLine())
        {
            Matcher matcher = PATTERN.matcher(line);
            if (matcher.matches())
                temp += 1;
            if (matcher.matches() && label.equals(matcher.group(LABEL_GROUP)))
            {
                link.put(label, temp);
                file.seek(backup);
                return;
            }
        }
    }

    /**
     * Prend en argument le nom du fichier contenant le programme en langage assembleur ARM et le convertit en fichier binaire.
     *
     * @param args les arguments contenant à l'indice 0 le nom du fichier source
     * @throws IOException si le fichier n'existe pas
     */
    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
            throw new IllegalArgumentException("File name is missing");
        new Assembler(args[0]).build();
    }

}
