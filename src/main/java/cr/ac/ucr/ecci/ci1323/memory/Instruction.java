package cr.ac.ucr.ecci.ci1323.memory;

import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;

/**
 * Represents an instruction, which is composed of an array of integers.
 *
 * @author Josue Leon Sarkis, Elias Calderon, Daniel Montes de Oca
 */
public class Instruction {

    private volatile int[] instructionFields;

    public Instruction(int[] instructionFields) {
        this.instructionFields = instructionFields;
    }

    public int getField(int fieldNumber) {
        return this.instructionFields[fieldNumber];
    }

    public int getOperationCode() {
        return this.instructionFields[SimulationConstants.OPCODE_FIELD_NUMBER];
    }

    public int[] getInstructionFields() {
        return this.instructionFields;
    }

    public String toString() {
        return instructionFields[0] + "\t" + instructionFields[1]+ "\t" + instructionFields[2]+ "\t" + instructionFields[3];
    }
}
