package top.nowandfuture.mod.imagesign.caches;


import java.util.*;

/**
 * This class is a Tree to spilt the images to different chunks with same size, or the class function is closer to
 *
 *
 */

public class ChunkTree {

    private List<MyChunk<ImageEntity, Entry<ImageEntity>>> imageChunks;

    public static void main(String[] args) {
        Deque deque = new ArrayDeque();
        deque.removeLast();

        Set a = new HashSet();

    }

    public int lengthOfLongestSubstring(String s) {
        Set<Character> contains = new HashSet();
        Deque<Character> queue = new ArrayDeque();
        int maxLength = 0;
        for(int i = 0; i < s.length(); i++){
            char c = s.charAt(i);
            if(contains.contains(c)){
                while(!queue.isEmpty()){
                    char f = queue.removeLast();
                    contains.remove(f);
                    if(f == c){
                        break;
                    }
                }
            }else{
                contains.add(c);
                queue.offerFirst(c);
                maxLength = Math.max(maxLength, queue.size());
            }

        }

        return maxLength;

    }

    public static class MyChunk<S, T extends Entry<S>>{
        private int edgeLength;
        private Vector3i pos;
        private List<T> entryList;

        public MyChunk(int edgeLength, Vector3i pos){
            this.edgeLength = edgeLength;
            this.pos = pos;
            entryList = new ArrayList<>();
        }

        public void addEntry(T entry){
            entryList.add(entry);
        }

        public void removeEntry(T entry){
            entryList.remove(entry);
        }

        public void clear(){
            entryList.clear();
        }

        public boolean contains(T t){
            return entryList.contains(t);
        }
    }

    public static class Entry<T>{
        private T data;

        public Entry(T data){
            this.data = data;

        }
    }


}
