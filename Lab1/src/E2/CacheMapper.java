package Dictionary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.filecache.DistributedCache;

public class CacheMapper extends Mapper<LongWritable, Text, Text, Text> {
	String fileName = null, language = null;
	public Map<String, String> translations = new HashMap<String, String>();
	HashSet<String> speech = new HashSet<>();

	public void setup(Context context) throws IOException, InterruptedException {
		// TODO: determine the name of the additional language based on the file
		// name
		// TODO: OPTIONAL: depends on your implementation -- create a HashMap of
		// translations (word, part of speech, translations) from output of
		// exercise 1
		speech.add("Noun");
		speech.add("Pronoun");
		speech.add("Verb");
		speech.add("Adverb");
		speech.add("Adjective");
		speech.add("Conjunction");
		speech.add("Preposition");

		try {
			Path[] cacheFilesLocal= DistributedCache.getLocalCacheFiles(context.getConfiguration());


			BufferedReader br = new BufferedReader(new FileReader(new File(cacheFilesLocal[0].toString())));
			String line = null;
			while ((line = br.readLine()) != null) {

				if(line.startsWith("#"))
					continue;
				String partOfSpeech = matchPattern(line);
				if (partOfSpeech == null)
					continue;
				try{
					String[] temp = line.split("\\t",2);

					translations.put(temp[0]+": ["+partOfSpeech+"]", temp[1].replaceAll("\\["+partOfSpeech+"\\]", ""));
				}
				catch(Exception e)
				{ 
					continue;
				}

			}
		} catch (Exception e) {
			// TODO: handle exception
		}
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

	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
		// TODO: perform a map-side join between the word/part-of-speech from
		// exercise 1 and the word/part-of-speech from the distributed cache
		// file

		// TODO: where there is a match from above, add language:translation to
		// the list of translations in the existing record (if no match, add
		// language:N/A
		String[] english=value.toString().split("\\t",2);		
		if (!translations.containsKey(english[0])) {
			context.write(new Text(value.toString()+"|latin:N/A"),new Text());
			return;
		}

		context.write(new Text(value.toString()+"|latin:" +translations.get(english[0])),new Text());

		//		
		/*for(String t: translations.keySet())
		{
			context.write(new Text(t+"Hello:"), new Text(translations.get(t)+"Hii"));
		}*/


	}

}
