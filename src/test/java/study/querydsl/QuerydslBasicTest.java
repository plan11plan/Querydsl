package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        //member1을 찾아라.
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }


    @Test
    public void startQuerydsl() {
        //member1을 찾아라.
        QMember m = new QMember("m");
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))//파라미터 바인딩 처리
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("> 참고: select , from 을 selectFrom 으로 합칠 수 있음")
    public void startQuerydsl2() {
        //member1을 찾아라.
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))//파라미터 바인딩 처리
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("검색 조건은 .and() , . or() 를 메서드 체인으로 연결할 수 있다.")
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.between(10, 30)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("AND 조건을 파라미터로 처리")
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"), // .and = ,
                        (member.age.eq(10)))
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /**
     * 단건 조회 = fetchOne();
     * 리스트 조회 =fetch();
     */
    @Test
    @DisplayName("fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환")
    public void fetch() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();
    }

    @Test
    @DisplayName("fetchOne() : 단 건 조회,결과가 없으면 : null")
    public void fetchOne() {
        Member fetchOne = queryFactory
                .selectFrom(QMember.member)
                .fetchOne();
    }

    @Test
    @DisplayName("처음 한 건 조회")
    public void fetchFirst() {
        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();
    }

    @Test
    @DisplayName("페이징 정보 포함, total count 쿼리 추가 실행")
    public void fetchResults() {
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();
        long totalCount = results.getTotal();
        List<Member> content = results.getResults();
        System.out.println("totalCount =" + totalCount);
    }

    @Test
    @DisplayName("count 쿼리로 변경해서 count 수 조회")
    public void fetchCount() {
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();
        System.out.println("count = " + count);
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(null last)
     */
    @Test
    @DisplayName("정렬 - desc,asc")
    public void sort() {
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        em.persist(new Member(null, 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    /**
     * 페이징
     */
    @Test
    @DisplayName("0~1까지 조회(2개)")
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(2)
                .fetch();
        assertThat(result.size()).isEqualTo(2);
        System.out.println("testResult = " + result);
    }

    @Test
    @DisplayName("count쿼리 한번,content 쿼리 한번")
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);

        System.out.println("total = " + queryResults.getTotal());
        System.out.println("limit = " + queryResults.getLimit());
        System.out.println("offset = " + queryResults.getOffset());
        System.out.println("size = " + queryResults.getResults().size());
    }

    /**
     * 집합
     */
    @Test
    public void aggregation() {
        //데이터를 조회하면 Tuple로 조회하게 되요.
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구하라.
     */
    @Test
    @DisplayName("팀의 이름과 각 팀의 평균 연령을 구하라.")
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team) //member.team과 team을 조인합니다.
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10+20)/2
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30+40)/2
    }

    /**
     * 기본 조인
     * 조인의 기본  문법은 첫 번째 파라미터에 조인 대상을 지정하고, 두 번 째 파라미터에
     * 별칭(alias)으로 사용할 Q 타입을 지정하면 된다.
     * <p>
     * ```java
     * join(조인 대상, 별칭으로 사용할 Q타입)
     * ```
     */
    @Test
    @DisplayName("팀 A에 소속된 모든 회원,기본 조인")
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타조인
     * 연관관계 없어도 조인
     */
    @Test
    @DisplayName("회원의 이름이 팀 이름과 같은 회원 조회")
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) //세타조인은 from절에 그냥  나열
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");


    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 "teamA"인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name="teamA"
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team) //join 하면 team 매치 안되는 멤버들은 빼고 가져옴
                .on(team.name.eq("teamA"))
                .fetch();
        //멤버는 다가져오는데 팀은, 팀A 하나만선택.
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 연관관계 없는 엔티티를 외부 조인
     * 주의! leftJoin() 부분에 일반 조인과 다르게 엔티티 하나만 들어간다.
     * 일반조인: leftJoin(member.team, team)
     * on조인: from(member).leftJoin(team).on(xxx)
     */
    @Test
    @DisplayName("회원의 이름이 팀 이름과 같은 대상 외부 조인")
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member) //세타조인은 from절에 그냥  나열
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        //이미 로딩이 됬는지= 초기화 됐는지 알려줌
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        //이미 로딩이 됬는지= 초기화 됐는지 알려줌
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isTrue();
    }

    /**
     * 서브쿼리
     */
    @Test
    @DisplayName("나이가 가장 많은 회원 조회")
    public void subQuery() {
        //앨리어스가 중복되면 안되는 경우에는 새로 만들어준다.
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age") //나이 필드를 꺼냄
                .containsExactly(40); // 정확히 40이니 ?
    }

    /**
     * 서브 쿼리
     */
    @Test
    @DisplayName("나이가 평균 이상인 회원")
    public void subQueryGoe() {
        //앨리어스가 중복되면 안되는 경우에는 새로 만들어준다.
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age") //나이 필드를 꺼냄
                .containsExactly(30, 40); // 정확히 30,40이니 ?
    }

    /**
     * 서브 쿼리
     * In 써보기
     */
    @Test
    @DisplayName("in쿼리 사용해보기. 10 초과만")
    public void subQueryIn() {
        //앨리어스가 중복되면 안되는 경우에는 새로 만들어준다.
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
        assertThat(result).extracting("age") //나이 필드를 꺼냄
                .containsExactly(20, 30, 40); // 정확히 20,30,40이니 ?
    }

    /**
     * select 절에서도 서브 쿼리 가능
     */
    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();
        for(Tuple tuple : result){
            System.out.println("tuple = "+ tuple);
        }
    }
    /**
     * Case 문 - select,조건절(where)에서 사용가능
     *
     */
    @Test
    @DisplayName("단순 Case문")
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("10살")
                        .when(20).then("20살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for(String s : result){
            System.out.println("결과 = "+ s);
        }

    }
    @Test
    @DisplayName("복잡 Case문-CaseBuilder")
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21살~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for(String s : result){
            System.out.println("결과 = "+s);
        }
    }

    /**
     * 상수,문자 더하기
     */
    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for(Tuple tuple : result){
            System.out.println("결과 = "+tuple);
        }
    }
    @Test
    @DisplayName("문자 더하기")
    public void concat(){
        //username_age
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        for(String s : result){
            System.out.println("결과 = " + s);
        }
    }

}
