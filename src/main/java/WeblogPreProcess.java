

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import mrbean.WebLogBean;
import mrbean.WebLogParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * 处理原始日志，过滤出真实pv请求 转换时间格式 对缺失字段填充默认值 对记录标记valid和invalid
 *
 */

/*
* 问题：
* 1.map函数的作用是什么
* 2.parser函数的作用
* 3.为什么没有reduce函数
* */

public class WeblogPreProcess {

    static class WeblogPreProcessMapper extends Mapper<LongWritable, Text, Text, NullWritable> {
        // 用来存储网站url分类数据
        Set<String> pages = new HashSet<String>();
        Text k = new Text();
        NullWritable v = NullWritable.get();

        /**
         * 从外部配置文件中加载网站的有用url分类数据 存储到maptask的内存中，用来对日志数据进行过滤
         */
        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            pages.add("/");
            pages.add("/about");
            pages.add("/black-ip-list/");
            pages.add("/cassandra-clustor/");
            pages.add("/finance-rhive-repurchase/");
            pages.add("/hadoop-family-roadmap/");
            pages.add("/hadoop-hive-intro/");
            pages.add("/hadoop-zookeeper-intro/");
            pages.add("/hadoop-mahout-roadmap/");

        }

        //1.map函数的作用就是将每一条信息对应封装到一个WebLogBean下
        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            String line = value.toString();
            //2.parser函数的作用就是将内容填充到WebLogBean对应属性下
            WebLogBean webLogBean = WebLogParser.parser(line);
            if (webLogBean != null) {
                // 过滤js/图片/css等静态资源
                WebLogParser.filtStaticResource(webLogBean, pages);
                /* if (!webLogBean.isValid()) return; */
                k.set(webLogBean.toString());
                context.write(k, v);
            }
        }

    }

    //3.没有reduce函数，所有数据就会直接由map函数写入到一个文件当中
    //数据预处理阶段只是把数据封装到WebLogBean下，不需要ReduceTask进行聚合处理

    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf);

        job.setJarByClass(WeblogPreProcess.class);

        job.setMapperClass(WeblogPreProcessMapper.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

//       FileInputFormat.setInputPaths(job, new Path(args[0]));
//       FileOutputFormat.setOutputPath(job, new Path(args[1]));
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        job.setNumReduceTasks(0);

        boolean res = job.waitForCompletion(true);
        System.exit(res?0:1);

    }

}