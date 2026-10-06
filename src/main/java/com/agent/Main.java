package com.agent;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Code Explain Agent 启动成功 (空壳测试) ===");
        System.out.println("输入 'exit' 退出程序。");
        System.out.println("当前工作目录: " + System.getProperty("user.dir"));
        System.out.println();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("你 > ");
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("程序已退出。");
                break;
            }
            if (input.isEmpty()) continue;

            System.out.println("Agent(占位) > 收到指令：" + input + " （等待接入真实逻辑）");
            System.out.println();
        }
        scanner.close();
    }
}