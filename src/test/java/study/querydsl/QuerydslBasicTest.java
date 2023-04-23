package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;


@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void testEntity() {
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

        em.flush();
        em.clear();

        List<Member> members = em.createQuery("select m from Member m", Member.class)
                .getResultList();

        for (Member member : members) {
            System.out.println("member = " + member);
            System.out.println("member.team = " + member.getTeam());
        }
    }

    @Test
    public void startJPQL() {
        //member1을 찾자
        Member findMember = em.createQuery("select m from Member m where m.username =:username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        Member findMember = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory.selectFrom(member)
                                .where(member.username.eq("member1")
                                        .and(member.age.eq(10)))
                                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    /* and 는 , 로 대체가능하다*/
    public void searchAndParam() {
        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1"),
                        (member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        System.out.println(results.getResults());
        System.out.println(results.getTotal());

        Long total = queryFactory
                .selectFrom(member)
                .fetchCount();

        System.out.println(total);
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순
     * 2. 회원 이름 올림차순
     * 단, 2에서 회원 이름이 없으면 마지막에 출력 (if null -> last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

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
        assertThat(memberNull.getUsername()).isEqualTo(null);
    }

    @Test
    public void paging1() {

        List<Member> result = queryFactory
                    .selectFrom(member)
                    .orderBy(member.username.desc())
                    .offset(1) //스킵하는 데이터
                    .limit(5) // 사이즈
                    .fetch();

        assertThat(result.size()).isEqualTo(3);
    }

    @Test
    public void paging2() {

        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //스킵하는 데이터
                .limit(5) // 사이즈
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getLimit()).isEqualTo(5);
        assertThat(result.getResults().size()).isEqualTo(3);
    }

    /**
     * 실무에선 잘 안쓰이고 dto 로 뽑아서 쓴다.
     */
    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory.select(
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
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() {
        List<Tuple> result = queryFactory.select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원을 찾아라
     */
    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

    }

    /**
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
    }

    /**
     * 회원과 팀을 조인하면서 팀 이름이 teamA인 팀만 조인해라, 회원은 모두 조회해라.
     */
    @Test
    public void joinOnFiltering() {
        List<Tuple> result1 = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();


//        List<Tuple> result2 = queryFactory
//                .select(member, team)
//                .from(member)
//                .join(member.team, team)
//                .where(team.name.eq("teamA"))
//                .fetch();

        for (Tuple tuple : result1) {
            System.out.println("tuple = "+ tuple);
        }
    }

    /**
     * 회원의 이름이 팀 이름과 같은 대상 외부 조인
     */
    @Test
    public void joinNoRelation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory.select(member,team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = "+ tuple);
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

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치조인 미적용").isFalse();
    }

    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() {
        
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(select(memberSub.age.max())
                        .from(memberSub)))
                .fetch();
        
    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQueryGoe() {

        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.goe(select(memberSub.age.avg())
                        .from(memberSub)))
                .fetch();
    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQueryIn() {

        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.in(select(memberSub.age)
                        .from(memberSub)
                        .where(memberSub.age.gt(10))))
                .fetch();
    }
    
    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory.select(member.username,
                select(memberSub.age.avg())
                        .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    
    @Test
    public void basicCase() {
        List<String> result = queryFactory.select(member.age
                .when(10).then("열살")
                .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);;
        }
    }

    /**
     * DB 에선 Raw 데이터만.
     * DB 에서 불러오지말고 어플리케이션 단에서 해결하자.
     */
    @Test
    public void complexCase() {
        List<String> result = queryFactory.select(new CaseBuilder()
                .when(member.age.between(0,20)).then("0~20살")
                .when(member.age.between(21,30)).then("21~30살")
                .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() {
        List<Tuple> result = queryFactory.select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        
        for(Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    
    @Test
    public void concat() {
        //문자로 바꿔줘야함 {username}_{age}
       List<String> result =  queryFactory.select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
    
    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * tuple 과 다른 클래스와 의존한다면 나쁜 설계다.
     * dto 로 변환을 해야 좋은 설계
     */
    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory.select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    /**
     * JPQL 을 이용해서 DTO 조회
     */
    @Test
    public void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * QueryDsl 을 이용한 Dto 조회 - 프로퍼티(Setter)
     * Dto 에 Setter 생성해야함
     */
    @Test
    public void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * QueryDsl을 이용한 Dto 조회 - 필드
     */
    @Test
    public void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * QueryDsl을 이용한 Dto 조회 - 생성자
     */
    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 프로퍼티나 필드와 이름이 다를경우 쓸 수 있는 방법 1 .as()
     */
    @Test
    public void findUserDtoByField() {
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), // userDto 에는 username 이 아니라 name 이다.
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * 프로퍼티나 필드와 이름이 다를경우 쓸 수 있는 방법 2 ExpressionUtils.as()
     */
    @Test
    public void findUserDtoByFieldSub() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), // userDto 에는 username 이 아니라 name 이다.
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub),"age")))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * Dto 에 @QueryProjection 추가 후 QueryDsl 컴파일 (Dto 도 Q 파일로 빌드)
     * 컴파일 에러를 잡을 수 있다는 장점이 있다
     * 문제점
     * 1. Q 파일을 생성해야한다.
     * 2. Dto 가 QueryDsl 을 의존한다.
     */
    @Test
    public void findDtoByQueryProjections() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * 동적쿼리 해결방법 1. BooleanBuilder
     */
    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory.selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * 동적쿼리 해결방법 2. where 다중파라미터
     */
    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory.selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private Predicate usernameEq(String usernameCond) {
        if(usernameCond == null) return null;
        return member.username.eq(usernameCond);
    }

    private Predicate ageEq(Integer ageCond) {
        if (ageCond == null) return null;
        return member.age.eq(ageCond);
    }

    // 조립할 수 있다.
    // 가독성이 좋아진다는 장점.
    // null 처리는 따로 해야함.
    private List<Member> searchMember2AllEq(String usernameCond, Integer ageCond) {
        return queryFactory.selectFrom(member)
                .where(allEq(usernameCond,ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq2(String usernameCond) {
        if(usernameCond == null) return null;
        return member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq2(Integer ageCond) {
        if (ageCond == null) return null;
        return member.age.eq(ageCond);
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq2(usernameCond).and(ageEq2(ageCond));
    }

    /**
     * bulk 연산
     */
    @Test
    @Commit
    public void bulkUpdate() {

        //member1 = 10 -> 비회원
        //member2 = 20 -> 비회원
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
    }

    /**
     * 모든 회원 나이 +1
     */
    @Test
    public void bulkAdd() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    /**
     * 18살 이상 회원은 지우기
     */
    @Test
    public void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }
}
