package main;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import static java.lang.Integer.toBinaryString;
import static main.Assembler.*;
import static main.InstructionType.*;
import static main.InstructionType.SHIFT;

/**
 * Definition des instructions.
 * Une instruction est caracterisee par son nom,
 * ainsi que les groupes necessairement non null pour que cette instruction soit valide.
 *
 * @author Alexandre Clement
 * @since 30/12/2016.
 *
 * @see InstructionType
 * @see Opcode
 */
enum Instructions
{
    AND(dataProcess(0),                     INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP),
    EOR(dataProcess(1),                     INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP),
    LSL_R(dataProcess(2),   SHIFT,  "LSL",  INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP),
    LSR_R(dataProcess(3),   SHIFT,  "LSR",  INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP),
    ASR_R(dataProcess(4),   SHIFT,  "ASR",  INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP),
    ADC(dataProcess(5),                     INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP),
    SBC(dataProcess(6),                     INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP),
    ROR(dataProcess(7),     SHIFT,  "ROR",  INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP),
    TST(dataProcess(8),                     INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP),
    RSB(dataProcess(9),                     INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP, TWO_OPERAND_OFFSET),
    CMP(dataProcess(10),                    INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP),
    CMN(dataProcess(11),                    INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP),
    ORR(dataProcess(12),                    INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP),
    MUL(dataProcess(13),                    INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP, THIRD_OPERAND),
    BIC(dataProcess(14),                    INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP),
    MVN(dataProcess(15),                    INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP),
    LSL_I(shift(0),         SHIFT,  "LSL",  INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP, TWO_OPERAND_OFFSET),
    LSR_I(shift(1),         SHIFT,  "LSR",  INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP, TWO_OPERAND_OFFSET),
    ASR_I(shift(2),         SHIFT,  "ASR",  INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP, TWO_OPERAND_OFFSET),
    ADD_R(addSub(0),                "ADD",  INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP, THIRD_OPERAND),
    ADD_I(addImmediate(),           "ADD",  INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP, TWO_OPERAND_OFFSET),
    SUB(addSub(1),                          INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, SECOND_OPERAND_GROUP, THIRD_OPERAND),
    MOV(move(),                             INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, ADDRESSING_OFFSET),
    STR_I(loadStoreImm8(0),        "STR",  INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, ADDRESSING_OFFSET),
    STR_R(loadStoreImm8R(0),       "STR",  INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, ADDRESSING_REGISTER, ADDRESSING_OFFSET_REGISTER),
    LDR_I(loadStoreImm8(1),        "LDR",  INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, ADDRESSING_OFFSET),
    LDR_R(loadStoreImm8R(1),       "LDR",  INSTRUCTION_GROUP, FIRST_OPERAND_GROUP, ADDRESSING_REGISTER, ADDRESSING_OFFSET_REGISTER),
    B(m -> "11011110",      BRANCH, "B",    INSTRUCTION_GROUP, BRANCH_LABEL_GROUP),
    BC(branchConditional(), BRANCH, "B",    INSTRUCTION_GROUP, CONDITION_GROUP, BRANCH_LABEL_GROUP),
    UNKNOWN_INSTRUCTION(m -> String.format("%16s", " "));

    private String name;
    private InstructionType type;
    private int[] requiredGroup;
    private Opcode opcode;

    Instructions(Opcode opcode, int... requiredGroup)
    {
        this.name = name();
        this.type = NULL;
        this.opcode = opcode;
        this.requiredGroup = requiredGroup;
    }

    Instructions(Opcode opcode, String name, int... requiredGroup)
    {
        this(opcode, requiredGroup);
        this.name = name;
    }

    Instructions(Opcode opcode, InstructionType type, String name, int... requiredGroup)
    {
        this(opcode, name, requiredGroup);
        this.type = type;
    }

    public String getOpcode(Matcher matcher)
    {
        return opcode.match(matcher).replace(' ', '0');
    }

    public InstructionType getType()
    {
        return type;
    }

    @Override
    public String toString()
    {
        return name;
    }

    /**
     * Renvoie l'instruction qui est indentifier par son nom et
     * le tuple contenant les groupes necessairement non null pour que l'instruction soit valide.
     *
     * @param value le nom de l'instruction
     * @param requiredGroup le tableau des groupes necessairement non null
     * @return l'instruction associee ou {@link main.Instructions#UNKNOWN_INSTRUCTION UNKNOW_INSTRUCTION} si l'instruction n'existe pas
     */
    public static Instructions getEnum(String value, int... requiredGroup)
    {
        List<Instructions> list = Arrays.asList(Instructions.values());
        return list.stream().filter(inst -> inst.match(value, requiredGroup)).findAny().orElse(UNKNOWN_INSTRUCTION);
    }

    public static Instructions[] ofType(InstructionType type)
    {
        List<Instructions> list = Arrays.asList(Instructions.values());
        return list.stream().filter(inst -> inst.type == type).toArray(Instructions[]::new);
    }

    private boolean match(String value, int[] requiredGroup)
    {
        return name.equalsIgnoreCase(value) && Arrays.equals(this.requiredGroup, requiredGroup);
    }

    private static Opcode dataProcess(int ordinal)
    {
        return m -> String.format("010000%4s%3s%3s", toBinaryString(ordinal), toBinaryString(Integer.valueOf(m.group(SECOND_OPERAND_GROUP))), toBinaryString(Integer.valueOf(m.group(FIRST_OPERAND_GROUP))));
    }

    private static Opcode multi()
    {
        return m -> String.format("0100001101%3s%3s", toBinaryString(Integer.valueOf(m.group(FIRST_OPERAND_GROUP))), toBinaryString(Integer.valueOf(m.group(SECOND_OPERAND_GROUP))));
    }

    private static Opcode multiWithRd()
    {
        return m -> String.format("0100001101%3s%s", toBinaryString(Integer.valueOf(m.group(SECOND_OPERAND_GROUP))), toBinaryString(Integer.valueOf(m.group(THIRD_OPERAND))));
    }

    private static Opcode shift(int ordinal)
    {
        return m -> String.format("000%2s%5s%3s%3s", toBinaryString(ordinal), toBinaryString(Integer.valueOf(m.group(TWO_OPERAND_OFFSET))), toBinaryString(Integer.valueOf(m.group(SECOND_OPERAND_GROUP))), toBinaryString(Integer.valueOf(m.group(FIRST_OPERAND_GROUP))));
    }

    private static Opcode addSub(int ordinal)
    {
        return m -> String.format("00011%2s%3s%3s%3s", toBinaryString(ordinal), toBinaryString(Integer.valueOf(m.group(THIRD_OPERAND))), toBinaryString(Integer.valueOf(m.group(SECOND_OPERAND_GROUP))), toBinaryString(Integer.valueOf(m.group(FIRST_OPERAND_GROUP))));
    }

    private static Opcode addImmediate()
    {
        return m -> String.format("0001110%3s%3s%3s", toBinaryString(Integer.valueOf(m.group(TWO_OPERAND_OFFSET))), toBinaryString(Integer.valueOf(m.group(SECOND_OPERAND_GROUP))), toBinaryString(Integer.valueOf(m.group(FIRST_OPERAND_GROUP))));
    }

    private static Opcode move()
    {
        return m -> String.format("00100%3s%8s", toBinaryString(Integer.valueOf(m.group(FIRST_OPERAND_GROUP))), toBinaryString(Integer.valueOf(m.group(ADDRESSING_OFFSET))));
    }

    private static Opcode loadStore(int ordinal)
    {
        return m -> String.format("0110%s%5s%3s%3s", toBinaryString(ordinal), toBinaryString(Integer.valueOf(m.group(ADDRESSING_OFFSET))), toBinaryString(Integer.valueOf(m.group(FIRST_OPERAND_GROUP))), toBinaryString(Integer.valueOf(m.group(FIRST_OPERAND_GROUP))));
    }

    private static Opcode loadStoreWithRegister(int ordinal)
    {
        return m -> String.format("0110%s%5s%3s%3s", toBinaryString(ordinal), toBinaryString(Integer.valueOf(m.group(ADDRESSING_OFFSET_REGISTER))), toBinaryString(Integer.valueOf(m.group(ADDRESSING_REGISTER))), toBinaryString(Integer.valueOf(m.group(FIRST_OPERAND_GROUP))));
    }

    private static Opcode loadStoreImm8(int ordinal)
    {
        return m -> String.format("1001%s%3s%8s", toBinaryString(ordinal), toBinaryString(Integer.valueOf(m.group(FIRST_OPERAND_GROUP))), toBinaryString(Integer.valueOf(m.group(ADDRESSING_OFFSET))));
    }

    private static Opcode loadStoreImm8R(int ordinal)
    {
        return m -> String.format("1001%s%3s%8s", toBinaryString(ordinal), toBinaryString(Integer.valueOf(m.group(FIRST_OPERAND_GROUP))), toBinaryString(Integer.valueOf(m.group(ADDRESSING_OFFSET_REGISTER))));
    }

    private static Opcode branchConditional()
    {
        return m -> String.format("1101%4s", toBinaryString(Condition.valueOf(m.group(CONDITION_GROUP).toUpperCase()).ordinal()));
    }
}
