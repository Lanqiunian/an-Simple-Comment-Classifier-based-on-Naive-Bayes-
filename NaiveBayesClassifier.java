import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

public class NaiveBayesClassifier {


    /////Document类型，String label存标签，private List<String>存已分词的文档
    static class Document {
        private String label;
        private List<String> words;
        public Document(String label, List<String> words) {//把去除停用词附加在Document内
            String fileName = "stopwords.txt";
            List<String> stopWords = null;
            try {
                stopWords = Files.readAllLines(Paths.get(fileName));
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.label = label;
            this.words = new ArrayList<>();
            for (String word : words) {
                if (!stopWords.contains(word)) {
                    this.words.add(word);
                }
            }
        }

        public String getLabel() {
            return label;
        }

        public List<String> getWords() {
            return words;
        }
    }
    //把数据读入到Document
    private static List<Document> createDocuments(List<List<String>> dataSet) {
        List<Document> documents = new ArrayList<>();
        for (List<String> data : dataSet) {
            String label = data.get(0);
            List<String> words = new ArrayList<>();
            for (int i = 1; i < data.size(); i++) {
                words.add(data.get(i));
            }
            Document doc = new Document(label, words);
            documents.add(doc);
        }
        return documents;
    }


    private static class Classifier {

        private Map<String, Integer> labelCounts = new HashMap<>(); // 每个标签的文档数量
        private Map<String, Integer> wordCounts = new HashMap<>(); // 每个单词出现的总次数
        private Map<String, Map<String, Integer>> wordCountsByLabel = new HashMap<>(); // 每个标签中每个单词出现的次数
        private Map<String, Integer> WordinDoc = new HashMap<>(); // 有多少个文档中，出现这个单词

        private int TrainSize;//训练集文档数

        // 训练分类器
        public void train(List<Document> trainingSet) {

            TrainSize=trainingSet.size();
            //统计每个标签的文档数量、每个单词出现的文档数量、每个标签中每个单词出现的次数
            //TF=文档概率=某个单词在一个文档中出现的概率
            //IDF=log(1/文档倒排概率)=总共的文档数/每个单词出现的文档数量

            for (Document doc : trainingSet){

            }

            for (Document doc : trainingSet) {
                List<String> words=doc.words;

                Map<String, Boolean> CheckedorNot = new HashMap<>();//遍历一个文档时，检测这个单词是否已经加一，不要重复计数
                for(String sample : words){//用于统计每个单词出现的文档数量
                    CheckedorNot.put(sample,false);
                }

                for(String sample : words){
                    if(!CheckedorNot.get(sample)) {
                        WordinDoc.put(sample, WordinDoc.getOrDefault(sample, 0) + 1);
                        CheckedorNot.put(sample,true);
                    }
                }

                String label = doc.label;
                labelCounts.put(label, labelCounts.getOrDefault(label, 0) + 1);
                Set<String> uniqueWords = new HashSet<>(words);//忽略重复词，用set方便

                for (String word : uniqueWords) {
                    wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                    if (!wordCountsByLabel.containsKey(label)) {
                        wordCountsByLabel.put(label, new HashMap<>());
                    }
                    wordCountsByLabel.get(label).put(word, wordCountsByLabel.get(label).getOrDefault(word, 0) + 1);
                }
            }
        }

        // 测试分类器
        public void test(List<Document> testSet,double threshold) {
            int numCorrect = 0; // 正确分类的文档数量
            int numTotal = 0; // 总共的文档数量
            int realTure = 0;//正确预测出的正例，用于计算召回率
            Map<String, Integer> TestlabelCounts = new HashMap<>();
            for (Document doc : testSet) {
                String trueLabel = doc.label;
                List<String> words = doc.words;
                TestlabelCounts.put(trueLabel, TestlabelCounts.getOrDefault(trueLabel, 0) + 1);
                String predictedLabel = classify(words,threshold);
                /*System.out.println("评论："+doc.getWords()+
                        "\n属于："+doc.getLabel()+
                        " \n分类："+predictedLabel+
                        "\n----------------------------------------");*/
                if (predictedLabel.equals(trueLabel)) {
                    numCorrect++;
                    if(predictedLabel=="spam")
                        realTure++;
                }
                numTotal++;
            }
            double Precision=(double) numCorrect / numTotal;
            double Recall_Rate=(double)realTure/TestlabelCounts.get("spam");
            double F_Score=(double)2*Recall_Rate*Precision/(Recall_Rate+Precision);
            System.out.println("threshold:"+threshold+"\n测试集文档数："+numTotal+"\n准确率："+Precision+"\n召回率："+Recall_Rate+"\nF-Score:" +F_Score);

        }

        /**
         * 使用朴素贝叶斯进行分类
         * @param words 已经分词的文本列表
         * @return bestLabel 分类的结果
         */
        private String classify(List<String> words,double threshold) {
            double bestScore = Double.NEGATIVE_INFINITY;
            String bestLabel = null;
            List<String> LabelSet=new ArrayList<>();
            LabelSet.add("spam");
            LabelSet.add("ham");
            for (String label : LabelSet) {
                double score = Math.log(labelCounts.get(label)); // 先验概率,
                /*得分(score)这个指标是朴素贝叶斯算法中用来评估文档所属标签的概率的一个重要指标。在代码中，得分是用以下公式来计算的：
                score   += Math.log((count + 1.0) / (labelCounts.get(label) + wordCounts.size()));
                这个公式可以拆分成两部分，分别是条件概率和先验概率。
                先验概率(labelCounts.get(label))是指在整个训练集中，标签(label)出现的概率，也可以理解为该标签在整个训练集中出现的频率。
                条件概率((count + 1.0) / (labelCounts.get(label) + wordCounts.size()))是指在给定标签(label)的条件下，某个单词(word)出现的概率。这里采用了拉普拉斯平滑，将分子加上了1，分母加上了训练集中所有不同的单词数(wordCounts.size())，避免了概率为0的情况。
                在循环中，每次计算一个标签的得分，最后选择得分最高的标签作为文档的分类结果。
                这种计算得分的方式是朴素贝叶斯算法的一种常见实现方式，也被广泛使用。*/


                Map<List<String>, Map<String, Integer>> Num_of_Word_in_this_Doc = new HashMap<>();



                Num_of_Word_in_this_Doc.put(words,new HashMap<>());
                Map<List<String>,Integer> Length_of_this_Doc=new HashMap<>();


                for (String word : words){

                    //统计文档中，某词出现次数
                    Num_of_Word_in_this_Doc.get(words).put(word, Num_of_Word_in_this_Doc.get(words).getOrDefault(word, 0) + 1);

                    //统计此文档，有几个词
                    Length_of_this_Doc.put(words,Length_of_this_Doc.getOrDefault(words,0)+1);
                }
                for (String word : words) {

                    //IDF=log(总共的文档数/（每个单词出现的文档数量+1）
                    double IDF=0;
                    if(WordinDoc.get(word)==null)
                        IDF=Math.log(TrainSize);//防止测试集中出现训练集中未出现的词，导致读出null报错
                    else
                        IDF=Math.log((double)TrainSize/(WordinDoc.get(word)+1));

                    //TF=文档概率=某个单词在一个文档中出现的概率
                    double TF=(double)Num_of_Word_in_this_Doc.get(words).get(word)/(double)Length_of_this_Doc.get(words);
                    /*if(TF*IDF>0.1)
                    System.out.println(word+":"+TF*IDF);*/

                    if(TF*IDF>threshold){//阈值设置，只统计超过这一指标的词，减少噪声

                        // count表示训练集中该标签下包含当前单词的文档数量。
                        // labelCounts.get(label)表示训练集中该标签出现的文档数量。
                        // wordCounts.size()表示训练集中所有不同的单词数。
                        int count = wordCountsByLabel.getOrDefault(label, Collections.emptyMap()).getOrDefault(word, 0);
                        score += Math.log((count + 1.0) / (labelCounts.get(label) + wordCounts.size()));
                        // log P(word | label)条件概率,
                        // 这里采用了拉普拉斯平滑，将分子加上了1，分母加上了训练集中所有不同的单词数(wordCounts.size())，避免了概率为0的情况。
                    }
                }
                if (bestLabel == null || score > bestScore) {
                    bestScore = score;
                    bestLabel = label;
                }
            }
            return bestLabel;
        }
    }
    /*这段代码是实现朴素贝叶斯分类器的核心部分，接受一个文档的单词列表作为输入，返回最佳标签（bestLabel）。
首先，代码定义了一个LabelSet列表，其中包含所有可能的标签。然后，代码迭代所有标签，并计算每个标签的得分（score），并找到得分最高的标签（bestLabel）。
在每个标签的迭代过程中，代码计算了该标签的先验概率（Math.log(labelCounts.get(label))），即在训练集中该标签的文档数量除以总文档数。
然后，对于该标签下的每个单词，代码计算了:
条件概率=（Math.log((count + 1.0) / (labelCounts.get(label) + wordCounts.size()))）。
其中，count表示在训练集中该标签下包含当前单词的文档数量。labelCounts.get(label)表示在训练集中该标签出现的文档数量。
wordCounts.size()表示在训练集中所有不同的单词数。
最后，代码返回得分最高的标签（bestLabel）。
这段代码实现了朴素贝叶斯分类器的基本算法，它的核心思想是利用贝叶斯定理，通过计算文档单词出现的条件概率来估计文档所属的标签。 */

    public static void main(String[] args) {


        for (double threshold=0;threshold<=0.5;threshold=threshold+0.01){

            System.out.println("_____________进度"+threshold*200+"%\n____________________");

            List<String[]> Documents=DataSetSplitter.creatEmailDocument(DataSetSplitter.readFile("..\\full\\index"," " ));

            List<String[]> trainSet=new ArrayList<>();
            List<String[]> testSet=new ArrayList<>();
            double Ratio=0.3;//测试集划分比例

            DataSetSplitter.process(Documents,trainSet,testSet,Ratio);
     /*   for(String[] doc:trainSet)
            System.out.println("标签"+doc[0]+"内容"+doc[1]+"\n");
        for(String[] doc:testSet)
            System.out.println("标签"+doc[0]+"内容"+doc[1]+"\n"+testSet.size());*/

            //分词,存入Document,并滤除停用词
            List<Document> trainingDocs=createDocuments(DataSetSplitter.Segmenter(trainSet));
            List<Document> testingDocs=createDocuments(DataSetSplitter.Segmenter(testSet));

            Classifier emailClassifier = new Classifier();
            emailClassifier.train(trainingDocs);
            emailClassifier.test(testingDocs,threshold);
        }
    }
}
