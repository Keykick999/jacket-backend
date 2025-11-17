package com.example.ThirdProject.term.repository;

import com.example.ThirdProject.term.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermRepository extends JpaRepository<Term, String> {

    // 프론트에서 약관 순서대로 표시하기 위해
    List<Term> findAllByOrderByDisplayOrderAsc();

    // 필수 약관만 조회 (회원가입 체크용)
    List<Term> findByIsRequiredTrueOrderByDisplayOrderAsc();

    // 선택 약관만 조회
    List<Term> findByIsRequiredFalseOrderByDisplayOrderAsc();

    // 제목으로 검색 (운영자 화면용)
    List<Term> findByTermTitleContaining(String keyword);

    List<Term> findByIsRequiredTrue();
}
