package cr.ac.ucr.ecci.ci1323.memory;

public class InstructionBus extends Bus {

    InstructionBlock[] instructionMemory;

    public InstructionBus() {
        super();
    }

    public InstructionBlock getInstructionBlock(int index) {
        return instructionMemory[index];
    }

    public void setInstructionBlock(int index, InstructionBlock instructionBlock) {
        this.instructionMemory[index] = instructionBlock;
    }

    public void setInstructionMemory(InstructionBlock[] instructionMemory) {
        this.instructionMemory = instructionMemory;
    }

    public void printInstructionMemory() {
        System.out.println(instructionMemory.length);
        for (int i = 0; i < instructionMemory.length; i++) {
            this.instructionMemory[i].printBlock();
        }
    }
}
