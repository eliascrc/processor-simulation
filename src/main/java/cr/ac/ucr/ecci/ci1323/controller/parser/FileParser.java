package cr.ac.ucr.ecci.ci1323.controller.parser;

import cr.ac.ucr.ecci.ci1323.commons.SimulationConstants;
import cr.ac.ucr.ecci.ci1323.context.Context;
import cr.ac.ucr.ecci.ci1323.context.ContextQueue;
import cr.ac.ucr.ecci.ci1323.memory.Instruction;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBlock;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBus;
import cr.ac.ucr.ecci.ci1323.exceptions.InvalidInstructionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FileParser {

    private List<InputStream> files;
    private ContextQueue contextQueue;
    private InstructionBus instructionBus;

    public FileParser(ContextQueue contextQueue, InstructionBus instructionBus) {
        this.contextQueue = contextQueue;
        this.instructionBus = instructionBus;
        boolean fileExists = true;
        this.files = new ArrayList<>();
        for (int i = 0; fileExists; i++) {
            ClassLoader classLoader = getClass().getClassLoader();
            fileExists = (classLoader.getResource(i + ".txt") != null);

            if (fileExists) {
                InputStream file = classLoader.getResourceAsStream(i + ".txt");
                this.files.add(file);
            }
        }
    }

    public void prepareSimulation() {
        List<String> instructions = this.readFiles();

        Instruction[] instructionBlockArray = new Instruction[SimulationConstants.TOTAL_INSTRUCTION_FIELDS];
        InstructionBlock[] instructionMemory = this.instructionBus.getInstructionMemory();
        int instructionMemoryIndex = 0;
        int instructionBlockIndex = 0;
        int neededInstructionBlocks = (int) Math.ceil((double) instructions.size() / SimulationConstants.INSTRUCTIONS_PER_BLOCK);
        for (String instructionString : instructions) {
            instructionBlockArray[instructionBlockIndex % 4] = this.parseInstruction(instructionString);
            instructionBlockIndex++;
            if(instructionBlockIndex % SimulationConstants.INSTRUCTIONS_PER_BLOCK == 0) {
                InstructionBlock instructionBlock = new InstructionBlock(instructionBlockArray);
                instructionMemory[instructionMemoryIndex] = instructionBlock;
                instructionBlockArray = new Instruction[SimulationConstants.TOTAL_INSTRUCTION_FIELDS];
                instructionMemoryIndex++;
            }
        }

        if(instructionMemoryIndex == neededInstructionBlocks - 1) {
            instructionMemory[instructionMemoryIndex] = new InstructionBlock(instructionBlockArray);
        }

        this.instructionBus.setInstructionMemory(instructionMemory);
    }

    private List<String> readFiles() {
        List<String> lines = new LinkedList<>();

        int programCounterIndex = SimulationConstants.INSTRUCTIONS_START;
        int contextNumber = 0;
        for (InputStream file: files) {
            Context context = new Context(programCounterIndex, contextNumber);
            List<String> newLines = getLinesFromFile(file);
            lines.addAll(newLines);
                programCounterIndex += SimulationConstants.WORD_SIZE * newLines.size();
            this.contextQueue.pushContext(context);
            contextNumber++;
        }

        return lines;
    }

    private List<String> getLinesFromFile(InputStream file) {
        List<String> fileLines = new LinkedList<>();

        try {
            fileLines = IOUtils.readLines(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileLines;
    }

    private int[] stringsToInts(String[] strings) {
        int [] ints = new  int[strings.length];
        for (int i = 0; i < strings.length; i++)
            ints[i] = Integer.parseInt(strings[i]);

        return ints;
    }

    private Instruction parseInstruction(String fileLine) {
        int[] instructionFields = stringsToInts(fileLine.split(" "));
        if (instructionFields.length != 4)
            throw new InvalidInstructionException("Instruction from file has " + instructionFields.length + " fields");

        return new Instruction(instructionFields);
    }
}
