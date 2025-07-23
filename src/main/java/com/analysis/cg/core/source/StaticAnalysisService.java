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

// class test{
//   public static void main(String[] args) {
//       String path  = "./tmp/repo/10b";
//       StaticAnalysisService staticAnalysisService = new StaticAnalysisService();
//       AstEntity astEntity = staticAnalysisService.methodCallGraph(path);
//       String callGraphText = staticAnalysisService.generateCallGraphJson(astEntity);
//       System.out.println(callGraphText);
      
//       // 测试特定文件的方法调用解析
//       staticAnalysisService.testSpecificFile(path, "src/test/org/apache/commons/cli/ParseRequiredTest.java");
//   }
// }

@Service
public class StaticAnalysisService {

    private String projectRootPath; // 存储项目根路径

    /**
     * 测试特定文件的方法调用解析
     * @param projectPath 项目路径
     * @param filePath 文件相对路径
     */
    public void testSpecificFile(String projectPath, String filePath) {
        try {
            System.out.println("\n\n========== 测试特定文件的方法调用解析 ==========");
            System.out.println("文件: " + filePath);
            
            // 设置解析器配置
            List<String> sourceRootPathList = findAllSourceRootPath(new File(projectPath));
            StaticJavaParser.setConfiguration(this.buildJavaParserConfig(sourceRootPathList));
            
            // 解析文件
            File file = new File(projectPath + "/" + filePath);
            CompilationUnit cu = StaticJavaParser.parse(file);
            
            // 获取所有方法声明
            List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
            System.out.println("找到 " + methods.size() + " 个方法");
            
            // 解析每个方法
            for (MethodDeclaration method : methods) {
                String methodName = method.getNameAsString();
                System.out.println("\n方法: " + methodName);
                
                // 获取方法体中的所有方法调用
                List<MethodCallExpr> methodCalls = method.getBody()
                        .map(body -> body.findAll(MethodCallExpr.class))
                        .orElse(Collections.emptyList());
                
                System.out.println("方法调用数量: " + methodCalls.size());
                
                // 分析每个方法调用
                for (MethodCallExpr methodCall : methodCalls) {
                    System.out.println("  调用: " + methodCall);
                    
                    // 尝试通用解析方法
                    String resolvedSignature = resolveMethodCallSignature(methodCall, method);
                    System.out.println("  解析结果: " + resolvedSignature);
                }
            }
            
            System.out.println("========== 测试结束 ==========\n");
        } catch (Exception e) {
            System.err.println("测试文件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 通用方法调用解析
     * @param methodCall 方法调用表达式
     * @param contextMethod 上下文方法
     * @return 解析后的方法签名
     */
    private String resolveMethodCallSignature(MethodCallExpr methodCall, MethodDeclaration contextMethod) {
        try {
            // 尝试使用JavaParser的符号解析器
            ResolvedMethodDeclaration resolve = methodCall.resolve();
            return resolve.getQualifiedSignature();
        } catch (Throwable throwable) {
            // 如果无法解析，尝试手动构建签名
            try {
                String methodName = methodCall.getNameAsString();
                String scope = "unknown";
                
                // 尝试获取作用域
                if (methodCall.getScope().isPresent()) {
                    try {
                        // 尝试获取作用域的完全限定类型
                        scope = methodCall.getScope().get().calculateResolvedType().describe();
                    } catch (Exception e) {
                        // 如果无法解析类型，尝试直接获取作用域的名称
                        String scopeName = methodCall.getScope().get().toString();
                        scope = scopeName;
                        
                        // 尝试从方法体中查找变量声明
                        if (contextMethod.getBody().isPresent()) {
                            // 在方法体中查找变量声明
                            String varType = findVariableTypeInMethod(contextMethod, scopeName);
                            if (varType != null) {
                                scope = varType;
                            }
                        }
                    }
                } else {
                    // 如果没有作用域，可能是当前类的方法调用或静态导入的方法
                    String className = contextMethod.findAncestor(ClassOrInterfaceDeclaration.class)
                            .map(ClassOrInterfaceDeclaration::getNameAsString)
                            .orElse("Unknown");
                    scope = className;
                }
                
                // 构建参数信息
                StringBuilder paramsInfo = new StringBuilder();
                paramsInfo.append("(");
                if (methodCall.getArguments() != null && !methodCall.getArguments().isEmpty()) {
                    for (int i = 0; i < methodCall.getArguments().size(); i++) {
                        if (i > 0) {
                            paramsInfo.append(", ");
                        }
                        try {
                            paramsInfo.append(methodCall.getArgument(i).calculateResolvedType().describe());
                        } catch (Exception e) {
                            paramsInfo.append("Object");
                        }
                    }
                }
                paramsInfo.append(")");
                
                return scope + "." + methodName + paramsInfo.toString();
            } catch (Exception e) {
                // 如果上述方法都失败，返回最基本的信息
                return "unknown." + methodCall.getNameAsString() + "()";
            }
        }
    }
    
    /**
     * 在方法中查找变量的类型
     * @param method 方法声明
     * @param variableName 变量名
     * @return 变量类型的全限定名，如果找不到则返回null
     */
    private String findVariableTypeInMethod(MethodDeclaration method, String variableName) {
        if (!method.getBody().isPresent()) {
            return null;
        }
        
        // 首先检查方法参数
        for (com.github.javaparser.ast.body.Parameter param : method.getParameters()) {
            if (param.getNameAsString().equals(variableName)) {
                try {
                    return param.getType().resolve().describe();
                } catch (Exception e) {
                    return param.getTypeAsString();
                }
            }
        }
        
        // 使用正则表达式在方法体中查找变量声明
        String methodBody = method.getBody().get().toString();
        
        // 常见的类型映射
        Map<String, String> commonTypes = new HashMap<>();
        commonTypes.put("GnuParser", "org.apache.commons.cli.GnuParser");
        commonTypes.put("PosixParser", "org.apache.commons.cli.PosixParser");
        commonTypes.put("Options", "org.apache.commons.cli.Options");
        commonTypes.put("CommandLine", "org.apache.commons.cli.CommandLine");
        commonTypes.put("OptionBuilder", "org.apache.commons.cli.OptionBuilder");
        
        // 查找形如 "Type varName" 或 "Type varName = ..." 的模式
        String pattern = "\\b([A-Za-z0-9_.<>]+)\\s+" + variableName + "\\b";
        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = r.matcher(methodBody);
        
        if (m.find()) {
            String typeName = m.group(1);
            
            // 检查是否是常见类型
            if (commonTypes.containsKey(typeName)) {
                return commonTypes.get(typeName);
            }
            
            // 尝试解析类型
            try {
                // 如果是简单类型名，可能需要添加包名
                if (!typeName.contains(".")) {
                    // 尝试从导入语句中查找
                    CompilationUnit cu = method.findAncestor(CompilationUnit.class).orElse(null);
                    if (cu != null) {
                        for (com.github.javaparser.ast.ImportDeclaration imp : cu.getImports()) {
                            if (imp.getNameAsString().endsWith("." + typeName)) {
                                return imp.getNameAsString();
                            }
                        }
                    }
                }
                return typeName;
            } catch (Exception e) {
                return typeName;
            }
        }
        
        return null;
    }
    
    public AstEntity methodCallGraph(String AbsoluteProjectPath) {
        try {
            this.projectRootPath = AbsoluteProjectPath; // 保存项目根路径
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
                List<String> callMethodSignatures = new ArrayList<>();
                
                // 记录已经处理过的方法调用，避免重复
                Set<String> processedCalls = new HashSet<>();
                
                for (MethodCallExpr methodCallExpr : methodCallExprs) {
                    // 使用通用方法解析方法调用
                    String signature = resolveMethodCallSignature(methodCallExpr, methodDeclaration);
                    
                    // 避免重复添加
                    if (!processedCalls.contains(signature)) {
                        callMethodSignatures.add(signature);
                        processedCalls.add(signature);
                    }
                }
                
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
        
        // 常见的Java源码目录模式
        String[] commonSourcePaths = {
            "/main/java",
            "/src/main/java",
            "/src/java",
            "/java",
            "/src",
            "/test/java",
            "/src/test/java",
            "/test",
            "/src/test",
            "/main/test",
            "/main/resources",
            "/src/main/resources",
            "/resources",
            "/src/resources"
        };
        
        // 递归查找所有可能的源码目录
        findPotentialSourceRoots(rootDir, sourceRootPathList);
        
        // 如果没有找到任何源码目录，尝试使用常见模式
        if (sourceRootPathList.isEmpty()) {
            for (String commonPath : commonSourcePaths) {
                File potentialSourceRoot = new File(rootDir.getAbsolutePath() + commonPath);
                if (potentialSourceRoot.exists() && potentialSourceRoot.isDirectory()) {
                    sourceRootPathList.add(potentialSourceRoot.getAbsolutePath());
                }
            }
        }
        
        // 如果仍然没有找到源码目录，将项目根目录作为源码目录
        if (sourceRootPathList.isEmpty()) {
            sourceRootPathList.add(rootDir.getAbsolutePath());
        }
        
        return sourceRootPathList;
    }
    
    /**
     * 递归查找可能的Java源码根目录
     */
    private void findPotentialSourceRoots(File directory, List<String> sourceRoots) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        // 检查当前目录是否包含Java文件
        boolean hasJavaFiles = false;
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".java")) {
                hasJavaFiles = true;
                break;
            }
        }
        
        // 如果当前目录包含Java文件，将其添加为源码根目录
        if (hasJavaFiles) {
            sourceRoots.add(directory.getAbsolutePath());
            return; // 不再向下递归
        }
        
        // 向下递归查找
        for (File file : files) {
            if (file.isDirectory()) {
                // 跳过一些明显不是源码目录的目录
                String dirName = file.getName().toLowerCase();
                if (!dirName.equals("target") && !dirName.equals("build") && 
                    !dirName.equals("bin") && !dirName.equals("out") &&
                    !dirName.equals(".git") && !dirName.equals(".idea") &&
                    !dirName.startsWith(".")) {
                    findPotentialSourceRoots(file, sourceRoots);
                }
            }
        }
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
        json.append("{\n");
        
        // 输出methods数组
        json.append("  \"methods\": [\n");

        Iterator<Map.Entry<String, AstEntity.MethodDeclareInfo>> iterator = 
            astEntity.getSignature2MethodDeclareMap().entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, AstEntity.MethodDeclareInfo> entry = iterator.next();
            String signature = entry.getKey();
            AstEntity.MethodDeclareInfo methodDeclare = entry.getValue();

            json.append("    {\n");
            json.append("      \"name\": \"").append(escapeJsonString(methodDeclare.getClassSimpleName()))
                .append(".").append(escapeJsonString(methodDeclare.getSimpleName())).append("\",\n");
            json.append("      \"location\": {\n");
            json.append("        \"startLine\": ").append(methodDeclare.getStartLine()).append(",\n");
            json.append("        \"endLine\": ").append(methodDeclare.getEndLine()).append("\n");
            json.append("      },\n");
            json.append("      \"signature\": \"").append(escapeJsonString(signature)).append("\",\n");
            json.append("      \"visibility\": \"").append(methodDeclare.isPublic() ? "public" : "non-public").append("\",\n");

            // 参数信息
            json.append("      \"parameters\": [");
            if (methodDeclare.getParamTypeList() != null && !methodDeclare.getParamTypeList().isEmpty()) {
                json.append("\n");
                Iterator<String> paramIterator = methodDeclare.getParamTypeList().iterator();
                while (paramIterator.hasNext()) {
                    json.append("        \"").append(escapeJsonString(paramIterator.next())).append("\"");
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
                    json.append("        \"").append(escapeJsonString(annotationIterator.next())).append("\"");
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
                        json.append("          \"name\": \"").append(escapeJsonString(calledMethod.getClassSimpleName()))
                            .append(".").append(escapeJsonString(calledMethod.getSimpleName())).append("\",\n");
                        json.append("          \"line\": ").append(calledMethod.getStartLine()).append(",\n");
                        json.append("          \"signature\": \"").append(escapeJsonString(callSig)).append("\",\n");
                        json.append("          \"type\": \"internal\"\n");
                        json.append("        }");
                    } else {
                        json.append("\n");
                        String methodName = callSig;
                        // 提取类名和方法名，忽略参数部分
                        int paramStart = callSig.indexOf('(');
                        if (paramStart > 0) {
                            methodName = callSig.substring(0, paramStart);
                        }
                        json.append("          \"name\": \"").append(escapeJsonString(methodName)).append("\",\n");
                        json.append("          \"line\": 0,\n");
                        json.append("          \"signature\": \"").append(escapeJsonString(callSig)).append("\",\n");
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

        json.append("  ],\n");
        
        // 添加methods_impl字段，存放函数实现代码
        json.append("  \"methods_impl\": {\n");
        
        iterator = astEntity.getSignature2MethodDeclareMap().entrySet().iterator();
        boolean firstMethod = true;
        
        while (iterator.hasNext()) {
            Map.Entry<String, AstEntity.MethodDeclareInfo> entry = iterator.next();
            String signature = entry.getKey();
            AstEntity.MethodDeclareInfo methodDeclare = entry.getValue();
            
            // 获取方法实现代码
            String methodImpl = getMethodImplementation(astEntity, methodDeclare);
            
            if (!firstMethod) {
                json.append(",\n");
            } else {
                firstMethod = false;
            }
            
            json.append("    \"").append(escapeJsonString(signature)).append("\": \"")
                .append(escapeJsonString(methodImpl)).append("\"");
        }
        
        json.append("\n  }\n}");
        return json.toString();
    }
    
    /**
     * 获取方法的实现代码
     * @param methodDeclare 方法声明信息
     * @return 方法的实现代码
     */
    private String getMethodImplementation(AstEntity astEntity, AstEntity.MethodDeclareInfo methodDeclare) {
        try {
            // 获取方法所在的类信息
            String classSignature = methodDeclare.getSignature();
            if (classSignature == null || classSignature.isEmpty()) {
                return "// Implementation not available";
            }
            
            // 获取类名部分（去掉方法名和参数）
            int methodNameStart = classSignature.lastIndexOf('.');
            if (methodNameStart <= 0) {
                return "// Cannot determine class name";
            }
            String className = classSignature.substring(0, methodNameStart);
            
            // 查找对应的类声明信息
            String classFilePath = null;
            for (AstEntity.InterfaceOrClassDeclareInfo classDeclare : astEntity.getSignature2InterfaceOrClassDeclareMap().values()) {
                if (classDeclare.getSignature().equals(className)) {
                    classFilePath = classDeclare.getClassFileRelativePath();
                    break;
                }
            }
            
            if (classFilePath == null || classFilePath.isEmpty()) {
                return "// Class file path not found";
            }
            
            // 读取源文件
            File sourceFile = new File(projectRootPath + "/" + classFilePath);
            if (!sourceFile.exists()) {
                return "// Source file not found: " + sourceFile.getAbsolutePath();
            }
            
            // 读取文件内容
            List<String> lines = java.nio.file.Files.readAllLines(sourceFile.toPath());
            
            // 提取方法实现部分
            int startLine = methodDeclare.getStartLine();
            int endLine = methodDeclare.getEndLine();
            
            if (startLine <= 0 || endLine <= 0 || startLine > lines.size() || endLine > lines.size()) {
                return "// Invalid line numbers: " + startLine + "-" + endLine;
            }
            
            StringBuilder methodImpl = new StringBuilder();
            for (int i = startLine - 1; i < endLine; i++) {
                methodImpl.append(lines.get(i)).append("\n");
            }
            
            return methodImpl.toString();
        } catch (Exception e) {
            return "// Error retrieving implementation: " + e.getMessage();
        }
    }
    
    /**
     * 转义JSON字符串中的特殊字符
     * @param input 输入字符串
     * @return 转义后的字符串
     */
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '\"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '/':
                    result.append("\\/");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }
}
