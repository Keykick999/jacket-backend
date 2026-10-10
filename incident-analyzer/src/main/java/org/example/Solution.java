package org.example;

import java.util.LinkedList;
import java.util.Queue;

class Solution {
    private enum Direction {
        NORTH,
        SOUTH,
        WEST,
        EAST
    }

    private static class Node {
        int y;
        int x;
        int weight;
        Direction direction;

        public Node(int y, int x, int weight, Direction direction) {
            this.y = y;
            this.x = x;
            this.weight = weight;
            this.direction = direction;
        }
    }

    private static int maxY;
    private static int maxX;

    public int solution(int[][] board) {
        maxY = board.length - 1;
        maxX = board[0].length - 1;

        return bfs(board, 0, 0, maxY, maxX);
    }

    public static int bfs(int[][] board, int srcY, int srcX, int destY, int destX) {
        Queue<Node> q = new LinkedList<>();
        int[][][] weight = new int[maxY + 1][maxX + 1][4];
        int result = Integer.MAX_VALUE;

        for (int i = 0; i <= maxY; i++) {
            for (int j = 0; j <= maxX; j++) {
                for (int k = 0; k < 4; k++) {
                    weight[i][j][k] = Integer.MAX_VALUE;
                }
            }
        }
        q.add(new Node(srcY, srcX, 0, Direction.SOUTH));
        q.add(new Node(srcY, srcX, 0, Direction.EAST));
        weight[srcY][srcX][1] = 0;
        weight[srcY][srcX][2] = 0;

        while (!q.isEmpty()) {
            Node curr = q.poll();
            int currY = curr.y;
            int currX =  curr.x;
            int currWeight = curr.weight;
            Direction currDirection = curr.direction;

            System.out.println("currY: " + currY + " currX: " + currX + " currWeight: " + currWeight + " currDirection: " + currDirection);


            // 목적지 도달
            if (currY == destY && currX == destX) {
                result = Math.min(result, currWeight);
            }

            // 이웃 노드 방문
            int[] dy = new int[]{1, -1, 0, 0};
            int[] dx = new int[]{0, 0, 1, -1};

            for (int i = 0; i < 4; i++) {
                int nextY = currY + dy[i];
                int nextX = currX + dx[i];

                // 범위에서 벗어남
                if (nextY < 0 || nextY > maxY || nextX < 0 || nextX > maxX) {
                    continue;
                }

                // 방향 구하기
                Direction nextDirection = getDirection(currY, currX, nextY, nextX);
                int nextDirectionInt = directionToInteger(nextDirection);

                // 벽
                if (board[nextY][nextX] == 1) {
                    continue;
                }

                // 방향에 따라 weight 계산
                int nextWeight = currWeight;
                if (currDirection == nextDirection) {   // 방향이 같음(100원)
                    nextWeight += 100;

                    if (nextWeight < weight[nextY][nextX][nextDirectionInt]) {
                        weight[nextY][nextX][nextDirectionInt] = nextWeight;
                        q.add(new Node(nextY, nextX, nextWeight, nextDirection));
                    }
                }
                else {  // 방향이 다르면 500원, 위치는 고정
                    nextWeight += 500;

                    if (nextWeight < weight[currY][currX][nextDirectionInt]) {
                        weight[currY][currX][nextDirectionInt] = nextWeight;
                        q.add(new Node(currY, currX, nextWeight, nextDirection));
                    }
                }
            }
        }

        return result;
    }

    // 방향: 북(0) 동(1) 남(2) 서(3)
    public static Direction getDirection(int currY, int currX, int nextY, int nextX) {
        if (currX == nextX) {
            if (currY < nextY) {
                return Direction.SOUTH;
            }
            else {
                return Direction.NORTH;
            }
        }
        else if (currY == nextY) {
            if (currX < nextX) {
                return Direction.EAST;
            }
            else {
                return Direction.WEST;
            }
        }

        throw new IllegalStateException("잘못된 상태");
    }

    public static int directionToInteger(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> -1;
        };
    }
}

// (0, 0) -> N - 1로 가는 도로를 만드는 데 비용
// 0: 빈 칸, 1: 벽
// bfs
// visited: boolean[][][]   (y, x, 방향): 방향: 북(0) 동(1) 남(2) 서(3)
// queue: LinkedList<int[]> (y, x, 방향, weight)
// 비용: 직선 도로 - 100원, 코너 - 500원
// 현재 위치, 다음 위치 -> queue에 넣을 때 확인: 비용 계산
// visited는 같은 위치인데 방향이 다름 or 방향이 같은데 더 최소 비용이면 방문 ok
// 방향 이동: 이전 방향을 기억해서 직진을 하던가 90도로 좌우 회전을 하거나(뒤로 가는 건 비효율이니까)

// 이동 가능 방법: 직진/ 좌회전/ 우회전




//  => 같은 방향인데 더 최소 비용이 나중에 나올 수 있나?: 나중에 나왔다는 건
//     돌아서 왔다는 건데 안 ㄷ
