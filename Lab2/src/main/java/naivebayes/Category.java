package naivebayes;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Category implements Serializable {

    String name;
    Map<String, Integer> wordCountMap = new HashMap<>();
    List<Document> documents = new ArrayList<>();

    public Category(String name) {
        this.name = name;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public int getWordCount(String word) {
        if (wordCountMap.containsKey(word))
            return wordCountMap.get(word);
        return 0;
    }

    public void addTrainingDocument(String input) {
        Document document = new Document();
        document.addDocument(input);
        documents.add(document);
        updateWordCountMap(document.getWordsMap());
    }

    public void addTrainingDocuments(Path path) throws IOException {
        Files.lines(path).forEach(input -> {
            if (input.toLowerCase().contains("subject:"))
                addTrainingDocument(input.split(":", 2)[1]);
        });
    }

    public Double getDocumentCount() {
        return Double.parseDouble(String.valueOf(documents.size()));
    }

    public void updateWordCountMap(HashMap<String, Integer> wordsMap) {
        for (String word : wordsMap.keySet()) {
            if (wordCountMap.containsKey(word))
                wordCountMap.put(word, wordCountMap.get(word) + wordsMap.get(word));
            else
                wordCountMap.put(word, wordsMap.get(word));
        }
    }

    public void addWord(String word) {
        if (wordCountMap.containsKey(word))
            wordCountMap.put(word, wordCountMap.get(word) + 1);
        else
            wordCountMap.put(word, 1);
    }

    public String getName() {
        return name;
    }

    public double getTotalWordCount() {
        double count = 0;
        count = (double) wordCountMap.entrySet().stream().map(p -> p.getValue()).collect(Collectors.summingInt(Integer::intValue));
        return count;
    }

    public int getWordCount() {
        return wordCountMap.size();
    }

    public int hashCode() {
        int hash = 7;
        hash = hash * 31 + wordCountMap.hashCode();
        hash = hash * 31 + (name == null ? 0 : name.hashCode());
        return hash;
    }

    public boolean equals(Object o) {
        if ((o instanceof Category) && (((Category) o).getName().equals(this.getName())))
            return true;
        return false;
    }
}
