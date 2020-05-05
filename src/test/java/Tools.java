import org.junit.Test;
import paths.Util;

import java.io.File;

import static load.GraphIO.mapsDir;

public class Tools {

    @Test
    public void fixFileNames() {
        File[] folders = new File(mapsDir).listFiles(File::isDirectory);
        if (folders == null) {
            return;
        }
        for (File folder : folders) {
            File[] legalFiles = folder.listFiles(file -> {
                boolean containsSCC = file.getName().contains("-scc");
                boolean containsGraph = file.getName().contains("-graph");
                boolean containsTMP = file.getName().contains(".tmp");
                return !containsSCC && !containsGraph && containsTMP;
            });
            if (legalFiles == null) {
                continue;
            }
            for (File file : legalFiles) {
                String name = file.getName();
                int insertPoint = name.indexOf("latest") + 6;
                String newName =  name.substring(0, insertPoint) + "-scc" + name.substring(insertPoint);
                File dest = new File(mapsDir + folder.getName() + "\\" + newName);
                System.out.println(file.renameTo(dest));
            }
        }
    }
}
