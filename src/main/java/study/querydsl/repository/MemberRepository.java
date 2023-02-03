package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import study.querydsl.entity.Member;

import java.util.List;

/**
 * 스프링 데이터 JPA 를 사용한 Repository
 */
public interface MemberRepository extends JpaRepository<Member,Long>,MemberRepositoryCustom, QuerydslPredicateExecutor<Member> {

    //select m from Member m where m.username =?
    List<Member> findByUsername(String username);
}
