package Dictionary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.StringUtils;

public class DictionaryReducer extends Reducer<Text, Text, Text, Text> {
	
		
	
	HashSet<String> languages = new HashSet<>();

	public void setup(Context context) {
		// TODO determine the language of the current file by looking at it's
		// name
		languages.add("french");
		languages.add("german");
		languages.add("italian");
		languages.add("portuguese");
		languages.add("spanish");
	}

	public void reduce(Text word, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		// TODO iterate through values, parse, transform, and write to context
		List<String> words=format(values);
		Collections.sort(words);
		String joined = StringUtils.join("|", words);
		context.write(word, new Text(joined));
	}
	
	public List<String> format(Iterable<Text> values)
	{
		HashMap<String, ArrayList> map =new HashMap<>();
		HashSet<String> foundTranslations=new HashSet<>();
		for(Text t:values)
		{
			String[] temp =t.toString().split(":");
			if(!map.containsKey(temp[0])){
				ArrayList<String> val=new ArrayList<>();
				val.add(temp[1]);
				map.put(temp[0],val);
				foundTranslations.add(temp[0]);
				continue;
			}
			map.get(temp[0]).add(temp[1]);
		}

		List<String> result=new ArrayList<>();
		for(String key:map.keySet())
			result.add(key+":"+StringUtils.join(",", map.get(key)));
		for(String s:languages)
			if(!foundTranslations.contains(s))
				result.add(s+":N/A");
		return result;
	}
	
}
