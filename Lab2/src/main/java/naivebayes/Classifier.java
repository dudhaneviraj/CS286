package naivebayes;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class Classifier implements Serializable {
    Map<String, Category> categories = new HashMap<>();
    Map<String, Integer> wordCountMap = new HashMap<>();
    double documentCount = 0;

    public void addTrainingDocument(String input, String category) {
        documentCount++;
        if (categories.containsKey(category))
            categories.get(category).addTrainingDocument(input);
        else {
            Category cat = new Category(category);
            categories.put(category, cat);
            cat.addTrainingDocument(input);
        }
        List<Document> documentList = categories.get(category).getDocuments();
        Document document = documentList.get(documentList.size() - 1);
        updateWordCountMap(document.getWordsMap());
    }


    public void addTrainingDataFile(Path path, String category) throws Exception {
        documentCount = documentCount + Files.lines(path).count();
        if (categories.containsKey(category))
            categories.get(category).addTrainingDocuments(path);
        else {
            Category cat = new Category(category);
            categories.put(category, cat);
            cat.addTrainingDocuments(path);
        }
        List<Document> documentList = categories.get(category).getDocuments();
        for (int i = documentList.size() - 1 - (int) Files.lines(path).count(); i < documentList.size(); i++) {
            Document document = documentList.get(i);
            updateWordCountMap(document.getWordsMap());
        }
    }


    public void updateWordCountMap(HashMap<String, Integer> wordsMap) {
        for (String word : wordsMap.keySet()) {
            if (wordCountMap.containsKey(word))
                wordCountMap.put(word, wordCountMap.get(word) + wordsMap.get(word));
            else
                wordCountMap.put(word, wordsMap.get(word));
        }
    }


    public String predict(String input) {
        if (input == null || input.length() == 0)
            return new String();
        String result = null;
        double max = Double.MIN_VALUE;
        List<String> inputList = getRelevantWords(input);
        for (String categoryName : categories.keySet()) {
            Category category = categories.get(categoryName);
            double temp = testCategory(inputList, category);
            if (temp > max) {
                result = categoryName;
                max = temp;
            }
        }
        return result;
    }

    public double getDocumentCount() {
        return documentCount;
    }

    public double testCategory(List<String> list, Category category) {
        double pCategory = (category.getDocumentCount() / getDocumentCount());
        double total = pCategory;
        for (String word : list)
            total *= ((double) (category.getWordCount(word) + 1)) / (category.getTotalWordCount() + getWordCount());
        return total;
    }

    public double getWordCount() {
        return (double) wordCountMap.size();
    }

    public List<String> getRelevantWords(String input) {
        StringBuilder builder = new StringBuilder();
        for (char ch : input.toCharArray())
            if (Character.isAlphabetic(ch) || ch == ' ')
                builder.append(Character.toLowerCase(ch));
        List<String> list = new ArrayList<>();
        for (String temp : builder.toString().split(" ")) {
            temp = temp.trim();
            if (temp.length() > 0)
                list.add(temp);
        }
        return list;
    }

    public void addCategory(String category) {
        if (!categories.containsKey(category))
            categories.put(category, new Category(category));
    }

    public Set<String> getCategories() {
        return categories.keySet();
    }
}