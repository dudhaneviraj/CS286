package classification.spark.naivebayes;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.feature.HashingTF;
import org.apache.spark.ml.feature.IDF;
import org.apache.spark.ml.feature.IDFModel;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import scala.Tuple2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by viraj on 5/21/16.
 */
public class SparkNaiveBayes {
        public static void sparkNaiveBayes()throws IOException
        {

            SparkConf conf = new SparkConf().setMaster("local").setAppName("SPARK SQL Application");

            // Create a Java version of the Spark Context from the configuration
            JavaSparkContext context = new JavaSparkContext(conf);
            SQLContext sqlContext = new SQLContext(context);
            JavaRDD<LabeledPoint> data=getData(context,sqlContext,"src/main/resources/ham","src/main/resources/spam");

            JavaRDD<LabeledPoint>[] splits = data.randomSplit(new double[] { 0.8, 0.2 });
            JavaRDD<LabeledPoint> training = splits[0];
            JavaRDD<LabeledPoint> testData = splits[1];
            NaiveBayesModel model = NaiveBayes.train(training.rdd(), 1.0);
            System.out.println("Spark Naive Bayes Accuracy:"+predictAccuracy(testData,model));
        }




    public static double predictAccuracy(JavaRDD<LabeledPoint> data,NaiveBayesModel model)
    {
        JavaPairRDD<Double, Double> predictionAndLabel =data.mapToPair(p->{
            return new Tuple2<Double, Double>(model.predict(p.features()), p.label());
        });
        return predictionAndLabel.filter(p-> { return p._1().equals(p._2()); }).count()/(double)data.count();
    }


    public static JavaRDD<LabeledPoint> getData(JavaSparkContext context, SQLContext sqlContext,String hamPath,String spamPath)throws IOException
        {
            List<Row> rows=new ArrayList<>();
            Files.walk(Paths.get(spamPath)).forEach(filePath->{
                try{
                    if(Files.isRegularFile(filePath))
                    {
                        JavaRDD<String> dataFrame = context.textFile(filePath.toString());
                        rows.addAll(dataFrame.map(p->RowFactory.create(1.0,p)).collect());
                    }
                }
                catch(Exception e){e.printStackTrace();}
            });
            Files.walk(Paths.get(hamPath)).forEach(filePath->{
                try{
                    if(Files.isRegularFile(filePath))
                    {
                        JavaRDD<String> dataFrame = context.textFile(filePath.toString());
                        rows.addAll(dataFrame.map(p->RowFactory.create(0.0,p)).collect());
                    }
                }
                catch(Exception e){e.printStackTrace();}
            });
            JavaRDD<Row> rowsRdd=context.parallelize(rows);
            StructType schema = new StructType(new StructField[]{
                    new StructField("CATEGORY", DataTypes.DoubleType, false, Metadata.empty()),
                    new StructField("sentence", DataTypes.StringType, false, Metadata.empty())
            });
            DataFrame sentenceData = sqlContext.createDataFrame(rowsRdd, schema);
            Tokenizer tokenizer = new Tokenizer().setInputCol("sentence").setOutputCol("words");
            DataFrame wordsData = tokenizer.transform(sentenceData);
            int numFeatures = 20;
            HashingTF hashingTF = new HashingTF()
                    .setInputCol("words")
                    .setOutputCol("rawFeatures");
            DataFrame featurizedData = hashingTF.transform(wordsData);
            IDF idf = new IDF().setInputCol("rawFeatures").setOutputCol("features");
            IDFModel idfModel = idf.fit(featurizedData);
            DataFrame rescaledData = idfModel.transform(featurizedData);
            JavaRDD<Row> row = rescaledData.toJavaRDD();
            JavaRDD<LabeledPoint> parsedData=row.map(r->{
                return new LabeledPoint(r.getDouble(0),(Vector)r.get(4));
            });
            return parsedData;
        }


}
