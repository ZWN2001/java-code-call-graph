package com.analysis.cg.core.source;

// import com.analysis.cg.manager.GraphvizManager;
import com.analysis.cg.model.AstEntity;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.apache.commons.collections4.CollectionUtils;
// import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class test{
  public static void main(String[] args) {
      String path  = "./tmp/repo/53d37f060d32f95d5ff960dc68cd9842";
      StaticAnalysisService staticAnalysisService = new StaticAnalysisService();
      AstEntity astEntity = staticAnalysisService.methodCallGraph(path);
      String callGraphText = staticAnalysisService.generateCallGraphJson(astEntity);
      System.out.println(callGraphText);

    //   // 生成输出文件名（使用时间戳）
    //   String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    //   String outputFileName = "callgraph_" + timestamp + ".json";
    //   String outputPath = Paths.get(path, outputFileName).toString();

    //   // 将分析结果写入文件
    //   try (FileWriter writer = new FileWriter(outputPath)) {
    //       writer.write(staticAnalysisService.generateCallGraphJson(astEntity));
    //       System.out.println("分析完成！结果已保存到: " + outputPath);
    //   } catch (Exception e) {
    //       System.err.println("错误: 无法写入输出文件: " + e.getMessage());
    //       System.exit(1);
    //   }
  }
}

@Service
public class StaticAnalysisService {

    public AstEntity methodCallGraph(String AbsoluteProjectPath) {
        try {
            File projectRoot = new File(AbsoluteProjectPath);
            List<String> allClassPathList = findAllJavaFiles(projectRoot);
            List<String> sourceRootPathList = findAllSourceRootPath(projectRoot);
            // Parse all source
            StaticJavaParser.setConfiguration(this.buildJavaParserConfig(sourceRootPathList));
            AstEntity astEntity = new AstEntity();
            allClassPathList.forEach(path -> {
                try {
                    this.parseInterfaceOrClass(astEntity, AbsoluteProjectPath, path);
                } catch (Exception ignored) {
                   
                }
            });
            return astEntity;
        } catch (Exception e) {
            return null;
        }
    }

    private void parseInterfaceOrClass(AstEntity astEntity, String projectRootPath, String path) throws FileNotFoundException {
        CompilationUnit cu = StaticJavaParser.parse(new File(projectRootPath + "/" + path));
        // 类型声明解析
        List<ClassOrInterfaceDeclaration> classDeclarations = cu.findAll(ClassOrInterfaceDeclaration.class);
        if (CollectionUtils.isEmpty(classDeclarations)) {
            return;
        }
        // 类解析(只解析顶层类定义，其他内部类方法会归属到其下）
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = classDeclarations.get(0);
        ResolvedReferenceTypeDeclaration resolve = classOrInterfaceDeclaration.resolve();
        AstEntity.InterfaceOrClassDeclareInfo interfaceOrClassDeclareInfo = new AstEntity.InterfaceOrClassDeclareInfo();
        interfaceOrClassDeclareInfo.setClassFileRelativePath(path);
        interfaceOrClassDeclareInfo.setSimpleName(classOrInterfaceDeclaration.getNameAsString());
        interfaceOrClassDeclareInfo.setSignature(resolve.getQualifiedName());
        interfaceOrClassDeclareInfo.setInterface(classOrInterfaceDeclaration.isInterface());
        interfaceOrClassDeclareInfo.setAbstract(classOrInterfaceDeclaration.isAbstract());
        NodeList<ClassOrInterfaceType> implementedTypes = classOrInterfaceDeclaration.getImplementedTypes();
        // 实现接口信息
        if (CollectionUtils.isNotEmpty(implementedTypes)) {
            Set<String> signatures = this.getClassSignatures(implementedTypes);
            interfaceOrClassDeclareInfo.setImplementInterfaceSignatures(signatures);
        }
        // 继承类信息
        NodeList<ClassOrInterfaceType> extendedTypes = classOrInterfaceDeclaration.getExtendedTypes();
        if (CollectionUtils.isNotEmpty(extendedTypes)) {
            Set<String> signatures = this.getClassSignatures(extendedTypes);
            interfaceOrClassDeclareInfo.setExtendClassSignatures(signatures);
        }

        // 声明方法解析
        List<MethodDeclaration> methodDeclarations = classOrInterfaceDeclaration.findAll(MethodDeclaration.class);
        if (CollectionUtils.isNotEmpty(methodDeclarations)) {
            Map<String, AstEntity.MethodDeclareInfo> methodDeclareInfoMap = methodDeclarations.stream()
                    .map(e -> this.parseMethod(e, classOrInterfaceDeclaration.getNameAsString()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(AstEntity.MethodDeclareInfo::getSignature, Function.identity(), (v1, v2) -> v1));
            astEntity.getSignature2MethodDeclareMap().putAll(methodDeclareInfoMap);

            // 项目入口识别（目前仅通过注解识别spring http入口，可以扩展其他的模式）
            NodeList<AnnotationExpr> annotations = classOrInterfaceDeclaration.getAnnotations();
            if (CollectionUtils.isNotEmpty(annotations)
                    && annotations.stream().map(AnnotationExpr::getNameAsString)
                    .anyMatch(e -> "Controller".equals(e) || "RestController".equals(e))) {
                methodDeclareInfoMap.forEach((signature, methodDeclareInfo) -> {
                    boolean isEndpoint = methodDeclareInfo.getAnnotationSimpleNames().stream()
                            .anyMatch(e -> "GetMapping".equals(e) || "PostMapping".equals(e)
                                    || "PutMapping".equals(e) || "DeleteMapping".equals(e) || "RequestMapping".equals(e));
                    if (isEndpoint) {
                        astEntity.getEndPointMethodSignatures().add(signature);
                    }
                });
            }
        }
        astEntity.getSignature2InterfaceOrClassDeclareMap().put(interfaceOrClassDeclareInfo.getSignature(), interfaceOrClassDeclareInfo);
    }

    private AstEntity.MethodDeclareInfo parseMethod(MethodDeclaration methodDeclaration, String classSimpleName) {
        try {
            ResolvedMethodDeclaration methodResolve = methodDeclaration.resolve();
            AstEntity.MethodDeclareInfo methodDeclareInfo = new AstEntity.MethodDeclareInfo();
            methodDeclareInfo.setSimpleName(methodDeclaration.getNameAsString());
            methodDeclareInfo.setSignature(methodResolve.getQualifiedSignature());
            methodDeclareInfo.setClassSimpleName(classSimpleName);
            methodDeclareInfo.setPublic(methodDeclaration.isPublic());
            
            // 添加位置信息
            methodDeclareInfo.setStartLine(methodDeclaration.getBegin().get().line);
            methodDeclareInfo.setEndLine(methodDeclaration.getEnd().get().line);
            
            // 填充方法参数信息
            if (CollectionUtils.isNotEmpty(methodDeclaration.getParameters())) {
                List<String> params = this.getParamSignatures(methodDeclaration);
                methodDeclareInfo.setParamTypeList(params);
            }
            // 填充注解信息
            NodeList<AnnotationExpr> annotations = methodDeclaration.getAnnotations();
            if (CollectionUtils.isNotEmpty(annotations)) {
                Set<String> annotationNames = annotations.stream().map(AnnotationExpr::getNameAsString).collect(Collectors.toSet());
                methodDeclareInfo.setAnnotationSimpleNames(annotationNames);
            }
            // 填充方法调用信息
            List<MethodCallExpr> methodCallExprs = methodDeclaration.getBody()
                    .map(e -> e.findAll(MethodCallExpr.class))
                    .orElse(Collections.emptyList());
            if (CollectionUtils.isNotEmpty(methodCallExprs)) {
                List<String> callMethodSignatures = methodCallExprs.stream().map(methodCallExpr -> {
                            try {
                                ResolvedMethodDeclaration resolve = methodCallExpr.resolve();
                                return resolve.getQualifiedSignature();
                            } catch (Throwable throwable) {
                                // 如果无法解析方法调用，尝试获取调用表达式的基本信息
                                try {
                                    String scope = methodCallExpr.getScope()
                                            .map(scopeExpr -> scopeExpr.calculateResolvedType().describe())
                                            .orElse("unknown");
                                    return scope + "." + methodCallExpr.getNameAsString() + "()";
                                } catch (Exception e) {
                                    return null;
                                }
                            }
                        }).filter(e -> e != null && !e.isEmpty())
                        .collect(Collectors.toList());
                methodDeclareInfo.setCallMethodSignatures(callMethodSignatures);
            }
            return methodDeclareInfo;
        } catch (Throwable e) {
           
            return null;
        }
    }

    private List<String> getParamSignatures(MethodDeclaration methodDeclaration) {
        return methodDeclaration.getParameters().stream()
                .map(parameter -> {
                    try {
                        return parameter.resolve().getType().asReferenceType().getQualifiedName();
                    } catch (Exception e) {
                        
                        return null;
                    }
                }).collect(Collectors.toList());
    }

    private Set<String> getClassSignatures(NodeList<ClassOrInterfaceType> types) {  
        return types.stream()
                .map(e -> {
                    try {
                        ResolvedReferenceType resolve = (ResolvedReferenceType) e.resolve();
                        return resolve.getQualifiedName();
                    } catch (Throwable ex) {
                        
                        return null;
                    }
                }).filter(e -> e != null && !e.isEmpty())
                .collect(Collectors.toSet());
    }

    private ParserConfiguration buildJavaParserConfig(List<String> sourcePath) {
        TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        CombinedTypeSolver combinedSolver = new CombinedTypeSolver(reflectionTypeSolver);
        sourcePath.forEach(path -> {
            try {
                TypeSolver javaParserTypeSolver = new JavaParserTypeSolver(new File(path));
                combinedSolver.add(javaParserTypeSolver);
            } catch (Exception ignored) {
                
            }
        });
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedSolver);
        return new ParserConfiguration().setSymbolResolver(symbolSolver);
    }


    private List<String> findAllSourceRootPath(File rootDir) {
        List<String> sourceRootPathList = new ArrayList<>();
        File[] files = rootDir.listFiles(File::isDirectory);
        if (null == files) {
            return Collections.emptyList();
        }
        for (File file : files) {
            if ("src".equals(file.getName())) {
                sourceRootPathList.add(file.getAbsolutePath() + "/main/java");
            } else {
                File[] srcFiles = file.listFiles((dir, name) -> "src".equals(name));
                if (null != srcFiles) {
                    for (File srcFile : srcFiles) {
                        sourceRootPathList.add(srcFile.getAbsolutePath() + "/main/java");
                    }
                }
            }
        }
        return sourceRootPathList;
    }

    /**
     * 递归查找目录下所有的Java文件
     * @param directory 要搜索的目录
     * @return 相对于项目根目录的Java文件路径列表
     */
    private List<String> findAllJavaFiles(File directory) {
        List<String> javaFiles = new ArrayList<>();
        if (!directory.exists() || !directory.isDirectory()) {
            return javaFiles;
        }

        String basePath = directory.getAbsolutePath();
        findAllJavaFilesRecursively(directory, basePath, javaFiles);
        return javaFiles;
    }

    private void findAllJavaFilesRecursively(File directory, String basePath, List<String> javaFiles) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                findAllJavaFilesRecursively(file, basePath, javaFiles);
            } else if (file.getName().endsWith(".java")) {
                // 获取相对路径
                String relativePath = file.getAbsolutePath().substring(basePath.length() + 1)
                    .replace('\\', '/');
                javaFiles.add(relativePath);
            }
        }
    }

    /**
     * 生成调用关系的JSON格式表示
     * @param astEntity AST实体
     * @return JSON格式的调用关系
     */
    public String generateCallGraphJson(AstEntity astEntity) {
        if (astEntity == null) {
            return "{\"error\": \"No call graph data available.\"}";
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n  \"methods\": [\n");

        Iterator<Map.Entry<String, AstEntity.MethodDeclareInfo>> iterator = 
            astEntity.getSignature2MethodDeclareMap().entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, AstEntity.MethodDeclareInfo> entry = iterator.next();
            String signature = entry.getKey();
            AstEntity.MethodDeclareInfo methodDeclare = entry.getValue();

            json.append("    {\n");
            json.append("      \"name\": \"").append(methodDeclare.getClassSimpleName())
                .append(".").append(methodDeclare.getSimpleName()).append("\",\n");
            json.append("      \"location\": {\n");
            json.append("        \"startLine\": ").append(methodDeclare.getStartLine()).append(",\n");
            json.append("        \"endLine\": ").append(methodDeclare.getEndLine()).append("\n");
            json.append("      },\n");
            json.append("      \"signature\": \"").append(signature).append("\",\n");
            json.append("      \"visibility\": \"").append(methodDeclare.isPublic() ? "public" : "non-public").append("\",\n");

            // 参数信息
            json.append("      \"parameters\": [");
            if (methodDeclare.getParamTypeList() != null && !methodDeclare.getParamTypeList().isEmpty()) {
                json.append("\n");
                Iterator<String> paramIterator = methodDeclare.getParamTypeList().iterator();
                while (paramIterator.hasNext()) {
                    json.append("        \"").append(paramIterator.next()).append("\"");
                    if (paramIterator.hasNext()) {
                        json.append(",");
                    }
                    json.append("\n");
                }
                json.append("      ");
            }
            json.append("],\n");

            // 注解信息
            json.append("      \"annotations\": [");
            if (methodDeclare.getAnnotationSimpleNames() != null && !methodDeclare.getAnnotationSimpleNames().isEmpty()) {
                json.append("\n");
                Iterator<String> annotationIterator = methodDeclare.getAnnotationSimpleNames().iterator();
                while (annotationIterator.hasNext()) {
                    json.append("        \"").append(annotationIterator.next()).append("\"");
                    if (annotationIterator.hasNext()) {
                        json.append(",");
                    }
                    json.append("\n");
                }
                json.append("      ");
            }
            json.append("],\n");

            // 调用方法信息
            json.append("      \"calls\": [");
            List<String> callSignatures = methodDeclare.getCallMethodSignatures();
            if (callSignatures != null && !callSignatures.isEmpty()) {
                json.append("\n");
                Iterator<String> callIterator = callSignatures.iterator();
                while (callIterator.hasNext()) {
                    String callSig = callIterator.next();
                    AstEntity.MethodDeclareInfo calledMethod = astEntity.getSignature2MethodDeclareMap().get(callSig);
                    json.append("        {");
                    if (calledMethod != null) {
                        json.append("\n");
                        json.append("          \"name\": \"").append(calledMethod.getClassSimpleName())
                            .append(".").append(calledMethod.getSimpleName()).append("\",\n");
                        json.append("          \"line\": ").append(calledMethod.getStartLine()).append(",\n");
                        json.append("          \"signature\": \"").append(callSig).append("\",\n");
                        json.append("          \"type\": \"internal\"\n");
                        json.append("        }");
                    } else {
                        json.append("\n");
                        json.append("          \"name\": \"").append(callSig.substring(callSig.lastIndexOf(".") + 1)).append("\",\n");
                        json.append("          \"line\": 0,\n");
                        json.append("          \"signature\": \"").append(callSig).append("\",\n");
                        json.append("          \"type\": \"external\"\n");
                        json.append("        }");
                    }
                    if (callIterator.hasNext()) {
                        json.append(",");
                    }
                    json.append("\n");
                }
                json.append("      ");
            }
            json.append("]\n");
            json.append("    }");
            if (iterator.hasNext()) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n}");
        return json.toString();
    }
}
