package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

/**
 * 스프링 데이터 JPA 를 사용한 Repository
 */
public interface MemberRepository extends JpaRepository<Member,Long>,MemberRepositoryCustom {

    //select m from Member m where m.username =?
    List<Member> findByUsername(String username);
}
