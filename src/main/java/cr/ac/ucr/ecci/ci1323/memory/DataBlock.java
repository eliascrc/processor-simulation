package cr.ac.ucr.ecci.ci1323.memory;

public class DataBlock {

    private int[] words;


    DataBlock(int[] words) {
        this.words = words;
    }

    public int[] getWords() {
        return this.words;
    }

    public int getWord(int index) {
        return this.words[index];
    }

}
