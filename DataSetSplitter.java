import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;

import javax.xml.transform.Result;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
///划分训练集，分词

public class DataSetSplitter {

    public List<String[]> testSet = new ArrayList<String[]>();
    public List<String[]> trainSet = new ArrayList<String[]>();
    public List<List<String>> trainWords = new ArrayList<>();
    public List<List<String>> testWords = new ArrayList<>();

    /**
     * 将数据集按照给定比例划分为训练集和测试集
     * @param dataset 待划分的数据集
     * @param trainSet 存储训练集数据的列表
     * @param testSet 存储测试集数据的列表
     * @param Ratio 训练集的比例
     */
    public static void process(List<String[]> dataset,
                               List<String[]> trainSet,
                               List<String[]> testSet,double Ratio) {
        if (!(Ratio>0||Ratio<1)) {
            throw new IllegalArgumentException("训练集比例应该在0-1之间！");
        }
        // 划分测试集和训练集的比例,先算出大小
        int testSize = (int) (dataset.size() * Ratio);
        /*System.out.println("数据集文档数为：" + dataset.size());//64620*/

        // 打乱数据集的顺序
        Collections.shuffle(dataset);

        // 取就行了
        for (int i = 0; i < dataset.size(); i++) {
            if (i < testSize) {
                testSet.add(dataset.get(i));
            } else {
                trainSet.add(dataset.get(i));
            }
        }
    }

    /**
     *分词
     * @param EmailSet 所有的email，第一列标签第二列文档的格式储存在List<String[]>
     * @return SegmentedText 分词后的文档，第一列标签，后面是分词往后加就完事
     */
    //分词
    public static List<List<String>>Segmenter(List<String[]> EmailSet) {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<List<String>> SegmentedText =  new ArrayList<>();
        int i=0;
        for (String[] sample : EmailSet) {

            String text = sample[1];//先单独取出评论文本
            List<SegToken> tokens = segmenter.process(text, JiebaSegmenter.SegMode.SEARCH);
            //进行分词
            List<String> words = new ArrayList<>();
            words.add(0, sample[0]);//分完词再把标签加到分词后文档的第0项，避免标签被分词
            for (SegToken token : tokens) {
                String word = token.word;
                if (!word.trim().isEmpty()) {
                    words.add(word);
                }
            }
            SegmentedText.add(words);
        }
        return SegmentedText;
    }


    /**
     * 读取数据集的INDEX文件，将标签及邮件目录存在List<String[]>
     * @param indexFile
     * @return records
     */
    //读入index,每个String存标签和目录
    public static List<String[]> readINDEX(String indexFile){
        List<String[]> records = new ArrayList<String[]>();
        try (BufferedReader br = new BufferedReader(new FileReader(indexFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(" ");
                records.add(values);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }

    public static List<String[]> creatEmailDocument(List<String[]> Index){
        List<String[]> EmailDocument=new ArrayList<>();
        for(String[] doc:Index){
            String Label=doc[0];//获取邮件文件的内容,再写个单纯读邮件的readEmail,处理邮件文件。考虑主题收发人等问题，然后存到list
            String content=readEmail(doc[1]);
            /*System.out.println(doc[1]);//输出查看一下进度*/
            String[] Email=new String[2];
            Email[0]=Label;
            Email[1]=content;
            EmailDocument.add(Email);
        }
        for(String[] sample:EmailDocument)
            sample[1]=removeNonChinese(sample[1]);
        return EmailDocument;
    }

    /**
     * 保留String中的中文，其余清除
     * @param str
     * @return sb.toString()
     */
    public static String removeNonChinese(String str) {
        if (str == null || str.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }



    public static String readEmail(String dir){
        String content=new String();
        BufferedReader br = null;
        try {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(dir), "GB2312");
            br = new BufferedReader(isr);
               /* br = new BufferedReader(new FileReader(dir));*/
                String text = null;
                //读取内容为null则表示读到了文件末尾
                while ((text = br.readLine()) != null) {
                    content=content+text;
                    /*System.out.println(text);*/
                }
            } catch (IOException e) {
                e.printStackTrace();
        }
        return content;
    }

    public void showthem() {
        // 输出划分后的训练集和测试集
        System.out.println("训练集:");
        for (int i = 0; i < trainSet.size(); i++) {
            String[] row = trainSet.get(i);
            List<String> words = trainWords.get(i);
            System.out.println(row[0] + "," + String.join(" ", words));
        }

        System.out.println("测试集:");
        for (int i = 0; i < testSet.size(); i++) {
            String[] row = testSet.get(i);
            List<String> words = testWords.get(i);
            System.out.println(row[0] + "," + String.join(" ", words));
        }
    }


//按格式读取csv文件，分词号为,
    public static List<String[]> readFile(String csvFile,String SplitSign) {
        List<String[]> records = new ArrayList<String[]>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(SplitSign);
                records.add(values);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }

}



