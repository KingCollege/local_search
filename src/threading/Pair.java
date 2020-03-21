package javaff.threading;

public class Pair<K, V> {
    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {return key;}
    public V getValue() {return value;}
    public void setValue(V v) {value = v;}

    public String toString() {
        return key + ":" + value;
    }
}