import com.sun.javafx.geom.transform.Identity;

import java.io.*;
import java.util.*;

/**
 * 2009.txt 代表词干提取后的文档，前面加了original的代表原始文档
 */
public class Main {
    public static void main(String[] args)throws IOException{

        //将文档按照不同年份分离开
        //divide_year();

        String s = "2009.txt";
        //得到该txt文件所有帖子文档的TF-IDF特征
        double W[][] = TF_IDF(s);
        //System.out.println(W[1].length);
        //定义聚类的个数,从20开始,逐渐递减,
        int cluster_num = 4;
        //cluster用来存储对应文档属于那个类
        int cluster[]= clustering(W,cluster_num);
        //将属于不同类的文章写入不同的文档中
        //先清空原来的内容
        for(int i=0;i<cluster_num;i++){
            FileOutputStream out = new FileOutputStream(i+"_original_"+s,false);
            out.write(new String("").getBytes());
            out.close();
        }
        //将聚类结果,联系到原来的帖子文本,分开
        String filename = "original_"+s;
        divide_cluster(cluster,filename);

        // count_by_year();
    }

    //获取TF_IDF特征量
    public static double[][] TF_IDF(String year)throws IOException{
        FileInputStream fis = new FileInputStream(year);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);
        String s = "";
        String text[];
        ArrayList<String> wordlist = new ArrayList<>();//在该年份下，有多少中不同的单词
        int postnum=0;//该年份有多少帖子
        while((s=br.readLine())!=null){
            String set[] = s.split(":");
            if(set[0].equals("title")||set[0].equals("body")||set[0].equals("tags")){
                set[1] = set[1].replace("  "," ");
                text = set[1].split(" ");
                for(int i=0;i<text.length;i++){
                    if(!text[i].equals("")&&!wordlist.contains(text[i])){
                        wordlist.add(text[i]);
                    }
                }
            }
            if(set[0].equals("creationdate")){
                postnum++;
            }
        }

        double W[][] = new double[postnum][wordlist.size()];
        for(int i=0;i<W.length;i++){
            for(int j=0;j<W[i].length;j++){
                W[i][j]=0.0;
            }
        }

        FileInputStream fis1 = new FileInputStream(year);
        InputStreamReader isr1 = new InputStreamReader(fis1);
        BufferedReader br1 = new BufferedReader(isr1);
        int postcount=0;
        String words="";
        while((s=br1.readLine())!=null){
            if(s.equals("*****row*****")){
                words.replace("  "," ");//去除刚才一步可能产生的连续空格
                //System.out.println(words);
                String wordset[] = words.split(" ");
                for(int j=0;j<wordset.length;j++){
                    if(!wordset[j].equals(""))
                        W[postcount][wordlist.indexOf(wordset[j])] +=1;//在该单词相应位置加1
                }
                //对于该行归一化
                for(int i=0;i<W[postcount].length;i++){
                    W[postcount][i] = W[postcount][i]/wordset.length;//该单词的频率处以所有单词个数，即为TF值
                }
                postcount++;
                words="";
            }else {
                String set[] = s.split(":");
                if(set[0].equals("title")||set[0].equals("body")||set[0].equals("tags")){
                    words = words+" "+set[1];
                }
            }
        }
        System.out.println("TF值计算完毕");
        //求IDF值，遍历各个单词，计算该单词出现的文本个数，IDF＝ log(D/doc_freq(wj)）
        for(int j=0;j<W[0].length;j++){
            int doc_freq=0;
            for(int i=0;i<W.length;i++){
                if(W[i][j]>0.0){
                    doc_freq++;
                }
            }
            double idf = Math.log(W.length/(doc_freq+1));
            //计算W[i][j]= TD(j)*IDF(j)
            for(int i=0;i<W.length;i++){
                W[i][j] = W[i][j]*idf;
            }
            for(int i=0;i<W.length;i++){
                //System.out.println(W[i][7]);
            }

        }
        System.out.println("TF-IDF特征量计算完毕");
        return W;

        /*
        //预处理SA.txt
        File f = new File("SA_processed.txt");
        FileWriter fw = new FileWriter(f);
        while((s=br.readLine())!=null){
            String set[] = s.split(":");
            if(set[0].equals("Body")||set[0].equals("Title")){
                s = set[0]+":"+data_prepare(set[1]);
                //System.out.println(data_prepare(set[1]));
            }
            fw.write(s+"\r\n");
        }
        fw.flush();
        fw.close();
        */

    }

    //对给定的特征矩阵，进行聚类k-means
    public static int[] clustering(double W[][],int n) throws IOException {
        //初始化n个点，作为聚类的点
        //int n=4;
        int wordcount = W[0].length;
        double k[][] = new double[n][wordcount];
        Random r=new Random();


        //迭代求解每个文档对于这几个k值的距离，距离最近的代表他属于那个类
        int cluster[] = new int[W.length];//用来存储对应文档属于那个类
        for(int i=0;i<cluster.length;i++){
            cluster[i]=0;
        }
        int cluster1[] = new int[W.length];//用来存储对应文档属于那个类
        int post_num_cluster[] = new int[n];//记录每个类有多少个文档
        double distance[] = new double[n]; //保存文档距离kn的距离
        int count=0;
        int change=0;
        for(int i=0;i<n;i++){
            int q = r.nextInt(W.length);
            for(int j=0;j<k[i].length;j++){
                k[i][j]=W[q][j];
                //System.out.println(k[i][j]);
            }
        }
        do {
            count++;
            System.out.println("第 "+count+" 次迭代..");
            for(int i=0;i<cluster.length;i++){
                cluster1[i]=cluster[i];
            }
            change=0;
            for(int i=0;i<n;i++){
                post_num_cluster[i]=0;
            }

            for(int i=0;i<W.length;i++){
                for(int m=0;m<distance.length;m++){
                    distance[m]=0.0;
                }
                for(int q=0;q<n;q++){
                    List<Double> list1 = new ArrayList<>() ;
                    List<Double> list2 = new ArrayList<>();
                    for(int j=0;j<W[i].length;j++){
                        list1.add(W[i][j]);
                        list2.add(k[q][j]);
                    }
                    //System.out.println(list2.size());
                    ComputerDecision computerDecition = new ComputerDecision(list1,
                            list2);
                    //System.out.println(computerDecition.sim());
                    distance[q]= computerDecition.sim();
                    list1.clear();
                    list2.clear();
                }
                //找到最小的那个距离,即为它所属的类
                post_num_cluster[shortest_distance(distance)]++;
                cluster[i]=shortest_distance(distance);
            }
            /*
            for(int i=0;i<W.length;i++){
                //计算该文档距离k的距离
                for(int m=0;m<distance.length;m++){
                    distance[m]=0.0;
                }
                for(int j=0;j<W[i].length;j++){
                    //if(distance[0]<100)
                        //System.out.println("距离0类的距离变化(计算前)："+String.format("%.6f",distance[0])+" "+lala);
                    //System.out.println("距离"+j+" "+W[i][j]+" "+k[0][j]+" "+Math.pow(W[i][j]-k[0][j],2));
                    for(int q=0;q<n;q++){
                        //System.out.println("距离0类的距离变化(计算前)："+distance[0]);
                        distance[q] = distance[q] + (W[i][j]-k[q][j])*(W[i][j]-k[q][j]);
                        //System.out.println("距离0类的距离变化（计算后）："+distance[0]);
                    }

                }
                //找到最小的那个距离,即为它所属的类
                post_num_cluster[shortest_distance(distance)]++;
                cluster[i]=shortest_distance(distance);
            }*/
            for(int i=0;i<n;i++){
                System.out.println("第 "+ i+" 个类有 "+post_num_cluster[i]+" 篇post");
            }
            // 对k重新取平均值
            //k归零
            for(int i=0;i<n;i++){
                for(int j=0;j<k[i].length;j++){
                    k[i][j]=0.0;
                }
            }
            for(int i=0;i<W.length;i++){
                for(int j=0;j<W[i].length;j++){
                    k[cluster[i]][j] += W[i][j];//先求和
                }
            }
            for(int i=0;i<n;i++){
                if(post_num_cluster[i]!=0){
                    for(int j=0;j<k[i].length;j++){
                        k[i][j]=k[i][j]/post_num_cluster[i];//再处以该类共有多少篇
                    }
                }
            }
            for(int i=0;i<cluster.length;i++){
                change = change+Math.abs(cluster1[i]-cluster[i]);
            }
        }while(change!=0);

        for(int i=0;i<n;i++){
            System.out.println("第 "+ i+" 个类有 "+post_num_cluster[i]+" 篇post");
        }

        int max_cluster = max_post_num(post_num_cluster);

        /*

        //对数量大的重新进行聚类
        double W1[][] = new double[post_num_cluster[max_cluster]][W[0].length];
        int post_count=0;
        for(int i=0;i<W.length;i++){
            if(cluster[i]==max_cluster){
                for(int j=0;j<W[i].length;j++){
                    W1[post_count][j]=W[i][j];
                }
                post_count++;
            }
        }
        clustering1(W1,10);*/
        return cluster;

    }

    public static void clustering1(double W[][],int n){
        //初始化n个点，作为聚类的点
        //int n=4;
        int wordcount = W[0].length;
        double k[][] = new double[n][wordcount];
        Random r=new Random();
        for(int i=0;i<n;i++){
            int q = r.nextInt(W.length);
            for(int j=0;j<k[i].length;j++){
                k[i][j]=W[q][j];
                //System.out.println(k[i][j]);
            }
        }

        //迭代求解每个文档对于这几个k值的距离，距离最近的代表他属于那个类
        int cluster[] = new int[W.length];//用来存储对应文档属于那个类
        for(int i=0;i<cluster.length;i++){
            cluster[i]=0;
        }
        int cluster1[] = new int[W.length];//用来存储对应文档属于那个类
        int post_num_cluster[] = new int[n];//记录每个类有多少个文档
        double distance[] = new double[n]; //保存文档距离kn的距离
        int count=0;
        int change=0;
        do {
            count++;
            System.out.println("第 "+count+" 次迭代..");
            for(int i=0;i<cluster.length;i++){
                cluster1[i]=cluster[i];
            }
            change=0;
            for(int i=0;i<n;i++){
                post_num_cluster[i]=0;
            }
            for(int i=0;i<W.length;i++){
                //计算该文档距离k的距离
                for(int m=0;m<distance.length;m++){
                    distance[m]=0.0;
                }
                for(int j=0;j<W[i].length;j++){
                    //if(distance[0]<100)
                    //System.out.println("距离0类的距离变化(计算前)："+String.format("%.6f",distance[0])+" "+lala);
                    //System.out.println("距离"+j+" "+W[i][j]+" "+k[0][j]+" "+Math.pow(W[i][j]-k[0][j],2));
                    for(int q=0;q<n;q++){
                        //System.out.println("距离0类的距离变化(计算前)："+distance[0]);
                        distance[q] = distance[q] + (W[i][j]-k[q][j])*(W[i][j]-k[q][j]);
                        //System.out.println("距离0类的距离变化（计算后）："+distance[0]);
                    }

                }
                /*
                for(int q=0;q<n;q++){
                    System.out.print(String.format("%.6f",distance[q])+" ");
                }
                System.out.println();
                System.out.println("距离最近的类为："+shortest_distance(distance));
                */
                //找到最小的那个距离,即为它所属的类
                post_num_cluster[shortest_distance(distance)]++;
                cluster[i]=shortest_distance(distance);
            }
            for(int i=0;i<n;i++){
                System.out.println("第 "+ i+" 个类有 "+post_num_cluster[i]+" 篇post");
            }
            // 对k重新取平均值
            //k归零
            for(int i=0;i<n;i++){
                for(int j=0;j<k[i].length;j++){
                    k[i][j]=0.0;
                }
            }
            for(int i=0;i<W.length;i++){
                for(int j=0;j<W[i].length;j++){
                    k[cluster[i]][j] += W[i][j];//先求和
                }
            }
            for(int i=0;i<n;i++){
                if(post_num_cluster[i]!=0){
                    for(int j=0;j<k[i].length;j++){
                        k[i][j]=k[i][j]/post_num_cluster[i];//再处以该类共有多少篇
                    }
                }
            }
            for(int i=0;i<cluster.length;i++){
                change = change+Math.abs(cluster1[i]-cluster[i]);
            }
        }while(change!=0);

        for(int i=0;i<n;i++){
            System.out.println("第 "+ i+" 个类有 "+post_num_cluster[i]+" 篇post");
        }
    }

    //返回最小距离的类号
    public static int shortest_distance(double distance[]){
        double min = distance[0];
        int flag =0;
        for(int i=1;i<distance.length;i++ ){
            if(distance[i]<min){
                min = distance[i];
                flag=i;
            }
        }
        return flag;
    }

    //返回文档数量最多的类号
    public static int max_post_num(int post_num_cluster[]){
        double max = post_num_cluster[0];
        int flag =0;
        for(int i=1;i<post_num_cluster.length;i++ ){
            if(post_num_cluster[i]>max){
                max = post_num_cluster[i];
                flag=i;
            }
        }
        return flag;
    }

    //将数据按年份分开
    public static void divide_year()throws IOException{
        FileInputStream fis = new FileInputStream("SA.txt");
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);
        String s ="";
        String id ="";
        String creationdate = "";
        String score = "" ;
        String viewcount = "";
        String title="";
        String body="";
        String tags="";
        String answercount="";
        String commentcount="";
        while((s=br.readLine())!=null){
            if(s.equals("*****row*****")){
                String year = creationdate.substring(0,4);
                File f = new File("original_"+year+".txt");
                FileWriter fw  = new FileWriter(f,Boolean.TRUE);//在文件后端持续写入
                fw.write("id:"+id+"\r\n");
                fw.write("creationdate:"+creationdate+"\r\n");
                fw.write("score:"+score+"\r\n");
                fw.write("viewcount:"+viewcount+"\r\n");
                fw.write("title:"+title+"\r\n");
                fw.write("body:"+body+"\r\n");
                fw.write("tags:"+tags+"\r\n");
                fw.write("answercount:"+answercount+"\r\n");
                fw.write("commentcount:"+commentcount+"\r\n");
                fw.write("*****row*****"+"\r\n");
                fw.flush();
                fw.close();
            }else {
                String set[] = s.split(":");
                if(set[0].equals("Id")){
                    id = set[1];
                }else if(set[0].equals("CreationDate")){
                    creationdate  = set[1];
                }else if(set[0].equals("Score")){
                    score = set[1];
                }else if(set[0].equals("ViewCount")){
                    viewcount = set[1];
                }else if(set[0].equals("Title")){
                    title = set[1];
                }else if(set[0].equals("Body")){
                    body = set[1];
                }else if(set[0].equals("Tags")){
                    tags = set[1];
                }else if(set[0].equals("AnswerCount")){
                    answercount = set[1];
                }else if(set[0].equals("CommentCount")){
                    commentcount = set[1];
                }
            }
        }
    }

    //将该年的数据，按不同类处理开
    public static void divide_cluster(int cluster[],String filenanme)throws IOException{
        FileInputStream fis = new FileInputStream(filenanme);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);
        String s ="";
        String id ="";
        String creationdate = "";
        String score = "" ;
        String viewcount = "";
        String title="";
        String body="";
        String tags="";
        String answercount="";
        String commentcount="";
        int i=0;
        while((s=br.readLine())!=null){
            if(s.equals("*****row*****")){
                int cluster_num = cluster[i];
                i++;
                File f = new File(cluster_num+"_"+filenanme);
                FileWriter fw  = new FileWriter(f,Boolean.TRUE);//在文件后端持续写入
                fw.write("id:"+id+"\r\n");
                fw.write("creationdate:"+creationdate+"\r\n");
                fw.write("score:"+score+"\r\n");
                fw.write("viewcount:"+viewcount+"\r\n");
                fw.write("title:"+title+"\r\n");
                fw.write("body:"+body+"\r\n");
                fw.write("tags:"+tags+"\r\n");
                fw.write("answercount:"+answercount+"\r\n");
                fw.write("commentcount:"+commentcount+"\r\n");
                fw.write("*****row*****"+"\r\n");
                fw.flush();
                fw.close();
            }else {
                String set[] = s.split(":");
                if(set[0].equals("id")){
                    id = set[1];
                }else if(set[0].equals("creationdate")){
                    creationdate  = set[1];
                }else if(set[0].equals("score")){
                    score = set[1];
                }else if(set[0].equals("viewcount")){
                    viewcount = set[1];
                }else if(set[0].equals("title")){
                    title = set[1];
                }else if(set[0].equals("body")){
                    body = set[1];
                }else if(set[0].equals("tags")){
                    tags = set[1];
                }else if(set[0].equals("answercount")){
                    answercount = set[1];
                }else if(set[0].equals("commentcount")){
                    commentcount = set[1];
                }
            }
        }
    }

    //单词词根化，去噪处理
    public static String data_prepare(String text){

        String s="";
        for(int i=0;i<text.length();i++){
            if(text.charAt(i)=='<'){
                //首先跳过第一个>
                do {
                    i++;
                }while (i<text.length()&&text.charAt(i)!='>');
                //在跳过第二个出现的>,即可将<> </>之间的内容略去
                do{
                    i++;
                }while (i<text.length()&&text.charAt(i)!='>');
                s = s+" ";
            }else if(!Character.isLetter(text.charAt(i))){
                s = s+" ";
            }else if(Character.isLetter(text.charAt(i))){
                //System.out.println(text.charAt(i));
                do {
                    s= s + text.charAt(i);
                    i++;
                }while (i<text.length()&&Character.isLetter(text.charAt(i)));
                s = s+" ";
            }
        }

        String set1[] = s.split(" ");
        String set2[] = s.split(" ");
        do{
            set2 = set1;
            s = s.replace("  "," ");
            set1 = s.split(" ");
        }while (set1.length!=set2.length);
       // System.out.println(s);
        s = delete_nonkeyword(s);
        //System.out.println(s);
        return s;
    }

    //去除非关键词
    public static String delete_nonkeyword(String text){
        text = text.toLowerCase();
        text = text.replace(" i "," ");
        text = text.replace(" we "," ");
        text = text.replace(" am "," ");
        text = text.replace(" my "," ");
        text = text.replace(" our "," ");
        text = text.replace(" is "," ");
        text = text.replace(" are "," ");
        text = text.replace(" be "," ");
        text = text.replace(" you "," ");
        text = text.replace(" they ","");
        text = text.replace(" it "," ");
        text = text.replace(" there "," ");
        text = text.replace(" here "," ");
        text = text.replace(" that "," ");
        text = text.replace(" this "," ");
        text = text.replace(" those "," ");
        text = text.replace(" these "," ");
        text = text.replace(" a "," ");
        text = text.replace(" an "," ");
        text = text.replace(" the "," ");
        text = text.replace(" in "," ");
        text = text.replace(" of "," ");
        text = text.replace(" to "," ");
        text = text.replace(" into "," ");
        text = text.replace(" off "," ");
        text = text.replace(" from "," ");
        text = text.replace(" for "," ");
        text = text.replace(" after "," ");
        text = text.replace(" before "," ");
        text = text.replace(" with "," ");
        text = text.replace(" on "," ");
        text = text.replace(" or "," ");
        text = text.replace(" do "," ");
        text = text.replace(" dose "," ");
        text = text.replace(" have "," ");
        text = text.replace(" and "," ");
        text = text.replace(" ve "," ");
        text = text.replace(" ll "," ");
        text = text.replace(" d "," ");
        text = text.replace(" s "," ");
        text = text.replace(" t "," ");
        text = text.replace(" m "," ");
        text = text.replace(" re "," ");
        text = text.replace(" not "," ");
        text = text.replace(" don "," ");
        text = text.replace(" doesn "," ");
        text = text.replace(" been "," ");
        text = text.replace(" how "," ");
        text = text.replace(" who "," ");
        text = text.replace(" what "," ");
        text = text.replace(" then "," ");
        text = text.replace(" so "," ");
        text = text.replace(" had "," ");
        text = text.replace(" was "," ");
        text = text.replace(" were "," ");
        text = text.replace(" more "," ");
        text = text.replace(" only "," ");
        text = text.replace(" up "," ");
        text = text.replace(" down "," ");
        text = text.replace(" he "," ");
        text = text.replace(" she "," ");
        text = text.replace(" his "," ");
        text = text.replace(" her "," ");
        text = text.replace(" want "," ");
        text = text.replace(" wants "," ");
        text = text.replace(" if "," ");
        text = text.replace(" but "," ");
        text = text.replace(" lastactivitydate "," ");
        return text;
    }

    //统计每年关于SA的内容总量
    public static void count_by_year()throws IOException{
        FileInputStream fis = new FileInputStream("SA.txt");
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);
        ArrayList<String> year = new ArrayList<String>();
        int count[] = new int[9];
        for(int i=0;i<9;i++){
            count[i]=0;
        }
        String s = "";
        while((s=br.readLine())!=null){
            String set[] = s.split(":");
            if(set[0].equals("CreationDate")){
                int date = Integer.parseInt(set[1].substring(0,4));//该post的时间
                count[date-2008]++;
            }
        }

        for(int i=0;i<9;i++){
            System.out.println((i+2008)+" "+count[i]);
        }
    }
}
