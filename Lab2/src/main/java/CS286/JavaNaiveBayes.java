package CS286;

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
 * Created by viraj on 5/22/16.
 */
public class JavaNaiveBayes {
    static JavaSparkContext context;
    static SparkConf conf;
    static SQLContext sqlContext;


    public static void build(String hamDir, String spamDir, String modelDir) throws Exception {
        conf = new SparkConf().setMaster("local").setAppName("SPARK SQL Application");
        context = new JavaSparkContext(conf);
        sqlContext = new SQLContext(context);
        JavaRDD<LabeledPoint> data = getData(hamDir, spamDir);
        JavaRDD<LabeledPoint>[] splits = data.randomSplit(new double[]{0.8, 0.2});
        JavaRDD<LabeledPoint> training = splits[0];
        JavaRDD<LabeledPoint> testData = splits[1];
        final NaiveBayesModel model = NaiveBayes.train(training.rdd(), 1.0);
        // Save and load model
        model.save(context.sc(), modelDir);
        System.out.println("accuracy=" + predictAccuracy(model,testData) * 100 + " %");
    }


    public static void predict(String modelDir, String path) throws Exception {
        final NaiveBayesModel model = NaiveBayesModel.load(context.sc(), modelDir);
        checkCategory(model,path);

    }


    public static void checkCategory(NaiveBayesModel model,String path) throws Exception{
        JavaRDD<LabeledPoint> data=getDataFromFile(path);
        JavaPairRDD<Double, Double> predictionAndLabel = data.mapToPair(p -> {
            return new Tuple2<Double, Double>(model.predict(p.features()), p.label());
        });
        double hamCount= predictionAndLabel.filter(p -> {
            return p._1().toString().equals("0.0");
        }).count();

        double spamCount= predictionAndLabel.filter(p -> {
            return p._1().toString().equals("1.0");
        }).count();

        if(hamCount>spamCount)
            System.out.println("classify=ham");
        else
            System.out.println("classify=spam");
    }

    public static double predictAccuracy(NaiveBayesModel model,JavaRDD<LabeledPoint> data) {
        JavaPairRDD<Double, Double> predictionAndLabel = data.mapToPair(p -> {
            return new Tuple2<Double, Double>(model.predict(p.features()), p.label());
        });
        return predictionAndLabel.filter(p -> {
            return p._1().equals(p._2());
        }).count() / (double) data.count();
    }


    public static JavaRDD<LabeledPoint> getData(String hamPath, String spamPath) throws IOException {
        List<Row> rows = new ArrayList<>();
        Files.walk(Paths.get(spamPath)).forEach(filePath -> {
            try {
                if (Files.isRegularFile(filePath)) {
                    JavaRDD<String> dataFrame = context.textFile(filePath.toString());
                    rows.addAll(dataFrame.filter(p->p.toLowerCase().contains("subject:")).map(p -> RowFactory.create(1.0, p)).collect());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Files.walk(Paths.get(hamPath)).forEach(filePath -> {
            try {
                if (Files.isRegularFile(filePath)) {
                    JavaRDD<String> dataFrame = context.textFile(filePath.toString());
                    rows.addAll(dataFrame.map(p -> RowFactory.create(0.0, p)).collect());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        JavaRDD<Row> rowsRdd = context.parallelize(rows);
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
        JavaRDD<LabeledPoint> parsedData = row.map(r -> {
            return new LabeledPoint(r.getDouble(0), (Vector) r.get(4));
        });
        return parsedData;
    }


    public static JavaRDD<LabeledPoint> getDataFromFile(String filePath) throws IOException {
        List<Row> rows = new ArrayList<>();
        try {
            JavaRDD<String> dataFrame = context.textFile(filePath);
            rows.addAll(dataFrame.filter(p->p.toLowerCase().contains("subject:")).map(p -> RowFactory.create(0.0, p)).collect());
        } catch (Exception e) {
            e.printStackTrace();
        }
        JavaRDD<Row> rowsRdd = context.parallelize(rows);
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
        JavaRDD<LabeledPoint> parsedData = row.map(r -> {
            return new LabeledPoint(r.getDouble(0), (Vector) r.get(4));
        });
        return parsedData;
    }


}
