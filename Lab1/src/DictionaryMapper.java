package Dictionary;

import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class DictionaryMapper extends Mapper<Text, Text, Text, Text> {
	// TODO define class variables for translation, language, and file name

	HashSet<String> speech = new HashSet<>();

	public void setup(Context context) {
		// TODO determine the language of the current file by looking at it's
		// name
		speech.add("Noun");
		speech.add("Pronoun");
		speech.add("Verb");
		speech.add("Adverb");	
		speech.add("Adjective");
		speech.add("Conjunction");
		speech.add("Preposition");
	}

	public String matchPattern(String text)
	{
		Pattern p = Pattern.compile("\\[(.*?)\\]");
		Matcher m = p.matcher(text);
		String pattern=null;
		int count=0;
		while(m.find()) 
		{    if(speech.contains(m.group(1)))
		    	pattern=m.group(1);
			count++;
		}	
		if(count>1)
			return null;
		return pattern;
	}
	public void map(Text key, Text value, Context context) throws IOException, InterruptedException {
		// TODO instantiate a tokenizer based on a regex for the file
		if (key == null || value == null)
			return;
		if (key.toString().startsWith("#"))
			return;
		String partOfSpeech=matchPattern(value.toString());
		if(partOfSpeech==null)
			return;
		Path filePath = ((FileSplit) context.getInputSplit()).getPath();
		String filePathString = ((FileSplit) context.getInputSplit()).getPath().toString();
		String fileName = ((FileSplit) context.getInputSplit()).getPath().getName();
		String val= fileName.split("\\.")[0] + ":" + value.toString().replaceAll("\\["+partOfSpeech+"\\]", "").replaceAll(";", ",");
		context.write(new Text(key.toString()+": "+"["+partOfSpeech+"]"), new Text(val));
	}
}
