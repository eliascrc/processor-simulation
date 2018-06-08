package main.java.cr.ac.ucr.ecci.ci1323.memory;

public class InstructionBlock {

    Instruction[] instructions;

    InstructionBlock(Instruction[] instructions) {
        this.instructions = instructions;
    }

    public Instruction getInstruction(int index) {
        return instructions[index];
    }
}
