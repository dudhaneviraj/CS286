package naivebayes;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

public class Document implements Serializable {
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

    public void addDocumentFile(File input) {
//
//		StringBuilder builder=new StringBuilder();
//		for(char ch:input.toCharArray())
//			if(Character.isAlphabetic(ch) || ch==' ')
//				builder.append(Character.toLowerCase(ch));
//
//		String[] inputList=builder.toString().split(" ");
//
//		for(String temp:inputList)
//		{
//			temp=temp.trim();
//			if(temp.length()==0)
//				continue;
//			if(wordsMap.containsKey(temp))
//				wordsMap.put(temp, wordsMap.get(temp)+1);
//			else
//				wordsMap.put(temp, 1);
//		}

    }


    public HashMap<String, Integer> getWordsMap() {
        return wordsMap;
    }
}
