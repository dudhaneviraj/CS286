package CS286;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class Main {

    public static void main(String[] args) throws Exception{

        Logger.getGlobal().getHandlers();
        for(Handler h:Logger.getGlobal().getHandlers())
            h.setLevel(Level.SEVERE);
        if(Files.exists(Paths.get(args[2])))
        Files.delete(Paths.get(args[2]));
        Files.createDirectories(Paths.get(args[2]));

        JavaNaiveBayes.build(args[0],args[1],args[2]);
        JavaNaiveBayes.predict(args[2],args[3]);

        MyNaiveBayes.build(args[0], args[1], args[2]);
        MyNaiveBayes.predict(args[2],args[3]);

    }
}