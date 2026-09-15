package org.example;

class Solution {
    public int solution(int[] numbers, int target) {
        int result = 0;
        boolean[][] visited = new boolean[numbers.length][51];

        result = dfs(0, 0, visited, numbers, target, 0);
        return result;
    }

    public static int dfs(int depth, int curr, boolean[][] visited, int[] numbers, int target, int result) {
        // 더 이상 방문할 곳 x
        if (depth == numbers.length - 1) {
            if (curr == target) {
                return result + 1;
            }

            return result;
        }

        // 다음 노드 설정
        int[] neighbors = new int[]{curr + numbers[depth + 1], curr - numbers[depth + 1]};

        // 다음 노드 방문
        for (int neighbor : neighbors) {
            if (!visited[depth + 1][neighbor]) {
                visited[depth + 1][neighbor] = true;
                result = dfs(depth + 1, neighbor, visited, numbers, target, result);
            }
        }

        return result;
    }
}

















