package com.example.ThirdProject.term.repository;
import com.example.ThirdProject.term.entity.UserTerm;
import com.example.ThirdProject.term.entity.UserTermId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTermRepository extends JpaRepository<UserTerm, UserTermId> {

    List<UserTerm> findByUserId(String userId);

    Optional<UserTerm> findByUserIdAndTermCode(String userId, String termCode);
}
