package com.agent;
import com.agent.Tool;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class RunCodeTool implements Tool {

    @Override
    public String name() { return "run_code"; }

    @Override
    public String description() {
        return "执行一段 Java 代码片段（仅支持单文件、无包声明、含 main 方法），返回标准输出/错误。参数 JSON: {\"code\":\"...\"}";
    }

    @Override
    public String execute(String argsJson) {
        try {
            // 极简参数解析（避免引入额外依赖）
            String code = argsJson.replaceAll(".*\"code\"\\s*:\\s*\"", "")
                    .replaceAll("\"\\s*\\}.*", "")
                    .replace("\\n", "\n")
                    .replace("\\t", "\t");

            if (code.isBlank() || !code.contains("main")) {
                return "❌ 代码为空或不包含 main 方法";
            }

            Path dir = Files.createTempDirectory("agent_run_");
            File javaFile = new File(dir.toFile(), "TempMain.java");
            Files.writeString(javaFile.toPath(), code);

            // 编译
            Process compile = new ProcessBuilder("javac", javaFile.getAbsolutePath())
                    .redirectErrorStream(true).start();
            String compileOut = readStream(compile.getInputStream());
            int cCode = compile.waitFor();
            if (cCode != 0) return "❌ 编译失败:\n" + compileOut;

            // 运行
            Process run = new ProcessBuilder("java", "-cp", dir.toString(), "TempMain")
                    .redirectErrorStream(true).start();
            String runOut = readStream(run.getInputStream());
            int rCode = run.waitFor();

            // 清理临时文件
            javaFile.delete();
            new File(dir.toFile(), "TempMain.class").delete();
            dir.toFile().delete();

            return (rCode == 0 ? "✅ 运行成功:\n" : "❌ 运行异常(code=" + rCode + "):\n") + runOut;

        } catch (Exception e) {
            return "❌ 执行异常: " + e.getMessage();
        }
    }

    private String readStream(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append("\n");
        return sb.toString();
    }
}