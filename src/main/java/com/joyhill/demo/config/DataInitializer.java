package com.joyhill.demo.config;

import com.joyhill.demo.common.util.KoreanNameGenerator;
import com.joyhill.demo.common.util.PhoneUtils;
import com.joyhill.demo.domain.*;
import com.joyhill.demo.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    private static final List<String> DEFAULT_TEAMS = List.of(
            "찬양팀", "예배팀", "함기팀", "함성팀", "새가족팀", "미디어사역팀", "LAB팀"
    );

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               VillageRepository villageRepository,
                               FamRepository famRepository,
                               TeamRoleRepository teamRoleRepository,
                               TeamRepository teamRepository) {
        return args -> {
            // ── 팀 초기화 (항상 실행 — 없는 팀만 추가) ──
            for (String teamName : DEFAULT_TEAMS) {
                if (!teamRepository.existsByTeamName(teamName)) {
                    teamRepository.save(new Team(teamName, ""));
                }
            }

            // ── 유저/조직 초기화 (최초 1회만) ──
            if (userRepository.count() > 0) return;

            Village village = villageRepository.save(
                    new Village(KoreanNameGenerator.villageName("홍성인"), "홍성인"));
            Fam leaderFam = famRepository.save(
                    new Fam(KoreanNameGenerator.famName("김민준"), village.getName(), "김민준"));

            // 모든 팸원은 users 테이블에 직접 등록
            // password = null, passwordChanged = false → 최초 로그인은 birth로 인증
            createUser(userRepository, "김민준", "010-1111-2222", "950315",
                    Role.leader, leaderFam.getName(), village.getName());
            createUser(userRepository, "박청년", "010-9999-0000", "001225",
                    Role.member, leaderFam.getName(), village.getName());
            createUser(userRepository, "홍성인", "010-3333-4444", "881020",
                    Role.village_leader, leaderFam.getName(), village.getName());
            createUser(userRepository, "정교역자", "010-5555-6666", "750601",
                    Role.pastor, null, null);
            User admin = createUser(userRepository, "관리자", "010-7777-8888", "700101",
                    Role.admin, null, null);

            teamRoleRepository.save(new TeamRole(admin.getId(), "새가족팀", true));
        };
    }

    private User createUser(UserRepository userRepository,
                            String name, String phone, String birth,
                            Role role, String famName, String villageName) {
        User user = new User();
        user.setName(name);
        user.setPhone(PhoneUtils.normalize(phone));
        user.setBirth(birth);
        user.setPassword(null);
        user.setRole(role);
        user.setFamName(famName);
        user.setVillageName(villageName);
        user.setPasswordChanged(false);
        return userRepository.save(user);
    }
}
