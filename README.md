# java-code-call-graph

代码改自 ： https://github.com/shawnxie94/java-call-graph-diff

使用mvnw编译项目：

```
./mvnw.cmd clean package
```

```
java -jar your_jar.jar path_to_project
```

使用json格式输出项目中各个方法的调用情况，包括更详细的函数信息，如：

```json
{
  "methods": [
    {
      "name": "EService.run",
      "location": {
        "startLine": 10,
        "endLine": 12
      },
      "signature": "com.analysis.cg.example.service.EService.run()",
      "visibility": "public",
      "parameters": [],
      "annotations": [],
      "calls": []
    },
    {
      "name": "StaticAnalysisService.buildJavaParserConfig",
      "location": {
        "startLine": 321,
        "endLine": 334
      },
      "signature": "com.analysis.cg.core.source.StaticAnalysisService.buildJavaParserConfig(java.util.List<java.lang.String>)",
      "visibility": "non-public",
      "parameters": [
        "java.util.List"
      ],
      "annotations": [],
      "calls": [
        {
          "signature": "java.lang.Iterable.forEach(java.util.function.Consumer<? super T>)",
          "type": "external"
        }
      ]
    },
    {
      "name": "JGitManager.remoteClone",
      "location": {
        "startLine": 187,
        "endLine": 206
      },
      "signature": "com.analysis.cg.manager.JGitManager.remoteClone(java.lang.String, java.io.File)",
      "visibility": "non-public",
      "parameters": [
        "java.lang.String",
        "java.io.File"
      ],
      "annotations": [],
      "calls": [
        {
          "signature": "java.io.File.getAbsolutePath()",
          "type": "external"
        }
      ]
    },
    {
      "name": "DService.run",
      "location": {
        "startLine": 15,
        "endLine": 18
      },
      "signature": "com.analysis.cg.example.service.DService.run()",
      "visibility": "public",
      "parameters": [],
      "annotations": [],
      "calls": [
        {
          "name": "EService.run",
          "line": 10,
          "type": "internal"
        }
      ]
    },
    {
      "name": "JGitManager.getRepository",
      "location": {
        "startLine": 99,
        "endLine": 109
      },
      "signature": "com.analysis.cg.manager.JGitManager.getRepository(java.lang.String)",
      "visibility": "public",
      "parameters": [
        "java.lang.String"
      ],
      "annotations": [],
      "calls": [
        {
          "signature": "java.lang.String.getBytes()",
          "type": "external"
        },
        {
          "signature": "java.io.File.exists()",
          "type": "external"
        },
        {
          "name": "JGitManager.getLocalRepo",
          "line": 208,
          "type": "internal"
        },
        {
          "name": "JGitManager.remoteClone",
          "line": 187,
          "type": "internal"
        }
      ]
    },
    {
      "name": "CService.run",
      "location": {
        "startLine": 15,
        "endLine": 18
      },
      "signature": "com.analysis.cg.example.service.CService.run()",
      "visibility": "public",
      "parameters": [],
      "annotations": [],
      "calls": [
        {
          "name": "EService.run",
          "line": 10,
          "type": "internal"
        }
      ]
    },
    {
      "name": "StaticAnalysisService.findAllSourceRootPath",
      "location": {
        "startLine": 337,
        "endLine": 357
      },
      "signature": "com.analysis.cg.core.source.StaticAnalysisService.findAllSourceRootPath(java.io.File)",
      "visibility": "non-public",
      "parameters": [
        "java.io.File"
      ],
      "annotations": [],
      "calls": [
        {
          "signature": "java.io.File.getAbsolutePath()",
          "type": "external"
        },
        {
          "signature": "java.util.Collections.emptyList()",
          "type": "external"
        },
        {
          "signature": "java.lang.String.equals(java.lang.Object)",
          "type": "external"
        },
        {
          "signature": "java.io.File.getName()",
          "type": "external"
        },
        {
          "signature": "java.util.List.add(E)",
          "type": "external"
        },
        {
          "signature": "java.io.File.getAbsolutePath()",
          "type": "external"
        },
        {
          "signature": "java.util.List.add(E)",
          "type": "external"
        },
        {
          "signature": "java.io.File.getAbsolutePath()",
          "type": "external"
        }
      ]
    },
    {
      "name": "EndPointController.endPoint",
      "location": {
        "startLine": 23,
        "endLine": 28
      },
      "signature": "com.analysis.cg.example.controller.EndPointController.endPoint()",
      "visibility": "public",
      "parameters": [],
      "annotations": [
        "GetMapping"
      ],
      "calls": [
        {
          "name": "AService.run",
          "line": 15,
          "type": "internal"
        },
        {
          "name": "BService.run",
          "line": 15,
          "type": "internal"
        }
      ]
    },
    {
      "name": "CodeApplication.main",
      "location": {
        "startLine": 9,
        "endLine": 11
      },
      "signature": "com.analysis.cg.CodeApplication.main(java.lang.String[])",
      "visibility": "public",
      "parameters": [
        "null"
      ],
      "annotations": [],
      "calls": []
    },
    {
      "name": "StaticAnalysisService.methodCallGraph",
      "location": {
        "startLine": 165,
        "endLine": 194
      },
      "signature": "com.analysis.cg.core.source.StaticAnalysisService.methodCallGraph(java.lang.String)",
      "visibility": "public",
      "parameters": [
        "java.lang.String"
      ],
      "annotations": [],
      "calls": [
        {
          "name": "JGitManager.getRepository",
          "line": 99,
          "type": "internal"
        },
        {
          "signature": "java.io.File.getAbsolutePath()",
          "type": "external"
        },
        {
          "signature": "java.util.stream.Collectors.toList()",
          "type": "external"
        },
        {
          "name": "StaticAnalysisService.findAllSourceRootPath",
          "line": 337,
          "type": "internal"
        },
        {
          "name": "StaticAnalysisService.buildJavaParserConfig",
          "line": 321,
          "type": "internal"
        },
        {
          "signature": "java.lang.Iterable.forEach(java.util.function.Consumer<? super T>)",
          "type": "external"
        },
        {
          "name": "StaticAnalysisService.parseInterfaceOrClass",
          "line": 196,
          "type": "internal"
        }
      ]
    },
    {
      "name": "BService.run",
      "location": {
        "startLine": 15,
        "endLine": 18
      },
      "signature": "com.analysis.cg.example.service.BService.run()",
      "visibility": "public",
      "parameters": [],
      "annotations": [],
      "calls": [
        {
          "name": "DService.run",
          "line": 15,
          "type": "internal"
        }
      ]
    },
    {
      "name": "GraphvizManager.drawGraph",
      "location": {
        "startLine": 27,
        "endLine": 57
      },
      "signature": "com.analysis.cg.manager.GraphvizManager.drawGraph(com.analysis.cg.model.AstEntity, java.util.Set<java.lang.String>)",
      "visibility": "public",
      "parameters": [
        "com.analysis.cg.model.AstEntity",
        "java.util.Set"
      ],
      "annotations": [],
      "calls": []
    },
    {
      "name": "AService.run",
      "location": {
        "startLine": 15,
        "endLine": 18
      },
      "signature": "com.analysis.cg.example.service.AService.run()",
      "visibility": "public",
      "parameters": [],
      "annotations": [],
      "calls": [
        {
          "name": "CService.run",
          "line": 15,
          "type": "internal"
        }
      ]
    },
    {
      "name": "StaticAnalysisService.codeChangeMethods",
      "location": {
        "startLine": 66,
        "endLine": 125
      },
      "signature": "com.analysis.cg.core.source.StaticAnalysisService.codeChangeMethods(java.lang.String, java.lang.String)",
      "visibility": "public",
      "parameters": [
        "java.lang.String",
        "java.lang.String"
      ],
      "annotations": [],
      "calls": [
        {
          "signature": "java.util.Collections.emptySet()",
          "type": "external"
        },
        {
          "name": "JGitManager.getRepository",
          "line": 99,
          "type": "internal"
        },
        {
          "signature": "java.util.Collections.emptySet()",
          "type": "external"
        },
        {
          "signature": "java.util.Collections.emptySet()",
          "type": "external"
        },
        {
          "signature": "java.io.File.getAbsolutePath()",
          "type": "external"
        },
        {
          "signature": "java.util.stream.Stream.filter(java.util.function.Predicate<? super T>)",
          "type": "external"
        },
        {
          "signature": "java.util.stream.Stream.map(java.util.function.Function<? super T, ? extends R>)",
          "type": "external"
        },
        {
          "signature": "java.util.Collection.stream()",
          "type": "external"
        },
        {
          "signature": "java.util.Collections.emptySet()",
          "type": "external"
        },
        {
          "signature": "java.util.stream.Stream.filter(java.util.function.Predicate<? super T>)",
          "type": "external"
        },
        {
          "signature": "java.util.stream.Stream.map(java.util.function.Function<? super T, ? extends R>)",
          "type": "external"
        },
        {
          "signature": "java.util.Collection.stream()",
          "type": "external"
        },
        {
          "signature": "java.util.Collections.emptySet()",
          "type": "external"
        }
      ]
    },
    {
      "name": "JGitManager.getLocalRepo",
      "location": {
        "startLine": 208,
        "endLine": 225
      },
      "signature": "com.analysis.cg.manager.JGitManager.getLocalRepo(java.io.File)",
      "visibility": "non-public",
      "parameters": [
        "java.io.File"
      ],
      "annotations": [],
      "calls": [
        {
          "signature": "java.io.File.getAbsolutePath()",
          "type": "external"
        }
      ]
    },
    {
      "name": "StaticAnalysisServiceTest.test",
      "location": {
        "startLine": 23,
        "endLine": 36
      },
      "signature": "com.analysis.cg.core.StaticAnalysisServiceTest.test()",
      "visibility": "public",
      "parameters": [],
      "annotations": [
        "Test"
      ],
      "calls": [
        {
          "name": "StaticAnalysisService.methodCallGraph",
          "line": 165,
          "type": "internal"
        },
        {
          "name": "GraphvizManager.drawGraph",
          "line": 27,
          "type": "internal"
        },
        {
          "signature": "java.util.Collections.emptySet()",
          "type": "external"
        },
        {
          "name": "StaticAnalysisService.codeChangeMethods",
          "line": 66,
          "type": "internal"
        },
        {
          "name": "GraphvizManager.drawGraph",
          "line": 27,
          "type": "internal"
        }
      ]
    },
    {
      "name": "StaticAnalysisService.getAllMethodDeclaration",
      "location": {
        "startLine": 149,
        "endLine": 163
      },
      "signature": "com.analysis.cg.core.source.StaticAnalysisService.getAllMethodDeclaration(java.lang.String, java.lang.String)",
      "visibility": "non-public",
      "parameters": [
        "java.lang.String",
        "java.lang.String"
      ],
      "annotations": [],
      "calls": [
        {
          "signature": "java.util.Collections.emptyMap()",
          "type": "external"
        }
      ]
    },
    {
      "name": "StaticAnalysisService.parseInterfaceOrClass",
      "location": {
        "startLine": 196,
        "endLine": 250
      },
      "signature": "com.analysis.cg.core.source.StaticAnalysisService.parseInterfaceOrClass(com.analysis.cg.model.AstEntity, java.lang.String, java.lang.String)",
      "visibility": "non-public",
      "parameters": [
        "com.analysis.cg.model.AstEntity",
        "java.lang.String",
        "java.lang.String"
      ],
      "annotations": [],
      "calls": [
        {
          "signature": "java.util.stream.Collectors.toMap(java.util.function.Function<? super T, ? extends K>, java.util.function.Function<? super T, ? extends U>, java.util.function.BinaryOperator<U>)",
          "type": "external"
        },
        {
          "signature": "java.util.function.Function.identity()",
          "type": "external"
        },
        {
          "signature": "java.util.Map.forEach(java.util.function.BiConsumer<? super K, ? super V>)",
          "type": "external"
        }
      ]
    },
    {
      "name": "GraphvizManager.getNode",
      "location": {
        "startLine": 59,
        "endLine": 65
      },
      "signature": "com.analysis.cg.manager.GraphvizManager.getNode(java.util.Set<java.lang.String>, java.lang.String, com.analysis.cg.model.AstEntity.MethodDeclareInfo)",
      "visibility": "non-public",
      "parameters": [
        "java.util.Set",
        "java.lang.String",
        "com.analysis.cg.model.AstEntity.MethodDeclareInfo"
      ],
      "annotations": [],
      "calls": [
        {
          "signature": "java.util.Set.contains(java.lang.Object)",
          "type": "external"
        }
      ]
    }
  ]
}
```




