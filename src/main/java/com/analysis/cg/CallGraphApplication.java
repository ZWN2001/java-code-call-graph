package com.analysis.cg;

import com.analysis.cg.core.source.StaticAnalysisService;
import com.analysis.cg.model.AstEntity;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
// import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;

@SpringBootApplication
public class CallGraphApplication {

    public static void main(String[] args) {
        SpringApplication.run(CallGraphApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(StaticAnalysisService staticAnalysisService) {
        return args -> {
            if (args.length < 1) {
                System.err.println("用法: java -jar callgraph.jar <项目根目录路径>");
                System.exit(1);
            }

            String projectPath = args[0];
            File projectDir = new File(projectPath);
            if (!projectDir.exists() || !projectDir.isDirectory()) {
                System.err.println("错误: 指定的路径不存在或不是目录");
                System.exit(1);
            }

            System.out.println("开始分析项目: " + projectPath);
            AstEntity astEntity = staticAnalysisService.methodCallGraph(projectPath);
            
            if (astEntity == null) {
                System.err.println("错误: 项目分析失败");
                System.exit(1);
            }

            // 生成输出文件名（使用时间戳）
//            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
//            String outputFileName = "callgraph_" + timestamp + ".json";
            String outputFileName = "callgraph.json";
            String outputPath = Paths.get(projectPath, outputFileName).toString();

            // 将分析结果写入文件
            try (FileWriter writer = new FileWriter(outputPath)) {
                writer.write(staticAnalysisService.generateCallGraphJson(astEntity));
                System.out.println("分析完成！结果已保存到: " + outputPath);
            } catch (Exception e) {
                System.err.println("错误: 无法写入输出文件: " + e.getMessage());
                System.exit(1);
            }
        };
    }
} 