package cr.ac.ucr.ecci.ci1323.controller.parser;

import cr.ac.ucr.ecci.ci1323.context.Context;
import cr.ac.ucr.ecci.ci1323.context.ContextQueue;
import cr.ac.ucr.ecci.ci1323.memory.Instruction;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBlock;
import cr.ac.ucr.ecci.ci1323.memory.InstructionBus;
import cr.ac.ucr.ecci.ci1323.support.InvalidInstructionException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FileParser {

    List<File> files;
    ContextQueue contextQueue;
    InstructionBus instructionBus;
    private static final int INSTRUCTIONS_START = 16 * 24;

    public FileParser(ContextQueue contextQueue, InstructionBus instructionBus) {
        this.contextQueue = contextQueue;
        this.instructionBus = instructionBus;
        boolean fileExists = true;
        this.files = new ArrayList<File>();
        for (int i = 0; fileExists; i++) {
            ClassLoader classLoader = getClass().getClassLoader();
            fileExists = (classLoader.getResource(i + ".txt") != null);

            if (fileExists) {
                File file = new File(classLoader.getResource(i + ".txt").getFile());
                this.files.add(file);
            }
        }
    }

    public void prepareSimulation() {
        List<String> instructions = this.readFiles();

        Instruction[] instructionBlockArray = new Instruction[4];
        InstructionBlock[] instructionMemory = new InstructionBlock[(int) Math.ceil((double) instructions.size() / 4)];
        int instructionMemoryIndex = 0;
        int instructionBlockIndex = 0;
        for (String instructionString : instructions) {
            instructionBlockArray[instructionBlockIndex % 4] = this.parseInstruction(instructionString);
            instructionBlockIndex++;
            if(instructionBlockIndex % 4 == 0) {
                InstructionBlock instructionBlock = new InstructionBlock(instructionBlockArray);
                instructionMemory[instructionMemoryIndex] = instructionBlock;
                instructionBlockArray = new Instruction[4];
                instructionMemoryIndex++;
            }
        }

        if(instructionMemoryIndex == instructionMemory.length - 1) {
            instructionMemory[instructionMemoryIndex] = new InstructionBlock(instructionBlockArray);
        }

        this.instructionBus.setInstructionMemory(instructionMemory);
    }

    private List<String> readFiles() {
        List<String> lines = new LinkedList<String>();

        int programCounterIndex = INSTRUCTIONS_START;
        int contextNumber = 0;
        for (File file: files) {
            Context context = new Context(programCounterIndex, contextNumber);
            List<String> newLines = getLinesFromFile(file);
            lines.addAll(newLines);
            programCounterIndex += 4 * newLines.size();
            this.contextQueue.pushContext(context);
            contextNumber++;
        }

        return lines;
    }

    private List<String> getLinesFromFile(File file) {
        List<String> fileLines = new LinkedList<String>();

        try {
            fileLines = FileUtils.readLines(file);
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
