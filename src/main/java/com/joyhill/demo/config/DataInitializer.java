package com.joyhill.demo.config;

import com.joyhill.demo.common.util.KoreanNameGenerator;
import com.joyhill.demo.common.util.PhoneUtils;
import com.joyhill.demo.domain.*;
import com.joyhill.demo.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

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
                               FamMemberRepository famMemberRepository,
                               TeamRoleRepository teamRoleRepository,
                               TeamRepository teamRepository,
                               PasswordEncoder passwordEncoder) {
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

            createUser(userRepository, passwordEncoder, "김민준", "010-1111-2222", "950315",
                    Role.leader, leaderFam.getName(), village.getName());
            createUser(userRepository, passwordEncoder, "박청년", "010-9999-0000", "001225",
                    Role.member, leaderFam.getName(), village.getName());
            createUser(userRepository, passwordEncoder, "홍성인", "010-3333-4444", "881020",
                    Role.village_leader, leaderFam.getName(), village.getName());
            createUser(userRepository, passwordEncoder, "정교역자", "010-5555-6666", "750601",
                    Role.pastor, null, null);
            User admin = createUser(userRepository, passwordEncoder, "관리자", "010-7777-8888", "700101",
                    Role.admin, null, null);

            FamMember leaderMember = new FamMember();
            leaderMember.setName("김민준");
            leaderMember.setFamName(leaderFam.getName());
            leaderMember.setPhone(PhoneUtils.normalize("010-1111-2222"));
            leaderMember.setRole(Role.leader);
            famMemberRepository.save(leaderMember);

            FamMember member = new FamMember();
            member.setName("박청년");
            member.setFamName(leaderFam.getName());
            member.setPhone(PhoneUtils.normalize("010-9999-0000"));
            member.setRole(Role.member);
            famMemberRepository.save(member);

            teamRoleRepository.save(new TeamRole(admin.getId(), "새가족팀", true));
        };
    }

    private User createUser(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            String name, String phone, String birth,
                            Role role, String famName, String villageName) {
        User user = new User();
        user.setName(name);
        user.setPhone(PhoneUtils.normalize(phone));
        user.setBirth(birth);
        user.setPassword(passwordEncoder.encode(birth));
        user.setRole(role);
        user.setFamName(famName);
        user.setVillageName(villageName);
        user.setPasswordChanged(false);
        return userRepository.save(user);
    }
}
