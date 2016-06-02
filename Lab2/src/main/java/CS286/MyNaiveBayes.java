package CS286;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


class Classifier implements Serializable {
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
//        List<Document> documentList = categories.get(category).getDocuments();
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


public class MyNaiveBayes implements Serializable{

    static Classifier classifier=new Classifier();



    public static void predict(String modelPath,String path) {
        readObject(modelPath);
        try {
            if (Files.isRegularFile(Paths.get(path))) {
                int spamCount = Files.lines(Paths.get(path)).mapToInt(line -> {
                    if (line.toLowerCase().contains("subject:"))
                        if (classifier.predict(line.split(":", 2)[1]).equals("SPAM"))
                            return 1;
                    return 0;
                }).sum();

                int hamCount = Files.lines(Paths.get(path)).mapToInt(line -> {
                    if (line.toLowerCase().contains("subject:"))
                        if (classifier.predict(line.split(":", 2)[1]).equals("HAM"))
                            return 1;
                    return 0;
                }).sum();
                if (hamCount > spamCount)
                    System.out.println("classify=ham");
                else
                    System.out.println("classify=spam");
            }
        } catch (Exception e) {
        }

    }
    public static void saveModel(String directory)
    {
        try
        {
            File yourFile = (directory!=null && directory.length()!=0)?new File(directory+"/Object.ser"):new File("Object.ser");
            if(!yourFile.exists())
                yourFile.createNewFile();
            FileOutputStream fout = (directory!=null && directory.length()!=0)?new FileOutputStream(directory+"/Object.ser",false):new FileOutputStream("Object.ser",false);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(classifier);
            oos.close();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void readObject(String directory)
    {
        try
        {
            FileInputStream fin = (directory!=null && directory.length()!=0)?new FileInputStream(directory+"/Object.ser"):new FileInputStream("Object.ser");
            ObjectInputStream ois = new ObjectInputStream(fin);
            classifier = (Classifier) ois.readObject();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }


    public static void predictAccuracy(String hamDir, String spamDir) throws Exception {
        int rightCount = getMatches(hamDir, "HAM") + getMatches(spamDir, "SPAM");
        int count = getLineCount(hamDir) + getLineCount(spamDir);
        System.out.println("accuracy=" + (double) rightCount * 100 / (double) count + "%");
    }


    public static int getMatches(String dir, String category) {
        Integer rightCount = 0;
        try {
            rightCount = Files
                    .walk(Paths.get(dir)).parallel()
                    .mapToInt(filePath -> {
                        try {
                            if (Files.isRegularFile(filePath)) {
                                return Files.lines(filePath).mapToInt(line -> {
                                    try {
                                        if (line.toLowerCase().contains("subject:"))
                                            if (classifier.predict(line.split(":", 2)[1]).equals(category))
                                                return 1;
                                    } catch (Exception e) {
                                    }
                                    return 0;
                                }).sum();
                            }
                            return 0;
                        } catch (Exception e) {
                            return 0;
                        }
                    }).sum();
        } catch (Exception e) {
        }
        return rightCount;
    }


    public static int getLineCount(String dir) {
        try {
            return Files
                    .walk(Paths.get(dir)).parallel()
                    .mapToInt(filePath -> {
                        try {
                            if (Files.isRegularFile(filePath)) {
                                return (int) Files.lines(filePath).filter(line -> line.toLowerCase().contains("subject:")).count();
                            }
                            return 0;
                        } catch (Exception e) {
                            return 0;
                        }
                    }).sum();
        } catch (Exception e) {
        }
        return 0;
    }

    public static void build(String hamDir, String spamDir, String modelDir) throws Exception {
        addTrainingDocuments(hamDir, spamDir);
        predictAccuracy(hamDir, spamDir);
        saveModel(modelDir);
    }


    public static void addTrainingDocuments(String hamDir, String spamDir) throws Exception {
        Files.walk(Paths.get(spamDir)).forEach(filePath -> {
            try {
                if (Files.isRegularFile(filePath))
                    classifier.addTrainingDataFile(Paths.get(filePath.toUri()), "SPAM");
            } catch (Exception e) {
            }
        });
        Files.walk(Paths.get(hamDir)).forEach(filePath -> {
            try {
                if (Files.isRegularFile(filePath))
                    classifier.addTrainingDataFile(Paths.get(filePath.toUri()), "HAM");
            } catch (Exception e) {
            }
        });
    }

}
class Category implements Serializable {

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




class Document implements Serializable {
    HashMap<String, Integer> wordsMap = new HashMap<>();

    public void addDocument(String input) {
        StringBuilder builder = new StringBuilder();
        for (char ch : input.toCharArray())
            if (Character.isAlphabetic(ch) || ch == ' ')
                builder.append(Character.toLowerCase(ch));

        String[] inputList = builder.toString().split(" ");

        for (String temp : inputList) {
            temp = temp.trim();
            if (temp.length() == 0)
                continue;
            if (wordsMap.containsKey(temp))
                wordsMap.put(temp, wordsMap.get(temp) + 1);
            else
                wordsMap.put(temp, 1);
        }
    }


    public HashMap<String, Integer> getWordsMap() {
        return wordsMap;
    }
}