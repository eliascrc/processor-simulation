package cr.ac.ucr.ecci.ci1323.memory;

import java.util.Arrays;

public class DataBlock implements Cloneable {

    private volatile int[] words;

    DataBlock(int[] words) {
        this.words = words;
    }

    @Override
    public DataBlock clone() {
        try {
            DataBlock dataBlockClone = (DataBlock) super.clone();
            dataBlockClone.setWords(Arrays.copyOf(this.words, this.words.length));
            return dataBlockClone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError("Unable to clone object of type [" + getClass().getName() + "]");
        }
    }

    public int[] getWords() {
        return this.words;
    }

    public int getWord(int index) {
        return this.words[index];
    }

    public void setWords(int[] words) {
        this.words = words;
    }
}
