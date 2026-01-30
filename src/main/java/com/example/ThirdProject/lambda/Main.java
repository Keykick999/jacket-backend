package com.example.ThirdProject.lambda;

import java.util.Arrays;
import java.util.List;

// 함수형 인터페이스를 람다로 구현
// 딱 메서드가 하나만 있어야 함. 그래서 따로 메서드명을 명시를 안 해줘도 문법적으로 오류가 없는 것임.
// Function이라는 함수형 인터페이스를 사용하는식으로 알고 있으면 됨
public class Main {
    public static void main(String[] args) {
        List<Integer> scores = Arrays.asList(45, 90, 65, 50, 85);

        List<String> highQulityScores = scores.stream()
                .filter(a -> a >= 80)
                .map(String::valueOf)
                .toList();

        System.out.println(highQulityScores);
    }
}








